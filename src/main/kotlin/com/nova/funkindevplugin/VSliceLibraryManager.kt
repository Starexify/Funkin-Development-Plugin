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
)

object VSliceLibraryManager {
  private val LOG = logger<VSliceLibraryManager>()
  private val gson = Gson()

  const val CONFIG_FILE_NAME = "vslice-libraries.json"

  fun getGlobalCache(): File = File(System.getProperty("user.home"), ".vslice_libs_cache/sources")

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
      //LOG.info("Downloading $name from $url")
      outputDir.mkdirs()
      val connection = URI(url).toURL().openConnection()

      ZipInputStream(connection.getInputStream()).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
          if (!entry.isDirectory) {
            val pathAfterRoot = entry.name.substringAfter("/")
            val finalPath = stripSourceFolders(pathAfterRoot)

            if (finalPath.isNotEmpty() && (finalPath.endsWith(".hx") || finalPath.endsWith(".hxc"))) {
              val file = File(outputDir, finalPath.replace(".hxc", ".hx"))
              file.parentFile?.mkdirs()
              file.outputStream().use { zip.copyTo(it) }
            }
          }
          zip.closeEntry()
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

  private fun stripSourceFolders(path: String): String {
    val sourceDirs = listOf("source/", "src/", "Source/", "Src/")
    for (dir in sourceDirs) {
      if (path.startsWith(dir, ignoreCase = true)) {
        return path.substring(dir.length)
      }
    }
    return path
  }
}

enum class DownloadStatus {
  DOWNLOADED, ALREADY_EXISTS, FAILED
}