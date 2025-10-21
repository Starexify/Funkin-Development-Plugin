package com.nova.funkindevplugin.projects

import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.openapi.extensions.ExtensionPointName

interface BuildSystemVSliceNewProjectWizard : NewProjectWizardMultiStepFactory<VSliceNewProjectWizard.Step> {
    companion object {
        const val DEFAULT_VSLICE_VERSION: String = "0.7.5"

        val EP_NAME: ExtensionPointName<BuildSystemVSliceNewProjectWizard> = ExtensionPointName<BuildSystemVSliceNewProjectWizard>("com.nova.FunkinDevPlugin.vslice.buildSystem")
    }
}