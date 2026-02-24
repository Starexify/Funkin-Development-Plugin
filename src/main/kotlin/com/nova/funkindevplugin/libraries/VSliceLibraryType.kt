package com.nova.funkindevplugin.libraries

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.LibraryType
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor
import com.intellij.openapi.vfs.VirtualFile
import icons.VSliceIcons
import javax.swing.Icon
import javax.swing.JComponent

class VSliceLibraryType : LibraryType<DummyLibraryProperties>(VSLICE_KIND) {
  companion object {
    val VSLICE_KIND = object : PersistentLibraryKind<DummyLibraryProperties>("VSlice") {
      override fun createDefaultProperties() = DummyLibraryProperties()
    }
  }

  override fun getCreateActionName() = "VSlice Library"

  override fun createNewLibrary(
    parentComponent: JComponent,
    contextDirectory: VirtualFile?,
    project: Project
  ): NewLibraryConfiguration? = null

  override fun getIcon(properties: DummyLibraryProperties?): Icon = VSliceIcons.VSliceIcon

  override fun createPropertiesEditor(editorComponent: LibraryEditorComponent<DummyLibraryProperties?>): LibraryPropertiesEditor? = null
}