package com.nova.funkindevplugin.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.plugins.haxe.ide.HaxeQualifiedNameProvider
import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent
import com.intellij.plugins.haxe.lang.psi.HaxeReference
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression
import com.intellij.plugins.haxe.lang.psi.HaxeVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.nova.funkindevplugin.services.VSliceProjectService

data class FieldBlacklist(
  val staticFields: List<String> = emptyList(),
  val instanceFields: List<String> = emptyList()
)

class BlacklistedImportInspection : LocalInspectionTool() {
  private val blacklistedClasses = setOf(
    "Sys",
    "cpp.Lib",
    "haxe.Unserializer",
    "funkin.mobile.util.AdMobUtil",
    "funkin.mobile.util.InAppPurchasesUtil",
    "funkin.mobile.util.InAppReviewUtil",
    "lime.system.CFFI",
    "lime.system.JNI",
    "lime.system.System",
    "openfl.Lib",
    "openfl.system.ApplicationDomain",
    "openfl.net.SharedObject",
    "openfl.desktop.NativeProcess",
    "funkin.external.android.CallbackUtil",
    "funkin.external.android.DataFolderUtil",
    "funkin.external.android.JNIUtil"
  )

  private val blacklistConfig = mapOf(
    "flixel.util.FlxSave" to FieldBlacklist(staticFields = listOf("resolveFlixelClasses")),
    "flixel.FlxG" to FieldBlacklist(staticFields = listOf("save")),
    "haxe.Unserializer" to FieldBlacklist(
      staticFields = listOf("run"),
      instanceFields = listOf("unserialize")
    ),
    "funkin.save.Save" to FieldBlacklist(
      instanceFields = listOf("data", "clearData", "setLevelScore", "setSongScore", "applySongRank")
    ),
    "openfl.net.Socket" to FieldBlacklist(instanceFields = listOf("readObject"))
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
    "funkin.util.macro"
  )

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val module = ModuleUtilCore.findModuleForFile(holder.file) ?: return PsiElementVisitor.EMPTY_VISITOR
    val service = holder.project.service<VSliceProjectService>()
    if (!service.isValidPolymodModule(module)) return PsiElementVisitor.EMPTY_VISITOR

    return object : HaxeVisitor() {
      // Check imports for blacklists
      override fun visitImportStatement(importStatement: HaxeImportStatement) {
        super.visitImportStatement(importStatement)
        val ref = importStatement.referenceExpression ?: return
        checkAndReport(ref.text.removeSuffix(".*"), importStatement)
      }

      // Check references(fields/calls) for blacklists
      override fun visitReferenceExpression(reference: HaxeReferenceExpression) {
        super.visitReferenceExpression(reference)
        if (reference.parent is HaxeImportStatement) return

        val qualifier = getQualifiedNameFromElement(reference) ?: return
        val className = qualifier.substringBefore("#")
        val fieldName = if (qualifier.contains("#")) qualifier.substringAfter("#") else null

        if (fieldName != null) {
          val config = blacklistConfig[className]
          if (config?.staticFields?.contains(fieldName) == true || config?.instanceFields?.contains(fieldName) == true) {
            val nameElement = reference.referenceNameElement ?: reference
            holder.registerProblem(
              nameElement,
              "Access to '$className.$fieldName' is blacklisted.",
              ProblemHighlightType.GENERIC_ERROR,
              RemoveBlacklistedFieldFix()
            )
            return
          }
        }

        val isExactMatch = blacklistedClasses.contains(className)
        val matchingPrefix = blacklistedPrefixes.find { className.startsWith("$it.") || className == it }

        if (isExactMatch || matchingPrefix != null) {
          holder.registerProblem(
            reference,
            "Usage of '$className' is blacklisted.",
            ProblemHighlightType.GENERIC_ERROR,
            RemoveImportQuickFix()
          )
        }
      }

      private fun getQualifiedNameFromElement(element: PsiElement): String? {
        val resolved = if (element is HaxeReference) element.resolve() else element

        return resolved?.let { elementToProcess ->
          val provider = HaxeQualifiedNameProvider()
          provider.getQualifiedName(elementToProcess)
        }
      }

      private fun checkAndReport(path: String, elementToUnderline: PsiElement) {
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
  override fun getFamilyName(): String = "Remove blacklisted code"

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val element = descriptor.psiElement
    val target = PsiTreeUtil.getParentOfType(element, HaxeCallExpression::class.java, false) ?: element

    val nextSibling = target.nextSibling
    if (nextSibling != null && nextSibling.text.trim() == ";") nextSibling.delete()

    target.delete()
  }
}

class RemoveBlacklistedFieldFix : LocalQuickFix {
  override fun getFamilyName(): String = "Remove blacklisted field"

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val element = descriptor.psiElement
    val call = PsiTreeUtil.getParentOfType(element, HaxeCallExpression::class.java, false)
    val target = (call ?: element)

    val dot = PsiTreeUtil.skipWhitespacesBackward(target)?.takeIf { it.text == "." }
    val next = PsiTreeUtil.skipWhitespacesForward(target)?.takeIf { it.text == ";" }

    next?.delete() // Remove the semicolon
    target.delete() // Remove the method call/field
    dot?.delete()  // Remove the dot
  }
}