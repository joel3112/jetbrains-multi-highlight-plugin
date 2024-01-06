package com.github.huoguangjin.multihighlight.action

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator

class MultiHighlightActionGroup : ActionGroup() {
    override fun getChildren(actionEvent: AnActionEvent?): Array<AnAction> {
        val actionGroupItems = ArrayList<AnAction>()

        actionGroupItems.add(MultiHighlightWithColorAction())
        actionGroupItems.add(Separator())
        actionGroupItems.add(MultiHighlightAction())
        actionGroupItems.add(MultiHighlightClearAction())

        return actionGroupItems.toTypedArray()
    }

}
