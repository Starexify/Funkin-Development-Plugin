package com.nova.funkindevplugin.projectWizard

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType
import com.intellij.plugins.haxe.ide.module.HaxeModuleType
import java.nio.file.Path

class VSliceModelBuilder : ModuleBuilder() {
  override fun getModuleType(): ModuleType<*>? = HaxeModuleType.getInstance()
  override fun isSuitableSdkType(sdkType: SdkTypeId): Boolean = sdkType is HaxeSdkType

  override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
    if (moduleJdk != null) {
      modifiableRootModel.sdk = moduleJdk
    } else {
      modifiableRootModel.inheritSdk()
    }

    val contentEntry = doAddContentEntry(modifiableRootModel)

    if (contentEntry != null && contentEntryPath != null) {
      val path = Path.of(contentEntryPath!!, "scripts")
      FileUtil.createDirectory(path.toFile())

      val sourceRootUrl = VfsUtil.pathToUrl(path.toString())
      contentEntry.addSourceFolder(sourceRootUrl, false)
    }
  }
}