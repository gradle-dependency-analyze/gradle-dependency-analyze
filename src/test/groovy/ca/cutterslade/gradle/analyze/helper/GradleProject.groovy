package ca.cutterslade.gradle.analyze.helper

class GradleProject {

    final String name
    final boolean rootProject

    boolean justWarn = false
    boolean warnCompileOnly = false
    boolean warnUsedUndeclared = false
    boolean warnUnusedDeclared = false
    boolean logDependencyInformationToFiles = false
    Set<GradleProject> subProjects = []
    Set<GroovyClass> mainClasses = []
    Set<GroovyClass> testClasses = []
    Set<GroovyClass> testFixturesClasses = []
    Set<String> plugins = []
    Set<String> allProjectPlugins = []
    Set<GradleDependency> dependencies = []
    String repositories
    String platformConfiguration = ""
    Map<String, String> additionalTasks = new LinkedHashMap<>()

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

    def withTestFixturesClass(GroovyClass clazz) {
        testFixturesClasses.add(clazz)
        this
    }

    def withPlugin(String plugin) {
        plugins.add(plugin)
        this
    }

    def withAllProjectsPlugin(String plugin) {
        allProjectPlugins.add(plugin)
        this
    }

    def justWarn() {
        justWarn = true
        this
    }

    def withWarnCompileOnly(boolean value) {
        warnCompileOnly = value
        this
    }

    def withWarnUsedUndeclared(boolean value) {
        warnUsedUndeclared = value
        this
    }

    def withWarnUnusedDeclared(boolean value) {
        warnUnusedDeclared = value
        this
    }

    def logDependencyInformationToFiles() {
        logDependencyInformationToFiles = true
        this
    }

    def withDependency(GradleDependency dep) {
        dependencies.add(dep)
        this
    }

    def withGradleDependency(String configuration) {
        dependencies.add(new GradleDependency(configuration: configuration, reference: 'localGroovy()'))
        this
    }

    def withAggregator(GradleDependency aggregator) {
        dependencies.add(aggregator)
        this
    }

    def withAdditionalTask(String taskName, String buildGradleSnippet) {
        additionalTasks.put(taskName, buildGradleSnippet)
        this
    }

    def withMavenRepositories() {
        repositories = "repositories {\n" +
                "    mavenLocal()\n" +
                "    mavenCentral()\n" +
                "}\n"
        this
    }

    def applyPlatformConfiguration() {
        platformConfiguration = "" +
                "configurations {\n" +
                "    myPlatform {\n" +
                "        canBeResolved = false\n" +
                "        canBeConsumed = false\n" +
                "    }\n" +
                "}\n" +
                "configurations.all {\n" +
                "    if (canBeResolved) {\n" +
                "        extendsFrom(configurations.myPlatform)\n" +
                "    }\n" +
                "}\n" +
                "dependencies {\n" +
                "    myPlatform platform(project(':platform'))\n" +
                "}\n"
        this
    }

    void create(File root) {
        additionalTasks.putIfAbsent("build", "")

        root.mkdirs()
        subProjects.each { it.create(new File(root, it.name)) }

        createBuildGradle(root)
        createSettingsGradle(root)

        if (!mainClasses.empty) {
            createClasses(root, "src/main/groovy", mainClasses)
        }
        if (!testClasses.empty) {
            createClasses(root, "src/test/groovy", testClasses)
        }
        if (!testFixturesClasses.empty) {
            createClasses(root, "src/testFixtures/groovy", testFixturesClasses)
        }
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

    private void createBuildGradle(final File root) {
        def buildGradle = ""
        if (!plugins.isEmpty()) {
            buildGradle += "plugins {\n"
            plugins.forEach {
                buildGradle += "  id '${it}'\n"
            }
            buildGradle += "}\n"
        }
        if (plugins.contains('java-platform')) {
            buildGradle += "javaPlatform {\n" +
                    "    allowDependencies()\n" +
                    "}\n"
        }
        buildGradle += repositories ?: ''
        if (!dependencies.isEmpty()) {
            buildGradle += "dependencies {\n"
            for (def dep : dependencies) {
                buildGradle += "  ${dep.get()}\n"
            }
            buildGradle += "}\n"
        }

        buildGradle += "\ndefaultTasks " + additionalTasks.keySet().collect { "'${it}'" }.join(", ") + "\n"

        buildGradle += platformConfiguration

        if (rootProject && !allProjectPlugins.empty) {
            buildGradle += "allprojects {\n"
            for (def plugin : allProjectPlugins) {
                buildGradle += "  apply plugin: '${plugin}'\n"
            }
            buildGradle += "}\n"
        }

        if (justWarn || warnUsedUndeclared || warnUnusedDeclared || logDependencyInformationToFiles || warnCompileOnly) {
            buildGradle += "analyzeClassesDependencies {\n" +
                    (justWarn ? "  justWarn = ${justWarn}\n" : "") +
                    (warnCompileOnly ? "  warnCompileOnly = ${warnCompileOnly}\n" : "") +
                    (warnUsedUndeclared ? "  warnUsedUndeclared = ${warnUsedUndeclared}\n" : "") +
                    (warnUnusedDeclared ? "  warnUnusedDeclared = ${warnUnusedDeclared}\n" : "") +
                    (logDependencyInformationToFiles ? "  logDependencyInformationToFiles = ${logDependencyInformationToFiles}\n" : "") +
                    "}\n"
        }

        additionalTasks.values().forEach(it -> buildGradle += "${it}\n")

        new File(root, "build.gradle").text = buildGradle
    }
}
