package com.github.huoguangjin.multihighlight.ui

import com.intellij.ui.JBColor
import java.awt.*
import java.awt.geom.Ellipse2D
import javax.swing.Icon

class ColoredCircleIcon(private var backgroundColor: Color) : Icon {
  private val ICON_WIDTH = 16
  private val ICON_HEIGHT = 16
  private val ICON_STROKE_WIDTH = 0.3f
  private val MARK_WIDTH = 5
  private val MARK_HEIGHT = 5
  private val MARK_STROKE_WIDTH = 0.6f

  companion object {
    private val iconsCache: MutableMap<String, ColoredCircleIcon> = HashMap()

    fun getInstance(
      backgroundColor: Color
    ): ColoredCircleIcon? {
      val hashCode = "#" + backgroundColor.hashCode()
      if (!ColoredCircleIcon.iconsCache.containsKey(hashCode)) {
        ColoredCircleIcon.iconsCache[hashCode] = ColoredCircleIcon(backgroundColor)
      }
      return ColoredCircleIcon.iconsCache[hashCode]
    }
  }


  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2d = g.create() as Graphics2D

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

    val circle: Shape = Ellipse2D.Double(
      (x + 1).toDouble(),
      (y + 1).toDouble(),
      (ICON_WIDTH - 2).toDouble(),
      (ICON_HEIGHT - 2).toDouble()
    )

    g2d.color = backgroundColor
    g2d.fill(circle)

    val strokeColor: Color = JBColor.namedColor("Tree.foreground", JBColor(0x555555, 0xbbbbbb))
    g2d.stroke = BasicStroke(ICON_STROKE_WIDTH)
    g2d.color = strokeColor
    g2d.draw(circle)

    g2d.stroke = BasicStroke(MARK_STROKE_WIDTH)
    g2d.dispose()
  }

  override fun getIconWidth(): Int {
    return ICON_WIDTH
  }

  override fun getIconHeight(): Int {
    return ICON_HEIGHT
  }
}
