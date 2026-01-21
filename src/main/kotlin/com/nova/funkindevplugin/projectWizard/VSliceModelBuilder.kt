package com.nova.funkindevplugin.projectWizard

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType
import com.intellij.plugins.haxe.ide.module.HaxeModuleType
import io.netty.util.internal.ResourcesUtil
import java.nio.file.Path

class VSliceModelBuilder : ModuleBuilder() {
  var libraryPath: String = ""
  var addSampleCode: Boolean = true

  override fun getModuleType(): ModuleType<*>? = HaxeModuleType.getInstance()
  override fun isSuitableSdkType(sdkType: SdkTypeId): Boolean = sdkType is HaxeSdkType

  override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
    if (moduleJdk != null) modifiableRootModel.sdk = moduleJdk else modifiableRootModel.inheritSdk()

    val contentEntry = doAddContentEntry(modifiableRootModel) ?: return
    val rootPath = contentEntryPath ?: return

    val scriptsPath = Path.of(rootPath, "scripts")
    FileUtil.createDirectory(scriptsPath.toFile())

    val rootFile = LocalFileSystem.getInstance().findFileByPath(rootPath) ?: return
    val scriptsFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(scriptsPath.toString())

    if (scriptsFile != null) contentEntry.addSourceFolder(scriptsFile, false)

    if (libraryPath.isNotEmpty()) {
      val libraryTable = modifiableRootModel.moduleLibraryTable
      val library = libraryTable.createLibrary("V-Slice-Modding-Libs")
      val libModel = library.modifiableModel
      libModel.addRoot(VfsUtil.pathToUrl(FileUtil.toSystemDependentName(libraryPath)), OrderRootType.CLASSES)
      libModel.commit()
    }

    createProjectFiles(rootFile, scriptsFile)
  }

  private fun createProjectFiles(rootFile: VirtualFile, scriptsFile: VirtualFile?) {
    val jsonTemplate = getResourceFileContent("_polymod_meta.json").replace("{{NAME}}", name ?: "")
    val jsonFile = rootFile.createChildData(this, "_polymod_meta.json")
    jsonFile.setBinaryContent(jsonTemplate.toByteArray())

    if (addSampleCode && scriptsFile != null) {
      val moduleTemplate = getResourceFileContent("MainModule.hxc")
      val hxcFile = scriptsFile.createChildData(this, "MainModule.hxc")
      hxcFile.setBinaryContent(moduleTemplate.trimIndent().toByteArray())
    }
  }

  private fun getResourceFileContent(fileName: String): String {
    val path = "fileTemplates/projectWizard/$fileName"
    return this::class.java.classLoader.getResourceAsStream(path)
      ?.bufferedReader()
      ?.use { it.readText() } ?: ""
  }
}