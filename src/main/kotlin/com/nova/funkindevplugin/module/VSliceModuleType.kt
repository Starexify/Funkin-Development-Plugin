package com.nova.funkindevplugin.module

import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import icons.VSliceIcons
import org.jetbrains.annotations.Nls
import javax.swing.Icon

class VSliceModuleType : ModuleType<VSliceModuleBuilder>(TYPE_ID) {
  companion object {
    const val TYPE_ID = "VSLICE_MODULE_TYPE"

    val INSTANCE: VSliceModuleType
      get() = ModuleTypeManager.getInstance().findByID(TYPE_ID) as VSliceModuleType
  }

  override fun createModuleBuilder(): VSliceModuleBuilder = VSliceModuleBuilder()

  override fun getName(): @Nls(capitalization = Nls.Capitalization.Title) String = "V-Slice Mod"

  override fun getDescription(): @Nls(capitalization = Nls.Capitalization.Sentence) String = "Create a new V-Slice mod."

  override fun getNodeIcon(isOpened: Boolean): Icon = VSliceIcons.POLYMOD_LOGO
}