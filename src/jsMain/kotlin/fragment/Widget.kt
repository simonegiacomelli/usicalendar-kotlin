package fragment

import nswf.Logger
import nswf.logger
import org.w3c.dom.*
import kotlin.browser.document
import kotlin.browser.window
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class HolderBase : ResourceWidget() {
    var notifyNewPushState = {}
    val widgets = mutableListOf<ResourceWidget>()
    abstract fun closeCurrent()
}

private val init = ResourceManager.apply {
    reg { WidgetManager() }.loadResource = false
    reg { WidgetManagerSingle() }.loadResource = false
}

open class WidgetManager : HolderBase() {

    val L by logger()

    override fun show(widget: ResourceWidget) {
        if (widgets.size > 0 && widgets.last() != widget)
            notifyNewPushState()

        widgets.remove(widget)
        widgets.add(widget)
        elementInstance.children.asList().forEach { it.remove() }
        widget.callBeforeShow()
        elementInstance.append(widget.elementInstance)
        widget.callAfterShow()
    }

    override fun close(widget: ResourceWidget) {
        if (widgets.size == 0) return
        if (widgets.last() != widget) {
            L.debug("${widget.instanceName} is not entitled to close()")
            return //throw Exception("$instanceName wtf!?");
        }

        widgets.removeAt(widgets.size - 1)
        if (widgets.size > 0) show(widgets.last())
    }

    override fun closeCurrent() {
        if (widgets.size == 1) return
        widgets.last().close()
    }

    fun remove(widget: KClass<out ResourceWidget>) {
        val matches = widgets.filter { it::class == widget }
        matches.forEach { widgets.remove(it) }
    }

}

class WidgetManagerSingle : HolderBase() {

    val L by logger()

    override fun show(widget: ResourceWidget) {
        notifyNewPushState()

        widgets.remove(widget)
        widgets.add(widget)
        widget.callBeforeShow()
        elementInstance.append(widget.elementInstance)
        widget.callAfterShow()
    }

    override fun close(widget: ResourceWidget) {
        fun ex(msg: Any?) = Exception("$instanceName wtf!? $msg ${widget.instanceName}")

        if (widgets.size == 0) throw ex("size=0");
        //if (widgets.last() != widget) throw ex("last is ${widgets.last().instanceName} !=");

        if (!widgets.remove(widget)) return //already removed

        val ele = widget.elementInstance
        if (elementInstance.children.asList()
                .filter { it == ele }.size != 1
        ) throw ex("element not found in holder children!");
        ele.remove()
    }

    override fun closeCurrent() {
        if (widgets.size == 0) return
        widgets.last().close()
    }

}


val Element.isWidgetTagName get() = this.tagName.toUpperCase().startsWith("W-")
fun tagName(kClass: KClass<out ResourceWidget>) = "W-${kClass.simpleName!!.toUpperCase()}"
val ResourceWidget.tagName get() = tagName(this::class)

val Element.hasWidgetInstance get() = this.asDynamic()["widget_instance"] != null
fun Element.widgetInstanceSet(value: ResourceWidget) {
    this.asDynamic()["widget_instance"] = value
}

val Element.widgetInstance: ResourceWidget
    get() {
        val dyn = this.asDynamic()
        if (hasWidgetInstance)
            return dyn["widget_instance"] as ResourceWidget
        val constructor = ResourceManager[tagName].widgetConstructor
            ?: throw Exception("No widget registered for $tagName")
        val instance = constructor()
        widgetInstanceSet(instance)
        instance.setElement(this as HTMLElement)
        return instance
    }

fun Element.ensureWidgetInstance() = if (isWidgetTagName) widgetInstance else Any()

object ResourceManager : ResourceManagerCls()
open class ResourceManagerCls {
    val resources by lazy { window.asDynamic()["widget_resources"] as Map<String, String> } //this comes from generated code

    inner class Options(val simpleName: String, val tagName: String, kClass: KClass<out Any>) {
        var loadResource = true
        val resourceContent by lazy { window.atob(resources.getOrElse("${simpleName}.html") { "" }) }
        var widgetConstructor: (() -> ResourceWidget)? = null
    }

    operator fun get(name: String) =
        simpleNames[name] ?: tags[name] ?: throw Exception("No widget can be found with name $name")

    operator fun <T : ResourceWidget> get(kclass: KClass<T>) = get(kclass.simpleName!!)

    fun contains(simpleName: String) = simpleNames.containsKey(simpleName) || tags.containsKey(simpleName)

    val L by logger()

    val simpleNames = mutableMapOf<String, Options>()
    val tags = mutableMapOf<String, Options>()


    inline fun <reified T : ResourceWidget> reg(noinline widgetConstructor: () -> T): Options {
        val opt = regClass { T::class }
        opt.widgetConstructor = widgetConstructor
        return opt
    }

    inline fun <reified T : ResourceWidget> reg(widgetClass: KClass<T>): Options {
        val opt = regClass { T::class }
        return opt
    }

    fun <T : ResourceWidget> regClass(widgetClass: () -> KClass<out T>): Options {
        val simpleName = widgetClass().simpleName!!
        val opt2 = simpleNames.get(simpleName)
        if (opt2 != null) return opt2
        val opt = Options(simpleName, tagName(widgetClass()), widgetClass())
        L.info("Registering ${opt.simpleName}")
        simpleNames[opt.simpleName] = opt
        tags[opt.tagName] = opt
        return opt
    }
    fun resourceFor(widget: ResourceWidget) = tags[tagName(widget::class)]?.resourceContent
        ?: throw Exception("No resource loaded for ${widget::class.simpleName}. Did you register the class?")
}

class Name(val instanceName: String) {

    val L by logger()

