package com.nova.funkindevplugin.projects

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.Panel

class VSliceNewProjectWizardStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
  private val haxeSdkType = HaxeSdkType.getInstance()
  private val haxeSdks: List<Sdk> = ProjectJdkTable.getInstance().getSdksOfType(haxeSdkType)
  val sdkProperty = propertyGraph.property<Sdk?>(haxeSdks.firstOrNull())

  override fun setupUI(builder: Panel) {
    super.setupUI(builder)
    setupHaxeSdkUI(builder)
  }

  fun setupHaxeSdkUI(builder: Panel) {
    builder.row("Haxe SDK:") {
      comboBox(haxeSdks, SimpleListCellRenderer.create("Select Haxe SDK") { sdk ->
        sdk?.let { "${it.name} (${it.versionString})" } ?: "No Haxe SDK found"
      })
    }
  }
}