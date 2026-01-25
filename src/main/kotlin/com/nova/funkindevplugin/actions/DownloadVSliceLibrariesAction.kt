package com.nova.funkindevplugin.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.nova.funkindevplugin.VSliceLibraryManager
import com.nova.funkindevplugin.build.VSliceLibrarySetup
import java.io.File

class DownloadVSliceLibrariesAction : AnAction("Download/Update VSlice Libraries") {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    // Ask user if they want to clear cache (for updates)
    val clearCache = Messages.showYesNoDialog(
      project,
      "Clear existing library cache before downloading?",
      "Download VSlice Libraries",
      "Clear and Update",
      "Download Missing Only",
      Messages.getQuestionIcon()
    ) == Messages.YES

    val config = VSliceLibraryManager.loadConfig(project)
    if (config == null) {
      Messages.showErrorDialog(project, "Failed to load library configuration", "Error")
      return
    }

    val globalCache = File(System.getProperty("user.home"), ".vslice_libs_cache")

    ProgressManager.getInstance().run(object : Task.Backgroundable(
      project,
      "Downloading VSlice Libraries",
      true
    ) {
      override fun run(indicator: ProgressIndicator) {
        if (clearCache && globalCache.exists()) {
          indicator.text = "Clearing library cache..."
          globalCache.deleteRecursively()
        }

        globalCache.mkdirs()

        // Download libraries
        config.libraries.entries.forEachIndexed { index, (name, url) ->
          indicator.fraction = index.toDouble() / config.libraries.size
          indicator.text = "Downloading $name..."

          val outputDir = File(globalCache, name)
          VSliceLibraryManager.downloadLibrary(name, url, outputDir)
        }

        // Merge libraries
        indicator.text = "Merging libraries..."
        config.mergeInto.forEach { (source, target) ->
          VSliceLibraryManager.mergeLibraries(source, target, globalCache)
        }
      }

      override fun onSuccess() {
        NotificationGroupManager.getInstance()
          .getNotificationGroup("VSlice Notifications")
          .createNotification(
            "VSlice Libraries",
            "Successfully downloaded ${config.libraries.size} libraries. Restart IntelliJ to apply changes.",
            NotificationType.INFORMATION
          )
          .notify(project)

        // Trigger library setup
        ApplicationManager.getApplication().invokeLater {
          VSliceLibrarySetup().runSetup(project)
        }
      }

      override fun onThrowable(error: Throwable) {
        Messages.showErrorDialog(project, "Failed to download libraries: ${error.message}", "Error")
      }
    })
  }
}