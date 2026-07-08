import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * PhoneDirectoryGUI.java
 *
 * The presentation layer (View + Controller) of the application. Built with
 * pure Java Swing - no external UI libraries. All business logic is
 * delegated to {@link ContactManager}; all disk I/O is delegated to
 * {@link FileManager}. This class is only responsible for laying out
 * components, wiring up event listeners, and reflecting data changes
 * on screen.
 *
 * Visual redesign: sidebar navigation, soft rounded "card" panels, pill
 * shaped accent buttons, a cleaner data table, and a refined dark theme.
 */
public class PhoneDirectoryGUI extends JFrame {

    // ---------------------------------------------------------------
    //  Color palette (light mode) - modern indigo / slate theme
    // ---------------------------------------------------------------
    private static final Color ACCENT        = new Color(99, 102, 241);   // indigo-500
    private static final Color ACCENT_DARK   = new Color(79, 70, 229);    // indigo-600
    private static final Color ACCENT_SOFT   = new Color(238, 239, 253);  // indigo-50
    private static final Color SIDEBAR_BG    = new Color(30, 27, 60);     // deep indigo/navy
    private static final Color SIDEBAR_BG2   = new Color(23, 21, 46);
    private static final Color BG_APP        = new Color(244, 245, 250);
    private static final Color CARD_WHITE    = Color.WHITE;
    private static final Color GREEN_BTN     = new Color(16, 163, 106);
    private static final Color BLUE_BTN      = new Color(59, 130, 246);
    private static final Color ORANGE_BTN    = new Color(245, 158, 11);
    private static final Color RED_BTN       = new Color(239, 68, 68);
    private static final Color GRAY_BTN      = new Color(107, 114, 128);
    private static final Color TEXT_DARK     = new Color(30, 32, 41);
    private static final Color TEXT_MUTED    = new Color(120, 126, 140);
    private static final Color TABLE_HEADER  = new Color(45, 41, 90);
    private static final Color ROW_STRIPE    = new Color(247, 247, 252);
    private static final Color ROW_HIGHLIGHT = new Color(255, 244, 199);
    private static final Color BORDER_SOFT   = new Color(228, 229, 238);

    // Dark mode palette
    private static final Color DARK_BG       = new Color(18, 19, 26);
    private static final Color DARK_PANEL    = new Color(28, 29, 39);
    private static final Color DARK_PANEL_2  = new Color(35, 37, 48);
    private static final Color DARK_TEXT     = new Color(226, 227, 235);
    private static final Color DARK_MUTED    = new Color(148, 151, 168);
    private static final Color DARK_BORDER   = new Color(48, 50, 64);
    private static final Color DARK_STRIPE   = new Color(24, 25, 34);

    private static final String FONT_FAMILY = pickAvailableFont();

    // ---------------------------------------------------------------
    //  Core components
    // ---------------------------------------------------------------
    private final ContactManager manager = new ContactManager();
    private final FileManager fileManager = new FileManager();

    private JTextField nameField;
    private JTextField phoneField;
    private JTextField searchField;

    private DefaultTableModel tableModel;
    private JTable table;

    private JLabel statusLabel;
    private JLabel totalContactsValue;
    private JLabel searchCounterValue;
    private JLabel recentlyAddedValue;

    private RoundedPanel totalCard, searchCard, recentCard;

    private JPanel rootPanel;
    private JPanel sidebar;
    private JPanel mainColumn;
    private RoundedPanel inputCard;
    private RoundedPanel tableCard;
    private JPanel statsRow;
    private JPanel statusBar;
    private JLabel brandTitle, brandSubtitle;
    private JLabel headerTitle, headerSubtitle;

    private java.util.List<ModernButton> allButtons = new java.util.ArrayList<>();
    private java.util.List<JLabel> sidebarLabels = new java.util.ArrayList<>();

    private boolean darkMode = false;
    private String highlightedPhone = null; // used to highlight search results

    public PhoneDirectoryGUI() {
        super("Phone Directory System");
        initLookAndFeel();
        buildUI();
        loadDataFromDisk();
        setupKeyboardShortcuts();
        refreshTable(manager.getAllContacts());
        updateStats();
        setStatus("Ready \u2014 " + manager.getTotalContacts() + " contact(s) loaded.");
    }

