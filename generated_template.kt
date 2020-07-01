package generated

//this file will be overwritten

private val start = generated_entry_point()

@JsName("generated_entry_point")
fun generated_entry_point() {
    JsProjectProperties.buildDate = "{BUILDDATE}"
    println("Build date: ${JsProjectProperties.buildDate}")
}
