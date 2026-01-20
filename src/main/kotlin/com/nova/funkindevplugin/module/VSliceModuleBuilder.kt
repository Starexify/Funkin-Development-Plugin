package com.nova.funkindevplugin.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel

class VSliceModuleBuilder : ModuleBuilder() {
  override fun getModuleType(): ModuleType<*> = VSliceModuleType.INSTANCE

  override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
    doAddContentEntry(modifiableRootModel)
  }
}