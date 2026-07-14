import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://artifacts.itemis.cloud/repository/maven-mps/")
    }
    mavenLocal()
}

val cvc5Version = "1.3.4"

fun cvc5OsClassifier(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val osPart = when {
        os.contains("mac") || os.contains("darwin") -> "osx"
        os.contains("linux") -> "linux"
        os.contains("windows") -> "windows"
        else -> error("Unsupported OS for cvc5 JNI: $os")
    }
    val archPart = when {
        arch == "aarch64" || arch == "arm64" -> "aarch_64"
        arch == "amd64" || arch == "x86_64" -> "x86_64"
        else -> error("Unsupported arch for cvc5 JNI: $arch")
    }
    return "${osPart}-${archPart}"
}

val cvc5Native: Configuration by configurations.creating

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("io.github.cvc5:cvc5:$cvc5Version")
    cvc5Native("io.github.cvc5:cvc5:$cvc5Version:${cvc5OsClassifier()}")
    implementation("org.antlr:antlr4-runtime:4.13.2")
    implementation("com.github.ajalt.clikt:clikt:5.0.3")

    testImplementation(kotlin("test"))
    testImplementation("org.yaml:snakeyaml:2.2")
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin/")
        runtimeClasspath += cvc5Native
    }
    test {
        runtimeClasspath += cvc5Native
    }
}

application {
    mainClass.set("julay.compiler.MainKt")
}

kotlin {
    jvmToolchain(18)
}

tasks.shadowJar {
    archiveBaseName.set("julayc")
    archiveVersion.set("")
    archiveClassifier.set("")
    from(cvc5Native.map { if (it.isDirectory) it else zipTree(it) })
}

tasks.named<JavaExec>("run") {
    classpath += cvc5Native
}

tasks.test {
    dependsOn(tasks.shadowJar)
    classpath += cvc5Native
}
