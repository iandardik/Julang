plugins {
    kotlin("jvm") version "2.1.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(18)
}

application {
    mainClass.set("RpcServerNativeKt")
}

tasks.shadowJar {
    archiveBaseName.set("RpcServerNative")
    archiveVersion.set("")
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "RpcServerNativeKt"
    }
}
