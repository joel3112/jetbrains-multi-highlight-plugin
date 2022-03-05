package com.github.huoguangjin.multihighlight.highlight

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.MarkupIterator
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Segment
import com.intellij.openapi.util.TextRange
import java.util.*
import kotlin.Comparator

class MultiHighlightManager(
  private val project: Project
) {

  private inline fun MarkupModelEx.useOverlappingIterator(
    startOffset: Int,
    endOffset: Int,
    block: (MarkupIterator<RangeHighlighterEx>) -> Unit
  ) {
    val iterator = overlappingIterator(startOffset, endOffset)
    try {
      return block(iterator)
    } finally {
      iterator.dispose()
    }
  }

  fun findHighlightAtCaret(editor: Editor): RangeHighlighter? {
    val map = getHighlightInfo(editor, false) ?: return null

    val caret = editor.caretModel.currentCaret
    val startOffset: Int
    val endOffset: Int
    if (caret.hasSelection()) {
      startOffset = caret.selectionStart
      endOffset = caret.selectionEnd
    } else {
      startOffset = caret.offset
      endOffset = startOffset
    }

    val markupModel = editor.markupModel as MarkupModelEx
    markupModel.useOverlappingIterator(startOffset, endOffset) {
      it.forEach { highlighter ->
        if (highlighter in map) {
          return highlighter
        }
      }
    }

    return null
  }

  fun isClearHighlights(editor: Editor): Boolean = findHighlightAtCaret(editor) != null

  fun addHighlighters(editor: Editor, textAttr: TextAttributes, textRanges: Collection<TextRange>) {
    val map = getHighlightInfo(editor, true)!!
    val info = MultiHighlightInfo(editor)

    val markupModel = editor.markupModel
    textRanges.forEach { textRange ->
      val highlighter = markupModel.addRangeHighlighter(
        textRange.startOffset,
        textRange.endOffset,
        MULTIHIGHLIGHT_LAYER,
        textAttr,
        HighlighterTargetArea.EXACT_RANGE
      )

      map[highlighter] = info
    }
  }

  fun removeHighlighters(editor: Editor, textRanges: MutableList<TextRange>) {
    val highlighters = getHighlighters(editor)
    if (highlighters.isEmpty()) {
      return
    }

    highlighters.sortWith(Comparator.comparingInt(Segment::getStartOffset))
    textRanges.sortWith(Comparator.comparingInt(Segment::getStartOffset))

    var i = 0
    var j = 0
    while (i < highlighters.size && j < textRanges.size) {
      val highlighter = highlighters[i]
      val highlighterRange = TextRange.create(highlighter)
      val textRange = textRanges[j]

      if (textRange == highlighterRange) {
        removeHighlighter(editor, highlighter)
        i++
      } else if (textRange.startOffset >= highlighterRange.endOffset) {
        i++
      } else if (textRange.endOffset <= highlighterRange.startOffset) {
        j++
      } else {
        i++
        j++
      }
    }
  }

  fun removeHighlighter(editor: Editor, highlighter: RangeHighlighter): Boolean {
    val map = getHighlightInfo(editor, false) ?: return false
    val info = map[highlighter] ?: return false

    val markupModel = info.editor.markupModel as MarkupModelEx
    if (markupModel.containsHighlighter(highlighter)) {
      highlighter.dispose()
    }

    map.remove(highlighter)
    return true
  }

  fun getHighlighters(editor: Editor): Array<out RangeHighlighter> {
    val map = getHighlightInfo(editor, false) ?: return RangeHighlighter.EMPTY_ARRAY
    return map.keys.toTypedArray()
  }

  private fun getHighlightInfo(editor: Editor, toCreate: Boolean): MutableMap<RangeHighlighter, MultiHighlightInfo>? {
    var map = editor.getUserData(MULTIHIGHLIGHT_INFO_KEY)

    if (map == null && toCreate) {
      map = WeakHashMap()
      editor.putUserData(MULTIHIGHLIGHT_INFO_KEY, map)
    }

    return map
  }

  class MultiHighlightInfo(val editor: Editor)

  companion object {

    const val MULTIHIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 1

    private val MULTIHIGHLIGHT_INFO_KEY: Key<MutableMap<RangeHighlighter, MultiHighlightInfo>> =
      Key.create("MULTIHIGHLIGHT_INFO_KEY")

    fun getInstance(project: Project): MultiHighlightManager = project.service()
  }
}
