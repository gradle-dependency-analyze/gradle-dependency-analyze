package ca.cutterslade.gradle.analyze.helper

class GroovyClass {
    String name
    boolean annotation
    Set<String> usedClasses = []
    Set<Tuple3<String, String, String>> usedClassConstants = []
    Set<Tuple4<String, String, String, Boolean>> classConstants = []
    Set<Tuple2<String, String>> classAnnotations = []

    GroovyClass(String name, annotation = false) {
        this.annotation = annotation
        this.name = name
    }

    def withClassAnnotation(String annotationName, String value) {
        classAnnotations.add(new Tuple2<String, String>(annotationName, value))
        this
    }

    def usesClass(String clazz) {
        usedClasses.add(clazz)
        this
    }

    def addClassConstant(String constantName, String constantType, String constantValue, boolean isFinal = true) {
        classConstants.add(new Tuple4<String, String, String, Boolean>(constantType, constantName, constantValue, isFinal))
        this
    }

    def usesClassConstant(String clazz, String constantName, String constantType) {
        usedClassConstants.add(new Tuple3(clazz, constantName, constantType))
        this
    }

    private def buildUsedClasses() {
        def out = ""
        for (String clazz : usedClasses) {
            def var = clazz.contains('.') ? clazz.substring(clazz.lastIndexOf('.') + 1) : clazz
            out += "  private ${clazz} _${var.toLowerCase()}\n"
        }
        out
    }

    private def buildUsedConstants() {
        def out = ""
        for (Tuple3<String, String, String> usedConst : usedClassConstants) {
            out += "  private ${usedConst.third} _${usedConst.second.toLowerCase()} = ${usedConst.first}.${usedConst.second}\n"
        }
        out
    }

    private def buildClassConstants() {
        def out = ""
        for (Tuple4<String, String, String, Boolean> classConst : classConstants) {
            out += "  public static ${classConst.fourth ? "final " : ""}${classConst.first} ${classConst.second} = ${classConst.third}\n"
        }
        out
    }

    private def buildClassAnnotation() {
        def out = ""
        for (Tuple2<String, String> annotation : classAnnotations) {
            out += "@${annotation.first}(${annotation.second})\n"
        }
        out
    }

    def create(File dir) {
        def out = ""
        if (annotation) {
            out += "import java.lang.annotation.ElementType\n"
            out += "import java.lang.annotation.Retention\n"
            out += "import java.lang.annotation.RetentionPolicy\n"
            out += "import java.lang.annotation.Target\n\n"
            out += "@Target(ElementType.TYPE)\n"
            out += "@Retention(RetentionPolicy.RUNTIME)\n"
            out += buildClassAnnotation()
            out += "public @interface ${name} {\n"
            out += "  String value()\n"
        } else {
            out += buildClassAnnotation()
            out += "class ${name} {\n"
            out += buildClassConstants()
            out += buildUsedClasses()
            out += buildUsedConstants()
        }
        out += "}"
        new File(dir, "${name}.groovy").text = out
    }
}
