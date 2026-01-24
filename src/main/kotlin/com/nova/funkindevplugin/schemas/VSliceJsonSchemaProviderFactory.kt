package com.nova.funkindevplugin.schemas

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

class VSliceJsonSchemaProviderFactory : JsonSchemaProviderFactory {
  override fun getProviders(project: Project): List<JsonSchemaFileProvider?> {
    val root = project.guessProjectDir()

    // Most Data schemes are similar here so we use a map
    val schemaMap = mapOf(
      "Character Data"        to ("data/characters/" to "character.json"),
      "Dialogue Box Schema"   to ("data/dialogue/boxes/" to "dialogue_box.json"),
      "Conversation Schema"   to ("data/dialogue/conversations/" to "conversation.json"),
      "Speaker Schema"        to ("data/dialogue/speakers/" to "speaker.json"),
      "Story Level Schema"    to ("data/levels/" to "level.json"),
      "Note Style Schema"     to ("data/notestyles/" to "notestyle.json"),
      "Player Schema"         to ("data/players/" to "player.json"),
      "Stage Schema"          to ("data/stages/" to "stage.json"),
      "Sticker Pack Schema"   to ("data/stickerpacks/" to "stickerpack.json"),
      "Album Schema"          to ("data/ui/freeplay/albums/" to "album.json"),
      "Freeplay Style Schema" to ("data/ui/freeplay/styles/" to "freeplayStyle.json")
    )

    val providers = schemaMap.map { (name, info) ->
      val (path, resource) = info
      createProvider(name, resource) { file ->
        val relPath = root?.let { VfsUtilCore.getRelativePath(file, it, '/') }
        relPath?.startsWith(path) == true
      }
    }.toMutableList()


    // Song/Music Metadata and Chart Data are different from the others so we add them in another map
    val patternSchemaMap = mapOf(
      "Song Metadata"   to Triple("data/songs/", "-metadata", "song/metadata.json"),
      "Song Chart"      to Triple("data/songs/", "-chart", "song/chart.json"),
      "Music Metadata"  to Triple("music/", "-metadata", "music-metadata.json")
    )

    providers.addAll(patternSchemaMap.map { (name, info) ->
      val (path, keyword, resource) = info
      createProvider(name, resource) { file ->
        val relPath = root?.let { VfsUtilCore.getRelativePath(file, it, '/') }
        relPath?.startsWith(path) == true && file.nameWithoutExtension.contains(keyword)
      }
    })


    // Polymod Metadata is also different
    providers.add(createProvider("Polymod Metadata", "_polymod_meta.json") {
      it.name == "_polymod_meta.json"
    })

    return providers
  }

  fun createProvider(
    name: String,
    schemaResourceName: String,
    condition: (VirtualFile) -> Boolean
  ): JsonSchemaFileProvider {
    return object : JsonSchemaFileProvider {
      override fun getName(): String = name
      override fun isAvailable(file: VirtualFile): Boolean {
        if (file.extension != "json") return false
        return condition(file)
      }

      override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema
      override fun getSchemaFile(): VirtualFile? = JsonSchemaProviderFactory.getResourceFile(
        VSliceJsonSchemaProviderFactory::class.java,
        "/schemas/$schemaResourceName"
      )
    }
  }
}