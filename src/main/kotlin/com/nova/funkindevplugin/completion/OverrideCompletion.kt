package com.nova.funkindevplugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.plugins.haxe.lang.psi.HaxeClass
import com.intellij.plugins.haxe.lang.psi.HaxeClassBody
import com.intellij.plugins.haxe.lang.psi.HaxeMethod
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class OverrideCompletion : CompletionContributor() {
  init {
    val pattern = PlatformPatterns.psiElement()
      .inside(HaxeClassBody::class.java)
      .andNot(PlatformPatterns.psiElement().inside(HaxeMethod::class.java))

    extend(CompletionType.BASIC, pattern, object : CompletionProvider<CompletionParameters>() {
      override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
      ) {
        val position = parameters.position
        val currentClass = PsiTreeUtil.getParentOfType(position, HaxeClass::class.java) ?: return

        val prefix = result.prefixMatcher.prefix
        val matcher = CamelHumpMatcher(prefix, false)
        val activeResult = result.withPrefixMatcher(matcher)

        val overridableMethods = findOverridableMethods(currentClass)

        for (method in overridableMethods) {
          val methodName = method.name ?: continue

          val isDeprecated = method.hasCompileTimeMetadata(HaxeMeta.DEPRECATED) || method.hasRuntimeMetadata(HaxeMeta.DEPRECATED)
          if (method.hasCompileTimeMetadata(HaxeMeta.NO_COMPLETION)) continue

          val containingClass = method.containingClass as? HaxeClass
          val isInterface = containingClass?.isInterface ?: false
          val originClass = containingClass?.name ?: "Super"
          val paramAndReturnType = method.model.getPresentableText(null).removePrefix(methodName)
          val displayPrefix = if (isInterface) "function " else "override function "

          val lookupElement = LookupElementBuilder.create(methodName)
            .withIcon(AllIcons.Nodes.Method)
            .withPresentableText("$displayPrefix$methodName")
            .withTailText(paramAndReturnType, true)
            .withTypeText(originClass, true)
            .withStrikeoutness(isDeprecated)
            .withLookupStrings(listOf(methodName, "override $methodName"))
            .withInsertHandler { ctx, _ -> insertOverrideTemplate(ctx, method) }

          val priority = if (isDeprecated) 10.0 else 1000.0
          activeResult.addElement(PrioritizedLookupElement.withPriority(lookupElement, priority))
        }
      }
    })
  }

  private fun findOverridableMethods(haxeClass: HaxeClass): List<HaxeMethod> {
    val ancestorMethods = haxeClass.getHaxeMethodsAncestor(true) ?: return emptyList()
    val localMethodNames = haxeClass.getHaxeMethodsSelf(null).mapNotNull { it.name }.toSet()

    return ancestorMethods.filter { method ->
      val name = method.name
      name != null && name != "new" && !localMethodNames.contains(name)
    }
  }

  private fun insertOverrideTemplate(context: InsertionContext, method: HaxeMethod) {
    val project = context.project
    val templateManager = TemplateManager.getInstance(project)
    val (fullSignature, superParams, returnTypeStr) = getMethodSignatureParts(method)
    val methodName = method.name ?: ""

    val document = context.document
    var startOffset = context.startOffset
    val tailOffset = context.tailOffset
    val textBefore = document.charsSequence.subSequence(0, startOffset).toString().trimEnd()
    if (textBefore.endsWith("override")) {
      val overrideIndex = textBefore.lastIndexOf("override")
      if (overrideIndex != -1) startOffset = overrideIndex
    }

    val visibility = if (method.isPublic) "public " else "private "

    // Check if the method is overridable BUT has no implementation
    val containingClass = method.containingClass as? HaxeClass
    val isInterface = containingClass?.isInterface ?: false
    val isAbstract = method.model.isAbstract

    val overrideKey = if (isInterface) "" else "override "
    val isVoid = returnTypeStr.contains("Void", ignoreCase = true)

    val body = if (isInterface || isAbstract) {
      if (isVoid) "" else "return null; // TODO"
    } else {
      if (isVoid) "super.$methodName($superParams);"
      else "return super.$methodName($superParams);"
    }

    val template = templateManager.createTemplate(
      "haxe_override", "haxe",
      "${overrideKey}${visibility}function $fullSignature {\n    $body\n    \$END$\n}"
    )

    template.isToReformat = true
    document.deleteString(startOffset, tailOffset)
    templateManager.startTemplate(context.editor, template)
  }

  /**
   * Extracts components from a method using its model.
   *
   * @param method The [HaxeMethod] to analyze.
   * @return A [Triple] containing:
   * - **first**: The full method signature (e.g., `update(dt:Float):Void`).
   * - **second**: A comma-separated string of parameter names only (e.g., `dt, multiplier`).
   * - **third**: The raw return type string (e.g., `Void` or `Int`).
   * @see HaxeMethodModel
   */
  private fun getMethodSignatureParts(method: HaxeMethod): Triple<String, String, String> {
    val model = method.model

    val fullSignature = model.getPresentableText(null)
    val superParams = model.parameters.mapNotNull { it.name }.joinToString(", ")
    val returnTypeStr = model.getReturnType(null).toString()

    return Triple(fullSignature, superParams, returnTypeStr)
  }
}