    fun instanceId(id: String): String {
        return instanceName + "_" + id
    }

    fun needRename(str: String) = str.isNotEmpty() && !str.startsWith(instanceName + "_")
    fun rename(original_id: String, element: Element) {
        if (!needRename(original_id)) return
        element.id = instanceId(original_id) //
        element.setAttribute("w-id", original_id)
    }

    override fun toString(): String {
        return instanceName
    }
}

object Instances {
    val tracking = mutableMapOf<String, Int>()
    fun next(name: String): Name {
        val counter = tracking.getOrPut(name) { 0 } + 1
        tracking[name] = counter
        return Name("$name${counter}")
    }

    fun next(name: KClass<out Any>) = next(name.simpleName!!)
}

abstract class ResourceWidget {

    inline fun logger(): Lazy<Logger> {
        return lazy { Logger(this.instanceName.toString()) }
    }

    //val children: List<ResourceWidget>        get() = elementInstance.children.asList().filter { it.isWidgetTagName }.map { it.widgetInstance }
    val children: List<ResourceWidget>
        get() = elementInstance.children.asList().toListRecursive(
            { it.isWidgetTagName }
            , { !it.isWidgetTagName }
            , { it.children.asList() })
            .map { it.widgetInstance }

    private val L by logger()

    protected var backingParent: ResourceWidget? = null
    val parent: ResourceWidget
        get() {
            if (backingParent == null) {
                findParent()
                if (backingParent == null)
                    throw Exception("$instanceName it was impossible to find a parent widget")
            }
            return backingParent!!
        }

    private fun findParent() {
        if (backingParent != null) return

        var cur = elementInstance.parentElement
        while (cur != null) {
            if (cur.isWidgetTagName)
                backingParent = cur.widgetInstance
            cur = cur.parentElement
        }
    }


    fun manage(w: ResourceWidget): ResourceWidget {
        w.backingParent = this
        return w
    }

    fun <T : ResourceWidget> create(instance: T): T {
        this.manage(instance)
        return instance
    }

    val instanceName by lazy { Instances.next(this::class) }
    open val resourceContent: String get() = ResourceManager.resourceFor(this)

    protected var backingElement: HTMLElement? = null

    val elementInstance: HTMLElement
        get() {
            if (backingElement == null) {
                val localName = tagName
                val ele = document.createElement(localName) as HTMLElement
                ele.widgetInstanceSet(this)
                setElement(ele)
            }
            return backingElement!!
        }

    val elementsMap: MutableMap<String, Element> = mutableMapOf()


    inner class Acquire(nodes: Collection<Element>) {

        val elementsToExpand = mutableListOf<Element>()

        init {
            acquire(nodes)
            elementsToExpand.forEach { it.widgetInstance }
        }

        fun acquire(nodes: Collection<Element>) {
            nodes.forEach { element ->
                element.acquire()
                if (!element.isWidgetTagName)
                    acquire(element.children.asList())
                else
                    elementsToExpand.add(element)
            }
        }

        private fun Element.acquire() {
            val originalId = if (id.isNotEmpty()) id else getAttribute("w-id").orEmpty()
            if (instanceName.needRename(originalId)) {
                elementsMap.put(originalId, this);
                instanceName.rename(originalId, this)
            }

        }


    }

    class Params(val children: List<Element>, val childNodes: List<Node>) {
        constructor(ele: HTMLElement) : this(ele.children.asList().toList(), ele.childNodes.asList().toList())
    }

    var renderDone = false
    lateinit var params: Params

    open fun setElement(element: HTMLElement) {
        backingElement = element
        params = Params(elementInstance)

        Acquire(params.children)  //if I would want to include params ids
        render()
        renderDone = true
        Acquire(elementInstance.children.asList()) //this comes after, so no name clash can injure the integrity of this widget
        afterRender()
    }

    open fun afterRender(){}


    open fun render() {
        elementInstance.innerHTML = resourceContent
    }

    val docu = Docu()
    val docuNull = DocuNull()


    inner class Docu {
        inline operator fun <reified T> getValue(thisRef: Any?, property: KProperty<*>): T {
            return docuNull.getValue<T>(thisRef, property)
                ?: throw Exception(
                    "id=$instanceName.${property.name} not found" +
                            "available names:[${elementsMap.keys.joinToString(",")}]"
                )
        }
    }

    inner class DocuNull {
        inline operator fun <reified T> getValue(thisRef: Any?, property: KProperty<*>): T? {

            elementInstance.widgetInstance //ensure expand

            val ele = elementsMap[property.name] ?: return null

            val simpleName = T::class.simpleName

            if (simpleName != null && ResourceManager.contains(simpleName)) {
                ele.ensureWidgetInstance()
                return ele.widgetInstance as T
            }

            try {
                return ele as T
            } catch (ex: Exception) {
                throw Exception("Exception for property $instanceName.${property.name}", ex)
            }
        }
    }


    open fun close(widget: ResourceWidget): Unit = throw Exception("nothing here")
    open fun show(widget: ResourceWidget): Unit = throw Exception("nothing here")
    fun show() {
        parent.show(this)
    }

    fun callBeforeShow() {
        beforeShow()
        children.forEach { it.callBeforeShow() }
    }

    open fun beforeShow(){}

    fun callAfterShow() {
        afterShow()
        children.forEach { it.callAfterShow() }
    }

    open fun afterShow(){}

    open fun close() = parent.close(this)
}

open class TemplateWidget(val template: HTMLTemplateElement) : ResourceWidget() {

    override fun render() {
        elementInstance.append(*template.clone().asList().toTypedArray())
    }

}

fun HTMLTemplateElement.newWidget() = TemplateWidget(this)

