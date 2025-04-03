plugins {
  id("ca.cutterslade.analyze")
}

repositories {
  mavenCentral()
}

subprojects {
  group = "com.github.rusio.gda"
}

defaultTasks = mutableListOf("build")
