plugins {
  id("com.github.rusio.gda.kotlin-library-conventions")
  id("ca.cutterslade.analyze")
}

repositories {
  mavenCentral()
}

dependencies {
  permitAggregatorUse("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  permitTestAggregatorUse("org.junit.jupiter:junit-jupiter:5.8.1")
}

version = "1.0.0-SNAPSHOT"
