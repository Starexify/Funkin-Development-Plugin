package com.nova.funkindevplugin.projects

import com.intellij.ide.wizard.LanguageNewProjectWizardData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.util.Key

interface BuildSystemVSliceNewProjectWizardData : LanguageNewProjectWizardData {

    companion object {
        val KEY = Key.create<BuildSystemVSliceNewProjectWizardData>(BuildSystemVSliceNewProjectWizardData::class.java.name)

        @JvmStatic
        val NewProjectWizardStep.vsliceBuildSystemData: BuildSystemVSliceNewProjectWizardData?
            get() = data.getUserData(KEY)
    }
}