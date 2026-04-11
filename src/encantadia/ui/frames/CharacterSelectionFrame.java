package encantadia.ui.frames;

import encantadia.BackstoryShowcase;
import encantadia.ScreenManager;
import encantadia.battle.EnemyFactory;
import encantadia.battle.arcade.ArcadeModeManager;
import encantadia.characters.*;
import encantadia.characters.Character;
import encantadia.gamemode.GameModeType;
import encantadia.story.CharacterStories;
import encantadia.ui.frames.battleModeFrames.ArcadeModeBattleFrame;
import encantadia.ui.frames.battleModeFrames.PVEBattleFrame;
import encantadia.ui.frames.battleModeFrames.PVPBattleFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

public class CharacterSelectionFrame extends JFrame {
    public static int FRAME_SIZE = 800;
    private final GameModeType gameModeType;
    private static final String BG_PATH    = "/resources/background (4).png";
    private static final String TITLE_PATH = "/resources/chooseSangreTitle.png";
    private JButton exitButton;

    private static final String[] FRAME_IMGS = {
            "/resources/TyroneFrameName.png",
            "/resources/ElanFrameName.png",
            "/resources/ClaireFrameName.png",
            "/resources/DirkFrameName.png",
            "/resources/FlamaraFrameName.png",
            "/resources/DeaFrameName.png",
            "/resources/AdamusFrameName.png",
            "/resources/TeraFrameName.png"
    };
    private static final String[] CHAR_NAMES = {
            "Tyrone", "Elan", "Claire", "Dirk",
            "Flamara", "Dea", "Adamus", "Tera"
    };
    private static final Color[] GLOW_COLORS = {
            new Color(0xFF, 0x60, 0x20),   // Tyrone  — orange-red
            new Color(0x40, 0xA0, 0xFF),   // Elan    — blue
            new Color(0x40, 0xE0, 0x60),   // Claire  — green
            new Color(0xFF, 0xCC, 0x30),   // Dirk    — gold
            new Color(0xFF, 0x40, 0x20),   // Flamara — red
            new Color(0x60, 0xA0, 0xFF),   // Dea     — blue
            new Color(0x30, 0xDD, 0x88),   // Adamus  — teal
            new Color(0xFF, 0xCC, 0x00),   // Tera    — yellow
    };

    private BgPanel        bgPanel;
    private ScaledImgPanel titlePanel;
    private CharFramePanel[] framePanels = new CharFramePanel[8];

    // ── Base Y positions for bob animation ────────────────────
    private final int[]    baseY   = new int[8];
    private double         animTick = 0;
    private Timer          animTimer;

    public CharacterSelectionFrame(GameModeType gameModeType) {
        this.gameModeType = gameModeType;
        init();
    }

    public CharacterSelectionFrame() { this(GameModeType.PVE); }

