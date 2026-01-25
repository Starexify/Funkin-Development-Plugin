package com.nova.funkindevplugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.plugins.haxe.lang.psi.HaxeMethod
import com.intellij.util.ProcessingContext

class FunctionCompletion : CompletionContributor() {
  init {
    val pattern = PlatformPatterns.psiElement().inside(HaxeMethod::class.java)

    extend(CompletionType.BASIC, pattern, object : CompletionProvider<CompletionParameters>() {
      override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
      ) {
        val traceLookup = LookupElementBuilder.create("trace")
          .withIcon(AllIcons.Nodes.Method)
          .withPresentableText("trace")
          .withTailText("(v:Dynamic, ?infos:PosInfos):Void", true)
          .withTypeText("Log to console")
          .withInsertHandler { ctx, _ ->
            val templateManager = TemplateManager.getInstance(ctx.project)
            val template = templateManager.createTemplate("haxe_trace", "haxe", "trace(\$END$);")
            ctx.document.deleteString(ctx.startOffset, ctx.tailOffset)
            templateManager.startTemplate(ctx.editor, template)
          }

        result.addElement(PrioritizedLookupElement.withPriority(traceLookup, 50.0))
      }
    })
  }
}