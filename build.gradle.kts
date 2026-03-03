import com.gradle.publish.PublishTask

plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "2.0.0"
    id("jacoco")
    id("pl.droidsonroids.jacoco.testkit") version "1.0.12"
    id("com.diffplug.spotless") version "8.2.1"
    id("com.adarshr.test-logger") version "4.0.0"
}

group = "ca.cutterslade.gradle"
version = "2.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.maven.shared:maven-dependency-analyzer:1.16.0") {
        exclude(group = "org.apache.maven")
    }
    implementation("org.apache.commons:commons-collections4:4.5.0")

    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<PublishTask>("publishPlugins") {
    dependsOn(tasks.check)
}

gradlePlugin {
    website = "https://github.com/gradle-dependency-analyze/gradle-dependency-analyze"
    vcsUrl = "https://github.com/gradle-dependency-analyze/gradle-dependency-analyze.git"
    plugins {
        create("dependencyAnalyze") {
            id = "ca.cutterslade.analyze"
            displayName = "Gradle Dependency Analyze"
            description = "Dependency analysis plugin for gradle. This plugin attempts to replicate the functionality" +
                " of the maven dependency plugin's analyze goals which fail the build if dependencies are declared" +
                " but not used or used but not declared."
            tags = listOf("dependency", "verification", "analyze")
            implementationClass = "ca.cutterslade.gradle.analyze.AnalyzeDependenciesPlugin"
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        html.required = true
        csv.required = true
    }
}

jacoco {
    toolVersion = "0.8.12"
}

spotless {
    java {
        googleJavaFormat()
    }
    kotlinGradle {
        ktlint()
    }
}
