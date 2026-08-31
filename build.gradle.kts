import java.net.URI

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

val tla2toolsVersion = "v1.8.0"
val tla2toolsJar = layout.buildDirectory.file("tla2tools/tla2tools.jar")

val downloadTla2tools by tasks.registering {
    description = "Download pinned tla2tools.jar for TLC smoke tests"
    val dest = tla2toolsJar
    outputs.file(dest)
    onlyIf {
        !dest.get().asFile.exists() || dest.get().asFile.length() == 0L
    }
    doLast {
        val out = dest.get().asFile
        out.parentFile.mkdirs()
        val url = URI(
            "https://github.com/tlaplus/tlaplus/releases/download/$tla2toolsVersion/tla2tools.jar",
        ).toURL()
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                url.openStream().use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                return@doLast
            } catch (e: Exception) {
                lastError = e
                out.delete()
                Thread.sleep(1_000L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Failed to download tla2tools.jar")
    }
}

tasks.test {
    dependsOn(tasks.shadowJar)
    dependsOn(downloadTla2tools)
    systemProperty("tla2tools.jar", tla2toolsJar.get().asFile.absolutePath)
}

tasks.register<JavaExec>("rendezvousMicrobench") {
    group = "benchmark"
    description = "No-HTTP SyncChannel/Select rendezvous microbench for async-profiler"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("julay.bench.RendezvousMicrobenchKt")
}

tasks.register("writeRuntimeClasspath") {
    val outFile = layout.buildDirectory.file("runtime-classpath.txt")
    outputs.file(outFile)
    doLast {
        outFile.get().asFile.writeText(sourceSets.main.get().runtimeClasspath.asPath)
    }
}
