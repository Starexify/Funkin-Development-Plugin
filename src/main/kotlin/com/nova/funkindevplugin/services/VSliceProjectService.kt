package com.nova.funkindevplugin.services

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.ide.actions.RevealFileAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.nova.funkindevplugin.DownloadStatus
import com.nova.funkindevplugin.VSliceLibraryManager
import com.nova.funkindevplugin.build.VSliceLibrarySetup
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.component1
import kotlin.collections.component2

@Service(Service.Level.PROJECT)
class VSliceProjectService(private val project: Project) {

  // Building Related
  fun buildMod() {
    val projectDir = File(project.basePath ?: return)
    val buildDir = File(projectDir, "build")

    // Try to use mod metadata for naming the zip
    val modMeta = parsePolymodMeta(projectDir)
    val modName = modMeta?.title?.replace(" ", "-") ?: project.name
    val fileName = "$modName-${modMeta?.mod_version}.zip"
    val zipFile = File(buildDir, fileName)

    ProgressManager.getInstance().run(object : Task.Backgroundable(
      project,
      "Building Mod $modName",
      true
    ) {
      private var fileCount = 0

      override fun run(indicator: ProgressIndicator) {
        indicator.text = "Preparing build directory..."

        if (!buildDir.exists()) buildDir.mkdirs()
        if (zipFile.exists()) zipFile.delete()

        indicator.fraction = 0.1
        indicator.text = "Reading ignore patterns..."

        // Load ignore patterns from .gitignore
        val ignorePatterns = loadIgnorePatterns(projectDir)

        indicator.fraction = 0.2
        indicator.text = "Scanning files..."

        // Get all files to zip (excluding ignored ones)
        val filesToZip = projectDir.walkTopDown()
          .filter { it.isFile }
          .filter { file -> !shouldIgnore(file, projectDir, ignorePatterns) }
          .toList()

        indicator.fraction = 0.3
        indicator.text = "Creating zip file..."

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
          filesToZip.forEachIndexed { index, file ->
            val relativePath = file.relativeTo(projectDir).path
            indicator.fraction = 0.3 + (0.7 * index / filesToZip.size)
            indicator.text = "Adding ${file.name}..."

            zip.putNextEntry(ZipEntry(relativePath))
            file.inputStream().use { input ->
              input.copyTo(zip)
            }
            zip.closeEntry()
            fileCount++
          }
        }

        indicator.fraction = 1.0
      }

      override fun onSuccess() {
        val buildVirtualDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(buildDir)
        buildVirtualDir?.refresh(false, true)

        NotificationGroupManager.getInstance()
          .getNotificationGroup("VSlice Notifications")
          .createNotification(
            "Build Successful",
            "Mod exported: $fileCount files packed into ${zipFile.name}",
            NotificationType.INFORMATION
          )
          .addAction(object : AnAction("Open Build Folder") {
            override fun actionPerformed(e: AnActionEvent) {
              RevealFileAction.openDirectory(buildDir.toPath())
            }
          })
          .notify(project)
      }

      override fun onThrowable(error: Throwable) {
        Messages.showErrorDialog(
          project,
          "Failed to build mod: ${error.message}",
          "Build Failed"
        )
      }
    })
  }

  fun parsePolymodMeta(projectDir: File): PolymodMeta? {
    val metaFile = File(projectDir, "_polymod_meta.json")
    if (!metaFile.exists()) return null

    return try {
      val gson = Gson()
      gson.fromJson(metaFile.readText(), PolymodMeta::class.java)
    } catch (e: JsonSyntaxException) {
      println("Failed to parse _polymod_meta.json: ${e.message}")
      null
    } catch (e: Exception) {
      println("Error reading _polymod_meta.json: ${e.message}")
      null
    }
  }

  fun loadIgnorePatterns(projectDir: File): List<String> {
    // Always include default patterns
    val defaultPatterns = listOf(
      ".git/",
      ".gitattributes",
      ".gitignore",
      ".idea/",
      "build/",
      "*.iml",
      ".DS_Store",
      "*.log",
      "vslice-libraries.json"
    )

    val gitignoreFile = File(projectDir, ".gitignore")
    if (!gitignoreFile.exists()) return defaultPatterns

    // Parse .gitignore and combine with defaults
    val gitignorePatterns = gitignoreFile.readLines()
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.startsWith("#") }

    return defaultPatterns + gitignorePatterns
  }

  fun shouldIgnore(file: File, projectDir: File, patterns: List<String>): Boolean {
    val relativePath = file.relativeTo(projectDir).path.replace(File.separator, "/")

    return patterns.any { pattern ->
      when {
        // Directory pattern
        pattern.endsWith("/") -> {
          val dirPattern = pattern.removeSuffix("/")
          relativePath.startsWith("$dirPattern/") || relativePath.contains("/$dirPattern/")
        }
        // Wildcard pattern
        pattern.contains("*") -> {
          val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .toRegex()
          regex.matches(relativePath) || regex.matches(file.name)
        }
        // Exact match
        else -> relativePath == pattern || file.name == pattern || relativePath.startsWith("$pattern/")
      }
    }
  }

  // Library Related
  fun updateLibraries(clearCache: Boolean) {
    val config = VSliceLibraryManager.loadConfigOrShowError(project) ?: return
    val globalCache = VSliceLibraryManager.getGlobalCache()

    ProgressManager.getInstance().run(object : Task.Backgroundable(
      project,
      "Downloading VSlice Libraries",
      true
    ) {
      private var downloadedCount = 0
      private var skippedCount = 0
      private var failedCount = 0

      override fun run(indicator: ProgressIndicator) {
        if (clearCache && globalCache.exists()) {
          indicator.text = "Clearing library cache..."
          globalCache.deleteRecursively()
        }

        globalCache.mkdirs()

        config.libraries.entries.forEachIndexed { index, (name, url) ->
          indicator.fraction = index.toDouble() / config.libraries.size
          indicator.text = "Processing $name..."

          val outputDir = File(globalCache, name)
          val status = VSliceLibraryManager.downloadLibrary(name, url, outputDir)

          when (status) {
            DownloadStatus.DOWNLOADED -> {
              downloadedCount++
              indicator.text = "Downloaded $name"
            }
            DownloadStatus.ALREADY_EXISTS -> {
              skippedCount++
              indicator.text = "$name already exists"
            }
            DownloadStatus.FAILED -> {
              failedCount++
              indicator.text = "Failed to download $name"
            }
          }
        }

        indicator.text = "Finalizing setup..."
        indicator.fraction = 1.0
      }

      override fun onSuccess() {
        val message = buildString {
          if (downloadedCount > 0) {
            append("Downloaded $downloadedCount new libraries. ")
          }
          if (skippedCount > 0) {
            append("Skipped $skippedCount existing. ")
          }
          if (failedCount > 0) {
            append("Failed $failedCount libraries. ")
          }
          if (downloadedCount == 0 && failedCount == 0) {
            append("All ${config.libraries.size} libraries are up to date.")
          }
        }.trim()

        val notificationType = if (failedCount > 0) NotificationType.WARNING else NotificationType.INFORMATION

        NotificationGroupManager.getInstance()
          .getNotificationGroup("VSlice Notifications")
          .createNotification(
            "VSlice Libraries",
            message,
            notificationType
          )
          .notify(project)

        if (downloadedCount > 0 || clearCache) {
          ApplicationManager.getApplication().invokeLater {
            VSliceLibrarySetup().runSetup(project)
          }
        }
      }

      override fun onThrowable(error: Throwable) {
        Messages.showErrorDialog(project, "Failed to download libraries: ${error.message}", "Error")
      }
    })
  }

  fun removeUnusedLibraries() {
    val config = VSliceLibraryManager.loadConfigOrShowError(project) ?: return
    val globalCache = VSliceLibraryManager.getGlobalCache()

    if (!globalCache.exists()) {
      NotificationGroupManager.getInstance()
        .getNotificationGroup("VSlice Notifications")
        .createNotification(
          "No Libraries Found",
          "Library cache does not exist.",
          NotificationType.INFORMATION
        )
        .notify(project)
      return
    }

    // Get all library folders in cache
    val cachedLibraries = globalCache.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()

    // Get libraries that should exist (from config)
    val configuredLibraries = config.libraries.keys.toSet()

    // Find unused libraries (in cache but not in config)
    val unusedLibraries = cachedLibraries.filter { it !in configuredLibraries }

    if (unusedLibraries.isEmpty()) {
      NotificationGroupManager.getInstance()
        .getNotificationGroup("VSlice Notifications")
        .createNotification(
          "No Unused Libraries",
          "All cached libraries are currently in use.",
          NotificationType.INFORMATION
        )
        .notify(project)
      return
    }

    // Ask user for confirmation
    val message = buildString {
      append("The following libraries are not in your vslice-libraries.json and will be deleted:\n\n")
      unusedLibraries.forEach { append("• $it\n") }
      append("\nThis will free up disk space. Continue?")
    }

    val result = Messages.showYesNoDialog(
      project,
      message,
      "Remove Unused Libraries",
      "Remove ${unusedLibraries.size} Libraries",
      "Cancel",
      Messages.getWarningIcon()
    )

    if (result != Messages.YES) return

    ProgressManager.getInstance().run(object : Task.Backgroundable(
      project,
      "Removing Unused Libraries",
      true
    ) {
      private var removedCount = 0
      private var failedCount = 0

      override fun run(indicator: ProgressIndicator) {
        unusedLibraries.forEachIndexed { index, libName ->
          indicator.fraction = index.toDouble() / unusedLibraries.size
          indicator.text = "Removing $libName..."

          val libDir = File(globalCache, libName)
          try {
            if (libDir.exists()) {
              libDir.deleteRecursively()
              removedCount++
            }
          } catch (e: Exception) {
            failedCount++
            println("Failed to delete $libName: ${e.message}")
          }
        }

        indicator.fraction = 1.0
      }

      override fun onSuccess() {
        val message = buildString {
          if (removedCount > 0) {
            append("Successfully removed $removedCount unused libraries.")
          }
          if (failedCount > 0) {
            append(" Failed to remove $failedCount libraries.")
          }
        }

        val notificationType = if (failedCount > 0) NotificationType.WARNING else NotificationType.INFORMATION

        NotificationGroupManager.getInstance()
          .getNotificationGroup("VSlice Notifications")
          .createNotification(
            "Libraries Removed",
            message,
            notificationType
          )
          .notify(project)
      }

      override fun onThrowable(error: Throwable) {
        Messages.showErrorDialog(project, "Failed to remove libraries: ${error.message}", "Error")
      }
    })
  }
}

data class PolymodMeta(
  val title: String? = null,
  val api_version: String? = null,
  val mod_version: String? = null,
)