package fragment

import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.window

object HotkeyWindow {
    val hk by lazy { Hotkey(window) }
    fun add(keys: String, callback: () -> Unit) {
        hk.add(keys, callback)
    }
}

class Hotkey(element: EventTarget) {
    val handlers: MutableMap<String, () -> Unit> = mutableMapOf()

    init {
        element.addEventListener("keydown", { event: Event ->
            _detectHotKey(event)

        }, false)
    }

    private fun _detectHotKey(ke: Event) {
        if (ke !is KeyboardEvent) return

        val keh = KeyboardEventHelper(ke)
        if (!keh.allowed) return

        if (!handlers.containsKey(keh.key)) return
        val res = handlers[keh.key]!!
        res()
        ke.preventDefault();
        ke.stopPropagation();

    }

    fun add(keys: String, callback: () -> Unit): Hotkey {
        handlers[keys] = callback;
        return this
    }


}

class KeyboardEventHelper(val ke: KeyboardEvent) {

    val allowed: Boolean
        get() = ALLOWED_KEY_IDENTIFIERS.containsKey(ke.keyCode)
    val key: String by lazy {
        var res = ""
        if (ke.ctrlKey) res += "CTRL-"
        if (ke.shiftKey) res += "SHIFT-"
        if (ke.altKey) res += "ALT-"
        res += ALLOWED_KEY_IDENTIFIERS[ke.keyCode]
        res
    }

}

val ALLOWED_KEY_IDENTIFIERS: Map<Int, String> = mapOf(

    8 to "BACKSPACE",
    9 to "TAB",
    13 to "ENTER",
    19 to "PAUSE",
    20 to "CAPS_LOCK",
    27 to "ESC",
    32 to "SPACE",
    33 to "PAGE_UP",
    34 to "PAGE_DOWN",
    35 to "END",
    36 to "HOME",
    37 to "LEFT",
    38 to "UP",
    39 to "RIGHT",
    40 to "DOWN",
    45 to "INSERT",
    46 to "DELETE",
    48 to "0",
    49 to "1",
    50 to "2",
    51 to "3",
    52 to "4",
    53 to "5",
    54 to "6",
    55 to "7",
    56 to "8",
    57 to "9",
    65 to "A",
    66 to "B",
    67 to "C",
    68 to "D",
    69 to "E",
    70 to "F",
    71 to "G",
    72 to "H",
    73 to "I",
    74 to "J",
    75 to "K",
    76 to "L",
    77 to "M",
    78 to "N",
    79 to "O",
    80 to "P",
    81 to "Q",
    82 to "R",
    83 to "S",
    84 to "T",
    85 to "U",
    86 to "V",
    87 to "W",
    88 to "X",
    89 to "Y",
    90 to "Z",
    91 to "LWIN",
    92 to "RWIN",
    112 to "F1",
    113 to "F2",
    114 to "F3",
    115 to "F4",
    116 to "F5",
    117 to "F6",
    118 to "F7",
    119 to "F8",
    120 to "F9",
    121 to "F10",
    122 to "F11",
    123 to "F12",
    144 to "NUM_LOCK",
    145 to "SCROL_LLOCK",
    186 to ";",
    187 to "=",
    188 to ",",
    189 to "-",
    190 to ".",
    191 to "/",
    192 to "`",
    219 to "[",
    220 to "\\",
    221 to "]",
    222 to "\""

)
