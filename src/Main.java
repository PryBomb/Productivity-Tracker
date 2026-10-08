import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Main.java
 * Personal Improvement Tracker - soft dark theme (Java Swing + MariaDB).
 * Uses Tracker.java as the model (one goal). Run this file's main().
 */
public class Main extends JFrame {

    // ===== COLORS =====
    // Soft dark palette: slate-navy, not pure black
    static final Color ACCENT = new Color(124, 131, 255);
    static final Color ACCENT_DARK = new Color(99, 102, 241);
    static final Color ACCENT_LIGHT = new Color(48, 53, 94);
    static final Color BG_TOP = new Color(32, 35, 52);
    static final Color BG_BOTTOM = new Color(22, 24, 37);
    static final Color CARD = new Color(41, 45, 66, 235);
    static final Color GLASS = new Color(41, 45, 66, 200);
    static final Color BORDER = new Color(62, 68, 98);
    static final Color STROKE = new Color(255, 255, 255, 24);
    static final Color GHOST = new Color(255, 255, 255, 16);
    static final Color INPUT = new Color(26, 29, 44);
    static final Color POPUP = new Color(34, 38, 57);
    static final Color TEXT = new Color(232, 234, 246);
    static final Color MUTED = new Color(154, 160, 189);
    static final Color HINT = new Color(110, 116, 148);
    static final Color GREEN = new Color(52, 211, 153);
    static final Color RED = new Color(248, 113, 113);
    static final Color AMBER = new Color(251, 191, 36);
    static final Color LIGHT = new Color(54, 59, 86);
    static final Color[] PALETTE = {
        new Color(99, 102, 241), new Color(16, 185, 129), new Color(245, 158, 11),
        new Color(236, 72, 153), new Color(14, 165, 233), new Color(139, 92, 246)
    };

    private static final String UNIT_MONEY = "money";
    private static final String UNIT_MINUTES = "minutes";
    private static final String UNIT_SESSIONS = "sessions";
    private static final String[] METRIC_LABELS = {
        "Money", "Time (minutes)", "Workout sessions"
    };

    static Font f(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    // ===== DATA + SCREEN PARTS =====
    private List<Tracker> all = new ArrayList<>();
    private int[] minutes = new int[7];
    private String filter = "All";

    private CardLayout pages = new CardLayout();
    private JPanel content = new JPanel(pages);
    private List<NavButton> navButtons = new ArrayList<>();
    private List<RoundButton> filterButtons = new ArrayList<>();
    private JLabel[] statValues = new JLabel[4];
    private JLabel todayLabel = new JLabel("0 min");
    private JTextField searchField;
    private JPanel grid = new JPanel();
    private Chart barChart = new Chart("Goal progress", 0);
    private Chart donutChart = new Chart("Completion", 1);
    private Chart weekChart = new Chart("Focus time - last 7 days", 2);

    public Main() {
        setTitle("Personal Improvement Tracker");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel background = new GradientBackground();
        background.setLayout(new BorderLayout());
        setContentPane(background);

        add(createSidebar(), BorderLayout.WEST);
        content.setOpaque(false);
        content.add(createDashboardPage(), "Dashboard");
        content.add(createGoalsPage(), "Goals");
        add(content, BorderLayout.CENTER);

        showPage("Dashboard");
        reload();
    }

    // ------------------------------------------------------------
    // SIDEBAR
    // ------------------------------------------------------------
    private JPanel createSidebar() {
        JPanel side = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(new Color(16, 18, 29, 175));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(STROKE);
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(240, 0));
        side.setBorder(BorderFactory.createEmptyBorder(26, 16, 20, 16));

        side.add(new Logo());
        side.add(Box.createVerticalStrut(30));

        JLabel menu = label("MENU", Font.BOLD, 11, HINT);
        menu.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        side.add(menu);
        side.add(Box.createVerticalStrut(10));
        side.add(nav("Dashboard", "Dashboard", 0));
        side.add(Box.createVerticalStrut(6));
        side.add(nav("Goals", "Goals", 1));
        side.add(Box.createVerticalGlue());

        RoundPanel today = new RoundPanel(new GridLayout(2, 1, 0, 2), alpha(ACCENT, 34), true);
        today.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        today.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        today.setAlignmentX(LEFT_ALIGNMENT);
        today.add(label("Focused today", Font.PLAIN, 12, MUTED));
        todayLabel.setFont(f(Font.BOLD, 24));
        todayLabel.setForeground(new Color(176, 181, 255));
        today.add(todayLabel);
        side.add(today);
        side.add(Box.createVerticalStrut(14));

        NavButton exit = new NavButton("Exit", 2);
        exit.addActionListener(e -> System.exit(0));
        side.add(exit);
        return side;
    }

    private NavButton nav(String text, String pageKey, int icon) {
        NavButton b = new NavButton(text, icon);
        b.setName(pageKey);
        b.addActionListener(e -> showPage(pageKey));
        navButtons.add(b);
        return b;
    }

    private void showPage(String name) {
        pages.show(content, name);
        for (NavButton b : navButtons) {
            b.setActive(name.equals(b.getName()));
        }
    }