    private static String pickAvailableFont() {
        String[] preferred = {"Segoe UI Variable Display", "Segoe UI", "Inter", "Poppins", "Arial"};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        java.util.Set<String> set = new java.util.HashSet<>(java.util.Arrays.asList(available));
        for (String f : preferred) {
            if (set.contains(f)) return f;
        }
        return Font.SANS_SERIF;
    }

    private void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default cross-platform L&F.
        }
    }

    // =================================================================
    //  UI CONSTRUCTION
    // =================================================================

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        setJMenuBar(buildMenuBar());

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(BG_APP);

        sidebar = buildSidebar();
        mainColumn = buildMainColumn();

        rootPanel.add(sidebar, BorderLayout.WEST);
        rootPanel.add(mainColumn, BorderLayout.CENTER);

        setContentPane(rootPanel);
    }

    // ---------------------------------------------------------------
    //  Menu bar
    // ---------------------------------------------------------------
    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem importItem = new JMenuItem("Import from CSV...");
        JMenuItem exportItem = new JMenuItem("Export to CSV...");
        JMenuItem printItem = new JMenuItem("Print Directory...");
        JMenuItem exitItem = new JMenuItem("Exit");
        importItem.addActionListener(e -> importCSV());
        exportItem.addActionListener(e -> exportCSV());
        printItem.addActionListener(e -> printDirectory());
        exitItem.addActionListener(e -> exitApplication());
        fileMenu.add(importItem);
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(printItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem addItem = new JMenuItem("Add Contact   (Ctrl+N)");
        JMenuItem updateItem = new JMenuItem("Update Selected");
        JMenuItem deleteItem = new JMenuItem("Delete Selected   (Del)");
        JMenuItem clearItem = new JMenuItem("Clear Fields");
        addItem.addActionListener(e -> { clearFields(); nameField.requestFocus(); });
        updateItem.addActionListener(e -> updateContact());
        deleteItem.addActionListener(e -> deleteContact());
        clearItem.addActionListener(e -> clearFields());
        editMenu.add(addItem);
        editMenu.add(updateItem);
        editMenu.add(deleteItem);
        editMenu.addSeparator();
        editMenu.add(clearItem);

        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem darkModeItem = new JCheckBoxMenuItem("Dark Mode");
        darkModeItem.addActionListener(e -> toggleDarkMode());
        viewMenu.add(darkModeItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        helpItem.addActionListener(e -> showHelpDialog());
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(helpItem);
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    // ---------------------------------------------------------------
    //  Sidebar (brand + quick actions + theme toggle)
    // ---------------------------------------------------------------
    private JPanel buildSidebar() {
        JPanel panel = new GradientPanel(SIDEBAR_BG, SIDEBAR_BG2);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(230, 0));
        panel.setBorder(new EmptyBorder(28, 22, 22, 22));

        // Brand block
        JLabel logo = new JLabel("\u260E");
        logo.setFont(new Font(FONT_FAMILY, Font.PLAIN, 34));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandTitle = new JLabel("Phone Directory");
        brandTitle.setFont(new Font(FONT_FAMILY, Font.BOLD, 20));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandSubtitle = new JLabel("Contact Management");
        brandSubtitle.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        brandSubtitle.setForeground(new Color(180, 178, 220));
        brandSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(logo);
        panel.add(Box.createVerticalStrut(10));
        panel.add(brandTitle);
        panel.add(Box.createVerticalStrut(2));
        panel.add(brandSubtitle);
        panel.add(Box.createVerticalStrut(32));

        panel.add(sidebarSectionLabel("QUICK ACTIONS"));
        panel.add(Box.createVerticalStrut(10));

        panel.add(sidebarLink("\u2795", "Add Contact", () -> { clearFields(); nameField.requestFocus(); }));
        panel.add(Box.createVerticalStrut(6));
        panel.add(sidebarLink("\uD83D\uDCCB", "Display All", this::displayAll));
        panel.add(Box.createVerticalStrut(6));
        panel.add(sidebarLink("\u2B07", "Import CSV", this::importCSV));
        panel.add(Box.createVerticalStrut(6));
        panel.add(sidebarLink("\u2B06", "Export CSV", this::exportCSV));
        panel.add(Box.createVerticalStrut(6));
        panel.add(sidebarLink("\uD83D\uDDA8", "Print Directory", this::printDirectory));

        panel.add(Box.createVerticalGlue());

        JCheckBox darkToggle = new JCheckBox("Dark Mode");
        darkToggle.setOpaque(false);
        darkToggle.setForeground(Color.WHITE);
        darkToggle.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        darkToggle.setFocusPainted(false);
        darkToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        darkToggle.addActionListener(e -> toggleDarkMode());

        JLabel version = new JLabel("v2.0 \u2022 Redesigned");
        version.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
        version.setForeground(new Color(150, 148, 190));
        version.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(new JSeparator() {{ setForeground(new Color(70, 66, 110)); setAlignmentX(Component.LEFT_ALIGNMENT); }});
        panel.add(Box.createVerticalStrut(10));
        panel.add(darkToggle);
        panel.add(Box.createVerticalStrut(8));
        panel.add(version);

        return panel;
    }

    private JLabel sidebarSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        lbl.setForeground(new Color(150, 148, 190));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarLabels.add(lbl);
        return lbl;
    }

    private JButton sidebarLink(String icon, String text, Runnable action) {
        JButton btn = new JButton(icon + "   " + text);
        btn.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        btn.setForeground(new Color(225, 224, 245));
        btn.setBackground(new Color(255, 255, 255, 0));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(9, 10, 9, 10));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(500, 36));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(new Color(255, 255, 255, 24));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(0,0,0,0), new EmptyBorder(9, 10, 9, 10)));
                btn.setOpaque(false);
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false);
            }
        });
        btn.addActionListener(e -> action.run());
        return btn;
    }

    // ---------------------------------------------------------------
    //  Main column (header + input card + table card + stats + status)
    // ---------------------------------------------------------------
    private JPanel buildMainColumn() {
        JPanel col = new JPanel(new BorderLayout());
        col.setBackground(BG_APP);
        col.setBorder(new EmptyBorder(24, 26, 20, 26));

        JPanel header = buildPageHeader();
        inputCard = buildInputCard();
        tableCard = buildTableCard();
        statsRow = buildStatsRow();
        statusBar = buildStatusBar();

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(header);
        top.add(Box.createVerticalStrut(16));
        top.add(inputCard);
        top.add(Box.createVerticalStrut(16));

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        bottom.add(Box.createVerticalStrut(14));
        bottom.add(statsRow);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(statusBar);

        col.add(top, BorderLayout.NORTH);
        col.add(tableCard, BorderLayout.CENTER);
        col.add(bottom, BorderLayout.SOUTH);
        return col;
    }

    private JPanel buildPageHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        headerTitle = new JLabel("Contacts");
        headerTitle.setFont(new Font(FONT_FAMILY, Font.BOLD, 26));
        headerTitle.setForeground(TEXT_DARK);

        headerSubtitle = new JLabel("Search, add, and manage your phone directory");
        headerSubtitle.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        headerSubtitle.setForeground(TEXT_MUTED);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);
        textCol.add(headerTitle);
        textCol.add(Box.createVerticalStrut(3));
        textCol.add(headerSubtitle);

        panel.add(textCol, BorderLayout.WEST);
        return panel;
    }

    // ---------------------------------------------------------------
    //  Input card (fields + live search + action buttons)
    // ---------------------------------------------------------------
    private RoundedPanel buildInputCard() {
        RoundedPanel card = new RoundedPanel(18, CARD_WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 22, 18, 22));

        // --- Fields row ---
        JPanel fieldsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        fieldsRow.setOpaque(false);

        JLabel nameLbl = makeFieldLabel("Name");
        nameField = makeTextField(16);
        nameField.setToolTipText("Enter the contact's full name");

        JLabel phoneLbl = makeFieldLabel("Phone");
        phoneField = makeTextField(14);
        phoneField.setToolTipText("Enter a 10-digit phone number, digits only");

        JPanel nameCol = labeledField(nameLbl, nameField);
        JPanel phoneCol = labeledField(phoneLbl, phoneField);

        fieldsRow.add(nameCol);
        fieldsRow.add(phoneCol);

        // --- Live search row ---
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        searchRow.setBorder(new EmptyBorder(14, 0, 14, 0));

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));

        searchField = makeTextField(0);
        searchField.setToolTipText("Type to instantly filter by name or phone number (Ctrl+F to focus)");
        searchField.putClientProperty("JTextField.placeholderText", "Search by name or phone number...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { liveFilter(); }
            public void removeUpdate(DocumentEvent e) { liveFilter(); }
            public void changedUpdate(DocumentEvent e) { liveFilter(); }
        });

        RoundedPanel searchWrap = new RoundedPanel(24, CARD_WHITE);
        searchWrap.setLayout(new BorderLayout());
        searchWrap.setBorderColor(BORDER_SOFT);
        searchWrap.setBorder(new EmptyBorder(4, 14, 4, 14));
        searchField.setBorder(new EmptyBorder(8, 8, 8, 8));
        searchWrap.add(searchIcon, BorderLayout.WEST);
        searchWrap.add(searchField, BorderLayout.CENTER);

        searchRow.add(searchWrap, BorderLayout.CENTER);

        // --- Buttons row ---
        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        buttonsRow.setOpaque(false);

        ModernButton addBtn      = makeButton("Add Contact", GREEN_BTN, "\u2795");
        ModernButton updateBtn   = makeButton("Update", BLUE_BTN, "\u270E");
        ModernButton deleteBtn   = makeButton("Delete", RED_BTN, "\uD83D\uDDD1");
        ModernButton searchNameBtn = makeButton("Search by Name", ORANGE_BTN, "\uD83D\uDC64");
        ModernButton searchPhoneBtn = makeButton("Search by Phone", ORANGE_BTN, "\u260E");
        ModernButton displayAllBtn  = makeButton("Display All", ACCENT, "\uD83D\uDCCB");
        ModernButton clearBtn    = makeButton("Clear", GRAY_BTN, "\u2716");
        ModernButton exitBtn     = makeButton("Exit", new Color(55, 58, 70), "\u23FB");

        addBtn.setToolTipText("Add a new contact (Ctrl+N)");
        updateBtn.setToolTipText("Update the currently selected contact");
        deleteBtn.setToolTipText("Delete the currently selected contact (Del)");
        searchNameBtn.setToolTipText("Search contacts by name");
        searchPhoneBtn.setToolTipText("Search contacts by phone number");
        displayAllBtn.setToolTipText("Show every contact in the directory");
        clearBtn.setToolTipText("Clear the input fields and table selection");
        exitBtn.setToolTipText("Close the application");

        addBtn.addActionListener(e -> addContact());
        updateBtn.addActionListener(e -> updateContact());
        deleteBtn.addActionListener(e -> deleteContact());
        searchNameBtn.addActionListener(e -> searchByName());
        searchPhoneBtn.addActionListener(e -> searchByPhone());
        displayAllBtn.addActionListener(e -> displayAll());
        clearBtn.addActionListener(e -> clearFields());
        exitBtn.addActionListener(e -> exitApplication());

        for (ModernButton b : new ModernButton[]{addBtn, updateBtn, deleteBtn, searchNameBtn,
                searchPhoneBtn, displayAllBtn, clearBtn, exitBtn}) {
            buttonsRow.add(b);
            allButtons.add(b);
        }

        JPanel stacked = new JPanel();
        stacked.setLayout(new BoxLayout(stacked, BoxLayout.Y_AXIS));
        stacked.setOpaque(false);
        stacked.add(fieldsRow);
        stacked.add(searchRow);
        stacked.add(buttonsRow);

        card.add(stacked, BorderLayout.CENTER);
        return card;
    }

    private JPanel labeledField(JLabel label, JTextField field) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(label);
        col.add(Box.createVerticalStrut(4));
        col.add(field);
        return col;
    }

    private JLabel makeFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    private JTextField makeTextField(int columns) {
        JTextField field = columns > 0 ? new JTextField(columns) : new JTextField();
        field.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_SOFT, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        field.setBackground(CARD_WHITE);
        field.setForeground(TEXT_DARK);
        field.setCaretColor(ACCENT);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(ACCENT, 1, true),
                        new EmptyBorder(8, 12, 8, 12)));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(darkMode ? DARK_BORDER : BORDER_SOFT, 1, true),
                        new EmptyBorder(8, 12, 8, 12)));
            }
        });
        return field;
    }

    private ModernButton makeButton(String text, Color color, String icon) {
        ModernButton btn = new ModernButton(icon + "  " + text, color);
        btn.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        return btn;
    }

    // ---------------------------------------------------------------
    //  Table card
    // ---------------------------------------------------------------
    private RoundedPanel buildTableCard() {
        RoundedPanel card = new RoundedPanel(18, CARD_WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(6, 6, 6, 6));

        String[] columns = {"Name", "Phone Number"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // editing happens via the fields, not inline
            }
        };
        table = new JTable(tableModel);
        table.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        table.setRowHeight(36);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ACCENT_SOFT);
        table.setSelectionForeground(TEXT_DARK);
        table.setGridColor(new Color(0, 0, 0, 0));
        table.setShowGrid(false);
        table.getTableHeader().setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        table.getTableHeader().setBackground(TABLE_HEADER);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(100, 42));
        table.getTableHeader().setBorder(null);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setBorder(null);

        table.setDefaultRenderer(Object.class, new HighlightRenderer());

        // Row selection -> populate fields
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.convertRowIndexToModel(table.getSelectedRow());
                nameField.setText((String) tableModel.getValueAt(row, 0));
                phoneField.setText((String) tableModel.getValueAt(row, 1));
            }
        });

        // Double-click a row -> ready to edit (fields already populated by selection)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setStatus("Editing mode: modify the fields above, then click Update.");
                }
            }
        });

        setupContextMenu();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CARD_WHITE);
        scrollPane.setVerticalScrollBar(scrollPane.createVerticalScrollBar());
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    /** Right-click context menu on the JTable: Edit / Delete. */
    private void setupContextMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Contact");
        JMenuItem deleteItem = new JMenuItem("Delete Contact");
        editItem.addActionListener(e -> setStatus("Fields loaded - modify and click Update."));
        deleteItem.addActionListener(e -> deleteContact());
        popup.add(editItem);
        popup.add(deleteItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        table.setRowSelectionInterval(row, row);
                        popup.show(table, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    /** Custom cell renderer: striped rows, soft highlight, dark-mode aware. */
    private class HighlightRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 16, 0, 16));
            int modelRow = tbl.convertRowIndexToModel(row);
            String phoneInRow = (String) tableModel.getValueAt(modelRow, 1);
            if (!isSelected) {
                if (highlightedPhone != null && highlightedPhone.equals(phoneInRow)) {
                    c.setBackground(ROW_HIGHLIGHT);
                } else if (row % 2 == 0) {
                    c.setBackground(darkMode ? DARK_PANEL : Color.WHITE);
                } else {
                    c.setBackground(darkMode ? DARK_STRIPE : ROW_STRIPE);
                }
                c.setForeground(darkMode ? DARK_TEXT : TEXT_DARK);
            } else {
                c.setForeground(TEXT_DARK);
            }
            return c;
        }
    }

    // ---------------------------------------------------------------
    //  Stats row
    // ---------------------------------------------------------------
    private JPanel buildStatsRow() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 14, 0));
        panel.setOpaque(false);

        totalCard = statCard("Total Contacts", ACCENT);
        totalContactsValue = (JLabel) totalCard.getClientProperty("valueLabel");

        searchCard = statCard("Searches Performed", ORANGE_BTN);
        searchCounterValue = (JLabel) searchCard.getClientProperty("valueLabel");

        recentCard = statCard("Recently Added", GREEN_BTN);
        recentlyAddedValue = (JLabel) recentCard.getClientProperty("valueLabel");

        panel.add(totalCard);
        panel.add(searchCard);
        panel.add(recentCard);
        return panel;
    }

    /** Builds a small stat card with a colored accent strip, title, and value. */
    private RoundedPanel statCard(String title, Color accent) {
        RoundedPanel card = new RoundedPanel(14, CARD_WHITE);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel dot = new JPanel();
        dot.setPreferredSize(new Dimension(6, 34));
        dot.setBackground(accent);
        dot.setOpaque(true);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        titleLbl.setForeground(TEXT_MUTED);

        JLabel valueLbl = new JLabel("0");
        valueLbl.setFont(new Font(FONT_FAMILY, Font.BOLD, 22));
        valueLbl.setForeground(TEXT_DARK);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);
        textCol.add(titleLbl);
        textCol.add(valueLbl);

        RoundedPanel dotWrap = new RoundedPanel(4, accent);
        dotWrap.setPreferredSize(new Dimension(6, 34));

        card.add(dotWrap, BorderLayout.WEST);
        card.add(textCol, BorderLayout.CENTER);
        card.putClientProperty("valueLabel", valueLbl);
        card.putClientProperty("titleLabel", titleLbl);
        return card;
    }

    // ---------------------------------------------------------------
    //  Status bar
    // ---------------------------------------------------------------
    private JPanel buildStatusBar() {
        RoundedPanel panel = new RoundedPanel(12, CARD_WHITE);
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(9, 18, 9, 18));

        statusLabel = new JLabel("Ready.");
        statusLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_MUTED);

        JLabel version = new JLabel("Phone Directory System v2.0");
        version.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
        version.setForeground(new Color(170, 174, 184));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(version, BorderLayout.EAST);
        return panel;
    }

    // =================================================================
    //  ACTIONS
    // =================================================================

    private void addContact() {
        try {
            manager.addContact(nameField.getText(), phoneField.getText());
            persistAndRefresh();
            JOptionPane.showMessageDialog(this, "Contact added successfully!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearFields();
            setStatus("Added contact at " + Utils.currentTimestamp() + ".");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void updateContact() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            showError("Please select a contact in the table to update.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String oldPhone = (String) tableModel.getValueAt(modelRow, 1);

        try {
            manager.updateContact(oldPhone, nameField.getText(), phoneField.getText());
            persistAndRefresh();
            JOptionPane.showMessageDialog(this, "Contact updated successfully!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearFields();
            setStatus("Updated contact at " + Utils.currentTimestamp() + ".");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteContact() {
        String phone;
        int viewRow = table.getSelectedRow();
        if (viewRow != -1) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            phone = (String) tableModel.getValueAt(modelRow, 1);
        } else if (Utils.isValidPhone(phoneField.getText().trim())) {
            phone = phoneField.getText().trim();
        } else {
            showError("Please select a contact in the table (or enter a valid phone number) to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this contact?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean removed = manager.deleteContact(phone);
            if (removed) {
                persistAndRefresh();
                clearFields();
                setStatus("Deleted contact at " + Utils.currentTimestamp() + ".");
            } else {
                showError("Contact not found.");
            }
        }
    }

    private void searchByName() {
        String query = nameField.getText().trim();
        if (query.isEmpty()) {
            showError("Enter a name in the Name field to search.");
            return;
        }
        List<Contact> results = manager.searchByName(query);
        if (results.isEmpty()) {
            setStatus("No contacts found matching name \"" + query + "\".");
            JOptionPane.showMessageDialog(this, "No contacts found for \"" + query + "\".",
                    "Search by Name", JOptionPane.INFORMATION_MESSAGE);
        } else {
            highlightedPhone = results.get(0).getPhoneNumber();
            refreshTable(results);
            setStatus("Found " + results.size() + " phone number(s) for \"" + query + "\".");
        }
        updateStats();
    }

    private void searchByPhone() {
        String query = phoneField.getText().trim();
        if (!Utils.isValidPhone(query)) {
            showError("Enter a valid 10-digit phone number in the Phone field to search.");
            return;
        }
        String name = manager.searchByPhone(query);
        if (name == null) {
            setStatus("No contact found with phone number " + query + ".");
            JOptionPane.showMessageDialog(this, "No contact found with phone number " + query + ".",
                    "Search by Phone", JOptionPane.INFORMATION_MESSAGE);
        } else {
            nameField.setText(name);
            highlightedPhone = query;
            refreshTable(java.util.List.of(new Contact(name, query)));
            setStatus("Phone " + query + " belongs to " + name + ".");
        }
        updateStats();
    }

    private void displayAll() {
        highlightedPhone = null;
        searchField.setText("");
        refreshTable(manager.getAllContacts());
        setStatus("Displaying all " + manager.getTotalContacts() + " contact(s).");
    }

    private void liveFilter() {
        highlightedPhone = null;
        String query = searchField.getText();
        refreshTable(manager.liveFilter(query));
    }

    private void clearFields() {
        nameField.setText("");
        phoneField.setText("");
        table.clearSelection();
        highlightedPhone = null;
        refreshTable(manager.getAllContacts());
        setStatus("Fields cleared.");
    }

    private void exitApplication() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit Phone Directory System?",
                "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    // ---------------------------------------------------------------
    //  Import / Export / Print
    // ---------------------------------------------------------------
    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("contacts_export.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                fileManager.exportToCSV(chooser.getSelectedFile().getAbsolutePath(), manager.getAllContacts());
                JOptionPane.showMessageDialog(this, "Contacts exported successfully!", "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                setStatus("Exported " + manager.getTotalContacts() + " contact(s) to CSV.");
            } catch (IOException ex) {
                showError("Failed to export: " + ex.getMessage());
            }
        }
    }

    private void importCSV() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                List<Contact> imported = fileManager.importFromCSV(chooser.getSelectedFile().getAbsolutePath());
                int count = manager.mergeFromList(imported);
                persistAndRefresh();
                JOptionPane.showMessageDialog(this, count + " contact(s) imported successfully!\n"
                        + (imported.size() - count) + " duplicate/invalid row(s) skipped.",
                        "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                setStatus("Imported " + count + " contact(s) from CSV.");
            } catch (IOException ex) {
                showError("Failed to import: " + ex.getMessage());
            }
        }
    }

    private void printDirectory() {
        try {
            boolean complete = table.print(JTable.PrintMode.FIT_WIDTH,
                    new java.text.MessageFormat("Phone Directory System"),
                    new java.text.MessageFormat("Page {0}"));
            setStatus(complete ? "Directory sent to printer." : "Print cancelled.");
        } catch (PrinterException ex) {
            showError("Printing failed: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    //  Dark mode
    // ---------------------------------------------------------------
    private void toggleDarkMode() {
        darkMode = !darkMode;
        Color bg = darkMode ? DARK_BG : BG_APP;
        Color panelBg = darkMode ? DARK_PANEL : CARD_WHITE;
        Color fg = darkMode ? DARK_TEXT : TEXT_DARK;
        Color muted = darkMode ? DARK_MUTED : TEXT_MUTED;
        Color border = darkMode ? DARK_BORDER : BORDER_SOFT;

        rootPanel.setBackground(bg);
        mainColumn.setBackground(bg);

        inputCard.setBackground(panelBg);
        tableCard.setBackground(panelBg);
        statusBar.setBackground(panelBg);

        headerTitle.setForeground(fg);
        headerSubtitle.setForeground(muted);
        statusLabel.setForeground(muted);

        nameField.setBackground(panelBg);
        nameField.setForeground(fg);
        phoneField.setBackground(panelBg);
        phoneField.setForeground(fg);
        searchField.setBackground(panelBg);
        searchField.setForeground(fg);

        table.setBackground(panelBg);
        table.setForeground(fg);
        table.getParent().getParent().setBackground(panelBg); // scrollpane viewport

        for (RoundedPanel card : new RoundedPanel[]{totalCard, searchCard, recentCard, inputCard, tableCard, (RoundedPanel) statusBar}) {
            card.setBackground(panelBg);
            card.setBorderColor(border);
            JLabel v = (JLabel) card.getClientProperty("valueLabel");
            JLabel t = (JLabel) card.getClientProperty("titleLabel");
            if (v != null) v.setForeground(fg);
            if (t != null) t.setForeground(muted);
        }

        table.repaint();
        revalidate();
        repaint();
        setStatus(darkMode ? "Dark mode enabled." : "Light mode enabled.");
    }

    // ---------------------------------------------------------------
    //  About / Help
    // ---------------------------------------------------------------
    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "Phone Directory System v2.0\n\n"
                        + "A Java Swing desktop CRM application demonstrating the\n"
                        + "Collections Framework (HashMap, ArrayList) for instant\n"
                        + "contact lookup, built for PS-71 (Telecom / CRM, Module D).\n\n"
                        + "Built with: Java, Swing, JTable, File I/O.\n"
                        + "Redesigned interface: sidebar navigation, card layout, dark mode.",
                "About Phone Directory System", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showHelpDialog() {
        JOptionPane.showMessageDialog(this,
                "Quick Guide:\n\n"
                        + "\u2022 Add Contact: fill Name + Phone, click 'Add Contact'.\n"
                        + "\u2022 Update: select a row (or type in fields), edit, click 'Update'.\n"
                        + "\u2022 Delete: select a row, click 'Delete', confirm.\n"
                        + "\u2022 Search by Name / Phone: type into the field, click the matching search button.\n"
                        + "\u2022 Live Search bar filters the table instantly as you type.\n"
                        + "\u2022 Right-click a row for a quick Edit/Delete menu.\n"
                        + "\u2022 Sidebar quick actions mirror the main buttons for fast access.\n\n"
                        + "Keyboard Shortcuts:\n"
                        + "  Ctrl+N  Focus Name field to add a new contact\n"
                        + "  Ctrl+S  Save current fields (Update if row selected, else Add)\n"
                        + "  Ctrl+F  Jump to the Live Search bar\n"
                        + "  Delete  Delete the selected row",
                "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------------------------------------------------------
    //  Keyboard shortcuts
    // ---------------------------------------------------------------
    private void setupKeyboardShortcuts() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newContact");
        am.put("newContact", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                clearFields();
                nameField.requestFocus();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "saveContact");
        am.put("saveContact", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (table.getSelectedRow() != -1) {
                    updateContact();
                } else {
                    addContact();
                }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "focusSearch");
        am.put("focusSearch", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocus();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteContact");
        am.put("deleteContact", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (table.getSelectedRow() != -1) {
                    deleteContact();
                }
            }
        });
    }

    // =================================================================
    //  HELPERS
    // =================================================================

    private void loadDataFromDisk() {
        List<Contact> loaded = fileManager.loadContacts(FileManager.DATA_FILE);
        manager.loadFromList(loaded);
    }

    private void persistAndRefresh() {
        fileManager.saveContacts(FileManager.DATA_FILE, manager.getAllContacts());
        highlightedPhone = manager.getLastAddedPhone();
        refreshTable(manager.getAllContacts());
        updateStats();
    }

    private void refreshTable(List<Contact> contacts) {
        tableModel.setRowCount(0);
        for (Contact c : contacts) {
            tableModel.addRow(new Object[]{c.getName(), c.getPhoneNumber()});
        }
    }

    private void updateStats() {
        totalContactsValue.setText(String.valueOf(manager.getTotalContacts()));
        searchCounterValue.setText(String.valueOf(manager.getSearchCount()));
        recentlyAddedValue.setText(manager.getLastAddedPhone() == null ? "-" : manager.getLastAddedPhone());
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    // =================================================================
    //  Custom rounded "card" panel with optional soft shadow + border
    // =================================================================
    private static class RoundedPanel extends JPanel {
        private int radius;
        private Color borderColor = null;

        RoundedPanel(int radius, Color bg) {
            this.radius = radius;
            setOpaque(false);
            setBackground(bg);
        }

        void setBorderColor(Color c) { this.borderColor = c; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // subtle shadow
            g2.setColor(new Color(20, 20, 40, 14));
            g2.fill(new RoundRectangle2D.Float(2, 3, getWidth() - 4, getHeight() - 4, radius, radius));

            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 2, getHeight() - 3, radius, radius));

            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, getWidth() - 3, getHeight() - 4, radius, radius));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =================================================================
    //  Simple vertical gradient background panel (used for the sidebar)
    // =================================================================
    private static class GradientPanel extends JPanel {
        private final Color top, bottom;
        GradientPanel(Color top, Color bottom) {
            this.top = top;
            this.bottom = bottom;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =================================================================
    //  Custom rounded, hover-aware pill button component
    // =================================================================
    private static class ModernButton extends JButton {
        private final Color baseColor;
        private final Color hoverColor;
        private boolean hovered = false;

        ModernButton(String text, Color baseColor) {
            super(text);
            this.baseColor = baseColor;
            this.hoverColor = baseColor.brighter();
            setContentAreaFilled(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setBorder(new EmptyBorder(9, 16, 9, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override
                public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // soft shadow
            g2.setColor(new Color(0, 0, 0, hovered ? 40 : 25));
            g2.fillRoundRect(0, 2, getWidth(), getHeight() - 2, 22, 22);
            g2.setColor(hovered ? hoverColor : baseColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            // Intentionally blank - the rounded background acts as the border.
        }
    }
}
