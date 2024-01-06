package com.github.huoguangjin.multihighlight.action

import com.github.huoguangjin.multihighlight.config.MultiHighlightConfig
import com.github.huoguangjin.multihighlight.ui.ColoredCircleIcon
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator

class MultiHighlightActionGroup : ActionGroup() {
  override fun getChildren(actionEvent: AnActionEvent?): Array<AnAction> {
    val actionGroupItems = ArrayList<AnAction>()

    actionGroupItems.addAll(getMultiHighlightActions())
    actionGroupItems.add(Separator())
    actionGroupItems.add(MultiHighlightAction())
    actionGroupItems.add(MultiHighlightClearAction())

    return actionGroupItems.toTypedArray()
  }

  private fun getMultiHighlightActions(): ArrayList<AnAction> {
    val multiHighlightActions = ArrayList<AnAction>()
    val items = MultiHighlightConfig.getInstance().namedTextAttrs

    items.forEach {
      val action = MultiHighlightWithColorAction(
        it,
        ColoredCircleIcon.getInstance(it.backgroundColor)!!
      )
      multiHighlightActions.add(action)
    }

    return multiHighlightActions
  }

}
