package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.service.ScalaBridge
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.FlatLaf
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class MainWindow(private val eventManager: EventManager) {

    private val frame = JFrame("Event Planner")
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)

    // Panels
    private val statsPanel = StatisticsPanel(eventManager)
    private val venuePanel = VenuePanel(eventManager)
    private val eventPanel = EventPanel(eventManager)
    private val participantPanel = ParticipantPanel(eventManager)
    private val registrationPanel = RegistrationPanel(eventManager)

    private val navButtons = mutableListOf<JToggleButton>()
    private val navGroup = ButtonGroup()
    private val sidebar = createSidebar()

    fun show() {
        setupFrame()
        setupLayout()

        // Listen for theme changes
        UIStyles.addThemeListener { applyTheme() }

        // FIXED: Open "Events" by default instead of Analytics
        cardLayout.show(contentPanel, "Events")
        frame.isVisible = true
    }

    private fun setupFrame() {
        FlatDarkLaf.setup()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(1380, 850)
        frame.setLocationRelativeTo(null)
    }

    private fun setupLayout() {
        frame.layout = BorderLayout()
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

    private fun applyTheme() {
        // 1. Update the Library Theme (FlatLaf)
        if (UIStyles.current.isDark) FlatDarkLaf.setup() else FlatLightLaf.setup()

        // 2. Refresh the whole component tree
        FlatLaf.updateUI()
        SwingUtilities.updateComponentTreeUI(frame)

        // 3. RE-APPLY Custom Colors
        contentPanel.background = UIStyles.background
        sidebar.repaint()

        // 4. Update Navigation Button Colors
        navButtons.forEach {
            if (!it.isSelected) it.foreground = UIStyles.textSecondary
            else it.foreground = Color.WHITE
        }

        // 5. Push Theme Down to Panels
        eventPanel.applyTheme()
        venuePanel.applyTheme()
        participantPanel.applyTheme()
        registrationPanel.applyTheme()
        statsPanel.applyTheme()

        frame.repaint()
    }

    private fun createSidebar(): JPanel {
        val sidebarPanel = object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                g.color = UIStyles.sidebarBackground
                g.fillRect(0, 0, width, height)
            }
        }
        sidebarPanel.preferredSize = Dimension(260, 0)

        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT, 25, 35))
        topPanel.isOpaque = false
        val brandLabel = JLabel("Event Planner")
        brandLabel.font = UIStyles.fontHeader
        brandLabel.foreground = Color.WHITE
        topPanel.add(brandLabel)

        val navPanel = JPanel()
        navPanel.layout = BoxLayout(navPanel, BoxLayout.Y_AXIS)
        navPanel.isOpaque = false
        navPanel.border = EmptyBorder(10, 15, 10, 15)

        val menuLabel = JLabel("MENU")
        menuLabel.font = UIStyles.fontSection
        menuLabel.foreground = Color(150, 150, 180)
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
                    foreground = if(default) Color.WHITE else UIStyles.textSecondary
                    maximumSize = Dimension(Int.MAX_VALUE, 50)
                    alignmentX = Component.LEFT_ALIGNMENT
                    if (default) isSelected = true
                }
                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    if (isSelected) {
                        g2.color = UIStyles.accentBlue
                        g2.fillRoundRect(0, 0, width, height, 10, 10)
                        foreground = Color.WHITE
                    } else {
                        foreground = Color(200, 200, 200)
                        if (model.isRollover) {
                            g2.color = Color(255, 255, 255, 20)
                            g2.fillRoundRect(0, 0, width, height, 10, 10)
                            foreground = Color.WHITE
                        }
                    }
                    super.paintComponent(g)
                }
            }
            btn.addActionListener { e ->
                cardLayout.show(contentPanel, e.actionCommand)
                if (e.actionCommand == "Analytics") statsPanel.refreshStats()
                navButtons.forEach { it.repaint() }
            }
            navButtons.add(btn)
            navGroup.add(btn)
            navPanel.add(btn)
            navPanel.add(Box.createVerticalStrut(5))
        }

        addNav("Events", "Events", true) // Default TRUE
        addNav("Venues", "Venues")
        addNav("Participants", "Participants")
        addNav("Registration", "Registration")
        addNav("Analytics", "Analytics")

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
            btn.foreground = Color(200, 200, 200)
            btn.font = UIStyles.fontBody
            btn.border = EmptyBorder(8, 0, 8, 0)
            btn.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            btn.addActionListener(action)
            btn.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) { btn.foreground = Color.WHITE }
                override fun mouseExited(e: MouseEvent) { btn.foreground = Color(200, 200, 200) }
            })
            bottomPanel.add(btn)
        }

        addAction("Generate Schedule") { generateSchedule() }
        addAction("Toggle Theme") { UIStyles.toggleTheme() }
        addAction("Save Database") { saveAllData() }
        addAction("Exit Application") { exitApplication() }

        sidebarPanel.add(topPanel, BorderLayout.NORTH)
        sidebarPanel.add(navPanel, BorderLayout.CENTER)
        sidebarPanel.add(bottomPanel, BorderLayout.SOUTH)

        return sidebarPanel
    }

    // --- LOGIC ---
    private fun saveAllData() {
        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground(): Boolean = eventManager.saveAllData()
            override fun done() {
                if (get()) JOptionPane.showMessageDialog(frame, "Saved successfully.")
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
        if (events.isEmpty()) { JOptionPane.showMessageDialog(frame, "No events."); return }

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
                val textArea = UIStyles.createTextArea(15, 40).apply { text = message; isEditable = false }
                JOptionPane.showMessageDialog(frame, UIStyles.createScrollPane(textArea), "Schedule", JOptionPane.PLAIN_MESSAGE)
            }
            is ScalaBridge.SchedulerResult.Error -> JOptionPane.showMessageDialog(frame, result.message, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
}