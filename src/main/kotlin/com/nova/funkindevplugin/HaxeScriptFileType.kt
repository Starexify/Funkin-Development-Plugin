package com.nova.funkindevplugin

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import icons.HaxeIcons
import javax.swing.Icon

class HaxeScriptFileType : LanguageFileType(Language.findLanguageByID("Haxe")!!) {
    override fun getName() = "Haxe Script File"
    override fun getDescription() = "Scripted Haxe files"
    override fun getDefaultExtension() = "hxc"
    override fun getIcon(): Icon? = HaxeIcons.HAXE_LOGO
}