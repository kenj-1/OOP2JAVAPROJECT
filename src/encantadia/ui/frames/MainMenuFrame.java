package encantadia.ui.frames;

import encantadia.ScreenManager;
import encantadia.gamemode.ArcadeMode;
import encantadia.gamemode.GameModeType;
import encantadia.gamemode.PVEMode;
import encantadia.gamemode.PVPMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;

public class MainMenuFrame extends JFrame {

    private JButton arcadeButton;
    private JButton PVPButton;
    private JButton PVEButton;
    private JButton exitGameButton;

    private static final String BG_PATH      = "/resources/background.png";
    private static final String COLUMNS_PATH = "/resources/columns.png";
    private static final String TITLE_PATH   = "/resources/mainMenu (1).png";
    private static final String HOLDER_PATH  = "/resources/mainMenuHolder.png";
    private static final String BTN_ARCADE   = "/resources/ArcadeButton (1).png";
    private static final String BTN_PVE      = "/resources/PVEButton (1).png";
    private static final String BTN_PVP      = "/resources/PVPButton (1).png";
    private static final String BTN_EXIT     = "/resources/exitButton (3).png";

    // Keep references for repositioning
    private ImagePanel holderPanel;
    private JPanel     buttonsInsideHolder;
    private JPanel     exitRow;
    private ScaledImagePanel titlePanel;

