package com.nova.funkindevplugin

import com.google.gson.Gson
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

data class LibraryConfig(
  val libraries: Map<String, String>,
  val mergeInto: Map<String, String> = emptyMap()
)

object VSliceLibraryManager {
  private val LOG = logger<VSliceLibraryManager>()
  private val gson = Gson()

  const val CONFIG_FILE_NAME = "vslice-libraries.json"

  fun getGlobalCache(): File = File(System.getProperty("user.home"), ".vslice_libs_cache")

  fun loadConfigOrShowError(project: Project): LibraryConfig? {
    val config = loadConfig(project)
    if (config == null) {
      Messages.showErrorDialog(project, "Failed to load library configuration", "Error")
    }
    return config
  }

  fun loadConfig(project: Project): LibraryConfig? {
    val configFile = File(project.basePath, CONFIG_FILE_NAME)
    if (!configFile.exists()) return null

    return try {
      gson.fromJson(configFile.readText(), LibraryConfig::class.java)
    } catch (e: Exception) {
      LOG.error("Failed to parse $CONFIG_FILE_NAME", e)
      null
    }
  }

  fun downloadLibrary(name: String, url: String, outputDir: File): DownloadStatus {
    if (outputDir.exists()) {
      LOG.info("$name already exists at ${outputDir.absolutePath}")
      return DownloadStatus.ALREADY_EXISTS
    }

    return try {
      LOG.info("Downloading $name from $url")
      outputDir.mkdirs()

      val uri = URI(url)
      val connection = uri.toURL().openConnection()
      connection.connect()

      ZipInputStream(connection.getInputStream()).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
          if (!entry.isDirectory) {
            var fileName = entry.name.substringAfter("/")
            if (fileName.isNotEmpty() && (fileName.endsWith(".hx") || fileName.endsWith(".hxc"))) {
              // Force .hxc to .hx
              if (fileName.endsWith(".hxc")) {
                fileName = fileName.substringBeforeLast(".") + ".hx"
              }

              val file = File(outputDir, fileName)
              file.parentFile?.mkdirs()
              file.outputStream().use { output ->
                zip.copyTo(output)
              }
            }
          }
          entry = zip.nextEntry
        }
      }

      LOG.info("Successfully downloaded $name")
      DownloadStatus.DOWNLOADED
    } catch (e: Exception) {
      LOG.error("Failed to download $name", e)
      DownloadStatus.FAILED
    }
  }

  fun mergeLibraries(sourceLib: String, targetLib: String, globalCache: File): Boolean {
    val sourceDir = File(globalCache, sourceLib)
    val targetDir = File(globalCache, targetLib)

    if (!sourceDir.exists() || !targetDir.exists()) {
      LOG.warn("Cannot merge: source or target missing")
      return false
    }

    LOG.info("Merging $sourceLib into $targetLib")

    val sourceRoot = findHaxeSourceRoot(sourceDir)
    val targetRoot = findHaxeSourceRoot(targetDir)

    return try {
      var copiedCount = 0
      sourceRoot.walkTopDown()
        .filter { it.isFile && (it.extension == "hx" || it.extension == "hxc") }
        .forEach { sourceFile ->
          val relativePath = sourceFile.relativeTo(sourceRoot)
          val targetFile = File(targetRoot, relativePath.path)
          targetFile.parentFile?.mkdirs()
          sourceFile.copyTo(targetFile, overwrite = true)
          copiedCount++
        }

      LOG.info("Merged $copiedCount files from $sourceLib to $targetLib")
      true
    } catch (e: Exception) {
      LOG.error("Failed to merge $sourceLib into $targetLib", e)
      false
    }
  }

  fun findHaxeSourceRoot(libDir: File): File {
    val candidates = listOf(libDir.name, "source", "src", "Source", "Src", "flixel")

    for (candidate in candidates) {
      val potential = File(libDir, candidate)
      if (potential.exists() && potential.isDirectory) {
        if (potential.walkTopDown().maxDepth(3).any { it.extension == "hx" }) {
          return potential
        }
      }
    }

    return libDir
  }
}

enum class DownloadStatus {
  DOWNLOADED, ALREADY_EXISTS, FAILED
}