    private void init() {

        setTitle("Choose Your Sangre  [" + gameModeType.name() + "]");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setResizable(true);

        JLayeredPane lp = new JLayeredPane();
        lp.setLayout(null);
        setContentPane(lp);

        bgPanel = new BgPanel(BG_PATH);
        lp.add(bgPanel, JLayeredPane.DEFAULT_LAYER);

        titlePanel = new ScaledImgPanel(TITLE_PATH);
        lp.add(titlePanel, JLayeredPane.PALETTE_LAYER);

        Character[] roster = buildRoster();
        for (int i = 0; i < 8; i++) {
            final int    idx = i;
            final Character ch = roster[i];

            framePanels[i] = new CharFramePanel(
                    FRAME_IMGS[i], CHAR_NAMES[i], GLOW_COLORS[i], i);

            framePanels[i].addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    onCharacterSelected(ch);
                }
                @Override public void mouseEntered(MouseEvent e) {
                    framePanels[idx].setHovered(true);
                }
                @Override public void mouseExited(MouseEvent e) {
                    framePanels[idx].setHovered(false);
                }
            });
            framePanels[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lp.add(framePanels[i], JLayeredPane.MODAL_LAYER);
        }

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { reposition(lp); }
        });

        // ── Animation timer: 60 fps ───────────────────────────
        animTimer = new Timer(16, e -> {
            animTick += 0.06;
            for (int i = 0; i < 8; i++) {
                // Each icon gets a phase offset so they don't all bob together
                double phase = animTick + i * (Math.PI / 4);
                int bob = (int)(Math.sin(phase) * 5);
                if (baseY[i] > 0) {
                    framePanels[i].setLocation(framePanels[i].getX(), baseY[i] + bob);
                }
                framePanels[i].repaint();
            }
        });
        animTimer.start();

        setVisible(true);
        ScreenManager.register(this);
        SwingUtilities.invokeLater(() -> reposition(lp));

        exitButton = new JButton();
        ImageIcon exitIcon = new ImageIcon(getClass().getResource("/resources/exitButton (1).png"));
        Image img = exitIcon.getImage();
        exitButton.setIcon(exitIcon);

        // Remove default button styling
                exitButton.setContentAreaFilled(false);
                exitButton.setBorderPainted(false);
                exitButton.setFocusPainted(false);
                exitButton.setOpaque(false);
                exitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Click action → go to Main Menu
                exitButton.addActionListener(e -> {
                    dispose();
                    new MainMenuFrame();
                });

                lp.add(exitButton, JLayeredPane.POPUP_LAYER);
    }

    @Override
    public void dispose() {
        if (animTimer != null) animTimer.stop();
        ScreenManager.unregister(this);
        super.dispose();
    }

    private void reposition(JLayeredPane pane) {
        int W = pane.getWidth();
        int H = pane.getHeight();
        if (W == 0 || H == 0) return;

        bgPanel.setBounds(0, 0, W, H);

        double scale = Math.min(W / 1024.0, H / 768.0);
        scale = Math.min(scale, 1.5);

        // Title
        int titleW = (int)(560 * scale);
        int titleH = (int)(titleW / 3.5);
        int titleX = (W - titleW) / 2;
        int titleY = (int)(30 * scale);
        titlePanel.setBounds(titleX, titleY, titleW, titleH);

        // OUTER FRAME SIZE
        int cellW = (int)(150 * scale);
        int cellH = (int)(150 * scale);
        int gapX  = (int)(36 * scale);
        int gapY  = (int)(32 * scale);
        int cols  = 4;

        int gridW = cols * cellW + (cols - 1) * gapX;
        int gridX = (W - gridW) / 2;
        int gridY = titleY + titleH + (int)(28 * scale);

        for (int i = 0; i < 8; i++) {
            int col = i % cols;
            int row = i / cols;
            int fx  = gridX + col * (cellW + gapX);
            int fy  = gridY + row * (cellH + gapY);
            framePanels[i].setBounds(fx, fy, cellW, cellH);
            baseY[i] = fy;
        }

        int exitW = (int)(140 * scale);
        int exitH = (int)(50 * scale);

        int exitX = (W - exitW) / 2;
        int exitY = gridY + (2 * cellH) + (int)(gapY * scale) + (int)(50 * scale);

        exitButton.setBounds(exitX, exitY, exitW, exitH);
        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/resources/exitButton (1).png"));
        Image scaledImg = originalIcon.getImage().getScaledInstance(exitW, exitH, Image.SCALE_SMOOTH);
        exitButton.setIcon(new ImageIcon(scaledImg));


        pane.revalidate();
        pane.repaint();
    }

    // ══════════════════════════════════════════════════════════
    //  Logic — UNTOUCHED
    // ══════════════════════════════════════════════════════════
    private void onCharacterSelected(Character character) {
        dispose();
        switch (gameModeType) {
            case PVE:    startPVEFlow(character);    break;
            case PVP:    startPVPFlow(character);    break;
            case ARCADE: startArcadeFlow(character); break;
        }
    }

    private void startPVEFlow(Character character) {
        Character enemy = EnemyFactory.getRandomEnemy(character);

        Runnable launchBattle = () -> new PVEBattleFrame(character, enemy);

        Runnable showEnemyStory = () -> new BackstoryShowcase(
                CharacterStories.getEnemyStory(enemy),
                CharacterStories.getEnemyTitle(enemy),
                launchBattle,
                () -> new CharacterSelectionFrame(GameModeType.PVE)
        );

        new BackstoryShowcase(
                CharacterStories.getCharacterStory(character),
                CharacterStories.getCharacterTitle(character),
                showEnemyStory,
                () -> new CharacterSelectionFrame(GameModeType.PVE)
        );
    }

    private void startPVPFlow(Character character) {

        Runnable goToP2Selection = () -> new PVPBattleFrame(character);

        new BackstoryShowcase(
                CharacterStories.getCharacterStory(character),
                CharacterStories.getCharacterTitle(character),
                goToP2Selection,
                () -> new CharacterSelectionFrame(GameModeType.PVP)
        );
    }

    private void startArcadeFlow(Character character) {
        Runnable launchTower = () -> new ArcadeTowerFrame(character, new ArcadeModeManager(character));

        new BackstoryShowcase(
                CharacterStories.getCharacterStory(character),
                CharacterStories.getCharacterTitle(character),
                launchTower,
                () -> new CharacterSelectionFrame(GameModeType.ARCADE)
        );
    }

    private Character[] buildRoster() {
        return new Character[]{
                new Tyrone(), new MakelanShere(), new Mary(), new Dirk(),
                new Flamara(), new Dea(), new Adamus(), new Tera()
        };
    }

    // ══════════════════════════════════════════════════════════
    //  CharFramePanel — image only, no external label
    //  Animations:
    //    • Continuous vertical bob (driven by parent Timer)
    //    • Hover: outer glow ring + scale-up 10%
    //    • Hover: name tooltip drawn inside the panel
    // ══════════════════════════════════════════════════════════
    private class CharFramePanel extends JPanel {
        private final Image  frameImg;
        private final String charName;
        private final Color  glowColor;
        private final int    slotIndex;
        private boolean      hovered  = false;
        private float        glowAlpha = 0f;
        private Timer        glowTimer;

        CharFramePanel(String imgPath, String name, Color glow, int idx) {
            this.frameImg  = loadImage(imgPath);
            this.charName  = name;
            this.glowColor = glow;
            this.slotIndex = idx;
            setOpaque(false);
            setLayout(null);

            // Smooth glow fade timer
            glowTimer = new Timer(20, e -> {
                if (hovered && glowAlpha < 1f) {
                    glowAlpha = Math.min(1f, glowAlpha + 0.08f);
                    repaint();
                } else if (!hovered && glowAlpha > 0f) {
                    glowAlpha = Math.max(0f, glowAlpha - 0.06f);
                    repaint();
                }
            });
            glowTimer.start();
        }

        void setHovered(boolean h) { this.hovered = h; }



        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            int W = getWidth();
            int H = getHeight();

            // ── Glow rings (drawn under the image) ────────────
            if (glowAlpha > 0f) {
                for (int ring = 4; ring >= 1; ring--) {
                    int spread = ring * 6;
                    float alpha = (glowAlpha * 0.25f) / ring;
                    g2.setColor(new Color(
                            glowColor.getRed(),
                            glowColor.getGreen(),
                            glowColor.getBlue(),
                            Math.min(255, (int)(alpha * 255))));
                    g2.setStroke(new BasicStroke(spread));
                    g2.drawRoundRect(
                            spread / 2, spread / 2,
                            W - spread, H - spread, 16, 16);
                }
            }

            float sc = hovered ? 1.08f : 1.0f;

            if (frameImg != null) {

                int iw = frameImg.getWidth(null);
                int ih = frameImg.getHeight(null);

                int targetW = CharacterSelectionFrame.FRAME_SIZE;

                double scale = ((double) targetW / iw) * sc;

                int drawW = (int)(iw * scale);
                int drawH = (int)(ih * scale);

                int drawX = (W - drawW) / 2;
                int drawY = (H - drawH) / 2;

                g2.drawImage(frameImg, drawX, drawY, drawW, drawH, null);

            } else {
                int drawW = 200;
                int drawH = 200;

                int drawX = (W - drawW) / 2;
                int drawY = (H - drawH) / 2;

                g2.setColor(new Color(60, 50, 30));
                g2.fillRoundRect(drawX, drawY, drawW, drawH, 12, 12);
            }
            // ── Name tooltip on hover (inside panel, bottom) ──
            if (glowAlpha > 0.05f) {
                int fontSize = Math.max(10, (int)(H * 0.13));
                g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (W - fm.stringWidth(charName)) / 2;
                int ty = H - (int)(H * 0.06);

                // Semi-transparent pill behind text
                int pillPad = 6;
                int pillW   = fm.stringWidth(charName) + pillPad * 2;
                int pillH   = fm.getHeight() + 2;
                int pillX   = (W - pillW) / 2;
                int pillY   = ty - fm.getAscent() - 2;

                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, glowAlpha * 0.85f));
                g2.setColor(glowColor.darker());
                g2.fillRoundRect(pillX, pillY, pillW, pillH, pillH, pillH);

                // Text
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, glowAlpha));
                g2.setColor(new Color(0, 0, 0, 160));
                g2.drawString(charName, tx + 1, ty + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(charName, tx, ty);
            }

            g2.dispose();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Background / image panels
    // ══════════════════════════════════════════════════════════
    private class BgPanel extends JPanel {
        private final Image img;
        BgPanel(String path) { img = loadImage(path); setOpaque(true); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                g2.dispose();
            } else { setBackground(Color.BLACK); }
        }
    }

    private class ScaledImgPanel extends JPanel {
        private final Image img;
        ScaledImgPanel(String path) { img = loadImage(path); setOpaque(false); }
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

    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        if (url == null) { System.err.println("Missing: " + path); return null; }
        return new ImageIcon(url).getImage();
    }

    private void createUIComponents() {}
}
