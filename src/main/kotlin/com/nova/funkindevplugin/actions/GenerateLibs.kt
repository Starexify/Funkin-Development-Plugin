package com.nova.funkindevplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class GenerateLibs : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        Messages.showInfoMessage("Testing", "Test")
    }
}