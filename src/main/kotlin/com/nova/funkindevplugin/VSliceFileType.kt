package com.nova.funkindevplugin

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import icons.VSliceIcons
import javax.swing.Icon

class VSliceFileType : LanguageFileType(Language.findLanguageByID("Haxe")!!) {
    override fun getName() = "Haxe Script File"
    override fun getDescription() = "Scripted Haxe files"
    override fun getDefaultExtension() = "hxc"
    override fun getIcon(): Icon? = VSliceIcons.POLYMOD_LOGO
}