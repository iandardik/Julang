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
    //implementation(files("../julayc.jar"))

    testImplementation("org.testng:testng:7.10.2")
    testImplementation("org.slf4j:slf4j-simple:1.6.1")
}

application {
    mainClass.set("julay.cli.MainKt")
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
    destinationDirectory.set(file("${layout.buildDirectory.get()}/../out/"))
}

tasks.test {
    workingDir = file("out/")
    useTestNG()
}
