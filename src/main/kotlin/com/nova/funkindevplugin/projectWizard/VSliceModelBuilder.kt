package com.nova.funkindevplugin.projectWizard

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType
import com.intellij.plugins.haxe.ide.module.HaxeModuleType
import com.intellij.sh.run.ShRunConfiguration
import java.io.File
import java.nio.file.Path

class VSliceModelBuilder : ModuleBuilder() {
  val iconName = "_polymod_icon.png"
  val metaName = "_polymod_meta.json"

  var modIconPath: String = ""
  var libraryPath: String = ""
  var addSampleScript: Boolean = true

  override fun getModuleType(): ModuleType<*>? = HaxeModuleType.getInstance()
  override fun isSuitableSdkType(sdkType: SdkTypeId): Boolean = sdkType is HaxeSdkType

  override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
    val project = modifiableRootModel.project
    if (moduleJdk != null) modifiableRootModel.sdk = moduleJdk else modifiableRootModel.inheritSdk()

    val contentEntry = doAddContentEntry(modifiableRootModel) ?: return
    val rootPath = contentEntryPath ?: return
    val ideaPath = Path.of(rootPath, ".idea")
    contentEntry.addExcludeFolder(VfsUtil.pathToUrl(ideaPath.toString()))

    val scriptsPath = Path.of(rootPath, "scripts")
    FileUtil.createDirectory(scriptsPath.toFile())

    val rootFile = LocalFileSystem.getInstance().findFileByPath(rootPath) ?: return
    val scriptsFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(scriptsPath.toString())
    if (scriptsFile != null) contentEntry.addSourceFolder(scriptsFile, false)

    createRunConfiguration(project)

    if (libraryPath.isNotEmpty()) {
      val libraryTable = modifiableRootModel.moduleLibraryTable
      val library = libraryTable.createLibrary("V-Slice-Modding-Libs")
      val libModel = library.modifiableModel
      libModel.addRoot(VfsUtil.pathToUrl(FileUtil.toSystemDependentName(libraryPath)), OrderRootType.CLASSES)
      libModel.commit()
    }

    val fileToOpen: VirtualFile? = createProjectFiles(rootFile, scriptsFile)
    scheduleOpenFile(project, fileToOpen)
  }

  private fun createRunConfiguration(project: Project) {
    val runManager = RunManager.getInstance(project)
    val basePath = project.basePath ?: return

    val targetPath = Path.of(basePath).parent?.parent?.toString() ?: basePath

    val factory = ConfigurationTypeUtil.findConfigurationType("ShConfigurationType")
      ?.configurationFactories
      ?.firstOrNull() ?: return

    val settings = runManager.createConfiguration("Run Funkin", factory)
    val runConfig = settings.configuration as ShRunConfiguration

    runConfig.isExecuteScriptFile = false
    runConfig.scriptText = "./Funkin"
    runConfig.scriptWorkingDirectory = targetPath
    runConfig.isExecuteInTerminal = true

    runManager.addConfiguration(settings)
    runManager.selectedConfiguration = settings
  }

  private fun createProjectFiles(rootFile: VirtualFile, scriptsFile: VirtualFile?): VirtualFile? {
    var createdMainFile: VirtualFile? = null

    // MOD METADATA
    val jsonTemplate = getResourceFileContent(metaName).replace("{{NAME}}", name ?: "")
    val jsonFile = rootFile.createChildData(this, metaName)
    jsonFile.setBinaryContent(jsonTemplate.toByteArray())

    // MOD ICON
    val iconBytes = if (modIconPath.isNotEmpty()) {
      val file = File(modIconPath)
      if (file.exists()) file.readBytes() else getResourceFileBytes(iconName)
    } else getResourceFileBytes(iconName)

    if (iconBytes.isNotEmpty()) {
      val iconFile = rootFile.findChild(iconName) ?: rootFile.createChildData(this, iconName)
      iconFile.setBinaryContent(iconBytes)
    }

    // SAMPLE SCRIPT
    if (addSampleScript && scriptsFile != null) {
      val moduleTemplate = getResourceFileContent("MainModule.hxc")
      val hxcFile = scriptsFile.createChildData(this, "MainModule.hxc")
      hxcFile.setBinaryContent(moduleTemplate.trimIndent().toByteArray())
      createdMainFile = hxcFile
    }

    return createdMainFile;
  }

  private fun scheduleOpenFile(project: Project, file: VirtualFile?) {
    if (file == null) return

    // StartupManager waits until the project window is actually visible
    StartupManager.getInstance(project).runAfterOpened {
      FileEditorManager.getInstance(project).openFile(file, true)
    }
  }

  private fun getResourceFileContent(fileName: String): String {
    val path = "fileTemplates/projectWizard/$fileName"
    return this::class.java.classLoader.getResourceAsStream(path)
      ?.bufferedReader()
      ?.use { it.readText() } ?: ""
  }

  private fun getResourceFileBytes(fileName: String): ByteArray {
    val path = "fileTemplates/projectWizard/$fileName"
    return this::class.java.classLoader.getResourceAsStream(path)
      ?.use { it.readAllBytes() } ?: ByteArray(0)
  }
}