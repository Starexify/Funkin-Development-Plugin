package com.nova.funkindevplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.io.File

class VSliceBuildToolWindowFactory : ToolWindowFactory {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val buildWindow = VSliceBuildToolWindow(project)
    val content = ContentFactory.getInstance().createContent(buildWindow.getContent(), "", false)
    toolWindow.contentManager.addContent(content)
  }

  override fun shouldBeAvailable(project: Project): Boolean {
    val polymodMeta = File(project.basePath, "_polymod_meta.json")
    return polymodMeta.exists()
  }
}