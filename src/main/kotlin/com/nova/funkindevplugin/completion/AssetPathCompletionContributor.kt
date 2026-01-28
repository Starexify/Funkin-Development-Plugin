package com.nova.funkindevplugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class AssetPathCompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      PlatformPatterns.psiElement().inside(JsonStringLiteral::class.java),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
          parameters: CompletionParameters,
          context: ProcessingContext,
          result: CompletionResultSet
        ) {
          val element = parameters.position.parent as? JsonStringLiteral ?: return
          val property = element.parent as? JsonProperty
          val propertyName = property?.name ?: return
          val currentFilePath = parameters.originalFile.virtualFile?.path ?: ""

          when (propertyName) {
            "albumArtAsset" -> addAssetsFromPath(parameters.position.project, "images/freeplay/albumRoll/", false, result)
            "titleAsset" -> addAssetsFromPath(parameters.position.project, "images/storymenu/titles/", false, result)
            "background" -> addAssetsFromPath(parameters.position.project, "images/", false, result)
            "albumTitleAsset" -> addAssetsFromPath(parameters.position.project, "images/freeplay/albumRoll/", true, result)
            "id" -> {
              val parentObject = property.parent?.parent as? JsonProperty
              if (parentObject?.name == "healthIcon") addIconsFromPath(parameters.position.project, result)
            }
            "assetPath" -> {
              when {
                currentFilePath.contains("characters/") -> addAssetsFromPath(parameters.position.project, "shared/images/characters/", true, result)
                currentFilePath.contains("levels/") -> addAssetsFromPath(parameters.position.project, "storymenu/props/", true, result)
                else -> {
                  addAssetsFromPath(parameters.position.project, "images/", null, result, prefix = "")
                  addAssetsFromPath(parameters.position.project, "shared/images/", null, result, prefix = "shared:")
                }
              }
            }

          }
        }
      }
    )
  }

  private fun addIconsFromPath(project: Project, result: CompletionResultSet) {
    val projectDir = project.guessProjectDir() ?: return
    val iconsDir = projectDir.findFileByRelativePath("images/icons/") ?: return

    iconsDir.children
      .filter { !it.isDirectory && it.extension?.lowercase() == "png" }
      .forEach { file ->
        val iconName = file.nameWithoutExtension.removePrefix("icon-")
        result.addElement(LookupElementBuilder.create(iconName).withIcon(AllIcons.FileTypes.Image).withTypeText("Health Icon"))
      }
  }

  private fun addAssetsFromPath(
    project: Project,
    basePath: String,
    needsXml: Boolean?,
    result: CompletionResultSet,
    prefix: String = ""
  ) {
    val assets = findAssets(project, basePath)

    val filtered = when (needsXml) {
      true -> assets.filter { (_, hasXml) -> hasXml }
      false -> assets.filter { (_, hasXml) -> !hasXml }
      null -> assets
    }

    filtered.forEach { (assetPath, isSpriteSheet) ->
      val fullPath = if (prefix.isNotEmpty()) "$prefix$assetPath" else assetPath

      val builder = LookupElementBuilder.create(fullPath)
        .withIcon(if (isSpriteSheet) AllIcons.Actions.EditScheme else AllIcons.FileTypes.Image)
        .withTypeText(if (isSpriteSheet) "Sparrow Sprite" else "Image")

      if (isSpriteSheet) builder.withTailText(" (xml)", true)

      result.addElement(builder)
    }
  }

  private fun findAssets(project: Project, basePath: String): List<Pair<String, Boolean>> {
    val results = mutableListOf<Pair<String, Boolean>>()
    val projectDir = project.guessProjectDir() ?: return results
    val assetsDir = projectDir.findFileByRelativePath(basePath) ?: return results
    val relativeBase = basePath
      .removePrefix("shared/")
      .removePrefix("images/")

    fun scanDirectory(dir: VirtualFile, currentPrefix: String = "") {
      val children = dir.children
      val files = children.filter { !it.isDirectory }
      val namesWithXml = files.filter { it.extension?.lowercase() == "xml" }
        .map { it.nameWithoutExtension }.toSet()

      val namesWithPng = files.filter { it.extension?.lowercase() == "png" }
        .map { it.nameWithoutExtension }.toSet()

      namesWithPng.forEach { name ->
        val hasXml = namesWithXml.contains(name)
        results.add(Pair("$relativeBase$currentPrefix$name", hasXml))
      }

      children.filter { it.isDirectory }.forEach { subDir ->
        scanDirectory(subDir, "$currentPrefix${subDir.name}/")
      }
    }

    scanDirectory(assetsDir)
    return results
  }
}