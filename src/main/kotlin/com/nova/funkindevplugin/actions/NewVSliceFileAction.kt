package com.nova.funkindevplugin.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.plugins.haxe.ide.module.HaxeModuleType
import com.intellij.psi.PsiDirectory
import com.nova.funkindevplugin.VSliceBundle
import icons.VSliceIcons
import org.jetbrains.annotations.NonNls

class NewVSliceFileAction :
  CreateFileFromTemplateAction("VSlice Script File", "Creates a new V-Slice script file", VSliceIcons.VSliceIcon) {
  override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
    builder.setTitle(VSliceBundle.message("vslice.new.class.title"))

    val kinds = listOf(
      // From most used to the least used classes
      "Module", "Empty", "Song", "Stage", "Level", "SongEvent", "NoteKind",
      "Conversation", "DialogueBox",
      "FunkinSprite", "Character", "PlayableCharacter",
      "BackingCard", "FreeplayDJ",
      "MusicBeatState", "MusicBeatSubState",
      "FlxRuntimeShader"
    )

    kinds.forEach { kind ->
      builder.addKind(kind, VSliceIcons.VSliceIcon, kind)
    }
  }

  /**
   * Checks if the project is a haxe module and contains a Polymod metadata file.
   *
   * @return false if the statement above is false
   */
  override fun isAvailable(dataContext: DataContext): Boolean {
    val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return false
    val viewFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext) ?: return false

    val module = ModuleUtilCore.findModuleForFile(viewFile, project)

    val isHaxe = module?.let { ModuleType.get(it) is HaxeModuleType } ?: false
    if (!isHaxe) return false

    val model = module?.let { ModuleRootManager.getInstance(it) }
    val hasVSliceMarker = model?.contentRoots?.any { root ->
      root.findChild("_polymod_meta.json") != null
    } ?: false

    return super.isAvailable(dataContext) && hasVSliceMarker
  }

  override fun getActionName(
    directory: PsiDirectory?,
    newName: @NonNls String,
    templateName: @NonNls String?
  ): @NlsContexts.Command String? {
    return "Create new ScriptedClass: $newName"
  }
}