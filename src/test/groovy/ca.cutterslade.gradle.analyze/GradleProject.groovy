package ca.cutterslade.gradle.analyze

class GradleProject {

    final File root
    final String name

    Set<GradleProject> subProjects = []
    Set<GroovyClass> mainClasses = []
    Set<GroovyClass> testClasses = []
    Set<String> plugins = ['groovy']
    Set<GradleDependency> dependencies = []

    GradleProject(File root, String name) {
        this.root = root
        this.name = name
    }

    def withSubProject(GradleProject project) {
        subProjects.add(project)
        this
    }

    def withMainClass(GroovyClass clazz) {
        mainClasses.add(clazz)
        this
    }

    def withTestClass(GroovyClass clazz) {
        testClasses.add(clazz)
        this
    }

    def withPlugin(String plugin) {
        plugins.add(plugin)
        this
    }

    def withDependency(GradleDependency dep) {
        dependencies.add(dep)
        this
    }

    void create() {
        root.mkdirs()
        subProjects.each { it.create() }

        createBuildGradle()
        createSettingsGradle()

        createClasses("src/main/groovy", mainClasses)
        createClasses("src/test/groovy", testClasses)
    }

    private void createClasses(String dir, Set<GroovyClass> classes) {

        def sourceDir = new File(root, dir)
        if (!sourceDir.mkdirs()) {
            throw new IllegalStateException("Could not create source dir ${sourceDir}")
        }

        for (def clazz : classes) {
            clazz.create(sourceDir)
        }
    }

    private void createSettingsGradle() {
        def settingsGradle = ""
        if (name != null) {
            settingsGradle += "rootProject.name = '${name}'\n"
        }

        for (def subProject : subProjects) {
            settingsGradle += "include(':${subProject.name}')\n"
        }

        if (!settingsGradle.isEmpty()) {
            new File(root, "settings.gradle").text = settingsGradle
        }
    }

    private void createBuildGradle() {
        def buildGradle = ""
        if (!plugins.isEmpty()) {
            buildGradle += "plugins {\n"
            for (def plugin : plugins) {
                buildGradle += "  id '${plugin}'\n"
            }
            buildGradle += "}\n"
        }

        if (!dependencies.isEmpty()) {
            buildGradle += "dependencies {\n"
            for (def dep : dependencies) {
                buildGradle += "  ${dep.get()}\n"
            }
            buildGradle += "}\n"
        }

        new File(root, "build.gradle").text = buildGradle
    }
}
