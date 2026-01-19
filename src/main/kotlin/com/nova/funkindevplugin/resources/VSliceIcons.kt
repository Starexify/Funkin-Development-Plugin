package com.nova.funkindevplugin.resources

import com.intellij.openapi.util.IconLoader.getIcon;
import javax.swing.Icon

object VSliceIcons {
    val VSliceIcon: Icon = load("/icons/v-slice.svg")

    private fun load(path: String): Icon {
        return getIcon(path, VSliceIcons::class.java)
    }
}