package com.nova.funkindevplugin.projectWizard

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.ide.projectWizard.NewProjectWizardCollector.Base.logAddSampleCodeChanged
import com.intellij.ide.projectWizard.NewProjectWizardCollector.Base.logAddSampleCodeFinished
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.openapi.module.Module
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep.Companion.ADD_SAMPLE_CODE_PROPERTY_NAME
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
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
  var addSampleCode by addSampleCodeProperty

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
    setupSampleCodeUI(builder)
  }

  fun setupSampleCodeUI(builder: Panel) {
    builder.row {
      checkBox(UIBundle.message("label.project.wizard.new.project.add.sample.code"))
        .bindSelected(addSampleCodeProperty)
        .whenStateChangedFromUi { logAddSampleCodeChanged(it) }
        .onApply { logAddSampleCodeFinished(addSampleCode) }
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

  private fun configureModuleBuilder(project: Project, builder: ModuleBuilder) {
    val moduleFile = Paths.get(path, name, name + ModuleFileType.DOT_DEFAULT_EXTENSION)

    builder.name = name
    builder.moduleFilePath = FileUtil.toSystemDependentName(moduleFile.toString())
    builder.contentEntryPath = FileUtil.toSystemDependentName("$path/$name")

    builder.moduleJdk = jdkChooser.selectedJdk

    WriteAction.run<Exception> {
      val module = builder.commitModule(project, null)

      if (addSampleCode && module != null) {
        val root = LocalFileSystem.getInstance().findFileByPath(builder.contentEntryPath!!)

        val scriptsDir = root?.findChild("scripts") ?: root?.createChildDirectory(this, "scripts")

        scriptsDir?.let { dir ->
          // Create a Main Module file
          val mainFile = dir.createChildData(this, "MainModule.hxc")
          val content = """
                    import funkin.modding.module.ScriptedModule;
                    
                    class MainModule extends ScriptedModule {
                        public function onCreate(event) {
                            trace("Hello world!");
                        }
                    }
                """.trimIndent()

          mainFile.setBinaryContent(content.toByteArray())

          //createRunConfiguration(project, module)
        }
      }
    }
  }

/*  fun createRunConfiguration(project: Project, module: Module) {
    val runManager = RunManager.getInstance(project)

    val type = ConfigurationTypeUtil.findConfigurationType(ShellConfigurationType::class.java)
    val factory = type.configurationFactories[0]

    val settings = runManager.createConfiguration("Run Funkin", factory)
    val config = settings.configuration as ShellRunConfiguration


    runManager.addConfiguration(settings)
    runManager.selectedConfiguration = settings
  }*/

  override fun setupProject(project: Project) {
    configureModuleBuilder(project, VSliceModelBuilder())

    super.setupProject(project)
  }
}