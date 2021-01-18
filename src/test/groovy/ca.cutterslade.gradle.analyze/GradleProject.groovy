package ca.cutterslade.gradle.analyze

class GradleProject {

    final String name
    final boolean rootProject

    Set<GradleProject> subProjects = []
    Set<GroovyClass> mainClasses = []
    Set<GroovyClass> testClasses = []
    Set<String> plugins = ['groovy']
    Set<GradleDependency> dependencies = []
    String repositories;
    Set<String> aggregators = []

    GradleProject(String name, boolean rootProject = false) {
        this.name = name
        this.rootProject = rootProject
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

    def withAggregator(GradleDependency aggregator) {
        dependencies.add(aggregator)
        this
    }

    def withMavenRepositories() {
        repositories = "repositories {\n" +
                "    mavenLocal()\n" +
                "    mavenCentral()\n" +
                "}\n"
        this
    }

    void create(File root) {
        root.mkdirs()
        subProjects.each { it.create(new File(root, it.name)) }

        createBuildGradle(root)
        createSettingsGradle(root)

        createClasses(root, "src/main/groovy", mainClasses)
        createClasses(root, "src/test/groovy", testClasses)
    }

    private static void createClasses(File root, String dir, Set<GroovyClass> classes) {

        def sourceDir = new File(root, dir)
        if (!sourceDir.mkdirs()) {
            throw new IllegalStateException("Could not create source dir ${sourceDir}")
        }

        for (def clazz : classes) {
            clazz.create(sourceDir)
        }
    }

    private void createSettingsGradle(File root) {
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

    private void createBuildGradle(File root) {
        def buildGradle = ""
        if (!plugins.isEmpty()) {
            buildGradle += "plugins {\n"
            for (def plugin : plugins) {
                buildGradle += "  id '${plugin}'\n"
            }
            buildGradle += "}\n"
        }
        buildGradle += repositories ?: ''
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
