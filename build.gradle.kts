import com.gradle.publish.PublishTask
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("groovy")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "1.3.0"
    id("jacoco")
    id("pl.droidsonroids.jacoco.testkit") version "1.0.12"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "ca.cutterslade.gradle"
version = "1.11.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.maven.shared:maven-dependency-analyzer:1.14.1") {
        exclude(group = "org.apache.maven")
    }
    implementation("org.apache.commons:commons-collections4:4.4")

    testImplementation("org.spockframework:spock-core:2.3-groovy-3.0") {
        exclude(group = "org.codehaus.groovy")
    }
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    testImplementation("io.github.java-diff-utils:java-diff-utils:4.12")
    testImplementation("commons-io:commons-io:2.17.0")
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
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
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
    groovy {
        importOrder()
        removeSemicolons()
        excludeJava()
    }
    kotlinGradle {
        ktlint()
    }
}
