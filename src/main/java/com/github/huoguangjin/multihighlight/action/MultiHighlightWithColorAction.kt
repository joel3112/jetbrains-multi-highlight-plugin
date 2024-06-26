package com.github.huoguangjin.multihighlight.action

import com.github.huoguangjin.multihighlight.config.NamedTextAttr
import com.github.huoguangjin.multihighlight.config.TextAttributesFactory
import com.github.huoguangjin.multihighlight.highlight.MultiHighlightHandler
import com.github.huoguangjin.multihighlight.highlight.MultiHighlightManager
import com.github.huoguangjin.multihighlight.highlight.MultiHighlightTextHandler
import com.github.huoguangjin.multihighlight.ui.ColoredCircleIcon
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.popup.WizardPopup
import org.jetbrains.annotations.NotNull
import java.awt.Color
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Icon

class MultiHighlightWithColorAction(val textAttr: NamedTextAttr, icon: ColoredCircleIcon) : AnAction(
  textAttr.name,
  "MultiHighlight: toggle highlight at caret with color ${textAttr.name}",
  icon
) {

  init {
    setInjectedContext(true)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project != null && e.getData(CommonDataKeys.EDITOR) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getRequiredData(CommonDataKeys.PROJECT)
    val editor = e.getRequiredData(CommonDataKeys.EDITOR)

    CommandProcessor.getInstance().executeCommand(project, {
      PsiDocumentManager.getInstance(project).commitAllDocuments()

      val multiHighlightManager = MultiHighlightManager.getInstance()
      if (multiHighlightManager.tryRemoveHighlighterAtCaret(editor)) {
        return@executeCommand
      }
//      val namedTextAttrs = TextAttributesFactory.getTextAttrs()
//      val colorList = namedTextAttrs.mapIndexed(::NamedTextAttrItem)
//      val listPopupStep = ColorListPopupStep(project, editor, "Highlight with color..", colorList).apply {
//        defaultOptionIndex = TextAttributesFactory.getNextTextAttrIndex()
//      }
//      JBPopupFactory.getInstance()
//        .createListPopup(listPopupStep).also(::addKeyStrokeAction)
//        .showInBestPositionFor(editor)

      try {
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        val selectionModel = editor.selectionModel

        if (psiFile != null && !selectionModel.hasSelection()) {
          MultiHighlightHandler(project, editor, psiFile, textAttr).highlight()
        } else {
          MultiHighlightTextHandler(project, editor, textAttr).highlight()
        }
      } catch (ex: IndexNotReadyException) {
        DumbService.getInstance(project)
          .showDumbModeNotification("MultiHighlight requires indices and cannot be performed until they are built")
      }
    }, "MultiHighlight", null)
  }

  private fun addKeyStrokeAction(listPopup: ListPopup) {
    if (listPopup !is WizardPopup) {
      return
    }

    val actionId = "MultiHighlightWithColor"
    val shortcuts = KeymapUtil.getActiveKeymapShortcuts(actionId).shortcuts // todo
    for (shortcut in shortcuts) {
      if (shortcut !is KeyboardShortcut || shortcut.secondKeyStroke != null) {
        continue
      }

      val keyStroke = shortcut.firstKeyStroke
      listPopup.registerAction(actionId, keyStroke, object : AbstractAction(actionId) {
        override fun actionPerformed(e: ActionEvent?) {
          listPopup.handleSelect(true)
        }
      })
    }
  }

  @NotNull
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}

private class ColorListPopupStep(
  private val project: Project,
  private val editor: Editor,
  title: String?,
  colorList: List<NamedTextAttrItem>,
) : BaseListPopupStep<NamedTextAttrItem>(title, colorList) {

  private var finalRunnable: Runnable? = null

  override fun isSpeedSearchEnabled() = true

  override fun getForegroundFor(item: NamedTextAttrItem): Color? = item.textAttr.foregroundColor

  override fun getBackgroundFor(item: NamedTextAttrItem): Color? = item.textAttr.backgroundColor

  override fun getSelectedIconFor(value: NamedTextAttrItem): Icon = AllIcons.Actions.Execute

  override fun onChosen(selectedValue: NamedTextAttrItem, finalChoice: Boolean): PopupStep<*>? {
    if (!finalChoice) {
      return super.onChosen(selectedValue, finalChoice)
    }

    finalRunnable = Runnable {
      try {
        val textAttr = selectedValue.textAttr

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        val selectionModel = editor.selectionModel

        if (psiFile != null && !selectionModel.hasSelection()) {
          MultiHighlightHandler(project, editor, psiFile, textAttr).highlight()
        } else {
          MultiHighlightTextHandler(project, editor, textAttr).highlight()
        }

        if (defaultOptionIndex == TextAttributesFactory.getNextTextAttrIndex()) {
          TextAttributesFactory.advanceTextAttrIndex()
        }
      } catch (ex: IndexNotReadyException) {
        DumbService.getInstance(project)
          .showDumbModeNotification("MultiHighlight requires indices and cannot be performed until they are built")
      }
    }

    return FINAL_CHOICE
  }

  override fun getFinalRunnable(): Runnable? = finalRunnable
}

private class NamedTextAttrItem(
  val index: Int,
  val textAttr: NamedTextAttr,
) {
  override fun toString() = "$index ${textAttr.name}"
}
