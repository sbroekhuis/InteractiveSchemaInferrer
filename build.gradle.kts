plugins {
    kotlin("jvm") version "1.8.0"
    idea
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

group = "com.github.sbroekhuis"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(19)
}

tasks.test {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.+")
//    implementation("com.github.saasquatch:json-schema-inferrer:0.2.0")
    implementation(fileTree("libs") { include("*.jar") })
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}
