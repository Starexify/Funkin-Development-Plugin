package com.nova.funkindevplugin.projectWizard

import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.ide.projectWizard.NewProjectWizardCollector.Base.logAddSampleCodeChanged
import com.intellij.ide.projectWizard.NewProjectWizardCollector.Base.logAddSampleCodeFinished
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep.Companion.ADD_SAMPLE_CODE_PROPERTY_NAME
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import com.nova.funkindevplugin.VSliceBundle
import java.nio.file.Paths

class VSliceNewProjectWizardStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent),
  NewProjectWizardBaseData by parent.baseData!! {

  private val project = context.project ?: ProjectManager.getInstance().defaultProject
  private val sdksModel = ProjectStructureConfigurable.getInstance(project).projectJdksModel

  private val jdkChooser = JdkComboBox(
    context.project,
    sdksModel,
    { it is HaxeSdkType },
    null,
    null,
    null
  )

  val addSampleCodeProperty = propertyGraph.property(true).bindBooleanStorage(ADD_SAMPLE_CODE_PROPERTY_NAME)
  val libraryPathProperty = propertyGraph.property("")
  val modIconPathProperty = propertyGraph.property("")

  var addSampleScript by addSampleCodeProperty

  init {
    HaxeSdkType.getInstance().ensureSdk()
  }

  override fun setupUI(builder: Panel) {
    sdksModel.reset(project)
    jdkChooser.reloadModel()

    val lastSdk = context.projectJdk
    if (lastSdk != null && lastSdk.sdkType is HaxeSdkType) {
      jdkChooser.selectedJdk = lastSdk
    }

    super.setupUI(builder)
    setupHaxeSdkUI(builder)
    setupModdingLibraryUI(builder)
    setupModIconUI(builder)
    setupSampleScriptUI(builder)
  }

  fun setupModdingLibraryUI(builder: Panel) {
    builder.row("Library Path:") {
      val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        .withTitle("Select V-Slice Library Folder")
        .withDescription("Choose the directory containing libraries for V-Slice modding.")

      textFieldWithBrowseButton(descriptor, context.project)
        .bindText(libraryPathProperty)
        .align(AlignX.FILL)
    }
  }

  fun setupModIconUI(builder: Panel) {
    builder.row("Mod Icon:") {
      val descriptor = FileChooserDescriptorFactory.singleFile()
        .withFileFilter { it.extension?.lowercase() == "png" }
        .withTitle("Select your mod's Icon")
        .withDescription("Choose the icon for your V-Slice mod.")

      textFieldWithBrowseButton(descriptor, context.project)
        .bindText(modIconPathProperty)
        .align(AlignX.FILL)
    }
  }

  fun setupSampleScriptUI(builder: Panel) {
    builder.row {
      checkBox(VSliceBundle.message("vslice.label.project.wizard.new.project.add.sample.script"))
        .bindSelected(addSampleCodeProperty)
        .whenStateChangedFromUi { logAddSampleCodeChanged(it) }
        .onApply { logAddSampleCodeFinished(addSampleScript) }
    }
  }

  fun setupHaxeSdkUI(builder: Panel) {
    builder.row("Haxe SDK:") {
      // Add the JdkComboBox directly to the DSL row
      cell(jdkChooser)
        .align(AlignX.FILL)
        .onApply {
          context.projectJdk = jdkChooser.selectedJdk
        }
    }
  }

  override fun setupProject(project: Project) {
    configureModuleBuilder(project, VSliceModelBuilder())

    super.setupProject(project)
  }

  private fun configureModuleBuilder(project: Project, builder: VSliceModelBuilder) {
    val moduleFile = Paths.get(path, name, name + ModuleFileType.DOT_DEFAULT_EXTENSION)

    builder.name = name
    builder.moduleFilePath = FileUtil.toSystemDependentName(moduleFile.toString())
    builder.contentEntryPath = FileUtil.toSystemDependentName("$path/$name")

    builder.moduleJdk = jdkChooser.selectedJdk

    builder.modIconPath = modIconPathProperty.get()
    builder.libraryPath = libraryPathProperty.get()
    builder.addSampleScript = addSampleScript

    builder.commit(project)
  }
}