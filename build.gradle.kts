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

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.microsoft.z3:java-jar:4.11.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")
    implementation("com.github.ajalt.clikt:clikt:5.0.3")

    testImplementation(kotlin("test"))
    testImplementation("org.yaml:snakeyaml:2.2")
}

application {
    mainClass.set("julay.compiler.MainKt")
}

kotlin {
    jvmToolchain(18)
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin/")
    }
}

tasks.shadowJar {
    archiveBaseName.set("julayc")
    archiveVersion.set("")
    archiveClassifier.set("")
}

tasks.test {
    dependsOn(tasks.shadowJar)
}