    public MainMenuFrame() {
        setTitle("Encantadia: Echoes of the Gem — Main Menu");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setResizable(true);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        setContentPane(layeredPane);

        // ── Layer 0: background ───────────────────────────────
        BackgroundPanel backgroundPanel = new BackgroundPanel(BG_PATH);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        // ── Layer 1: columns ──────────────────────────────────
        ScaledImagePanel columnsPanel = new ScaledImagePanel(COLUMNS_PATH);
        layeredPane.add(columnsPanel, JLayeredPane.PALETTE_LAYER);

        // ── Layer 2: title ────────────────────────────────────
        titlePanel = new ScaledImagePanel(TITLE_PATH);
        layeredPane.add(titlePanel, JLayeredPane.MODAL_LAYER);

        // ── Layer 3: stone holder — contains the 3 buttons ───
        holderPanel = new ImagePanel(HOLDER_PATH);
        holderPanel.setLayout(new GridBagLayout());
        layeredPane.add(holderPanel, JLayeredPane.POPUP_LAYER);

        // Buttons stacked inside the holder
        arcadeButton = createImageButton(BTN_ARCADE);
        PVEButton    = createImageButton(BTN_PVE);
        PVPButton    = createImageButton(BTN_PVP);

        buttonsInsideHolder = new JPanel();
        buttonsInsideHolder.setOpaque(false);
        buttonsInsideHolder.setLayout(new BoxLayout(buttonsInsideHolder, BoxLayout.Y_AXIS));

        PVPButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        PVEButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        arcadeButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonsInsideHolder.add(Box.createVerticalGlue());
        buttonsInsideHolder.add(PVPButton);
        buttonsInsideHolder.add(Box.createVerticalStrut(14));
        buttonsInsideHolder.add(PVEButton);
        buttonsInsideHolder.add(Box.createVerticalStrut(14));
        buttonsInsideHolder.add(arcadeButton);
        buttonsInsideHolder.add(Box.createVerticalGlue());

        holderPanel.add(buttonsInsideHolder);

        // ── Layer 4: exit button — below the holder ───────────
        exitGameButton = createImageButton(BTN_EXIT);
        exitRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        exitRow.setOpaque(false);
        exitRow.add(exitGameButton);
        layeredPane.add(exitRow, JLayeredPane.DRAG_LAYER);

        // ── Actions ───────────────────────────────────────────
        PVPButton.addActionListener(e    -> launchMode(GameModeType.PVP));
        PVEButton.addActionListener(e    -> launchMode(GameModeType.PVE));
        arcadeButton.addActionListener(e -> launchMode(GameModeType.ARCADE));
        exitGameButton.addActionListener(e -> {
            dispose();
            new WelcomeScreenPage();
        });

        // ── Resize ────────────────────────────────────────────
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                reposition(layeredPane, backgroundPanel, columnsPanel);
            }
        });

        setVisible(true);
        ScreenManager.register(this);
        SwingUtilities.invokeLater(() -> reposition(layeredPane, backgroundPanel, columnsPanel));
    }
    @Override
    public void dispose() {
        ScreenManager.unregister(this);
        super.dispose();
    }

    // ── Layout ────────────────────────────────────────────────
    private void reposition(JLayeredPane pane, JPanel bg, JPanel columns) {
        int w = pane.getWidth();
        int h = pane.getHeight();
        if (w == 0 || h == 0) return;

        bg.setBounds(0, 0, w, h);
        columns.setBounds(0, 0, w, h);

        // ── Title ─────────────────────────────────────────────────
        int titleW = (int)(w * 0.42);
        int titleH = (int)(titleW * 0.28);
        int titleX = (w - titleW) / 2;
        int titleY = (int)(h * 0.04);
        titlePanel.setBounds(titleX, titleY, titleW, titleH);

        // ── Holder ────────────────────────────────────────────────
        int holderW = (int)(w * 0.38);         // slightly narrower
        int holderH = (int)(h * 0.58);         // slightly shorter so exit fits
        int holderX = (w - holderW) / 2;
        int holderY = titleY + titleH + (int)(h * 0.015);  // tighter gap below title
        holderPanel.setBounds(holderX, holderY, holderW, holderH);

        // ── Buttons inside holder ─────────────────────────────────
        int btnW = (int)(holderW * 0.76);
        int btnH = (int)(btnW * 0.24);         // shorter, matches wooden sign ratio

        int usableH   = (int)(holderH * 0.75); // usable area inside stone border
        int totalBtnH = btnH * 3;
        int gap       = Math.max(10, (usableH - totalBtnH) / 4);

        for (JButton btn : new JButton[]{PVPButton, PVEButton, arcadeButton}) {
            btn.setPreferredSize(new Dimension(btnW, btnH));
            btn.setMinimumSize(new Dimension(btnW, btnH));
            btn.setMaximumSize(new Dimension(btnW, btnH));
        }

        buttonsInsideHolder.setBounds(0, 0, holderW, holderH);
        buttonsInsideHolder.removeAll();
        buttonsInsideHolder.add(Box.createVerticalGlue());
        buttonsInsideHolder.add(PVPButton);
        buttonsInsideHolder.add(Box.createVerticalStrut(gap));
        buttonsInsideHolder.add(PVEButton);
        buttonsInsideHolder.add(Box.createVerticalStrut(gap));
        buttonsInsideHolder.add(arcadeButton);
        buttonsInsideHolder.add(Box.createVerticalGlue());

        // ── Exit button — raised closer to the holder ─────────────
        int exitW = (int)(btnW * 0.46);
        int exitH = (int)(exitW * 0.45);
        exitGameButton.setPreferredSize(new Dimension(exitW, exitH));
        exitGameButton.setMinimumSize(new Dimension(exitW, exitH));
        exitGameButton.setMaximumSize(new Dimension(exitW, exitH));

        // ✅ Reduced gap between holder bottom and exit button
        int exitRowY = holderY + holderH + (int)(h * 0.008);
        exitRow.setBounds((w - exitW) / 2, exitRowY, exitW, exitH + 4);

        holderPanel.revalidate();
        holderPanel.repaint();
        exitRow.revalidate();
        exitRow.repaint();
    }

    private void launchMode(GameModeType mode) {
        dispose();
        switch (mode) {
            case PVE:    new PVEMode();    break;
            case PVP:    new PVPMode();    break;
            case ARCADE: new ArcadeMode(); break;
        }
    }

    // ── Image button ──────────────────────────────────────────
    private JButton createImageButton(String path) {
        Image img = loadImage(path);
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                if (img == null) { super.paintComponent(g); return; }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int iw = img.getWidth(null), ih = img.getHeight(null);
                if (iw <= 0 || ih <= 0) { g2.dispose(); return; }
                double scale = Math.min((double)getWidth()/iw, (double)getHeight()/ih);
                int dw = (int)(iw*scale), dh = (int)(ih*scale);
                int x = (getWidth()-dw)/2,  y = (getHeight()-dh)/2;
                if (getModel().isRollover())
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.80f));
                g2.drawImage(img, x, y, dw, dh, null);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        if (url == null) { System.err.println("Missing: " + path); return null; }
        return new ImageIcon(url).getImage();
    }

    private class BackgroundPanel extends JPanel {
        private final Image img;
        BackgroundPanel(String path) { img = loadImage(path); setOpaque(true); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        }
    }

    private class ScaledImagePanel extends JPanel {
        private final Image img;
        ScaledImagePanel(String path) { img = loadImage(path); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        }
    }

    private class ImagePanel extends JPanel {
        private final Image img;
        ImagePanel(String path) { img = loadImage(path); setOpaque(false); }
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

    private void createUIComponents() {}
}