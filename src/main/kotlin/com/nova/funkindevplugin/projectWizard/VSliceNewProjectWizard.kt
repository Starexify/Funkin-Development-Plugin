package com.nova.funkindevplugin.projectWizard

import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard
import icons.VSliceIcons
import javax.swing.Icon

class VSliceNewProjectWizard : LanguageGeneratorNewProjectWizard {
    override val name = "V-Slice Mod"
    override val icon: Icon = VSliceIcons.VSliceIcon
    override val ordinal = 250

    override fun createStep(parent: NewProjectWizardStep) = VSliceNewProjectWizardStep(parent)
}