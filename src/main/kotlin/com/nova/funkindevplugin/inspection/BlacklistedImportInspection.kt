package com.nova.funkindevplugin.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression
import com.intellij.plugins.haxe.lang.psi.HaxeVisitor
import com.intellij.psi.PsiElementVisitor

class BlacklistedImportInspection : LocalInspectionTool() {
  private val blacklistedClasses = setOf(
    "Sys",
    "cpp.Lib",
    "haxe.Unserializer",
    "flixel.util.FlxSave",
    "funkin.mobile.util.AdMobUtil",
    "funkin.mobile.util.InAppPurchasesUtil",
    "funkin.mobile.util.InAppReviewUtil",
    "lime.system.CFFI",
    "lime.system.JNI",
    "lime.system.System",
    "openfl.Lib",
    "openfl.system.ApplicationDomain",
    "openfl.net.SharedObject",
    "openfl.desktop.NativeProcess"
  )

  private val blacklistedPrefixes = setOf(
    "extension.androidtools",
    "extension.haptics",
    "extension.admob",
    "extension.iapcore",
    "extension.iarcore",
    "extension.webviewcore",
    "funkin.api",
    "polymod",
    "hscript",
    "io.newgrounds",
    "sys",
    "funkin.util.macro",
    "funkin.external.android.CallbackUtil",
    "funkin.external.android.DataFolderUtil",
    "funkin.external.android.JNIUtil"
  )

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = holder.file
    if (!file.name.endsWith(".hxc", ignoreCase = true)) return PsiElementVisitor.EMPTY_VISITOR

    return object : HaxeVisitor() {
      // Check imports for blacklists
      override fun visitImportStatement(importStatement: HaxeImportStatement) {
        super.visitImportStatement(importStatement)
        val ref = importStatement.referenceExpression ?: return
        checkAndReport(ref.text.removeSuffix(".*"), importStatement)
      }

      // Check reference for blacklists
      override fun visitReferenceExpression(reference: HaxeReferenceExpression) {
        super.visitReferenceExpression(reference)

        val fullText = reference.text
        if (reference.parent is HaxeImportStatement) return

        checkAndReport(fullText, reference)
      }

      private fun checkAndReport(path: String, elementToUnderline: com.intellij.psi.PsiElement) {
        val isExactMatch = blacklistedClasses.contains(path)
        val matchingPrefix = blacklistedPrefixes.find { prefix ->
          path == prefix || path.startsWith("$prefix.")
        }

        if (isExactMatch || matchingPrefix != null) {
          val reason = if (isExactMatch) "is blacklisted" else "belongs to blacklisted package '$matchingPrefix'"

          holder.registerProblem(
            elementToUnderline,
            "Usage of '$path' $reason and is not allowed in scripts.",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            RemoveImportQuickFix()
          )
        }
      }
    }
  }
}

class RemoveImportQuickFix : LocalQuickFix {
  override fun getFamilyName(): String = "Remove blacklisted import"

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val element = descriptor.psiElement
    element.delete()
  }
}