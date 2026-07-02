package julay.compiler

import java.io.File
import java.nio.file.Path

fun resolveCompilerJar(): Path {
    val codeSource = Julayc::class.java.protectionDomain?.codeSource?.location
    if (codeSource != null) {
        val path = Path.of(codeSource.toURI())
        if (path.toString().endsWith(".jar", ignoreCase = true)) {
            return path.toAbsolutePath().normalize()
        }
    }

    System.getenv("JULAYC_JAR")?.let { envPath ->
        val path = Path.of(envPath)
        if (path.toFile().exists()) {
            return path.toAbsolutePath().normalize()
        }
    }

    File("julayc.jar").takeIf { it.exists() }?.let {
        return it.toPath().toAbsolutePath().normalize()
    }

    File("build/libs/julayc.jar").takeIf { it.exists() }?.let {
        return it.toPath().toAbsolutePath().normalize()
    }

    return Path.of("julayc.jar").toAbsolutePath().normalize()
}

fun Path.toGradlePathLiteral(): String =
    toAbsolutePath().normalize().toString().replace("\\", "/")
