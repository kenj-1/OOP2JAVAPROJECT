package encantadia;

import encantadia.gamemode.GameModeType;
import encantadia.story.*;
import encantadia.ui.frames.CharacterSelectionFrame;
import encantadia.ui.frames.MainMenuFrame;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;

public class BackstoryShowcase extends JFrame {

    private static final String BG_PATH        = "/resources/background (3).png";
    private static final String PARCHMENT_PATH = "/resources/base1SBS.png";

    private static final int    PARAGRAPHS_PER_PAGE = 10;
    private static final int    CHAR_DELAY_MS       = 18;
    private static final double PARCHMENT_RATIO     = 0.42;

    private static final Color INK_DARK = new Color(0x3A, 0x18, 0x04);
    private static final Color INK_MID  = new Color(0x5C, 0x2E, 0x08);
    private static final Color INK_GOLD = new Color(0x8B, 0x60, 0x20);
    private static final Color ORNAMENT = new Color(0xA0, 0x72, 0x28);

    private JTextPane storyPane;
    private JButton   continueButton;
    private JButton   skipButton;
    private JLabel    pageLabel;
    private JPanel    parchmentPanel;
    private JPanel    innerPanel;

    // ── Story data ────────────────────────────────────────────
    private final String[]  paragraphs;
    private final String    storyTitle;  // used in HTML builder
    private final Runnable  onFinish;    // called when last page is done
    private final int       totalPages;
    private int             currentPage = 0;

    private int fontSize  = 8;
    private int titleSize = 19;

    private volatile Thread animThread = null;

    // ══════════════════════════════════════════════════════════
    //  Constructor A — StoryType based (mode lores + game lore)
    // ══════════════════════════════════════════════════════════
    public BackstoryShowcase(StoryType storyType) {
        this(storyType, null);
    }

    public BackstoryShowcase(StoryType storyType, GameModeType gameModeType) {
        this(
                GameStories.getParagraphs(storyType),
                GameStories.getTitle(storyType),
                () -> {
                    if (storyType == StoryType.GAME_LORE) {
                        new MainMenuFrame();
                    } else {
                        new CharacterSelectionFrame(
                                gameModeType != null ? gameModeType : GameModeType.PVE
                        );
                    }
                }
        );
    }

    // ══════════════════════════════════════════════════════════
    //  Constructor B — Custom paragraphs + title + callback
    //  Used for character backstories and enemy reveals.
    // ══════════════════════════════════════════════════════════
    public BackstoryShowcase(String[] paragraphs, String title, Runnable onFinish) {
        this.paragraphs = paragraphs;
        this.storyTitle = title;
        this.onFinish   = onFinish;
        this.totalPages = (int) Math.ceil((double) paragraphs.length / PARAGRAPHS_PER_PAGE);

        setTitle("Encantadia \u2013 " + title);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);
        setLayout(new BorderLayout());

