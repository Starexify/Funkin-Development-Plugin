package com.nova.funkindevplugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.plugins.haxe.lang.psi.HaxeClass
import com.intellij.plugins.haxe.lang.psi.HaxeClassBody
import com.intellij.plugins.haxe.lang.psi.HaxeMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class ConstructorCompletion : CompletionContributor() {
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

        val hasConstructor = currentClass.getHaxeMethodsSelf(null).any { it.name == "new" }
        if (hasConstructor) return

        val lookupElement = LookupElementBuilder.create("new")
          .withIcon(AllIcons.Nodes.Method)
          .withPresentableText("function new")
          .withTailText("(...) {...}", true)
          .withTypeText("Constructor", true)
          .withLookupStrings(listOf("new", "function new", "constructor"))
          .withInsertHandler { ctx, _ -> insertConstructorTemplate(ctx, currentClass) }

        result.addElement(PrioritizedLookupElement.withPriority(lookupElement, 2000.0))
      }
    })
  }

  private fun insertConstructorTemplate(context: InsertionContext, haxeClass: HaxeClass) {
    val project = context.project
    val templateManager = TemplateManager.getInstance(project)

    val superConstructor = haxeClass.getHaxeMethodsAncestor(true)
      ?.find { it.name == "new" }

    val (signature, superCall) = if (superConstructor != null) {
      val model = superConstructor.model
      val fullSig = model.getPresentableText(null).replace("new", "new")
      val paramNames = model.parameters.joinToString(", ") { it.name ?: "" }
      Pair(fullSig, "super($paramNames);")
    } else {
      Pair("new()", "")
    }

    val template = templateManager.createTemplate(
      "vslice_new", "haxe",
      "public function $signature {\n    $superCall\n    \$END$\n}"
    )

    template.isToReformat = true

    context.document.deleteString(context.startOffset, context.tailOffset)
    templateManager.startTemplate(context.editor, template)
  }
}