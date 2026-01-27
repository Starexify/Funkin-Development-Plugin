package com.nova.funkindevplugin.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.nova.funkindevplugin.services.VSliceProjectService
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode

class VSliceBuildToolWindow(private val project: Project) {
  private val mainPanel: JPanel = JPanel(BorderLayout())
  private val taskTree: Tree

  init {
    val actionGroup = DefaultActionGroup().apply {
      add(BuildAction())
      add(UpdateLibrariesAction())
      //addSeparator()
    }

    val toolbar = ActionManager.getInstance().createActionToolbar("VSliceBuildToolbar", actionGroup, true)
    toolbar.targetComponent = mainPanel
    mainPanel.add(toolbar.component, BorderLayout.NORTH)

    // Create tree with tasks
    val root = DefaultMutableTreeNode("VSlice Project")
    val buildTasks = DefaultMutableTreeNode("Build Tasks").apply {
      add(DefaultMutableTreeNode("Build Mod"))
    }

    val libraries = DefaultMutableTreeNode("Libraries").apply {
      add(DefaultMutableTreeNode("Update All"))
      add(DefaultMutableTreeNode("Remove Unused"))
    }

    root.add(buildTasks)
    root.add(libraries)

    taskTree = Tree(root).apply {
      isRootVisible = false

      // Add double-click handler
      addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          if (e.clickCount == 2) executeSelectedTask()
        }
      })
    }

    val scrollPane = JBScrollPane(taskTree)
    mainPanel.add(scrollPane, BorderLayout.CENTER)
  }

  private fun executeSelectedTask() {
    val service = project.service<VSliceProjectService>()
    val path = taskTree.selectionPath ?: return
    val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
    val taskName = node.userObject.toString()

    when (taskName) {
      "Update All" -> service.updateLibraries(true)
      "Remove Unused" -> service.removeUnusedLibraries()
      "Build Mod" -> service.buildMod()
      else -> println("Executing task: $taskName")
    }
  }

  fun getContent() = mainPanel

  private inner class BuildAction : AnAction("Build", "Build project", AllIcons.Actions.Compile) {
    override fun actionPerformed(e: AnActionEvent) {
      val service = project.service<VSliceProjectService>()
      service.buildMod()
    }
  }

  private inner class UpdateLibrariesAction : AnAction("Update Libs", "Update libraries", AllIcons.Actions.Download) {
    override fun actionPerformed(e: AnActionEvent) {
      val service = project.service<VSliceProjectService>()

      val result = Messages.showYesNoCancelDialog(
        project,
        "Choose how to update libraries:",
        "Download VSlice Libraries",
        "Clear and Update",
        "Download Missing Only",
        "Cancel",
        Messages.getQuestionIcon()
      )

      when (result) {
        Messages.YES -> service.updateLibraries(clearCache = true)
        Messages.NO -> service.updateLibraries(clearCache = false)
        Messages.CANCEL -> return // User cancelled
      }
    }
  }
}