    // ------------------------------------------------------------
    // DASHBOARD PAGE
    // ------------------------------------------------------------
    private JPanel createDashboardPage() {
        int hour = LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"));

        JPanel page = new JPanel(new BorderLayout(0, 20));
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(28, 32, 26, 32));
        page.add(pageHeader(greeting, date), BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 100));
        String[] names = { "Total goals", "In progress", "Completed", "Avg progress" };
        Color[] colors = { ACCENT, AMBER, GREEN, new Color(244, 114, 182) };
        for (int i = 0; i < 4; i++) {
            statValues[i] = new JLabel("0");
            statValues[i].setFont(f(Font.BOLD, 32));
            statValues[i].setForeground(colors[i]);
            RoundPanel card = new RoundPanel(new BorderLayout());
            card.setBorder(BorderFactory.createEmptyBorder(14, 20, 12, 20));
            card.add(label(names[i], Font.PLAIN, 13, MUTED), BorderLayout.NORTH);
            card.add(statValues[i], BorderLayout.CENTER);
            stats.add(card);
        }

        donutChart.setPreferredSize(new Dimension(330, 0));
        barChart.setPreferredSize(new Dimension(0, 205));
        JPanel middle = new JPanel(new BorderLayout(16, 0));
        middle.setOpaque(false);
        middle.add(weekChart, BorderLayout.CENTER);
        middle.add(donutChart, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(stats, BorderLayout.NORTH);
        body.add(middle, BorderLayout.CENTER);
        body.add(barChart, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel pageHeader(String title, String subtitle) {
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label(title, Font.BOLD, 28, TEXT));
        text.add(label(subtitle, Font.PLAIN, 14, MUTED));

        RoundButton add = new RoundButton("+  New Goal", ACCENT_DARK, Color.WHITE).gradient();
        add.addActionListener(e -> {
            showPage("Goals");
            doAdd();
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        right.setOpaque(false);
        right.add(add);

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(text, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ------------------------------------------------------------
    // GOALS PAGE (search + filters + cards)
    // ------------------------------------------------------------
    private JPanel createGoalsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(28, 32, 20, 32));

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(pageHeader("Goals", "Track, log and finish what matters"), BorderLayout.NORTH);

        // search box
        RoundField search = new RoundField("", "Search goals...").withSearchIcon();
        search.setPreferredSize(new Dimension(280, 40));
        searchField = search;
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { renderGoals(); }
            public void removeUpdate(DocumentEvent e) { renderGoals(); }
            public void changedUpdate(DocumentEvent e) { renderGoals(); }
        });

        // filter chips
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        chips.setOpaque(false);
        for (String name : new String[] { "All", "Active", "Completed" }) {
            RoundButton b = new RoundButton(name, GHOST, MUTED).outline(STROKE).pill();
            b.addActionListener(e -> {
                filter = name;
                styleFilters();
                renderGoals();
            });
            filterButtons.add(b);
            chips.add(b);
        }
        styleFilters();

        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setOpaque(false);
        bar.add(searchField, BorderLayout.WEST);
        bar.add(chips, BorderLayout.CENTER);
        top.add(bar, BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        // scrolling area with the cards
        grid.setOpaque(false);
        ScrollPanel wrapper = new ScrollPanel(new BorderLayout());
        wrapper.add(grid, BorderLayout.NORTH);
        page.add(darkScroll(wrapper), BorderLayout.CENTER);
        return page;
    }

    private void styleFilters() {
        for (RoundButton b : filterButtons) {
            boolean on = b.getText().equals(filter);
            b.setScheme(on ? ACCENT_DARK : GHOST, on ? Color.WHITE : MUTED);
        }
    }

    // Rebuilds the goal cards using the search text and the filter chip
    private void renderGoals() {
        String q = searchField.getText().trim().toLowerCase();
        List<Tracker> list = new ArrayList<>();
        for (Tracker g : all) {
            boolean done = "COMPLETED".equals(g.getStatus());
            if (filter.equals("Active") && done) continue;
            if (filter.equals("Completed") && !done) continue;
            if (!q.isEmpty() && !g.getTitle().toLowerCase().contains(q)
                    && !g.getCategory().toLowerCase().contains(q)) continue;
            list.add(g);
        }

        grid.removeAll();
        if (list.isEmpty()) {
            grid.setLayout(new GridLayout(1, 1));
            RoundPanel empty = new RoundPanel(new BorderLayout());
            empty.setPreferredSize(new Dimension(0, 160));
            JLabel msg = label("No goals here yet. Click \"+ New Goal\" to add one.", Font.PLAIN, 15, MUTED);
            msg.setHorizontalAlignment(SwingConstants.CENTER);
            empty.add(msg, BorderLayout.CENTER);
            grid.add(empty);
        } else {
            grid.setLayout(new GridLayout(0, 3, 16, 16));
            for (Tracker g : list) {
                grid.add(createCard(g));
            }
        }
        grid.revalidate();
        grid.repaint();
    }

    private JPanel createCard(Tracker g) {
        boolean done = "COMPLETED".equals(g.getStatus());
        Color catColor = PALETTE[Math.abs(g.getCategory().toLowerCase().hashCode()) % PALETTE.length];

        RoundPanel card = new RoundPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setPreferredSize(new Dimension(250, 200));

        // top: category chip + delete
        JPanel chipBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chipBox.setOpaque(false);
        chipBox.add(new Chip(g.getCategory(), catColor));
        RoundButton del = new RoundButton("\u00d7", GHOST, MUTED).withHover(alpha(RED, 50), RED);
        del.setFont(f(Font.PLAIN, 20));
        del.setBorder(BorderFactory.createEmptyBorder(0, 9, 2, 9));
        del.addActionListener(e -> doDelete(g));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(chipBox, BorderLayout.WEST);
        top.add(del, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // middle: title, info, progress
        JLabel title = label(g.getTitle(), Font.BOLD, 17, TEXT);
        boolean overdue = isOverdue(g);
        JLabel info = label(infoText(g), Font.PLAIN, 12, overdue ? RED : MUTED);
        JLabel metricInfo = label(metricSummary(g), Font.PLAIN, 12, MUTED);
        JPanel barRow = new JPanel(new BorderLayout(10, 0));
        barRow.setOpaque(false);
        barRow.add(new Bar(g.getProgress(), done ? GREEN : ACCENT), BorderLayout.CENTER);
        barRow.add(label(g.getProgress() + "%", Font.BOLD, 12, TEXT), BorderLayout.EAST);
        barRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        barRow.setAlignmentX(LEFT_ALIGNMENT);

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.add(title);
        mid.add(Box.createVerticalStrut(4));
        mid.add(info);
        mid.add(Box.createVerticalStrut(4));
        mid.add(metricInfo);
        mid.add(Box.createVerticalGlue());
        mid.add(barRow);
        card.add(mid, BorderLayout.CENTER);

        // bottom: action buttons
        JPanel actions = new JPanel(new GridLayout(1, done ? 1 : 4, 6, 0));
        actions.setOpaque(false);
        RoundButton his = new RoundButton("History", GHOST, TEXT).outline(STROKE).compact();
        his.addActionListener(e -> doHistory(g));
        if (!done) {
            RoundButton log = new RoundButton("+ Log", alpha(ACCENT, 40), new Color(176, 181, 255)).compact();
            RoundButton upd = new RoundButton("Edit", GHOST, TEXT).outline(STROKE).compact();
            RoundButton fin = new RoundButton("Done", alpha(GREEN, 38), GREEN).compact();
            log.addActionListener(e -> doLog(g));
            upd.addActionListener(e -> doEdit(g));
            fin.addActionListener(e -> doComplete(g));
            actions.add(log);
            actions.add(upd);
            actions.add(his);
            actions.add(fin);
        } else {
            actions.add(his);
        }
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private boolean isOverdue(Tracker g) {
        return !"COMPLETED".equals(g.getStatus()) && g.getTargetDate() != null
                && LocalDate.parse(g.getTargetDate()).isBefore(LocalDate.now());
    }

    private String infoText(Tracker g) {
        String logs = g.getLogCount() + (g.getLogCount() == 1 ? " log" : " logs");
        if ("COMPLETED".equals(g.getStatus())) {
            return "Completed  \u00b7  " + logs;
        }
        if (g.getTargetDate() == null) {
            return "No deadline  \u00b7  " + logs;
        }
        long d = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(g.getTargetDate()));
        String due = d > 1 ? "Due in " + d + " days" : d == 1 ? "Due tomorrow"
                   : d == 0 ? "Due today" : "Overdue by " + (-d) + (d == -1 ? " day" : " days");
        return due + "  \u00b7  " + logs;
    }

    // ------------------------------------------------------------
    // LOADING DATA
    // ------------------------------------------------------------
    private void reload() {
        try {
            all = getGoals(null);
            minutes = getMinutesLast7Days();
        } catch (SQLException e) {
            showError("Could not load data", e);
            return;
        }

        int done = 0, sum = 0;
        for (Tracker t : all) {
            sum += t.getProgress();
            if ("COMPLETED".equals(t.getStatus())) {
                done++;
            }
        }
        statValues[0].setText("" + all.size());
        statValues[1].setText("" + (all.size() - done));
        statValues[2].setText("" + done);
        statValues[3].setText((all.isEmpty() ? 0 : sum / all.size()) + "%");
        todayLabel.setText(minutes[6] + " min");

        String[] days = new String[7];
        for (int i = 0; i < 7; i++) {
            days[i] = LocalDate.now().minusDays(6 - i).getDayOfWeek().toString().substring(0, 3);
        }
        for (Chart c : new Chart[] { barChart, donutChart, weekChart }) {
            c.setData(all, minutes, days);
        }
        renderGoals();
    }

    // ------------------------------------------------------------
    // DIALOG HELPERS (all dialogs share the same dark glass look)
    // ------------------------------------------------------------
    private JDialog newDialog(int w, int h) {
        final JDialog d = new JDialog(this, "", Dialog.ModalityType.APPLICATION_MODAL);
        d.setUndecorated(true);
        d.setSize(w, h);
        d.setLocationRelativeTo(this);
        try {
            d.setBackground(new Color(0, 0, 0, 0));
        } catch (UnsupportedOperationException ignored) {
            // translucent windows not supported: the dialog just keeps square corners
        }
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return d;
    }

    private void focusOnOpen(JDialog d, JComponent c) {
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                c.requestFocusInWindow();
            }
        });
    }

    private JPanel dialogHeader(JDialog d, String title, String subtitle) {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(label(title, Font.BOLD, 24, TEXT));
        box.add(Box.createVerticalStrut(5));
        box.add(label(subtitle, Font.PLAIN, 13, MUTED));

        RoundButton close = new RoundButton("\u00d7", GHOST, MUTED).withHover(alpha(RED, 45), RED);
        close.setFont(f(Font.PLAIN, 22));
        close.setBorder(BorderFactory.createEmptyBorder(2, 11, 4, 11));
        close.addActionListener(e -> d.dispose());
        JPanel closeBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeBox.setOpaque(false);
        closeBox.add(close);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(box, BorderLayout.WEST);
        header.add(closeBox, BorderLayout.EAST);
        return header;
    }

    private JPanel formPanel() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(22, 0, 10, 0));
        return form;
    }

    private JLabel errorLabel() {
        return label(" ", Font.PLAIN, 12, RED);
    }

    private JPanel footer(JComponent... buttons) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        for (JComponent b : buttons) {
            footer.add(b);
        }
        return footer;
    }

    private JPanel fieldBlock(String caption, JComponent field) {
        return fieldBlock(label(caption, Font.BOLD, 12, MUTED), field);
    }

    private JPanel fieldBlock(JLabel caption, JComponent field) {
        JPanel block = new JPanel(new BorderLayout(0, 7));
        block.setOpaque(false);
        block.setAlignmentX(LEFT_ALIGNMENT);
        block.add(caption, BorderLayout.NORTH);
        block.add(field, BorderLayout.CENTER);
        field.setPreferredSize(new Dimension(0, 42));
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, block.getPreferredSize().height));
        return block;
    }

    private static String defaultMetricForCategory(String category) {
        if ("Finance".equals(category)) {
            return UNIT_MONEY;
        }
        if ("Fitness".equals(category)) {
            return UNIT_SESSIONS;
        }
        return UNIT_MINUTES;
    }

    private static String metricLabel(String unit) {
        if (UNIT_MONEY.equals(unit)) return "Money";
        if (UNIT_SESSIONS.equals(unit)) return "Workout sessions";
        return "Time (minutes)";
    }

    private static String metricUnitForLabel(String label) {
        if ("Money".equals(label)) return UNIT_MONEY;
        if ("Workout sessions".equals(label)) return UNIT_SESSIONS;
        return UNIT_MINUTES;
    }

    private static String amountPrompt(String unit) {
        if (UNIT_MONEY.equals(unit)) return "e.g. 25.50";
        if (UNIT_SESSIONS.equals(unit)) return "e.g. 1";
        return "e.g. 30";
    }

    private static String amountPromptLabel(String unit) {
        if (UNIT_MONEY.equals(unit)) return "Amount added";
        if (UNIT_SESSIONS.equals(unit)) return "Sessions completed";
        return "Minutes spent";
    }

    private static String metricValueText(BigDecimal value, String unit) {
        String formatted = new java.text.DecimalFormat("#,##0.##").format(value);
        if (UNIT_MONEY.equals(unit)) return formatted + " amount";
        if (UNIT_SESSIONS.equals(unit)) return formatted + (BigDecimal.ONE.compareTo(value) == 0
                ? " session" : " sessions");
        return formatted + " min";
    }

    private static String metricSummary(Tracker goal) {
        String current = new java.text.DecimalFormat("#,##0.##").format(goal.getCurrentValue());
        String target = new java.text.DecimalFormat("#,##0.##").format(goal.getTargetValue());
        if (UNIT_MONEY.equals(goal.getMetricUnit())) {
            return "Amount: " + current + " / " + target;
        }
        if (UNIT_SESSIONS.equals(goal.getMetricUnit())) {
            return current + " / " + target + " sessions";
        }
        return current + " / " + target + " min";
    }

    // ------------------------------------------------------------
    // GOAL DIALOG (new + edit)
    // ------------------------------------------------------------
    private GoalFormData showGoalDialog(Tracker existing) {
        final boolean creating = existing == null;
        final JDialog dialog = newDialog(540, 570);
        final GoalFormData[] result = new GoalFormData[1];

        GlassDialogPanel root = new GlassDialogPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(28, 30, 24, 30));
        root.add(dialogHeader(dialog,
                creating ? "Create a new goal" : "Edit goal",
                creating ? "Set something meaningful and make progress every day."
                         : "Update the details of your goal."), BorderLayout.NORTH);

        JPanel form = formPanel();

        RoundField titleField = new RoundField(creating ? "" : existing.getTitle(), "e.g. Finish the Java course");
        form.add(fieldBlock("Goal title", titleField));
        form.add(Box.createVerticalStrut(14));

        RoundCombo categoryBox = new RoundCombo(new String[] {
                "Health", "Study", "Career", "Fitness", "Finance", "Personal", "Skills", "Other" });
        if (!creating && existing.getCategory() != null) {
            categoryBox.setSelectedItem(existing.getCategory());
            if (categoryBox.getSelectedIndex() < 0) {
                categoryBox.addItem(existing.getCategory());
                categoryBox.setSelectedItem(existing.getCategory());
            }
        }
        form.add(fieldBlock("Category", categoryBox));
        form.add(Box.createVerticalStrut(14));

        RoundCombo metricBox = new RoundCombo(METRIC_LABELS);
        metricBox.setSelectedItem(metricLabel(creating
                ? defaultMetricForCategory(String.valueOf(categoryBox.getSelectedItem()))
                : existing.getMetricUnit()));
        RoundField targetValueField = new RoundField(
                creating ? "" : existing.getTargetValue().stripTrailingZeros().toPlainString(),
                "Enter a target");
        form.add(fieldBlock("Measure", metricBox));
        form.add(Box.createVerticalStrut(14));
        JLabel targetCaption = label("Target (" + metricBox.getSelectedItem() + ")",
                Font.BOLD, 12, MUTED);
        form.add(fieldBlock(targetCaption, targetValueField));
        form.add(Box.createVerticalStrut(14));

        boolean canChangeMetric = creating || existing.getLogCount() == 0;
        metricBox.setEnabled(canChangeMetric);
        categoryBox.setEnabled(canChangeMetric);
        metricBox.setToolTipText(canChangeMetric ? null
                : "The category and measure cannot be changed after progress has been logged.");
        categoryBox.setToolTipText(metricBox.getToolTipText());
        categoryBox.addActionListener(e -> {
            if (canChangeMetric) {
                metricBox.setSelectedItem(metricLabel(
                        defaultMetricForCategory(String.valueOf(categoryBox.getSelectedItem()))));
                targetCaption.setText("Target (" + metricBox.getSelectedItem() + ")");
            }
        });
        metricBox.addActionListener(e ->
                targetCaption.setText("Target (" + metricBox.getSelectedItem() + ")"));

        // Target date
        JPanel dateRow = new JPanel(new BorderLayout(10, 0));
        dateRow.setOpaque(false);

        Switch enableDate = new Switch("Set target date");
        java.util.Date initialDate = new java.util.Date();
        if (!creating && existing.getTargetDate() != null) {
            try {
                initialDate = new java.util.Date(Date.valueOf(existing.getTargetDate()).getTime());
                enableDate.setSelected(true);
            } catch (Exception ignored) {
            }
        }

        RoundSpinner dateSpinner = new RoundSpinner(
                new SpinnerDateModel(initialDate, null, null, java.util.Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "MMM d, yyyy");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setPreferredSize(new Dimension(190, 42));
        Runnable applyDate = () -> {
            boolean on = enableDate.isSelected();
            dateSpinner.setEnabled(on);
            dateEditor.getTextField().setEnabled(on);
            dateSpinner.repaint();
        };
        enableDate.addActionListener(e -> applyDate.run());
        applyDate.run();

        dateRow.add(enableDate, BorderLayout.WEST);
        dateRow.add(dateSpinner, BorderLayout.EAST);
        form.add(fieldBlock("Target date", dateRow));

        JLabel error = errorLabel();
        form.add(Box.createVerticalStrut(10));
        form.add(error);
        form.add(Box.createVerticalGlue());

        // Footer
        RoundButton cancel = new RoundButton("Cancel", GHOST, TEXT).outline(STROKE);
        RoundButton save = new RoundButton(creating ? "+  Create Goal" : "Save Changes",
                ACCENT_DARK, Color.WHITE).gradient();
        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            String goalTitle = titleField.getText().trim();
            if (goalTitle.isEmpty()) {
                error.setText("Please enter a goal title.");
                titleField.requestFocusInWindow();
                return;
            }
            String metricUnit = metricUnitForLabel(String.valueOf(metricBox.getSelectedItem()));
            BigDecimal targetValue;
            try {
                targetValue = new BigDecimal(targetValueField.getText().trim());
                if (targetValue.signum() <= 0) {
                    throw new NumberFormatException();
                }
                if (targetValue.compareTo(new BigDecimal("999999999999.99")) > 0) {
                    throw new NumberFormatException();
                }
                if (UNIT_MONEY.equals(metricUnit)) {
                    if (targetValue.stripTrailingZeros().scale() > 2) {
                        throw new NumberFormatException();
                    }
                } else if (targetValue.stripTrailingZeros().scale() > 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                error.setText(UNIT_MONEY.equals(metricUnit)
                        ? "Enter a positive amount with up to 2 decimal places."
                        : "Enter a positive whole-number target.");
                targetValueField.requestFocusInWindow();
                return;
            }
            String category = String.valueOf(categoryBox.getSelectedItem());
            Date targetDate = null;
            if (enableDate.isSelected()) {
                java.util.Date selected = (java.util.Date) dateSpinner.getValue();
                targetDate = new Date(selected.getTime());
            }
            result[0] = new GoalFormData(goalTitle, category, targetDate, targetValue, metricUnit);
            dialog.dispose();
        });

        root.add(form, BorderLayout.CENTER);
        root.add(footer(cancel, save), BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(save);
        focusOnOpen(dialog, titleField);
        dialog.setVisible(true);
        return result[0];
    }

    static class GoalFormData {
        String title;
        String category;
        Date targetDate;
        BigDecimal targetValue;
        String metricUnit;

        GoalFormData(String title, String category, Date targetDate, BigDecimal targetValue,
                     String metricUnit) {
            this.title = title;
            this.category = category;
            this.targetDate = targetDate;
            this.targetValue = targetValue;
            this.metricUnit = metricUnit;
        }
    }

    // ------------------------------------------------------------
    // ACTIONS (each works on the goal whose card you clicked)
    // ------------------------------------------------------------
    private void doAdd() {
        GoalFormData data = showGoalDialog(null);
        if (data == null) return;

        try {
            addGoal(data.title, data.category, data.targetDate, data.targetValue, data.metricUnit);
            reload();
        } catch (SQLException e) {
            showError("Could not add goal", e);
        }
    }

    private void doEdit(Tracker g) {
        GoalFormData data = showGoalDialog(g);
        if (data == null) return;

        try {
            updateGoal(g.getGoalId(), data.title, data.category, data.targetDate,
                    data.targetValue, data.metricUnit);
            reload();
        } catch (SQLException e) {
            showError("Could not update goal", e);
        }
    }

    private void doLog(Tracker g) {
        final JDialog d = newDialog(500, 400);
        final boolean[] saved = { false };
        final String[] noteOut = { "" };
        final BigDecimal[] amountOut = { BigDecimal.ZERO };

        GlassDialogPanel root = new GlassDialogPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(28, 30, 24, 30));
        root.add(dialogHeader(d, "Log progress", g.getTitle()), BorderLayout.NORTH);

        JPanel form = formPanel();
        RoundField noteField = new RoundField("", "e.g. Finished chapter 3 exercises");
        RoundField amountField = new RoundField("", amountPrompt(g.getMetricUnit()));
        form.add(fieldBlock("What did you do today?", noteField));
        form.add(Box.createVerticalStrut(14));
        form.add(fieldBlock(amountPromptLabel(g.getMetricUnit())
                + " (progress updates automatically)", amountField));
        JLabel error = errorLabel();
        form.add(Box.createVerticalStrut(10));
        form.add(error);
        form.add(Box.createVerticalGlue());

        RoundButton cancel = new RoundButton("Cancel", GHOST, TEXT).outline(STROKE);
        RoundButton save = new RoundButton("Save entry", ACCENT_DARK, Color.WHITE).gradient();
        cancel.addActionListener(e -> d.dispose());
        save.addActionListener(e -> {
            String note = noteField.getText().trim();
            if (note.isEmpty()) {
                error.setText("The note cannot be empty.");
                noteField.requestFocusInWindow();
                return;
            }
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountField.getText().trim());
                if (amount.signum() <= 0) {
                    throw new NumberFormatException();
                }
                if (amount.compareTo(new BigDecimal("999999999999.99")) > 0) {
                    throw new NumberFormatException();
                }
                if (UNIT_MONEY.equals(g.getMetricUnit())) {
                    if (amount.stripTrailingZeros().scale() > 2) {
                        throw new NumberFormatException();
                    }
                } else if (amount.stripTrailingZeros().scale() > 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                error.setText(UNIT_MONEY.equals(g.getMetricUnit())
                        ? "Enter a positive amount with up to 2 decimal places."
                        : "Enter a positive whole number.");
                amountField.requestFocusInWindow();
                return;
            }
            noteOut[0] = note;
            amountOut[0] = amount;
            saved[0] = true;
            d.dispose();
        });

        root.add(form, BorderLayout.CENTER);
        root.add(footer(cancel, save), BorderLayout.SOUTH);
        d.setContentPane(root);
        d.getRootPane().setDefaultButton(save);
        focusOnOpen(d, noteField);
        d.setVisible(true);

        if (!saved[0]) {
            return;
        }
        try {
            boolean logged = addDailyProgress(g.getGoalId(), noteOut[0], amountOut[0], g.getMetricUnit());
            if (!logged) {
                showMessage("This goal is no longer active or its measure changed. Reload and try again.");
                reload();
                return;
            }
            reload();
            for (Tracker t : all) {
                if (t.getGoalId() == g.getGoalId() && t.getProgress() >= 100) {
                    showMessage("You reached 100%! Click \"Done\" on the card to finish this goal.");
                }
            }
        } catch (SQLException e) {
            showError("Could not save the entry", e);
        }
    }

    private void doComplete(Tracker g) {
        if (!confirm("Complete goal", "Mark \"" + g.getTitle() + "\" as completed?", "Complete", false)) {
            return;
        }
        try {
            markCompleted(g.getGoalId());
            reload();
        } catch (SQLException e) {
            showError("Could not complete goal", e);
        }
    }

    private void doDelete(Tracker g) {
        if (!confirm("Delete goal",
                "Delete \"" + g.getTitle() + "\"?\nIts daily entries will be deleted too.", "Delete", true)) {
            return;
        }
        try {
            deleteGoal(g.getGoalId());
            reload();
        } catch (SQLException e) {
            showError("Could not delete goal", e);
        }
    }

    private void doHistory(Tracker g) {
        List<String> logs;
        try {
            logs = getRecentLogs(g.getGoalId(), 15, g.getMetricUnit());
        } catch (SQLException e) {
            showError("Could not load history", e);
            return;
        }

        final JDialog d = newDialog(540, 540);
        GlassDialogPanel root = new GlassDialogPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(28, 30, 24, 30));
        root.add(dialogHeader(d, g.getTitle(), "History  \u00b7  last 15 entries"), BorderLayout.NORTH);

        ScrollPanel list = new ScrollPanel(null);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        if (logs.isEmpty()) {
            JLabel none = label("No daily entries yet.", Font.PLAIN, 14, MUTED);
            list.add(none);
        } else {
            for (String line : logs) {
                list.add(historyRow(line));
                list.add(Box.createVerticalStrut(8));
            }
        }
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(20, 0, 14, 0));
        center.add(darkScroll(list), BorderLayout.CENTER);

        RoundButton close = new RoundButton("Close", ACCENT_DARK, Color.WHITE).gradient();
        close.addActionListener(e -> d.dispose());

        root.add(center, BorderLayout.CENTER);
        root.add(footer(close), BorderLayout.SOUTH);
        d.setContentPane(root);
        d.getRootPane().setDefaultButton(close);
        d.setVisible(true);
    }

    // One history entry: "date  |  N min  |  note"
    private JComponent historyRow(String line) {
        String[] p = line.split("\\s*\\|\\s*", 3);
        String date = p.length > 0 ? p[0] : "";
        String mins = p.length > 1 ? p[1] : "";
        String note = p.length > 2 ? p[2] : "";

        RoundPanel row = new RoundPanel(new BorderLayout(0, 6), new Color(255, 255, 255, 12), true);
        row.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(label(date, Font.PLAIN, 12, MUTED), BorderLayout.WEST);
        JPanel chipBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        chipBox.setOpaque(false);
        chipBox.add(new Chip(mins, ACCENT));
        head.add(chipBox, BorderLayout.EAST);

        JLabel text = new JLabel("<html><div style='width:360px'>" + esc(note) + "</div></html>");
        text.setFont(f(Font.PLAIN, 14));
        text.setForeground(TEXT);

        row.add(head, BorderLayout.NORTH);
        row.add(text, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ------------------------------------------------------------
    // MESSAGE / CONFIRM DIALOGS
    // ------------------------------------------------------------
    private void showMessage(String text) {
        messageDialog("Heads up", text, "OK", false, false);
    }

    private boolean confirm(String title, String text, String yesText, boolean danger) {
        return messageDialog(title, text, yesText, true, danger);
    }

    private void showError(String title, SQLException e) {
        String msg = e.getMessage() == null ? "Unknown error" : e.getMessage();
        messageDialog(title, msg, "OK", false, true);
    }

    private boolean messageDialog(String title, String message, String yesText,
                                  boolean cancelable, boolean danger) {
        final boolean[] ok = { false };
        final JDialog d = newDialog(440, 220);

        GlassDialogPanel root = new GlassDialogPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(26, 30, 22, 30));
        root.add(label(title, Font.BOLD, 20, TEXT), BorderLayout.NORTH);

        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(f(Font.PLAIN, 14));
        text.setForeground(MUTED);
        text.setBorder(BorderFactory.createEmptyBorder(10, 0, 24, 0));
        text.setSize(new Dimension(380, Short.MAX_VALUE));
        text.setPreferredSize(new Dimension(380, text.getPreferredSize().height));
        root.add(text, BorderLayout.CENTER);

        RoundButton yes = danger
                ? new RoundButton(yesText, new Color(239, 68, 68), Color.WHITE)
                : new RoundButton(yesText, ACCENT_DARK, Color.WHITE).gradient();
        yes.addActionListener(e -> {
            ok[0] = true;
            d.dispose();
        });
        if (cancelable) {
            RoundButton cancel = new RoundButton("Cancel", GHOST, TEXT).outline(STROKE);
            cancel.addActionListener(e -> d.dispose());
            root.add(footer(cancel, yes), BorderLayout.SOUTH);
        } else {
            root.add(footer(yes), BorderLayout.SOUTH);
        }

        d.setContentPane(root);
        d.getRootPane().setDefaultButton(yes);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        return ok[0];
    }

    // ------------------------------------------------------------
    // SMALL HELPERS
    // ------------------------------------------------------------
    static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(f(style, size));
        l.setForeground(color);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    // Slightly brighter version of a color (used for hover)
    static Color hoverOf(Color c) {
        int a = c.getAlpha();
        return new Color(Math.min(255, c.getRed() + 16), Math.min(255, c.getGreen() + 16),
                Math.min(255, c.getBlue() + 16), a < 255 ? Math.min(255, a + 24) : 255);
    }

    // Slightly darker version of a color (used while pressed)
    static Color pressedOf(Color c) {
        return new Color((int) (c.getRed() * 0.85), (int) (c.getGreen() * 0.85),
                (int) (c.getBlue() * 0.85), c.getAlpha());
    }

    // Scroll pane with transparent background and a slim modern scrollbar
    static JScrollPane darkScroll(JComponent view) {
        JScrollPane sp = new JScrollPane(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setViewportBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        JScrollBar bar = sp.getVerticalScrollBar();
        bar.setUI(new SlimScrollBarUI());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(10, 0));
        bar.setUnitIncrement(18);
        return sp;
    }

    // Panel that follows the width of the scroll pane
    static class ScrollPanel extends JPanel implements Scrollable {
        ScrollPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 96; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    static class SlimScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() { }

        @Override
        protected JButton createDecreaseButton(int orientation) { return zero(); }

        @Override
        protected JButton createIncreaseButton(int orientation) { return zero(); }

        private JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            b.setOpaque(false);
            b.setBorder(null);
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = smooth(g);
            g2.setColor(new Color(255, 255, 255, isDragging ? 110 : isThumbRollover() ? 85 : 50));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------
    // CUSTOM-DRAWN COMPONENTS
    // ------------------------------------------------------------

    // Dark gradient background with soft glowing blobs
    static class GradientBackground extends JPanel {
        GradientBackground() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, BG_TOP, w, h, BG_BOTTOM));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(124, 131, 255, 40));
            g2.fillOval(-120, -100, 380, 380);
            g2.setColor(new Color(56, 189, 248, 22));
            g2.fillOval(w - 300, 60, 380, 380);
            g2.setColor(new Color(192, 132, 252, 24));
            g2.fillOval(w - 440, h - 260, 440, 440);

            g2.dispose();
        }
    }

    // Dark glass panel used by every dialog
    static class GlassDialogPanel extends JPanel {
        GlassDialogPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth() - 1;
            int h = getHeight() - 1;

            g2.setColor(new Color(33, 37, 56));
            g2.fillRoundRect(0, 0, w, h, 28, 28);

            g2.setColor(new Color(255, 255, 255, 40));
            g2.drawRoundRect(0, 0, w, h, 28, 28);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // App logo at the top of the sidebar
    static class Logo extends JPanel {
        Logo() {
            setOpaque(false);
            setPreferredSize(new Dimension(190, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setAlignmentX(LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setPaint(new GradientPaint(4, 3, new Color(143, 150, 255), 38, 37, ACCENT_DARK));
            g2.fillRoundRect(4, 3, 34, 34, 11, 11);
            // target icon: outer ring, inner ring, center dot
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(10, 9, 22, 22);
            g2.drawOval(15, 14, 12, 12);
            g2.fillOval(19, 18, 4, 4);
            g2.setColor(TEXT);
            g2.setFont(f(Font.BOLD, 21));
            g2.drawString("Tracker", 48, 28);
            g2.dispose();
        }
    }

    // Dark card with a soft shadow, rounded corners and a thin light outline
    static class RoundPanel extends JPanel {
        private final Color bg;
        private final boolean outline;

        RoundPanel(LayoutManager layout) {
            this(layout, CARD, true);
        }

        RoundPanel(LayoutManager layout, Color bg, boolean outline) {
            super(layout);
            this.bg = bg;
            this.outline = outline;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth() - 1;
            int h = getHeight() - 1;

            // soft shadow
            g2.setColor(new Color(0, 0, 0, 45));
            g2.fillRoundRect(2, 5, Math.max(0, w - 3), Math.max(0, h - 4), 22, 22);

            // surface
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w, h, 22, 22);

            // subtle inner highlight
            g2.setColor(new Color(255, 255, 255, 10));
            g2.drawRoundRect(1, 1, Math.max(0, w - 2), Math.max(0, h - 2), 22, 22);

            if (outline) {
                g2.setColor(STROKE);
                g2.drawRoundRect(0, 0, w, h, 22, 22);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Modern rounded button: hover, pressed, optional outline, gradient or pill shape
    static class RoundButton extends JButton {
        private Color bg, fg, outline, hoverBg, hoverFg;
        private boolean hover, pressed, gradient, pill;
        private int radius = 14;

        RoundButton(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg;
            this.fg = fg;
            setForeground(fg);
            setFont(f(Font.BOLD, 13));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    setForeground(hoverFg != null ? hoverFg : RoundButton.this.fg);
                    repaint();
                }
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    pressed = false;
                    setForeground(RoundButton.this.fg);
                    repaint();
                }
                public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        RoundButton outline(Color c) { this.outline = c; return this; }
        RoundButton gradient() { this.gradient = true; return this; }
        RoundButton pill() { this.pill = true; return this; }

        RoundButton withHover(Color hoverBg, Color hoverFg) {
            this.hoverBg = hoverBg;
            this.hoverFg = hoverFg;
            return this;
        }

        void setScheme(Color bg, Color fg) {
            this.bg = bg;
            this.fg = fg;
            setForeground(fg);
            repaint();
        }

        RoundButton compact() {
            setFont(f(Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 2));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            int arc = pill ? h : radius;

            if (gradient) {
                Color top = new Color(143, 150, 255);
                Color bottom = new Color(99, 102, 241);
                if (hover) {
                    top = hoverOf(top);
                    bottom = hoverOf(bottom);
                }
                if (pressed) {
                    top = pressedOf(top);
                    bottom = pressedOf(bottom);
                }
                g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(255, 255, 255, 45));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            } else {
                Color c = pressed ? pressedOf(bg)
                        : hover ? (hoverBg != null ? hoverBg : hoverOf(bg)) : bg;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                if (outline != null) {
                    g2.setColor(outline);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                }
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Sidebar item: drawn icon, hover glow, glowing active state with an accent bar
    static class NavButton extends JButton {
        private final int icon;
        private boolean active, hover;

        NavButton(String text, int icon) {
            super(text);
            this.icon = icon;
            setFont(f(Font.BOLD, 14));
            setForeground(MUTED);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 48, 0, 0));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(190, 44));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setAlignmentX(LEFT_ALIGNMENT);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; updateColor(); }
                public void mouseExited(MouseEvent e) { hover = false; updateColor(); }
            });
        }

        void setActive(boolean a) {
            active = a;
            updateColor();
        }

        private void updateColor() {
            setForeground(active ? new Color(176, 181, 255)
                    : hover ? (icon == 2 ? RED : TEXT) : MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            if (active) {
                g2.setPaint(new GradientPaint(0, 0, alpha(ACCENT, 70), w, 0, alpha(ACCENT, 10)));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, h / 2 - 10, 4, 20, 4, 4);
            } else if (hover) {
                g2.setColor(icon == 2 ? alpha(RED, 28) : new Color(255, 255, 255, 14));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            }
            g2.setColor(getForeground());
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = h / 2;
            if (icon == 0) {            // dashboard: four small squares
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        g2.drawRoundRect(17 + i * 10, cy - 10 + j * 10, 7, 7, 3, 3);
                    }
                }
            } else if (icon == 1) {     // goals: target
                g2.drawOval(17, cy - 10, 20, 20);
                g2.drawOval(23, cy - 4, 8, 8);
            } else if (icon == 2) {     // exit: door with arrow
                g2.drawPolyline(new int[] { 26, 18, 18, 26 }, new int[] { cy - 9, cy - 9, cy + 9, cy + 9 }, 4);
                g2.drawLine(24, cy, 36, cy);
                g2.drawPolyline(new int[] { 32, 36, 32 }, new int[] { cy - 4, cy, cy + 4 }, 3);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Small colored category label
    static class Chip extends JLabel {
        private final Color color;

        Chip(String text, Color color) {
            super(text);
            this.color = color;
            setFont(f(Font.BOLD, 11));
            setForeground(new Color(
                    (color.getRed() + 255 * 2) / 3,
                    (color.getGreen() + 255 * 2) / 3,
                    (color.getBlue() + 255 * 2) / 3));
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(alpha(color, 55));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Thin progress bar
    static class Bar extends JComponent {
        private final int value;
        private final Color color;

        Bar(int value, Color color) {
            this.value = value;
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int y = getHeight() / 2 - 4;
            g2.setColor(BORDER);
            g2.fillRoundRect(0, y, getWidth(), 8, 8, 8);
            if (value > 0) {
                g2.setColor(color);
                g2.fillRoundRect(0, y, Math.max(8, getWidth() * value / 100), 8, 8, 8);
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------
    // FORM CONTROLS (dark, rounded)
    // ------------------------------------------------------------

    // Rounded text field with placeholder text (and optional search icon)
    static class RoundField extends JTextField {
        private final String prompt;
        private boolean searchIcon;

        RoundField(String text, String prompt) {
            super(text);
            this.prompt = prompt;
            setOpaque(false);
            setFont(f(Font.PLAIN, 14));
            setForeground(TEXT);
            setCaretColor(TEXT);
            setSelectionColor(alpha(ACCENT, 120));
            setSelectedTextColor(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { repaint(); }
                public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        RoundField withSearchIcon() {
            searchIcon = true;
            setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 14));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            g2.setColor(INPUT);
            g2.fillRoundRect(0, 0, w, h, 14, 14);
            g2.setColor(hasFocus() ? ACCENT : BORDER);
            g2.drawRoundRect(0, 0, w, h, 14, 14);
            if (searchIcon) {
                g2.setColor(MUTED);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cy = getHeight() / 2;
                g2.drawOval(16, cy - 7, 10, 10);
                g2.drawLine(25, cy + 2, 29, cy + 6);
            }
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && prompt != null) {
                Graphics2D p = smooth(g);
                FontMetrics fm = getFontMetrics(getFont());
                p.setColor(HINT);
                p.setFont(getFont());
                p.drawString(prompt, getInsets().left, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                p.dispose();
            }
        }
    }

    // Rounded dark drop-down
    static class RoundCombo extends JComboBox<String> {
        RoundCombo(String[] items) {
            super(items);
            setUI(new DarkComboUI());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setBackground(POPUP);
            setForeground(TEXT);
            setFont(f(Font.PLAIN, 14));
            setMaximumRowCount(8);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean selected, boolean focus) {
                    JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, false);
                    l.setFont(f(Font.PLAIN, 14));
                    l.setForeground(TEXT);
                    l.setOpaque(true);
                    l.setBackground(selected ? new Color(62, 68, 128) : POPUP);
                    l.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
                    return l;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            g2.setColor(INPUT);
            g2.fillRoundRect(0, 0, w, h, 14, 14);
            g2.setColor(isPopupVisible() || hasFocus() ? ACCENT : BORDER);
            g2.drawRoundRect(0, 0, w, h, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class DarkComboUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = smooth(g);
                    g2.setColor(MUTED);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    g2.drawPolyline(new int[] { cx - 5, cx, cx + 5 }, new int[] { cy - 2, cy + 3, cy - 2 }, 3);
                    g2.dispose();
                }
            };
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder());
            return b;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) { }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            Object v = comboBox.getSelectedItem();
            if (v == null) return;
            Graphics2D g2 = smooth(g);
            g2.setFont(f(Font.PLAIN, 14));
            g2.setColor(TEXT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(v.toString(), bounds.x + 14,
                    bounds.y + (bounds.height + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }

        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup p = (BasicComboPopup) super.createPopup();
            p.setBorder(BorderFactory.createLineBorder(BORDER));
            return p;
        }
    }

    // Rounded dark spinner (numbers and dates)
    static class RoundSpinner extends JSpinner {
        RoundSpinner(SpinnerModel model) {
            super(model);
            setUI(new DarkSpinnerUI());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            styleEditor(this);
        }

        @Override
        public void setEditor(JComponent editor) {
            super.setEditor(editor);
            styleEditor(this);
        }

        private static void styleEditor(JSpinner sp) {
            JComponent ed = sp.getEditor();
            ed.setOpaque(false);
            if (ed instanceof JSpinner.DefaultEditor) {
                JFormattedTextField tf = ((JSpinner.DefaultEditor) ed).getTextField();
                tf.setOpaque(false);
                tf.setFont(f(Font.PLAIN, 14));
                tf.setForeground(TEXT);
                tf.setDisabledTextColor(HINT);
                tf.setCaretColor(TEXT);
                tf.setSelectionColor(alpha(ACCENT, 120));
                tf.setSelectedTextColor(Color.WHITE);
                tf.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 6));
                tf.setHorizontalAlignment(JTextField.LEFT);
                tf.addFocusListener(new FocusAdapter() {
                    public void focusGained(FocusEvent e) { sp.repaint(); }
                    public void focusLost(FocusEvent e) { sp.repaint(); }
                });
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            boolean focused = getEditor() instanceof JSpinner.DefaultEditor
                    && ((JSpinner.DefaultEditor) getEditor()).getTextField().hasFocus();
            g2.setColor(isEnabled() ? INPUT : new Color(28, 31, 46));
            g2.fillRoundRect(0, 0, w, h, 14, 14);
            g2.setColor(!isEnabled() ? new Color(48, 53, 78) : focused ? ACCENT : BORDER);
            g2.drawRoundRect(0, 0, w, h, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class DarkSpinnerUI extends BasicSpinnerUI {
        private JSpinner sp;

        @Override
        public void installUI(JComponent c) {
            sp = (JSpinner) c;
            super.installUI(c);
        }

        @Override
        protected Component createNextButton() { return arrow(true); }

        @Override
        protected Component createPreviousButton() { return arrow(false); }

        private void step(boolean up) {
            if (sp == null || !sp.isEnabled()) return;
            Object v = up ? sp.getNextValue() : sp.getPreviousValue();
            if (v != null) {
                sp.setValue(v);
            }
        }

        // Small chevron button; keeps stepping while the mouse is held down
        private JButton arrow(boolean up) {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = smooth(g);
                    boolean on = sp != null && sp.isEnabled();
                    g2.setColor(!on ? new Color(80, 86, 116) : getModel().isPressed() ? TEXT : MUTED);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    if (up) {
                        g2.drawPolyline(new int[] { cx - 4, cx, cx + 4 }, new int[] { cy + 2, cy - 2, cy + 2 }, 3);
                    } else {
                        g2.drawPolyline(new int[] { cx - 4, cx, cx + 4 }, new int[] { cy - 2, cy + 2, cy - 2 }, 3);
                    }
                    g2.dispose();
                }
            };
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setFocusable(false);
            b.setBorder(null);
            b.setPreferredSize(new Dimension(28, 14));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final Timer repeat = new Timer(80, e -> step(up));
            repeat.setInitialDelay(400);
            b.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { step(up); repeat.start(); }
                public void mouseReleased(MouseEvent e) { repeat.stop(); }
                public void mouseExited(MouseEvent e) { repeat.stop(); }
            });
            return b;
        }
    }

    // Toggle switch with a label
    static class Switch extends JCheckBox {
        Switch(String text) {
            super(text);
            setFont(f(Font.PLAIN, 13));
            setOpaque(false);
            setFocusPainted(false);
            setBorder(null);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(40 + 10 + fm.stringWidth(getText()), 28);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = 40, h = 22, y = (getHeight() - h) / 2;
            g2.setColor(isSelected() ? ACCENT_DARK : new Color(70, 76, 108));
            g2.fillRoundRect(0, y, w, h, h, h);
            g2.setColor(Color.WHITE);
            g2.fillOval(isSelected() ? w - h + 3 : 3, y + 3, h - 6, h - 6);
            g2.setFont(getFont());
            g2.setColor(isSelected() ? TEXT : MUTED);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), w + 10, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    // Dashboard chart card. type 0 = goal progress bars, 1 = ring, 2 = focus minutes per day
    static class Chart extends JPanel {
        private final String title;
        private final int type;
        private List<Tracker> goals = new ArrayList<>();
        private int[] minutes = new int[7];
        private String[] days = { "", "", "", "", "", "", "" };
        private float anim = 1f;

        Chart(String title, int type) {
            this.title = title;
            this.type = type;
            setOpaque(false);
        }

        // New data: the chart grows in smoothly
        void setData(List<Tracker> goals, int[] minutes, String[] days) {
            this.goals = goals;
            this.minutes = minutes;
            this.days = days;
            anim = 0f;
            Timer timer = new Timer(16, null);
            timer.addActionListener(e -> {
                anim += 0.07f;
                if (anim >= 1f) {
                    anim = 1f;
                    timer.stop();
                }
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            g2.setColor(new Color(0, 0, 0, 45));
            g2.fillRoundRect(2, 5, Math.max(0, w - 3), Math.max(0, h - 4), 22, 22);
            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, w, h, 22, 22);
            g2.setColor(STROKE);
            g2.drawRoundRect(0, 0, w, h, 22, 22);
            g2.setColor(TEXT);
            g2.setFont(f(Font.BOLD, 15));
            g2.drawString(title, 22, 34);
            if (type == 0) {
                drawBars(g2);
            } else if (type == 1) {
                drawRing(g2);
            } else {
                drawWeek(g2);
            }
            g2.dispose();
        }

        private void centerText(Graphics2D g2, String s, int cx, int y) {
            g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, y);
        }

        private void drawBars(Graphics2D g2) {
            g2.setFont(f(Font.PLAIN, 13));
            if (goals.isEmpty()) {
                g2.setColor(MUTED);
                centerText(g2, "No goals yet - add one!", getWidth() / 2, getHeight() / 2 + 14);
                return;
            }
            int trackX = 210, trackW = getWidth() - trackX - 70;
            for (int i = 0; i < Math.min(goals.size(), 4); i++) {
                Tracker t = goals.get(i);
                int y = 56 + i * 34;
                String name = t.getTitle().length() > 24 ? t.getTitle().substring(0, 23) + "..." : t.getTitle();
                g2.setColor(TEXT);
                g2.drawString(name, 22, y + 11);
                g2.setColor(BORDER);
                g2.fillRoundRect(trackX, y + 2, trackW, 8, 8, 8);
                int fill = (int) (trackW * t.getProgress() / 100.0 * anim);
                if (fill > 0) {
                    g2.setColor("COMPLETED".equals(t.getStatus()) ? GREEN : ACCENT);
                    g2.fillRoundRect(trackX, y + 2, Math.max(8, fill), 8, 8, 8);
                }
                g2.setColor(MUTED);
                g2.drawString(t.getProgress() + "%", trackX + trackW + 12, y + 11);
            }
        }

        private void drawRing(Graphics2D g2) {
            int total = goals.size(), done = 0;
            for (Tracker t : goals) {
                if ("COMPLETED".equals(t.getStatus())) {
                    done++;
                }
            }
            int d = Math.max(60, Math.min(getWidth() - 110, getHeight() - 110));
            int x = (getWidth() - d) / 2, y = 54;
            g2.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(BORDER);
            g2.draw(new Arc2D.Double(x, y, d, d, 0, 360, Arc2D.OPEN));
            if (total > 0) {
                double doneExt = 360.0 * done / total * anim;
                double activeExt = 360.0 * (total - done) / total * anim;
                if (activeExt > 4) {
                    g2.setColor(ACCENT);
                    g2.draw(new Arc2D.Double(x, y, d, d, 90 - doneExt, -Math.max(0, activeExt - 8), Arc2D.OPEN));
                }
                if (doneExt > 0) {
                    g2.setColor(GREEN);
                    g2.draw(new Arc2D.Double(x, y, d, d, 90, -doneExt, Arc2D.OPEN));
                }
            }
            g2.setColor(TEXT);
            g2.setFont(f(Font.BOLD, 24));
            centerText(g2, (total == 0 ? 0 : done * 100 / total) + "%", getWidth() / 2, y + d / 2 + 9);

            g2.setFont(f(Font.PLAIN, 12));
            int ly = getHeight() - 22, lx = getWidth() / 2 - 85;
            g2.setColor(ACCENT);
            g2.fillOval(lx, ly - 9, 10, 10);
            g2.setColor(MUTED);
            g2.drawString("In progress " + (total - done), lx + 15, ly);
            g2.setColor(GREEN);
            g2.fillOval(lx + 100, ly - 9, 10, 10);
            g2.setColor(MUTED);
            g2.drawString("Done " + done, lx + 115, ly);
        }

        private void drawWeek(Graphics2D g2) {
            int max = 30, sum = 0;
            for (int m : minutes) {
                max = Math.max(max, m);
                sum += m;
            }
            g2.setFont(f(Font.PLAIN, 12));
            g2.setColor(MUTED);
            String totalText = sum + " min total";
            g2.drawString(totalText, getWidth() - 22 - g2.getFontMetrics().stringWidth(totalText), 34);

            int left = 28, base = getHeight() - 34, chartH = base - 70;
            for (int i = 0; i <= 2; i++) {          // faint guide lines
                g2.setColor(LIGHT);
                g2.drawLine(left, base - chartH * i / 2, getWidth() - left, base - chartH * i / 2);
            }
            int slot = (getWidth() - 2 * left) / 7, bw = Math.min(40, slot * 55 / 100);
            for (int i = 0; i < 7; i++) {
                int x = left + i * slot + (slot - bw) / 2;
                int h = (int) (chartH * (double) minutes[i] / max * anim);
                if (minutes[i] == 0) {
                    g2.setColor(BORDER);
                    g2.fillRoundRect(x, base - 4, bw, 4, 4, 4);
                } else {
                    g2.setColor(i == 6 ? ACCENT : new Color(70, 77, 134));
                    g2.fillRoundRect(x, base - Math.max(h, 6), bw, Math.max(h, 6), 10, 10);
                    g2.setColor(TEXT);
                    centerText(g2, minutes[i] + "m", x + bw / 2, base - Math.max(h, 6) - 7);
                }
                g2.setColor(i == 6 ? TEXT : MUTED);
                centerText(g2, days[i], x + bw / 2, base + 20);
            }
        }
    }

    // ------------------------------------------------------------
    // DATABASE CODE (all the SQL is here)
    // ------------------------------------------------------------

    // ====== DATABASE SETTINGS (change to match your MariaDB) ======
    private static final String URL = "jdbc:mariadb://localhost:3306/improvement_tracker";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        try (ResultSet columns = conn.getMetaData().getColumns(conn.getCatalog(), null, table, null)) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void ensureColumn(Connection conn, String table, String column, String definition)
            throws SQLException {
        if (!hasColumn(conn, table, column)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition)) {
                ps.executeUpdate();
            }
        }
    }

    private static void ensureMetricSchema(Connection conn) throws SQLException {
        ensureColumn(conn, "goals", "target_value", "DECIMAL(14,2) NULL");
        ensureColumn(conn, "goals", "metric_unit", "VARCHAR(20) NULL");
        ensureColumn(conn, "daily_progress", "amount", "DECIMAL(14,2) NULL");

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE goals SET target_value = target_minutes WHERE target_value IS NULL")) {
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE goals SET metric_unit = ? WHERE metric_unit IS NULL OR metric_unit = ''")) {
            ps.setString(1, UNIT_MINUTES);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE daily_progress SET amount = minutes_spent WHERE amount IS NULL")) {
            ps.executeUpdate();
        }
    }

    // Gets goals. If keyword is null, gets ALL goals.
    public static List<Tracker> getGoals(String keyword) throws SQLException {
        List<Tracker> list = new ArrayList<>();

        String sql = "SELECT g.goal_id, g.title, g.category, g.target_date, "
                   + "g.progress, g.status, g.target_value, g.metric_unit, "
                   + "COALESCE(SUM(d.amount), 0) AS current_value, COUNT(d.log_id) AS log_count "
                   + "FROM goals g LEFT JOIN daily_progress d ON g.goal_id = d.goal_id ";
        if (keyword != null) {
            sql += "WHERE g.title LIKE ? OR g.category LIKE ? ";
        }
        sql += "GROUP BY g.goal_id ORDER BY g.goal_id";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (keyword != null) {
                ps.setString(1, "%" + keyword + "%");
                ps.setString(2, "%" + keyword + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Tracker(
                    rs.getInt("goal_id"),
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getString("target_date"), // null if no deadline
                    rs.getInt("progress"),
                    rs.getString("status"),
                    rs.getInt("log_count"),
                    rs.getBigDecimal("target_value"),
                    rs.getBigDecimal("current_value"),
                    rs.getString("metric_unit")));
            }
        }
        return list;
    }

    // Add goal. The target is interpreted in the goal's metric unit.
    public static void addGoal(String title, String category, Date targetDate,
                               BigDecimal targetValue, String metricUnit)
            throws SQLException {
        int legacyTarget = UNIT_MINUTES.equals(metricUnit)
                ? targetValue.min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue()
                : 1;
        String sql = "INSERT INTO goals (title, category, target_date, target_minutes, "
                   + "target_value, metric_unit, created_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?, CURDATE())";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, category);
            if (targetDate == null) {
                ps.setNull(3, Types.DATE);
            } else {
                ps.setDate(3, targetDate);
            }
            ps.setInt(4, legacyTarget);
            ps.setBigDecimal(5, targetValue);
            ps.setString(6, metricUnit);
            ps.executeUpdate();
        }
    }

    // Edit goal details. Category and metric are locked in the UI once logs exist.
    public static boolean updateGoal(int id, String title, String category, Date targetDate,
                                     BigDecimal targetValue, String metricUnit) throws SQLException {
        int legacyTarget = UNIT_MINUTES.equals(metricUnit)
                ? targetValue.min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue()
                : 1;
        String sql = "UPDATE goals SET title = ?, category = ?, target_date = ?, target_minutes = ?, "
                   + "target_value = ?, metric_unit = ?, progress = CASE WHEN status = 'COMPLETED' "
                   + "THEN 100 ELSE LEAST(100, FLOOR((SELECT COALESCE(SUM(amount), 0) "
                   + "FROM daily_progress WHERE goal_id = ?) * 100 / ?)) END WHERE goal_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, category);
            if (targetDate == null) {
                ps.setNull(3, Types.DATE);
            } else {
                ps.setDate(3, targetDate);
            }
            ps.setInt(4, legacyTarget);
            ps.setBigDecimal(5, targetValue);
            ps.setString(6, metricUnit);
            ps.setInt(7, id);
            ps.setBigDecimal(8, targetValue);
            ps.setInt(9, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Save an entry, then recalculate progress using the goal's own metric.
    public static boolean addDailyProgress(int id, String note, BigDecimal amount, String metricUnit)
            throws SQLException {
        int legacyMinutes = UNIT_MINUTES.equals(metricUnit)
                ? amount.min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue()
                : 0;
        String insert = "INSERT INTO daily_progress (goal_id, log_date, note, minutes_spent, amount) "
                      + "SELECT goal_id, CURDATE(), ?, ?, ? FROM goals "
                      + "WHERE goal_id = ? AND status = 'ACTIVE' AND metric_unit = ?";
        String recalc = "UPDATE goals SET progress = LEAST(100, "
                      + "FLOOR((SELECT COALESCE(SUM(amount), 0) FROM daily_progress WHERE goal_id = ?) "
                      + "* 100 / target_value)) WHERE goal_id = ? AND target_value > 0";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean saved;
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, note);
                    ps.setInt(2, legacyMinutes);
                    ps.setBigDecimal(3, amount);
                    ps.setInt(4, id);
                    ps.setString(5, metricUnit);
                    saved = ps.executeUpdate() > 0;
                }
                if (saved) {
                    try (PreparedStatement ps = conn.prepareStatement(recalc)) {
                        ps.setInt(1, id);
                        ps.setInt(2, id);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return saved;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }

    // Last entries of one goal, newest first
    public static List<String> getRecentLogs(int goalId, int limit, String metricUnit) throws SQLException {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT log_date, note, amount FROM daily_progress "
                   + "WHERE goal_id = ? ORDER BY log_id DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                logs.add(rs.getString("log_date") + "  |  "
                        + metricValueText(rs.getBigDecimal("amount"), metricUnit) + "  |  "
                        + rs.getString("note"));
            }
        }
        return logs;
    }

    // Minutes spent per day for the last 7 days (oldest first, today last)
    public static int[] getMinutesLast7Days() throws SQLException {
        int[] result = new int[7];
        String sql = "SELECT d.log_date, SUM(d.amount) AS total FROM daily_progress d "
                   + "JOIN goals g ON g.goal_id = d.goal_id "
                   + "WHERE d.log_date >= CURDATE() - INTERVAL 6 DAY "
                   + "AND g.metric_unit = ? GROUP BY d.log_date";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UNIT_MINUTES);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LocalDate d = rs.getDate("log_date").toLocalDate();
                long ago = java.time.temporal.ChronoUnit.DAYS.between(d, LocalDate.now());
                if (ago >= 0 && ago <= 6) {
                    result[6 - (int) ago] = rs.getInt("total");
                }
            }
        }
        return result;
    }

    // 6. Mark completed. Returns false if goal not found or already COMPLETED.
    public static boolean markCompleted(int id) throws SQLException {
        String sql = "UPDATE goals SET status = 'COMPLETED', progress = 100 "
                   + "WHERE goal_id = ? AND status = 'ACTIVE'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // 7. Delete goal (its daily entries are deleted too)
    public static boolean deleteGoal(int id) throws SQLException {
        String sql = "DELETE FROM goals WHERE goal_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------
    // START THE PROGRAM
    // ------------------------------------------------------------
    public static void main(String[] args) {
        // Every control is custom-drawn, so the default Java look is the cleanest base
        // (no Nimbus: it fights with custom colors).

        // Test the database connection first
        try (Connection conn = getConnection()) {
            ensureMetricSchema(conn);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Cannot prepare the database for goal tracking:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}