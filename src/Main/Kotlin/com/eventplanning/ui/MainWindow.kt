package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.service.ScalaBridge
import com.formdev.flatlaf.FlatDarkLaf
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.EmptyBorder

class MainWindow(private val eventManager: EventManager) {

    private val frame = JFrame("Event Planner")
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)
    private val navGroup = ButtonGroup()
    private val navButtons = mutableListOf<JToggleButton>()

    // Load panels
    private val statsPanel = StatisticsPanel(eventManager)
    private val venuePanel = VenuePanel(eventManager)
    private val eventPanel = EventPanel(eventManager)
    private val participantPanel = ParticipantPanel(eventManager)
    private val registrationPanel = RegistrationPanel(eventManager)

    fun show() {
        setupFrame()
        setupLayout()
        cardLayout.show(contentPanel, "Dashboard")
        frame.isVisible = true
    }

    private fun setupFrame() {
        // Force Dark Theme
        try { FlatDarkLaf.setup() } catch (e: Exception) {}

        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(1380, 850)
        frame.setLocationRelativeTo(null)
    }

    private fun setupLayout() {
        frame.layout = BorderLayout()

        val sidebar = createSidebar()

        contentPanel.background = UIStyles.background
        contentPanel.border = EmptyBorder(30, 40, 30, 40)
        contentPanel.add(eventPanel, "Events")
        contentPanel.add(venuePanel, "Venues")
        contentPanel.add(participantPanel, "Participants")
        contentPanel.add(registrationPanel, "Registration")
        contentPanel.add(statsPanel, "Analytics")

        frame.add(sidebar, BorderLayout.WEST)
        frame.add(contentPanel, BorderLayout.CENTER)
    }

    private fun createSidebar(): JPanel {
        val sidebar = object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                g.color = Color(12, 12, 12) // Darker than main bg
                g.fillRect(0, 0, width, height)
            }
        }
        sidebar.preferredSize = Dimension(260, 0)
        sidebar.border = BorderFactory.createMatteBorder(0, 0, 0, 1, UIStyles.tableBorder)

        // Branding
        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT, 25, 35))
        topPanel.isOpaque = false
        val brandLabel = JLabel("Event Planner")
        brandLabel.font = UIStyles.fontHeader
        brandLabel.foreground = UIStyles.textPrimary
        topPanel.add(brandLabel)

        // Navigation
        val navPanel = JPanel()
        navPanel.layout = BoxLayout(navPanel, BoxLayout.Y_AXIS)
        navPanel.isOpaque = false
        navPanel.border = EmptyBorder(10, 15, 10, 15)

        val menuLabel = JLabel("MENU")
        menuLabel.font = UIStyles.fontSection
        menuLabel.foreground = UIStyles.textMuted
        menuLabel.border = EmptyBorder(0, 10, 15, 0)
        navPanel.add(menuLabel)

        fun addNav(text: String, target: String, default: Boolean = false) {
            val btn = object : JToggleButton(text) {
                init {
                    actionCommand = target
                    horizontalAlignment = SwingConstants.LEFT
                    isFocusPainted = false
                    isBorderPainted = false
                    isContentAreaFilled = false
                    isOpaque = false
                    border = EmptyBorder(12, 20, 12, 20)
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    font = UIStyles.fontBold
                    foreground = UIStyles.textSecondary
                    maximumSize = Dimension(Int.MAX_VALUE, 50)
                    alignmentX = Component.LEFT_ALIGNMENT
                    if (default) { isSelected = true; foreground = UIStyles.textPrimary }
                }
                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    if (isSelected) {
                        g2.color = UIStyles.cardBackground
                        g2.fillRoundRect(0, 0, width, height, 10, 10)
                        g2.color = UIStyles.accentGreen
                        g2.fillRoundRect(0, 10, 4, height - 20, 4, 4)
                        foreground = UIStyles.textPrimary
                    } else if (model.isRollover) {
                        g2.color = UIStyles.hoverOverlay
                        g2.fillRoundRect(0, 0, width, height, 10, 10)
                    }
                    super.paintComponent(g)
                }
            }
            btn.addActionListener { e ->
                cardLayout.show(contentPanel, e.actionCommand)
                if (e.actionCommand == "Dashboard") statsPanel.refreshStats()
                navButtons.forEach { it.repaint() }
            }
            navGroup.add(btn)
            navButtons.add(btn)
            navPanel.add(btn)
            navPanel.add(Box.createVerticalStrut(5))
        }

        addNav("Events", "Events", true)
        addNav("Venues", "Venues")
        addNav("Participants", "Participants")
        addNav("Registration", "Registration")
        addNav("Analytics", "Analytics")

        // Actions
        val bottomPanel = JPanel()
        bottomPanel.layout = BoxLayout(bottomPanel, BoxLayout.Y_AXIS)
        bottomPanel.isOpaque = false
        bottomPanel.border = EmptyBorder(20, 25, 40, 25)

        fun addAction(text: String, action: ActionListener) {
            val btn = JButton(text)
            btn.horizontalAlignment = SwingConstants.LEFT
            btn.alignmentX = Component.LEFT_ALIGNMENT
            btn.isContentAreaFilled = false
            btn.isBorderPainted = false
            btn.foreground = UIStyles.textSecondary
            btn.font = UIStyles.fontBody
            btn.border = EmptyBorder(8, 0, 8, 0)
            btn.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            btn.addActionListener(action)
            btn.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) { btn.foreground = UIStyles.textPrimary }
                override fun mouseExited(e: MouseEvent) { btn.foreground = UIStyles.textSecondary }
            })
            bottomPanel.add(btn)
        }

        addAction("Generate Schedule") { generateSchedule() }
        addAction("Save Database") { saveAllData() }
        addAction("Exit Application") { exitApplication() }

        sidebar.add(topPanel, BorderLayout.NORTH)
        sidebar.add(navPanel, BorderLayout.CENTER)
        sidebar.add(bottomPanel, BorderLayout.SOUTH)

        return sidebar
    }

    // --- LOGIC ---
    private fun saveAllData() {
        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground(): Boolean = eventManager.saveAllData()
            override fun done() {
                if (get()) JOptionPane.showMessageDialog(frame, "Saved successfully.", "System", JOptionPane.INFORMATION_MESSAGE)
                else JOptionPane.showMessageDialog(frame, "Save failed.", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
        worker.execute()
    }

    private fun exitApplication() {
        if (JOptionPane.showConfirmDialog(frame, "Save before exiting?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            eventManager.saveAllData()
        }
        System.exit(0)
    }

    private fun generateSchedule() {
        val events = eventManager.getAllEvents()
        val venues = eventManager.getAllVenues()
        if (events.isEmpty()) { JOptionPane.showMessageDialog(frame, "No events.", "Info", JOptionPane.INFORMATION_MESSAGE); return }

        val worker = object : SwingWorker<ScalaBridge.SchedulerResult, Void>() {
            override fun doInBackground() = ScalaBridge.generateSchedule(events, venues)
            override fun done() { displayScheduleResult(get()) }
        }
        worker.execute()
    }

    private fun displayScheduleResult(result: ScalaBridge.SchedulerResult) {
        when (result) {
            is ScalaBridge.SchedulerResult.Success -> {
                val message = buildString {
                    appendLine("AUTO-SCHEDULE RESULTS")
                    appendLine("--------------------------------")
                    result.schedule.forEach { e -> appendLine("${e.eventTitle} @ ${e.venue} [${e.dateTime}]") }
                }
                val textArea = UIStyles.createTextArea(15, 40).apply {
                    text = message
                    isEditable = false
                }
                JOptionPane.showMessageDialog(frame, UIStyles.createScrollPane(textArea), "Schedule", JOptionPane.PLAIN_MESSAGE)
            }
            is ScalaBridge.SchedulerResult.Error -> JOptionPane.showMessageDialog(frame, result.message, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
}