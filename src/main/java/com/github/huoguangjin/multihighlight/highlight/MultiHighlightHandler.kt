package com.github.huoguangjin.multihighlight.highlight

import com.github.huoguangjin.multihighlight.config.TextAttributesFactory
import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.model.psi.impl.targetSymbols
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiCompiledFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil
import java.text.MessageFormat

class MultiHighlightHandler(
  private val project: Project,
  private val editor: Editor,
  private val psiFile: PsiFile,
) {

  fun highlight() {
    if (tryRemoveHighlighters()) {
      return
    }

    if (highlightCustomUsages()) {
      return
    }

    DumbService.getInstance(project).withAlternativeResolveEnabled {
      if (highlightSymbols()) {
        return@withAlternativeResolveEnabled
      }

      MultiHighlightTextHandler(project, editor, psiFile).highlight()
    }
  }

  fun tryRemoveHighlighters(): Boolean {
    val editor = InjectedLanguageEditorUtil.getTopLevelEditor(editor)

    val multiHighlightManager = MultiHighlightManager.getInstance()
    val highlighter = multiHighlightManager.findHighlightAtCaret(editor) ?: return false

    multiHighlightManager.removeHighlighters(editor, highlighter)
    return true
  }

  fun highlightCustomUsages(): Boolean {
    val handler = HighlightUsagesHandler.createCustomHandler<PsiElement>(editor, psiFile) ?: return false

    handler.featureId?.let(FeatureUsageTracker.getInstance()::triggerFeatureUsed)

    val textRanges = thisLogger().runAndLogException {
      HighlightUsagesHandlerHelper.findUsages(handler)
    }
    if (textRanges == null) {
      // fallback to highlight by HighlightUsagesHandlerBase
      handler.highlightUsages()
      return true
    }

    val multiHighlightManager = MultiHighlightManager.getInstance()
    highlightTextRanges(multiHighlightManager, editor, textRanges)
    return true
  }

  fun highlightSymbols(): Boolean {
    @Suppress("UnstableApiUsage")
    val allTargets = targetSymbols(psiFile, editor.caretModel.offset)
    if (allTargets.isEmpty()) {
      return false
    }

    val editor = InjectedLanguageEditorUtil.getTopLevelEditor(editor)
    var file = if (psiFile is PsiCompiledFile) psiFile.decompiledPsiFile else psiFile
    file = InjectedLanguageManager.getInstance(project).getTopLevelFile(file)

    val multiHighlightManager = MultiHighlightManager.getInstance()
    for (target in allTargets) {
      val textRanges = HighlightUsagesHelper.getUsageRanges(file, target)
      highlightTextRanges(multiHighlightManager, editor, textRanges)
    }

    return true
  }

  fun highlightTextRanges(
    multiHighlightManager: MultiHighlightManager,
    editor: Editor,
    textRanges: MutableList<TextRange>,
  ) {
    val textAttr = TextAttributesFactory.getNextTextAttr()
    multiHighlightManager.addHighlighters(editor, textAttr, textRanges)

    val highlightCount = textRanges.size
    WindowManager.getInstance().getStatusBar(project).info = if (highlightCount > 0) {
      MessageFormat.format("{0} {0, choice, 1#usage|2#usages} highlighted", highlightCount)
    } else {
      "No usages highlighted"
    }
  }
}
