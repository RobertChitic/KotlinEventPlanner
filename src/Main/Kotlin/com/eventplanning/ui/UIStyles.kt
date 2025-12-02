package com.eventplanning.ui

import java.awt.*
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer

object UIStyles {

    // === THEME DEFINITION ===
    data class Theme(
        val isDark: Boolean,
        val background: Color,
        val cardBackground: Color,
        val inputBackground: Color,
        val sidebarBackground: Color,
        val textPrimary: Color,
        val textSecondary: Color,
        val textMuted: Color,
        val tableBorder: Color,
        val tableSelection: Color,

        // Accents
        val accentGreen: Color = Color(30, 215, 96),
        val accentBlue: Color = Color(65, 105, 225),
        val accentRed: Color = Color(225, 50, 50),
        val accentOrange: Color = Color(255, 165, 0),
        val accentPurple: Color = Color(138, 43, 226),
        val accentPink: Color = Color(255, 105, 180)
    )

    // === PRESETS ===
    private val DarkTheme = Theme(
        isDark = true,
        background = Color(18, 18, 18),
        cardBackground = Color(33, 33, 33),
        inputBackground = Color(60, 60, 60),
        sidebarBackground = Color(12, 12, 12),
        textPrimary = Color(255, 255, 255),
        textSecondary = Color(179, 179, 179),
        textMuted = Color(110, 110, 110),
        tableBorder = Color(50, 50, 50),
        tableSelection = Color(50, 50, 50)
    )

    private val LightTheme = Theme(
        isDark = false,
        background = Color(240, 242, 245),
        cardBackground = Color(255, 255, 255),
        inputBackground = Color(255, 255, 255),
        sidebarBackground = Color(12, 12, 12),
        textPrimary = Color(30, 30, 30),
        textSecondary = Color(80, 80, 80),
        textMuted = Color(120, 120, 120),
        tableBorder = Color(220, 220, 220),
        tableSelection = Color(230, 240, 255)
    )

    // === STATE MANAGEMENT ===
    var current: Theme = DarkTheme
        private set

    private val listeners = mutableListOf<() -> Unit>()

    fun addThemeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun toggleTheme() {
        current = if (current == DarkTheme) LightTheme else DarkTheme
        listeners.forEach { it() }
    }

    // === ACCESSORS (Dynamic) ===
    val background get() = current.background
    val cardBackground get() = current.cardBackground
    val inputBackground get() = current.inputBackground
    val sidebarBackground get() = current.sidebarBackground
    val textPrimary get() = current.textPrimary
    val textSecondary get() = current.textSecondary
    val textMuted get() = current.textMuted
    val tableBorder get() = current.tableBorder
    val tableSelection get() = current.tableSelection

    val accentGreen get() = current.accentGreen
    val accentBlue get() = current.accentBlue
    val accentRed get() = current.accentRed
    val accentOrange get() = current.accentOrange
    val accentPurple get() = current.accentPurple
    val accentPink get() = current.accentPink

    // === FONTS ===
    val fontHeader = Font("Segue UI", Font.BOLD, 26)
    val fontSection = Font("Segue UI", Font.BOLD, 12)
    val fontBody = Font("Segue UI", Font.PLAIN, 14)
    val fontBold = Font("Segue UI", Font.BOLD, 14)

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
            border = BorderFactory.createLineBorder(tableBorder, 1)
        }
    }

    // FIXED: Removed the call to styleComboBox() here.
    // We only set basic properties. Full styling happens in applyTheme() when L&F is ready.
    fun createComboBox(): JComboBox<Any> {
        val combo = JComboBox<Any>()
        combo.font = fontBody
        combo.background = inputBackground
        combo.foreground = textPrimary
        return combo
    }

    // FIXED: Deep styling with Custom Renderer to fix white dropdowns
    fun styleComboBox(combo: JComboBox<*>) {
        // 1. Style Main Component
        combo.background = inputBackground
        combo.foreground = textPrimary
        combo.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(tableBorder, 1),
            EmptyBorder(4, 8, 4, 8)
        )

        // 2. Custom Renderer for Dropdown Items
        combo.setRenderer(object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

                // Dynamic Item Colors
                background = if (isSelected) tableSelection else inputBackground
                foreground = textPrimary

                border = EmptyBorder(5, 10, 5, 10)
                return this
            }
        })

        // 3. Ensure editor background updates
        val editor = combo.editor.editorComponent
        if (editor is JComponent) {
            editor.background = inputBackground
            editor.foreground = textPrimary
        }

        combo.repaint()
    }

    // Buttons
    fun createPrimaryButton(text: String): JButton = createStyledButton(text, accentGreen, Color.BLACK)
    fun createSecondaryButton(text: String): JButton = createStyledButton(text, Color(100, 100, 100), Color.WHITE)
    fun createDangerButton(text: String): JButton = createStyledButton(text, accentRed, Color.WHITE)
    fun createAccentButton(text: String): JButton = createStyledButton(text, accentBlue, Color.WHITE)

    private fun createStyledButton(text: String, bgColor: Color, fgColor: Color): JButton {
        return object : JButton(text) {
            init {
                font = fontBold
                foreground = fgColor
                background = bgColor
                isFocusPainted = false
                isBorderPainted = false
                isContentAreaFilled = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                border = EmptyBorder(10, 20, 10, 20)
            }
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = if (model.isRollover) bgColor.brighter() else bgColor
                g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 20f, 20f))
                super.paintComponent(g)
            }
        }
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
            selectionForeground = if(current.isDark) accentGreen else Color.BLACK
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