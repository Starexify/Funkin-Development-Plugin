package com.nova.funkindevplugin.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
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

  override fun getActionName(
    directory: PsiDirectory?,
    newName: @NonNls String,
    templateName: @NonNls String?
  ): @NlsContexts.Command String? {
    return "Create my Class: $newName"
  }
}