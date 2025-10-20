package com.nova.funkindevplugin.projects

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.util.UserDataHolder
import com.nova.funkindevplugin.resources.VSliceIcons
import javax.swing.Icon

class VSliceNewProjectWizard : LanguageGeneratorNewProjectWizard {
    override val name = "V-Slice Mod"
    override val icon: Icon = VSliceIcons.VSliceIcon

    override fun createStep(parent: NewProjectWizardStep) = Step(parent)

    class Step(parent: NewProjectWizardStep) : NewProjectWizardStep {
        override val context: WizardContext
            get() = TODO("Not yet implemented")
        override val propertyGraph: PropertyGraph
            get() = TODO("Not yet implemented")
        override val keywords: NewProjectWizardStep.Keywords
            get() = TODO("Not yet implemented")
        override val data: UserDataHolder
            get() = TODO("Not yet implemented")
    }
}