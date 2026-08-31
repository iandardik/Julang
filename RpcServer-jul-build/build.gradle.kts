plugins {
    kotlin("jvm") version "2.1.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


repositories {
    mavenCentral()
}

dependencies {
    implementation(files("/Users/idardik/Documents/CMU/Julang/build/libs/julayc.jar"))
}

application {
    mainClass.set("RpcServerKt")
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        kotlin {
            srcDir(".")
            include("RpcServer.kt")
        }
    }
}

tasks.shadowJar {
    archiveBaseName.set("RpcServer")
    archiveVersion.set("")
    archiveClassifier.set("")
    destinationDirectory.set(file("$buildDir/../.."))
}