        add(new BgPanel(BG_PATH), BorderLayout.CENTER);
        buildParchmentPanel();
        add(parchmentPanel, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { updateLayout(); }
        });
        ScreenManager.register(this);
        setVisible(true);
        SwingUtilities.invokeLater(() -> {
            updateLayout();
            animatePage(0);
        });
    }
    @Override
    public void dispose() {
        ScreenManager.unregister(this);
        super.dispose();
    }

    // ══════════════════════════════════════════════════════════
    //  UI construction
    // ══════════════════════════════════════════════════════════
    private void buildParchmentPanel() {
        parchmentPanel = new BgPanel(PARCHMENT_PATH);
        parchmentPanel.setLayout(new BorderLayout());

        storyPane = new JTextPane();
        storyPane.setContentType("text/html");
        storyPane.setEditable(false);
        storyPane.setOpaque(false);
        storyPane.setBackground(new Color(0, 0, 0, 0));
        storyPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        JScrollPane scroll = new JScrollPane(storyPane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        styleScrollBar(scroll.getVerticalScrollBar());

        pageLabel = new JLabel("", SwingConstants.LEFT);
        pageLabel.setFont(new Font("Serif", Font.ITALIC, 8));
        pageLabel.setForeground(ORNAMENT);

        continueButton = makeStyledButton("Continue  \u00bb");
        continueButton.setEnabled(false);
        continueButton.addActionListener(e -> handleContinue());

        skipButton = makeStyledButton("Skip  \u00bb\u00bb");
        skipButton.addActionListener(e -> handleSkip());

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightButtons.setOpaque(false);
        rightButtons.add(skipButton);
        rightButtons.add(continueButton);

        JPanel btnRow = new JPanel(new BorderLayout());
        btnRow.setOpaque(false);
        btnRow.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        btnRow.add(pageLabel,    BorderLayout.WEST);
        btnRow.add(rightButtons, BorderLayout.EAST);

        innerPanel = new JPanel(new BorderLayout(0, 4));
        innerPanel.setOpaque(false);
        innerPanel.add(scroll,  BorderLayout.CENTER);
        innerPanel.add(btnRow,  BorderLayout.SOUTH);

        parchmentPanel.add(innerPanel, BorderLayout.CENTER);
    }

    // ══════════════════════════════════════════════════════════
    //  Responsive layout
    // ══════════════════════════════════════════════════════════
    private void updateLayout() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        int parchmentH = (int)(h * PARCHMENT_RATIO);
        parchmentPanel.setPreferredSize(new Dimension(w, parchmentH));

        int hPad = (int)(w * 0.09);
        int vTop = (int)(parchmentH * 0.07);
        int vBot = (int)(parchmentH * 0.04);
        innerPanel.setBorder(BorderFactory.createEmptyBorder(vTop, hPad, vBot, hPad));

        int btnW = Math.max(110, (int)(w * 0.13));
        continueButton.setPreferredSize(new Dimension(btnW, 36));
        skipButton.setPreferredSize(new Dimension(btnW, 36));

        fontSize  = Math.max(8, (int)(parchmentH * 0.060));
        titleSize = (int)(fontSize * 1.20);

        if (animThread == null || !animThread.isAlive()) {
            renderFull(currentPage, fontSize, titleSize);
        }

        revalidate();
        repaint();
    }

    // ══════════════════════════════════════════════════════════
    //  Animation
    // ══════════════════════════════════════════════════════════
    private void animatePage(int page) {
        currentPage = page;
        stopAnimThread();

        continueButton.setEnabled(false);
        continueButton.setText(isLastPage() ? "Begin  \u00bb" : "Continue  \u00bb");
        pageLabel.setText("  Page " + (page + 1) + " of " + totalPages);

        final int fs   = fontSize;
        final int ts   = titleSize;
        final int from = page * PARAGRAPHS_PER_PAGE;
        final int to   = Math.min(from + PARAGRAPHS_PER_PAGE, paragraphs.length);

        animThread = new Thread(() -> {
            try {
                StringBuilder rawBody = new StringBuilder();
                for (int i = from; i < to; i++) rawBody.append(paragraphs[i]);
                final String fullHtml = buildPageHtml(rawBody.toString(), page == 0, fs, ts);

                final int bodyOpen  = fullHtml.indexOf("<body>") + "<body>".length();
                final int firstPara = indexOfFirstParagraph(fullHtml, bodyOpen);
                final int closing   = fullHtml.lastIndexOf("</body>");

                final String prefix   = fullHtml.substring(0, firstPara);
                final String animBody = fullHtml.substring(firstPara, closing);
                final String suffix   = "</body></html>";

                final StringBuilder visible = new StringBuilder(animBody.length());
                boolean inTag = false;

                for (int i = 0; i < animBody.length(); i++) {
                    if (Thread.currentThread().isInterrupted()) return;

                    char c = animBody.charAt(i);
                    visible.append(c);

                    if (c == '<') { inTag = true;  continue; }
                    if (c == '>') { inTag = false; continue; }
                    if (inTag)    { continue; }

                    final String snap = prefix + visible + suffix;
                    SwingUtilities.invokeLater(() -> {
                        storyPane.setText(snap);
                        int len = storyPane.getDocument().getLength();
                        storyPane.setCaretPosition(Math.max(0, len));
                    });
                    Thread.sleep(CHAR_DELAY_MS);
                }

                SwingUtilities.invokeLater(() -> {
                    storyPane.setText(fullHtml);
                    storyPane.setCaretPosition(0);
                    continueButton.setEnabled(true);
                });

            } catch (InterruptedException ignored) {}
        }, "anim-page-" + page);

        animThread.setDaemon(true);
        animThread.start();
    }

    private static int indexOfFirstParagraph(String html, int searchFrom) {
        int idx = html.indexOf("<p", searchFrom);
        return (idx >= 0) ? idx : searchFrom;
    }

    private void stopAnimThread() {
        if (animThread != null && animThread.isAlive()) {
            animThread.interrupt();
            try { animThread.join(400); } catch (InterruptedException ignored) {}
        }
        animThread = null;
    }

    private void renderFull(int page, int fs, int ts) {
        int from = page * PARAGRAPHS_PER_PAGE;
        int to   = Math.min(from + PARAGRAPHS_PER_PAGE, paragraphs.length);
        StringBuilder body = new StringBuilder();
        for (int i = from; i < to; i++) body.append(paragraphs[i]);
        storyPane.setText(buildPageHtml(body.toString(), page == 0, fs, ts));
        storyPane.setCaretPosition(0);
    }

    // ══════════════════════════════════════════════════════════
    //  Navigation
    // ══════════════════════════════════════════════════════════
    private void handleSkip() {
        if (animThread != null && animThread.isAlive()) {
            stopAnimThread();
            renderFull(currentPage, fontSize, titleSize);
            continueButton.setEnabled(true);
        }
    }

    private void handleContinue() {
        if (isLastPage()) proceed();
        else animatePage(currentPage + 1);
    }

    private boolean isLastPage() { return currentPage >= totalPages - 1; }

    private void proceed() {
        dispose();
        if (onFinish != null) onFinish.run();
    }

    // ══════════════════════════════════════════════════════════
    //  HTML builder — uses storyTitle field (not storyType)
    // ══════════════════════════════════════════════════════════
    private String buildPageHtml(String body, boolean showTitle, int fs, int ts) {
        String inkDark  = toHex(INK_DARK);
        String inkMid   = toHex(INK_MID);
        String inkGold  = toHex(INK_GOLD);
        String ornament = toHex(ORNAMENT);
        int divSize   = Math.max(10, fs - 1);
        int smallSize = Math.max(10, fs - 2);
        int dropSize  = (int)(fs * 2.1);

        String divider    = "<div class=\"divider\">&#x2015;&#x2015;&#x2015; &#x2726; &#x2015;&#x2015;&#x2015;</div>";
        String titleBlock = showTitle
                ? "<h1>" + storyTitle + "</h1>" + divider
                : "";
        String styledBody = addDropCap(body, fs);

        return "<html><head><style>"
                + "body{font-family:'Georgia','Times New Roman',serif;"
                +      "font-size:" + fs + "px;color:" + inkDark + ";"
                +      "margin:0;padding:0;line-height:1.85;text-align:justify;}"
                + "h1{font-size:" + ts + "px;letter-spacing:3px;color:" + inkMid + ";"
                +    "text-align:center;margin:0 0 4px 0;font-variant:small-caps;font-style:italic;}"
                + "p{margin:0 0 6px 0;text-indent:1.6em;}"
                + "p.first{text-indent:0;margin-top:6px;}"
                + "span.dropcap{float:left;font-size:" + dropSize + "px;line-height:0.75;"
                +              "font-family:'Georgia',serif;color:" + inkMid + ";"
                +              "padding-right:5px;padding-top:6px;font-weight:bold;font-style:italic;}"
                + ".divider{text-align:center;color:" + ornament + ";font-size:" + divSize + "px;"
                +           "letter-spacing:4px;margin:4px 0 8px 0;}"
                + ".footer-line{text-align:center;color:" + inkGold + ";font-size:" + smallSize + "px;"
                +              "font-style:italic;margin-top:6px;}"
                + "</style></head><body>"
                + titleBlock + styledBody
                + "</body></html>";
    }

    private static String addDropCap(String body, int fs) {
        int pStart = body.indexOf("<p>");
        if (pStart < 0) return body;
        int idx = pStart + 3;
        while (idx < body.length()) {
            char c = body.charAt(idx);
            if (c == '<') {
                int end = body.indexOf('>', idx);
                if (end >= 0) { idx = end + 1; continue; }
                break;
            }
            if (Character.isLetter(c)) break;
            idx++;
        }
        if (idx >= body.length()) return body;
        char drop = body.charAt(idx);
        return body.substring(0, pStart)
                + "<p class=\"first\">"
                + body.substring(pStart + 3, idx)
                + "<span class=\"dropcap\">" + drop + "</span>"
                + body.substring(idx + 1);
    }

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ══════════════════════════════════════════════════════════
    //  Scroll bar styling
    // ══════════════════════════════════════════════════════════
    private void styleScrollBar(JScrollBar bar) {
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(6, 0));
        bar.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor            = new Color(0x8B, 0x5E, 0x3C, 180);
                trackColor            = new Color(0, 0, 0, 0);
                thumbDarkShadowColor  = new Color(0, 0, 0, 0);
                thumbHighlightColor   = new Color(0, 0, 0, 0);
                thumbLightShadowColor = new Color(0, 0, 0, 0);
            }
            @Override protected JButton createDecreaseButton(int o) { return zero(); }
            @Override protected JButton createIncreaseButton(int o) { return zero(); }
            private JButton zero() {
                JButton b = new JButton();
                Dimension z = new Dimension(0, 0);
                b.setPreferredSize(z); b.setMinimumSize(z); b.setMaximumSize(z);
                return b;
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  Button factory
    // ══════════════════════════════════════════════════════════
    private JButton makeStyledButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover   = getModel().isRollover();
                boolean enabled = isEnabled();
                Color top = hover
                        ? new Color(150, 100, 35, 240)
                        : new Color(100,  58, 14, enabled ? 220 : 100);
                Color bot = hover
                        ? new Color(110,  72, 20, 240)
                        : new Color( 70,  38,  6, enabled ? 220 : 100);
                g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bot));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(200, 155, 60, enabled ? 220 : 80));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 14, 14);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(255, 210, 100, enabled ? 80 : 30));
                g2.drawRoundRect(3, 3, getWidth()-6, getHeight()-6, 10, 10);
                g2.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 8));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(new Color(0, 0, 0, 80));
                g2.drawString(getText(), tx + 1, ty + 1);
                g2.setColor(enabled ? new Color(245, 220, 155) : new Color(180, 145, 90));
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 38));
        return btn;
    }

    // ══════════════════════════════════════════════════════════
    //  Background panel
    // ══════════════════════════════════════════════════════════
    private class BgPanel extends JPanel {
        private final Image img;
        BgPanel(String path) {
            URL url = getClass().getResource(path);
            img = (url != null) ? new ImageIcon(url).getImage() : null;
            setOpaque(true);
            setBackground(Color.BLACK);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
            g2.dispose();
        }
    }
}