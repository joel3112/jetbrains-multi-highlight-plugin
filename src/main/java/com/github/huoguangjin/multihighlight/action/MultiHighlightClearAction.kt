package com.github.huoguangjin.multihighlight.action

import com.github.huoguangjin.multihighlight.highlight.MultiHighlightManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil
import org.jetbrains.annotations.NotNull


class MultiHighlightClearAction : AnAction(
  "Clear in Current Editor",
  "MultiHighlight: clear highlights in current editor",
  null
) {

  init {
    setInjectedContext(true)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.getData(CommonDataKeys.EDITOR) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = InjectedLanguageEditorUtil.getTopLevelEditor(e.getRequiredData(CommonDataKeys.EDITOR))

    val multiHighlightManager = MultiHighlightManager.getInstance()
    multiHighlightManager.removeAllHighlighters(editor)
  }

  @NotNull
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}
