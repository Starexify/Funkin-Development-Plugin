package com.nova.funkindevplugin.projects

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard
import com.intellij.ide.wizard.setupProjectFromBuilder
import com.intellij.openapi.module.GeneralModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.project.Project
import com.intellij.plugins.haxe.ide.module.HaxeModuleType
import com.nova.funkindevplugin.module.VSliceModuleType
import icons.VSliceIcons
import javax.swing.Icon

class VSliceNewProjectWizard : LanguageGeneratorNewProjectWizard {
    override val name = "V-Slice Mod"
    override val icon: Icon = VSliceIcons.VSliceIcon
    override val ordinal = 250

    override fun createStep(parent: NewProjectWizardStep) = Step(parent)

    class Step(parent: NewProjectWizardStep) :
        AbstractNewProjectWizardStep(parent) {
        override fun setupProject(project: Project) {
            val moduleType = VSliceModuleType.INSTANCE
            val builder = moduleType.createModuleBuilder()
            setupProjectFromBuilder(project, builder)
        }
    }
}