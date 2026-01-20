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
      "Module",
      "NoteKind",
      "Level",
      "Stage",
      "Song",
      "SongEvent",
      "NoteStyle",
      "Strumline",
      "Conversation",
      "Speaker",
      "DialogueBox",
      "PlayableCharacter",
      "Bopper",
      "StageProp",
      "FunkinSprite",
      "Character",
      "BackingCard",
      "StickerPack",
      "FreeplayStyle",
      "FreeplayDJ",
      "Album",
      "MusicBeatState",
      "MusicBeatSubState",
      "FlxRuntimeShader",
      "FlxSprite",
      "FlxStrip",
      "FlxBasic",
      "FlxObject",
      "FlxTypedSpriteGroup",
      "FlxTypedGroup",
      "FlxState",
      "FlxSubState",
      "FlxTransitionableState"
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