import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("org.openjfx.javafxplugin") version "0.0.8"
    idea
    application
}

javafx {
    version = "11.0.2"
    modules("javafx.controls", "javafx.graphics")
}

group = "com.github.sbroekhuis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("org.kordamp.ikonli:ikonli-fontawesome5-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.fxmisc.richtext:richtextfx:0.11.0")
    // https://mvnrepository.com/artifact/org.apache.commons/commons-math3
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.networknt:json-schema-validator:1.0.84")
    testImplementation(kotlin("test"))
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    test {
        useJUnitPlatform()

        systemProperty("java.util.logging.config.file", "${project.buildDir}/resources/test/logging-test.properties")

        testLogging {
            showStandardStreams = true
        }

    }
}

application {
    mainClass.set("app.MainKt")
}
