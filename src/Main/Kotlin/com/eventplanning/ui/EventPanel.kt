// ... imports remain the same ...
package com.eventplanning.ui

import com.eventplanning.domain.Event
import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import com.eventplanning.service.ScalaBridge
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter
import java.awt.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID
import java.awt.event.ItemEvent

// ... Class definition starts ...
class EventPanel(
    private val eventManager: EventManager
) : JPanel() {

    // ... (Properties and Init block remain the same as previous correct version) ...
    private var displayedEvents: List<Event> = emptyList()
    private var editingEventId: String? = null
    private val tableModel = object : DefaultTableModel(arrayOf("Title", "Date", "Time", "Venue", "Occupancy"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    // ... (Table setup logic same as before) ...
    private val eventTable = object : JTable(tableModel) {
        override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
            val component = super.prepareRenderer(renderer, row, column)
            if (!isRowSelected(row)) {
                val modelRow = convertRowIndexToModel(row)
                val event = displayedEvents.getOrNull(modelRow)
                if (event != null && event.isFull()) {
                    component.background = Color(255, 235, 235)
                } else {
                    component.background = Color.WHITE
                }
            }
            return component
        }
    }
    private val tableSorter = TableRowSorter(tableModel)
    private val searchField = JTextField(15)
    private val titleField = JTextField(20)
    private val startDateSpinner = JSpinner(SpinnerDateModel())
    private val hoursSpinner = JSpinner(SpinnerNumberModel(2, 0, 24, 1))
    private val minutesSpinner = JSpinner(SpinnerNumberModel(0, 0, 59, 15))
    private val venueCombo = JComboBox<VenueItem>()
    private val venueInfoLabel = JLabel("Select a venue...")
    private val descriptionArea = JTextArea(5, 20)
    private val maxParticipantsField = JTextField(10)
    private val createButton = JButton("Create Event")
    private val clearButton = JButton("Clear")
    private val editButton = JButton("Edit Event")
    private val findVenueBtn = JButton("Find Slot")
    private val deleteButton = JButton("Delete Event")

    init {
        layout = BorderLayout()
        eventTable.rowSorter = tableSorter
        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        eventTable.rowHeight = 30
        eventTable.showHorizontalLines = true
        eventTable.gridColor = Color.LIGHT_GRAY
        eventTable.fillsViewportHeight = true
        eventTable.columnModel.getColumn(4).cellRenderer = CapacityCellRenderer()
        searchField.putClientProperty("JTextField.placeholderText", "🔍 Filter...")
        venueInfoLabel.foreground = Color.GRAY
        deleteButton.apply { foreground = Color.WHITE; background = Color(220, 53, 69); isOpaque = true }
        editButton.apply { foreground = Color.WHITE; background = Color(65, 105, 225); isOpaque = true }

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = createTablePanel()
        splitPane.rightComponent = createFormPanel()
        splitPane.dividerLocation = 600
        splitPane.resizeWeight = 0.7
        add(splitPane, BorderLayout.CENTER)
        setupListeners()
        refreshEventTable()
        refreshVenueCombo()
    }

    // ... (CapacityCellRenderer, createTablePanel, createFormPanel, setupListeners, loadEventForEditing, updateEvent, refreshEventTable, createEvent, deleteSelectedEvent, refreshVenueCombo, clearFields SAME AS BEFORE) ...

    // I am omitting the duplicate methods to focus on the changed findAvailableVenue method below.
    // Assume all other methods (UI creation, CRUD operations) are exactly as provided in the previous correct response.

    private class CapacityCellRenderer : DefaultTableCellRenderer() {
        private val progressBar = JProgressBar(0, 100)
        init { progressBar.isStringPainted = true; progressBar.border = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3), BorderFactory.createLineBorder(Color.LIGHT_GRAY)); progressBar.isOpaque = true }
        override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
            if (isSelected) progressBar.background = table?.selectionBackground else progressBar.background = Color.WHITE
            val strValue = value as? String ?: "0/0"
            try { val parts = strValue.split("/"); if (parts.size == 2) { val c = parts[0].toInt(); val m = parts[1].toInt(); progressBar.maximum = m; progressBar.value = c; progressBar.string = "$c / $m"; val p = if (m > 0) (c.toDouble() / m * 100).toInt() else 0; progressBar.foreground = when { p >= 100 -> Color(220, 53, 69); p >= 75 -> Color(255, 193, 7); else -> Color(40, 167, 69) } } } catch (e: Exception) { progressBar.string = strValue }
            return progressBar
        }
    }
    private fun createTablePanel(): JPanel { val p = JPanel(BorderLayout()); p.border = BorderFactory.createTitledBorder("Event Schedule"); val t = JPanel(FlowLayout(FlowLayout.LEFT)); t.add(JLabel("Filter:")); t.add(searchField); p.add(t, BorderLayout.NORTH); p.add(JScrollPane(eventTable), BorderLayout.CENTER); val b = JPanel(FlowLayout(FlowLayout.LEFT)); b.add(editButton); b.add(Box.createHorizontalStrut(10)); b.add(deleteButton); p.add(b, BorderLayout.SOUTH); return p }
    private fun createFormPanel(): JPanel { val p = JPanel(GridBagLayout()); p.border = BorderFactory.createTitledBorder("Event Details"); val c = GridBagConstraints(); c.insets = Insets(5, 5, 5, 5); c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL; var y = 0; fun r(l: String, o: JComponent) { c.gridx = 0; c.gridy = y; c.weightx = 0.0; p.add(JLabel(l), c); c.gridx = 1; c.weightx = 1.0; p.add(o, c); y++ }; r("Title:", titleField); val se = JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd HH:mm"); startDateSpinner.editor = se; startDateSpinner.value = Date(); r("Start Time:", startDateSpinner); val dp = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)); dp.add(hoursSpinner); dp.add(JLabel(" h ")); dp.add(minutesSpinner); dp.add(JLabel(" m")); r("Duration:", dp); r("Venue:", venueCombo); c.gridx = 1; c.gridy = y; p.add(venueInfoLabel, c); y++; r("Capacity:", maxParticipantsField); descriptionArea.lineWrap = true; descriptionArea.wrapStyleWord = true; val sd = JScrollPane(descriptionArea); c.gridx = 0; c.gridy = y; c.gridwidth = 2; c.weighty = 1.0; c.fill = GridBagConstraints.BOTH; p.add(sd, c); y++; val bp = JPanel(FlowLayout(FlowLayout.RIGHT)); bp.add(clearButton); bp.add(findVenueBtn); bp.add(createButton); c.gridy = y; c.weighty = 0.0; c.fill = GridBagConstraints.HORIZONTAL; p.add(bp, c); return p }
    private fun setupListeners() { createButton.addActionListener { if (editingEventId == null) createEvent() else updateEvent() }; findVenueBtn.addActionListener { findAvailableVenue() }; clearButton.addActionListener { clearFields() }; deleteButton.addActionListener { deleteSelectedEvent() }; editButton.addActionListener { loadEventForEditing() }; venueCombo.addItemListener { e -> if (e.stateChange == ItemEvent.SELECTED) { val v = (e.item as? VenueItem)?.venue; if (v!=null) venueInfoLabel.text = "✓ ${v.name} (Cap: ${v.capacity})" } }; venueCombo.addPopupMenuListener(object : PopupMenuListener { override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) { refreshVenueCombo() }; override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}; override fun popupMenuCanceled(e: PopupMenuEvent?) {} }); searchField.document.addDocumentListener(object : DocumentListener { override fun insertUpdate(e: DocumentEvent?) { filter() }; override fun removeUpdate(e: DocumentEvent?) { filter() }; override fun changedUpdate(e: DocumentEvent?) { filter() }; fun filter() { val t = searchField.text; if (t.trim().isEmpty()) tableSorter.setRowFilter(null) else tableSorter.setRowFilter(RowFilter.regexFilter("(?i)$t")) } }) }
    private fun loadEventForEditing() { val r = eventTable.selectedRow; if (r == -1) { JOptionPane.showMessageDialog(this, "Select an event.", "Error", JOptionPane.WARNING_MESSAGE); return }; val m = eventTable.convertRowIndexToModel(r); val e = displayedEvents.getOrNull(m) ?: return; titleField.text = e.title; startDateSpinner.value = Date.from(e.dateTime.atZone(ZoneId.systemDefault()).toInstant()); hoursSpinner.value = e.duration.toHours().toInt(); minutesSpinner.value = e.duration.toMinutesPart(); maxParticipantsField.text = e.maxParticipants.toString(); descriptionArea.text = e.description; for (i in 0 until venueCombo.itemCount) { if (venueCombo.getItemAt(i).venue.id == e.venue.id) { venueCombo.selectedIndex = i; break } }; editingEventId = e.id; createButton.text = "Save Changes"; createButton.background = Color(255, 193, 7); createButton.foreground = Color.BLACK; clearButton.text = "Cancel Edit"; eventTable.isEnabled = false; editButton.isEnabled = false; deleteButton.isEnabled = false }
    private fun updateEvent() { val id = editingEventId ?: return; val old = eventManager.getEventById(id) ?: return; val t = titleField.text.trim(); val v = venueCombo.selectedItem as? VenueItem; val d = descriptionArea.text.trim(); val c = maxParticipantsField.text.toIntOrNull(); if (t.isBlank() || v == null || c == null || c <= 0 || c > v.venue.capacity) { JOptionPane.showMessageDialog(this, "Invalid Input", "Error", JOptionPane.ERROR_MESSAGE); return }; val dur = Duration.ofHours((hoursSpinner.value as Number).toLong()).plusMinutes((minutesSpinner.value as Number).toLong()); val dt = LocalDateTime.ofInstant((startDateSpinner.value as Date).toInstant(), ZoneId.systemDefault()); val up = old.copy(title = t, dateTime = dt, venue = v.venue, description = d, duration = dur, maxParticipants = c); val w = object : SwingWorker<Boolean, Void>() { override fun doInBackground() = eventManager.modifyEvent(up); override fun done() { if (get()) { refreshEventTable(); clearFields(); JOptionPane.showMessageDialog(this@EventPanel, "Updated!", "Success", JOptionPane.INFORMATION_MESSAGE) } else { JOptionPane.showMessageDialog(this@EventPanel, "Failed", "Error", JOptionPane.ERROR_MESSAGE) } } }; w.execute() }
    private fun refreshEventTable() { tableModel.rowCount = 0; val dF = DateTimeFormatter.ofPattern("yyyy-MM-dd"); val tF = DateTimeFormatter.ofPattern("HH:mm"); displayedEvents = eventManager.getAllEvents().sortedBy { it.dateTime }; displayedEvents.forEach { e -> val end = e.getEndTime(); tableModel.addRow(arrayOf(e.title, e.dateTime.format(dF), "${e.dateTime.format(tF)} - ${end.format(tF)}", e.venue.name, "${e.getCurrentCapacity()}/${e.maxParticipants}")) } }
    private fun createEvent() { val t = titleField.text.trim(); val v = venueCombo.selectedItem as? VenueItem; val d = descriptionArea.text.trim(); val c = maxParticipantsField.text.toIntOrNull(); if (t.isBlank() || v == null || c == null || c <= 0 || c > v.venue.capacity) { JOptionPane.showMessageDialog(this, "Invalid Input", "Error", JOptionPane.ERROR_MESSAGE); return }; val dur = Duration.ofHours((hoursSpinner.value as Number).toLong()).plusMinutes((minutesSpinner.value as Number).toLong()); val dt = LocalDateTime.ofInstant((startDateSpinner.value as Date).toInstant(), ZoneId.systemDefault()); val e = Event(UUID.randomUUID().toString(), t, dt, v.venue, d, dur, c); if (eventManager.getAllEvents().any { it.conflictsWith(e) }) { JOptionPane.showMessageDialog(this, "Conflict!", "Error", JOptionPane.ERROR_MESSAGE); return }; val w = object : SwingWorker<Boolean, Void>() { override fun doInBackground() = eventManager.addEvent(e); override fun done() { if (get()) { refreshEventTable(); clearFields(); JOptionPane.showMessageDialog(this@EventPanel, "Saved!", "Success", JOptionPane.INFORMATION_MESSAGE) } } }; w.execute() }
    private fun deleteSelectedEvent() { val r = eventTable.selectedRow; if (r == -1) return; val m = eventTable.convertRowIndexToModel(r); val e = displayedEvents.getOrNull(m) ?: return; if (JOptionPane.showConfirmDialog(this, "Delete '${e.title}'?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { val w = object : SwingWorker<Boolean, Void>() { override fun doInBackground() = eventManager.deleteEvent(e); override fun done() { if (get()) { refreshEventTable(); JOptionPane.showMessageDialog(this@EventPanel, "Deleted.", "Success", JOptionPane.INFORMATION_MESSAGE) } } }; w.execute() } }
    private fun refreshVenueCombo() { val s = venueCombo.selectedItem; venueCombo.removeAllItems(); eventManager.getAllVenues().forEach { venueCombo.addItem(VenueItem(it)) }; if (s != null) venueCombo.selectedItem = s }
    private fun clearFields() { titleField.text=""; descriptionArea.text=""; maxParticipantsField.text=""; venueInfoLabel.text="Select a venue..."; eventTable.clearSelection(); editingEventId = null; createButton.text = "Create Event"; createButton.background = null; createButton.foreground = null; clearButton.text = "Clear"; eventTable.isEnabled = true; editButton.isEnabled = true; deleteButton.isEnabled = true; startDateSpinner.value = Date(); hoursSpinner.value = 2; minutesSpinner.value = 0; venueCombo.selectedIndex = -1 }
    private data class VenueItem(val venue: Venue) { override fun toString() = "${venue.name} (Cap: ${venue.capacity})" }

    // --- UPDATED FIND AVAILABLE VENUE METHOD ---
    private fun findAvailableVenue() {
        try {
            val cap = maxParticipantsField.text.toIntOrNull()
            if (cap == null) {
                JOptionPane.showMessageDialog(this, "Enter capacity to find venues.", "Info", JOptionPane.WARNING_MESSAGE)
                return
            }

            val hours = (hoursSpinner.value as Number).toLong()
            val minutes = (minutesSpinner.value as Number).toLong()
            val dur = Duration.ofHours(hours).plusMinutes(minutes)
            val dt = LocalDateTime.ofInstant((startDateSpinner.value as Date).toInstant(), ZoneId.systemDefault())

            findVenueBtn.isEnabled = false

            val worker = object : SwingWorker<ScalaBridge.SlotFinderResult, Void>() {
                override fun doInBackground(): ScalaBridge.SlotFinderResult {
                    return ScalaBridge.findAvailableVenues(
                        eventManager.getAllVenues(),
                        eventManager.getAllEvents(),
                        cap,
                        dt,
                        dur
                    )
                }

                override fun done() {
                    findVenueBtn.isEnabled = true
                    try {
                        when (val result = get()) {
                            is ScalaBridge.SlotFinderResult.Success -> {
                                val slots = result.slots
                                if (slots.isEmpty()) {
                                    JOptionPane.showMessageDialog(this@EventPanel, "No venues available for the selected time or future slots.", "Info", JOptionPane.INFORMATION_MESSAGE)
                                } else {
                                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                                    val sb = StringBuilder("Available Venues & Slots:\n\n")

                                    slots.forEach { slot ->
                                        val startStr = slot.startTime.format(timeFormatter)
                                        val endStr = slot.freeUntil.format(timeFormatter)

                                        sb.append("• ${slot.venue.name} (Cap: ${slot.venue.capacity})\n")
                                        sb.append("   Start: $startStr\n")
                                        sb.append("   Free Until: $endStr\n\n")
                                    }

                                    // Show in scroll pane if list is long
                                    val textArea = JTextArea(sb.toString())
                                    textArea.isEditable = false
                                    textArea.background = UIManager.getColor("Panel.background")

                                    JOptionPane.showMessageDialog(
                                        this@EventPanel,
                                        JScrollPane(textArea).apply { preferredSize = Dimension(350, 250) },
                                        "Venues Found",
                                        JOptionPane.INFORMATION_MESSAGE
                                    )
                                }
                            }
                            is ScalaBridge.SlotFinderResult.Error -> {
                                JOptionPane.showMessageDialog(this@EventPanel, result.message, "Error", JOptionPane.ERROR_MESSAGE)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        JOptionPane.showMessageDialog(this@EventPanel, "Error: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                    }
                }
            }
            worker.execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}