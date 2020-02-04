package ca.cutterslade.gradle.analyze

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
            out += "  ${clazz} ${clazz}\n"
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
