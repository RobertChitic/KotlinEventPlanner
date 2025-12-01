package com.eventplanning.ui

import java.awt.*
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer

object UIStyles {

    // === COLOR PALETTE ===
    val background = Color(18, 18, 18)
    val cardBackground = Color(33, 33, 33)
    val inputBackground = Color(60, 60, 60)
    val hoverOverlay = Color(255, 255, 255, 15)

    val textPrimary = Color(255, 255, 255)
    val textSecondary = Color(179, 179, 179)
    val textMuted = Color(110, 110, 110)

    val accentGreen = Color(30, 215, 96)
    val accentBlue = Color(65, 105, 225)
    val accentRed = Color(225, 50, 50)

    val tableBorder = Color(50, 50, 50)
    val tableSelection = Color(50, 50, 50)

    // === FONTS ===
    val fontHeader = Font("Segoe UI", Font.BOLD, 26)
    val fontSection = Font("Segoe UI", Font.BOLD, 12)
    val fontBody = Font("Segoe UI", Font.PLAIN, 14)
    val fontBold = Font("Segoe UI", Font.BOLD, 14)

    // === COMPONENT FACTORIES ===

    fun createHeaderLabel(text: String): JLabel {
        return JLabel(text).apply {
            font = fontHeader
            foreground = textPrimary
            border = EmptyBorder(0, 0, 20, 0)
        }
    }

    fun createSectionLabel(text: String): JLabel {
        return JLabel(text.uppercase()).apply {
            font = fontSection
            foreground = textMuted
            border = EmptyBorder(15, 0, 10, 0)
        }
    }

    fun createLabel(text: String): JLabel {
        return JLabel(text).apply {
            font = fontBody
            foreground = textSecondary
        }
    }

    fun createTextField(columns: Int = 15): JTextField {
        return JTextField(columns).apply {
            font = fontBody
            background = inputBackground
            foreground = textPrimary
            caretColor = accentGreen
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tableBorder, 1),
                EmptyBorder(8, 10, 8, 10)
            )
        }
    }

    fun createTextArea(rows: Int = 4, cols: Int = 20): JTextArea {
        return JTextArea(rows, cols).apply {
            font = fontBody
            background = inputBackground
            foreground = textPrimary
            caretColor = accentGreen
            lineWrap = true
            wrapStyleWord = true
            border = EmptyBorder(10, 10, 10, 10)
        }
    }

    // --- BUTTONS ---

    fun createPrimaryButton(text: String): JButton = createStyledButton(text, accentGreen, Color.BLACK)
    fun createSecondaryButton(text: String): JButton = createStyledButton(text, Color(80, 80, 80), textPrimary)
    fun createDangerButton(text: String): JButton = createStyledButton(text, accentRed, textPrimary)
    fun createAccentButton(text: String): JButton = createStyledButton(text, accentBlue, textPrimary)

    private fun createStyledButton(text: String, bgColor: Color, fgColor: Color): JButton {
        return object : JButton(text) {
            private var isHovering = false
            init {
                font = fontBold
                foreground = fgColor
                isFocusPainted = false
                isBorderPainted = false
                isContentAreaFilled = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                border = EmptyBorder(10, 20, 10, 20)
                addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseEntered(e: java.awt.event.MouseEvent) { isHovering = true; repaint() }
                    override fun mouseExited(e: java.awt.event.MouseEvent) { isHovering = false; repaint() }
                })
            }
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = if (isHovering) bgColor.brighter() else bgColor
                g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 20f, 20f))
                super.paintComponent(g)
            }
        }
    }

    // --- NEW: COMBO BOX ---
    fun createComboBox(): JComboBox<Any> {
        val combo = JComboBox<Any>()
        combo.font = fontBody
        combo.background = inputBackground
        combo.foreground = textPrimary
        combo.border = BorderFactory.createLineBorder(tableBorder, 1)
        return combo
    }

    // --- CARDS & TABLES ---

    fun createCardPanel(): JPanel {
        return object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = cardBackground
                g2.fillRoundRect(0, 0, width, height, 16, 16)
            }
        }.apply {
            isOpaque = false
            border = EmptyBorder(20, 20, 20, 20)
        }
    }

    fun styleTable(table: JTable) {
        table.apply {
            background = cardBackground
            foreground = textPrimary
            selectionBackground = tableSelection
            selectionForeground = accentGreen
            gridColor = tableBorder
            rowHeight = 40
            font = fontBody
            setShowGrid(false)
            setShowHorizontalLines(true)
            intercellSpacing = Dimension(0, 1)
            fillsViewportHeight = true

            tableHeader.apply {
                background = cardBackground
                foreground = textMuted
                font = fontSection
                border = BorderFactory.createMatteBorder(0, 0, 2, 0, tableBorder)
                (defaultRenderer as? DefaultTableCellRenderer)?.horizontalAlignment = SwingConstants.LEFT
            }
        }
    }

    fun createScrollPane(view: Component): JScrollPane {
        return JScrollPane(view).apply {
            border = BorderFactory.createLineBorder(tableBorder)
            viewport.background = cardBackground
            background = cardBackground
        }
    }
}