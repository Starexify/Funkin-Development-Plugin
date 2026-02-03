package com.nova.funkindevplugin.projectWizard

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType
import com.intellij.plugins.haxe.ide.module.HaxeModuleType
import com.intellij.sh.run.ShRunConfiguration
import com.nova.funkindevplugin.VSliceLibraryManager
import java.io.File
import java.nio.file.Path

class VSliceModelBuilder : ModuleBuilder() {
  val librariesName = "vslice-libraries.json"
  val iconName = "_polymod_icon.png"
  val metaName = "_polymod_meta.json"
  val mainModule = "MainModule.hxc"
  val importHX = "import.hx"

  var modIconPath: String = ""
  var addSampleScript: Boolean = true
  var addBaseFolders: Boolean = false

  override fun getModuleType(): ModuleType<*>? = HaxeModuleType.getInstance()
  override fun isSuitableSdkType(sdkType: SdkTypeId): Boolean = sdkType is HaxeSdkType

  override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
    val project = modifiableRootModel.project
    if (moduleJdk != null) modifiableRootModel.sdk = moduleJdk else modifiableRootModel.inheritSdk()

    val contentEntry = doAddContentEntry(modifiableRootModel) ?: return
    val rootPath = contentEntryPath ?: return
    val ideaPath = Path.of(rootPath, ".idea")
    contentEntry.addExcludeFolder(VfsUtil.pathToUrl(ideaPath.toString()))

    if (addBaseFolders) createBaseFolders(rootPath)

    val scriptsPath = Path.of(rootPath, "scripts")
    FileUtil.createDirectory(scriptsPath.toFile())

    val rootFile = LocalFileSystem.getInstance().findFileByPath(rootPath) ?: return
    val scriptsFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(scriptsPath.toString())
    if (scriptsFile != null) contentEntry.addSourceFolder(scriptsFile, false)

    createRunConfiguration(project)

    setupLibraries(rootFile);

    val fileToOpen: VirtualFile? = createProjectFiles(rootFile, scriptsFile)
    scheduleOpenFile(project, fileToOpen)
  }

  /**
   * Creates the base folders such as `data`, `images`, `shared', `shaders` etc when creating a new project.
   *
   * @param rootPath The path to the project's root directory.
   */
  fun createBaseFolders(rootPath: String) {
    val foldersToCreate = listOf(
      "data/characters",
      "data/dialogue/boxes",
      "data/dialogue/conversations",
      "data/dialogue/speakers",
      "data/levels",
      "data/notestyles",
      "data/players",
      "data/songs",
      "data/stages",
      "data/stickerpacks",
      "data/ui/freeplay/albums",
      "data/ui/freeplay/styles",
      "images",
      "music",
      "shaders",
      "sounds",
      "shared/images",
      "shared/music",
      "shared/sounds",
      "fonts",
      "songs",
      "videos/videos",
    )

    foldersToCreate.forEach { subPath ->
      val dir = File(rootPath, subPath)
      if (!dir.exists()) FileUtil.createDirectory(dir)
    }

    val rootFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(rootPath)
    if (rootFile != null) {
      VfsUtil.markDirtyAndRefresh(false, true, true, rootFile)
    }
  }

  /**
   * Creates the base folders such as `data`, `images`, `shared', `shaders` etc when creating a new project.
   *
   * @param project the project
   * @param rootPath The path to the project's root directory.
   */
  fun setupLibraries(rootFile: VirtualFile) {
    val configTemplate = getResourceFileContent(librariesName)
    if (configTemplate.isNotEmpty()) {
      val configFile = rootFile.createChildData(this, VSliceLibraryManager.CONFIG_FILE_NAME)
      configFile.setBinaryContent(configTemplate.toByteArray())
      VfsUtil.markDirtyAndRefresh(false, true, true, configFile)
    }
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

    // DEFAULT IMPORTS
    if (scriptsFile != null) {
      val importTemplate = getResourceFileContent(importHX)
      val importFile = scriptsFile.createChildData(this, importHX)
      importFile.setBinaryContent(importTemplate.toByteArray())
    }

    // SAMPLE SCRIPT
    if (addSampleScript && scriptsFile != null) {
      val moduleTemplate = getResourceFileContent(mainModule)
      val hxcFile = scriptsFile.createChildData(this, mainModule)
      hxcFile.setBinaryContent(moduleTemplate.trimIndent().toByteArray())
      createdMainFile = hxcFile
    }

    return createdMainFile;
  }

  /**
   * Opens a file in the given project.
   *
   * @param project the project
   * @param file The file to open when the project was opened
   */
  fun scheduleOpenFile(project: Project, file: VirtualFile?) {
    if (file == null) return

    ApplicationManager.getApplication().invokeLater({
      FileEditorManager.getInstance(project).openFile(file, true)
    }, ModalityState.nonModal())
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