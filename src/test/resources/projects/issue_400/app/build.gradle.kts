plugins {
    application
    id("ca.cutterslade.analyze")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.contrast)
    constraints {
        implementation(libs.gson)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

application {
    mainClass = "org.example.App"
}
