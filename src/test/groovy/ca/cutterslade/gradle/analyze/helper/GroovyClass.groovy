package ca.cutterslade.gradle.analyze.helper

class GroovyClass {
    String name
    Set<String> usedClasses = []

    GroovyClass(String name) {
        this.name = name
    }

    def usesClass(String clazz) {
        usedClasses.add(clazz)
        this
    }

    private def buildUsedClasses() {
        def out = ""
        for (String clazz : usedClasses) {
            def var = clazz.contains('.') ? clazz.substring(clazz.lastIndexOf('.') + 1) : clazz
            out += "  ${clazz} _${var.toLowerCase()}\n"
        }
        out
    }

    def create(File dir) {
        def out = "class ${name} {\n"
        out += buildUsedClasses()
        out += "}"
        new File(dir, "${name}.groovy").text = out
    }
}
