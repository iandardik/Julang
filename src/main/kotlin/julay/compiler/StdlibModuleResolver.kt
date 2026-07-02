package julay.compiler

import java.net.URI
import java.net.URL
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createTempDirectory
import kotlin.io.path.isRegularFile

private const val STDLIB_RESOURCE_PREFIX = "stdlib/"

fun moduleFileName(modulePath: String): String = modulePath.replace('.', '/') + ".jul"

fun stdlibResourcePath(modulePath: String): String = STDLIB_RESOURCE_PREFIX + moduleFileName(modulePath)

fun resolveModuleSourcePath(modulePath: String, searchPath: List<Path>): Path? {
  for (base in searchPath) {
    val candidate = base.resolve(moduleFileName(modulePath))
    if (candidate.isRegularFile()) {
      return candidate
    }
  }
  return resolveClasspathStdlibPath(modulePath)
}

private fun resolveClasspathStdlibPath(modulePath: String): Path? {
  val resourcePath = stdlibResourcePath(modulePath)
  val classLoader = Thread.currentThread().contextClassLoader
    ?: StdlibModuleResolver::class.java.classLoader
  val url = classLoader.getResource(resourcePath) ?: return null
  return materializeResourcePath(url, resourcePath)
}

private fun materializeResourcePath(url: URL, resourcePath: String): Path {
  return when (url.protocol) {
    "file" -> Path.of(url.toURI())
    "jar" -> {
      val jarPath = jarFilePath(url)
      val tempRoot = createTempDirectory("julay-stdlib-")
      tempRoot.toFile().deleteOnExit()
      val out = tempRoot.resolve(resourcePath)
      Files.createDirectories(out.parent)
      openJarFileSystem(jarPath).use { fs ->
        val inJar = fs.getPath(resourcePath)
        Files.copy(inJar, out, StandardCopyOption.REPLACE_EXISTING)
      }
      out
    }
    else -> throw RuntimeException("Unsupported stdlib resource URL: $url")
  }
}

private fun jarFilePath(url: URL): Path {
  val uri = url.toURI()
  val spec = uri.schemeSpecificPart
  val jarUri = spec.substringBefore('!')
  return Path.of(URI(jarUri))
}

private fun openJarFileSystem(jarPath: Path): FileSystem {
  return try {
    FileSystems.newFileSystem(jarPath)
  } catch (_: java.nio.file.FileSystemAlreadyExistsException) {
    FileSystems.getFileSystem(URI.create("jar:${jarPath.toUri()}"))
  }
}

object StdlibModuleResolver
