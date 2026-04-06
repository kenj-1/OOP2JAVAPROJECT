package encantadia.ui.frames.battleModeFrames;

import encantadia.ScreenManager;
import encantadia.battle.ai.EnemyAI;
import encantadia.battle.arcade.ArcadeModeManager;
import encantadia.battle.engine.TurnManager;
import encantadia.battle.result.TurnResult;
import encantadia.battle.skill.Skill;
import encantadia.characters.*;
import encantadia.characters.Character;
import encantadia.ui.frames.ArcadeTowerFrame;
import encantadia.ui.frames.ArcadeVictoryFrame;
import encantadia.ui.frames.MainMenuFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ArcadeModeBattleFrame — one instance = one fight.
 *
 * Key features:
 *   • Full-screen pixel-art cave background
 *   • BattleCanvas paints the full HUD via Graphics2D (portrait frames,
 *     HP bars, name pills, turn indicator, enemy counter)
 *   • Pill-shaped skill buttons bottom-left, mini battle log bottom-center
 *   • Custom in-frame overlays for ALL reward / defeat popups
 *   • Randomized 3-skill ultimate draft: picks 3 "tier-3" skills from the
 *     global roster EXCLUDING the player's own ultimate
 */
public class ArcadeModeBattleFrame extends JFrame {

    // ── Resources ─────────────────────────────────────────────
    private static final String BG_PATH          = "/resources/backgroundArcade.png";
    private static final int    ENEMY_TURN_DELAY  = 1100;
    private boolean ultimateUnlocked = false;

    private static final String[] FRAME_IMGS = {
            "/resources/tyroneFrame (1).png", "/resources/elanFrame (1).png",
            "/resources/claireFrame (1).png", "/resources/dirkFrame (1).png",
            "/resources/flamaraFrame (1).png", "/resources/deaFrame (1).png",
            "/resources/adamusFrame (1).png",  "/resources/teraFrame (1).png"
    };
    private static final String[] CHAR_NAMES = {
            "Tyrone","Elan","Claire","Dirk","Flamara","Dea","Adamus","Tera"
    };

    // ── Colours ────────────────────────────────────────────────
    private static final Color PLAYER_CLR = new Color(0x2E, 0x8B, 0x57);
    private static final Color ENEMY_CLR  = new Color(0xB0, 0x2A, 0x2A);
    private static final Color BOSS_CLR   = new Color(0xAA, 0x00, 0xFF);
    private static final Color GOLD       = new Color(0xC8, 0xA0, 0x28);
    private static final Color CREAM      = new Color(0xFF, 0xF5, 0xDC);
    private static final Color LOG_FG     = new Color(0xD4, 0xC5, 0xA0);
    private static final Color ORANGE_LOW = new Color(0xCC, 0x88, 0x22);
    private static final Color RED_CRIT   = new Color(0xCC, 0x22, 0x22);
    private static final Color GREEN_RDY  = new Color(0x60, 0xCC, 0x60);

    // ── Characters & engine ───────────────────────────────────
    private final Character         playerCharacter;
    private final Character         enemyCharacter;
    private final ArcadeModeManager arcadeManager;
    private TurnManager       turnManager;

    // processingTurn is ALWAYS false on construction — the per-instance fix
    private volatile boolean processingTurn = false;

    // ── Layered pane refs ─────────────────────────────────────
    private BattleCanvas battleCanvas;
    private JPanel       skillsLayer;
    private JPanel       overlayLayer;
    private JTextArea    battleLog;
    private JButton[]    skillBtns = new JButton[4];
    private JLabel[]     cdLabels  = new JLabel[4];

    // ══════════════════════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════════════════════
    public ArcadeModeBattleFrame(Character player, ArcadeModeManager manager) {
        this.playerCharacter = player;
        this.arcadeManager   = manager;
        this.enemyCharacter  = manager.getCurrentEnemy();

        if (enemyCharacter == null) {
            SwingUtilities.invokeLater(() -> new ArcadeVictoryFrame(player));
            return;
        }

        playerCharacter.reset();
        enemyCharacter.reset();
        this.turnManager = new TurnManager(playerCharacter, enemyCharacter);

        boolean isBoss = arcadeManager.isFinalBoss();
        int idx   = arcadeManager.getCurrentIndex() + 1;
        int total = arcadeManager.getTotalEnemies();

        setTitle("Arcade Mode — " + player.getName() + " vs " + enemyCharacter.getName());
        setSize(1024, 768);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        buildUI(isBoss, idx, total);
        registerHotkeys();
        setVisible(true);
        ScreenManager.register(this);

        refreshUI();
        rebuildSkillSlots();
        refreshCdRow();
        updateTurnState();

        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log(isBoss ? "⚡  FINAL BOSS BATTLE!" : "⚔  Arcade Battle " + idx + " of " + total);
        log(playerCharacter.getName() + "  vs  " + enemyCharacter.getName());
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    public ArcadeModeBattleFrame(Character player) {
        this(player, new ArcadeModeManager(player));
    }

    @Override
    public void dispose() { ScreenManager.unregister(this); super.dispose(); }

    // ══════════════════════════════════════════════════════════
    //  UI — JLayeredPane
    // ══════════════════════════════════════════════════════════
    private void buildUI(boolean isBoss, int idx, int total) {
        JLayeredPane lp = new JLayeredPane();
        lp.setLayout(null);
        setContentPane(lp);

        BgPanel bg = new BgPanel(BG_PATH);
        lp.add(bg, JLayeredPane.DEFAULT_LAYER);

        battleCanvas = new BattleCanvas(isBoss, idx, total);
        lp.add(battleCanvas, JLayeredPane.PALETTE_LAYER);

        skillsLayer = new JPanel(null);
        skillsLayer.setOpaque(false);
        buildSkillsLayer();
        lp.add(skillsLayer, JLayeredPane.MODAL_LAYER);

        overlayLayer = new JPanel(null);
        overlayLayer.setOpaque(false);
        overlayLayer.setVisible(false);
        lp.add(overlayLayer, JLayeredPane.POPUP_LAYER);

        lp.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int W = lp.getWidth(), H = lp.getHeight();
                if (W == 0 || H == 0) return;
                bg.setBounds(0, 0, W, H);
                battleCanvas.setBounds(0, 0, W, H);
                skillsLayer.setBounds(0, 0, W, H);
                overlayLayer.setBounds(0, 0, W, H);
                layoutSkillsLayer(W, H);
            }
        });
    }

    // ── Skill buttons + mini log ──────────────────────────────
    private void buildSkillsLayer() {
        JPanel skillPanel = new JPanel();
        skillPanel.setOpaque(false);
        skillPanel.setLayout(new BoxLayout(skillPanel, BoxLayout.Y_AXIS));

        for (int i = 0; i < 4; i++) {
            final int si = i;
            skillBtns[i] = makePillButton("—",
                    new Color(0x70, 0x14, 0x14), new Color(0xFF, 0x99, 0x99));
            skillBtns[i].setEnabled(false);
            skillBtns[i].addActionListener(e -> onPlayerSkill(si));
            cdLabels[i] = new JLabel("LOCKED", SwingConstants.CENTER);
            cdLabels[i].setFont(new Font("SansSerif", Font.BOLD, 9));
            cdLabels[i].setForeground(new Color(0x60, 0x50, 0x30));
            JPanel slot = new JPanel(new BorderLayout(0, 2));
            slot.setOpaque(false);
            slot.add(skillBtns[i], BorderLayout.CENTER);
            slot.add(cdLabels[i], BorderLayout.SOUTH);
            skillPanel.add(slot);
            if (i < 3) skillPanel.add(Box.createVerticalStrut(4));
        }
        skillsLayer.add(skillPanel);
        skillsLayer.putClientProperty("skills", skillPanel);

        battleLog = new JTextArea();
        battleLog.setFont(new Font("Monospaced", Font.PLAIN, 10));
        battleLog.setForeground(LOG_FG);
        battleLog.setOpaque(false);
        battleLog.setEditable(false);
        battleLog.setLineWrap(true);
        battleLog.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(battleLog,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        JPanel logHolder = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0xC8, 0xA0, 0x28, 70));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        logHolder.setOpaque(false);
        logHolder.add(scroll, BorderLayout.CENTER);
        skillsLayer.add(logHolder);
        skillsLayer.putClientProperty("log", logHolder);
    }

    private void rebuildSkillSlots() {
        List<Skill> skills = playerCharacter.getSkills();
        for (int i = 0; i < 4; i++) {
            if (skillBtns[i] == null) continue;
            if (i < skills.size()) {
                skillBtns[i].setText(skills.get(i).getName());
                skillBtns[i].setEnabled(false);
                cdLabels[i].setText("READY");
                cdLabels[i].setForeground(GREEN_RDY);
            } else {
                skillBtns[i].setText("—");
                skillBtns[i].setEnabled(false);
                cdLabels[i].setText("LOCKED");
                cdLabels[i].setForeground(new Color(0x60, 0x50, 0x30));
            }
        }
        skillsLayer.revalidate();
        skillsLayer.repaint();
    }

    private void layoutSkillsLayer(int W, int H) {
        JPanel sp = (JPanel) skillsLayer.getClientProperty("skills");
        JPanel lh = (JPanel) skillsLayer.getClientProperty("log");
        if (sp == null) return;
        double sc = Math.min(W / 1024.0, H / 768.0);
        int skillW = (int)(170*sc), skillH = (int)(140*sc);
        sp.setBounds((int)(10*sc), H-skillH-(int)(16*sc), skillW, skillH);
        if (lh != null) {
            int lw = (int)(340*sc), lhh = (int)(130*sc);
            lh.setBounds((W-lw)/2, H-lhh-(int)(16*sc), lw, lhh);
        }
        skillsLayer.revalidate(); skillsLayer.repaint();
    }

    // ══════════════════════════════════════════════════════════
    //  Turn logic
    // ══════════════════════════════════════════════════════════
    private void onPlayerSkill(int si) {
        if (processingTurn || !turnManager.isPlayerTurn()) return;
        processingTurn = true;
        setPlayerEnabled(false);

        TurnResult res = turnManager.executeSkill(playerCharacter, enemyCharacter, si);
        flushResult(res); refreshUI();

        if (res.isTargetDefeated()) { onPlayerWon(); return; }
        if (res.isTurnStolen())     { setPlayerEnabled(true); processingTurn = false; return; }

        turnManager.advanceTurn(); refreshCdRow(); updateTurnState();
        new Timer(ENEMY_TURN_DELAY, e -> doEnemyTurn()) {{ setRepeats(false); start(); }};
    }

    private void doEnemyTurn() {
        int si = EnemyAI.chooseSkill(enemyCharacter, turnManager.getCooldownManager());
        TurnResult res = turnManager.executeSkill(enemyCharacter, playerCharacter, si);
        flushResult(res); refreshUI();

        if (res.isTargetDefeated()) { onPlayerLost(); return; }
        if (res.isTurnStolen()) {
            new Timer(ENEMY_TURN_DELAY, e -> doEnemyTurn()) {{ setRepeats(false); start(); }};
            return;
        }

        turnManager.advanceTurn(); refreshCdRow(); updateTurnState();
        setPlayerEnabled(true); processingTurn = false;
    }

    private void updateTurnState() {
        boolean myTurn = turnManager.isPlayerTurn();
        setPlayerEnabled(myTurn);
        if (battleCanvas != null) battleCanvas.setPlayerActive(myTurn);
    }

    // ══════════════════════════════════════════════════════════
    //  Win / Lose
    // ══════════════════════════════════════════════════════════
    private void onPlayerWon() {
        setPlayerEnabled(false); processingTurn = false;
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log("🏆  " + playerCharacter.getName() + " defeated " + enemyCharacter.getName() + "!");
        arcadeManager.recordVictory();

        new Timer(1100, e -> {
            ((Timer)e.getSource()).stop();
            SwingUtilities.invokeLater(this::processVictoryOutcome);
        }) {{ setRepeats(false); start(); }};
    }

    private void processVictoryOutcome() {
        if (arcadeManager.isFinished()) {
            dispose(); new ArcadeVictoryFrame(playerCharacter); return;
        }

        if (arcadeManager.shouldGiveHPBoost()) {
            playerCharacter.increaseMaxHP(ArcadeModeManager.HP_BOOST_AMT);
            log("✨  Reward: +" + ArcadeModeManager.HP_BOOST_AMT + " Max HP!");
            showInfoOverlay(
                    "✨  POWER SURGE",
                    "+" + ArcadeModeManager.HP_BOOST_AMT + " Max HP granted!\nYou grow stronger with each victory.",
                    this::transitionToTower);
        } else if (arcadeManager.shouldGiveUltimate()) {
            showUltimateDraftOverlay(this::transitionToTower);
        } else {
            transitionToTower();
        }
    }

    private void transitionToTower() {
        dispose(); new ArcadeTowerFrame(playerCharacter, arcadeManager);
    }

    private void onPlayerLost() {
        setPlayerEnabled(false); processingTurn = false;
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log("💀  " + enemyCharacter.getName() + " has defeated you!");

        new Timer(900, e -> {
            ((Timer)e.getSource()).stop();
            showInfoOverlay(
                    "💀  FALLEN",
                    "You have fallen in battle.\n" + enemyCharacter.getName() + " proved too powerful.\n\nReturning to main menu...",
                    () -> { dispose(); new MainMenuFrame(); });
        }) {{ setRepeats(false); start(); }};
    }

    // ══════════════════════════════════════════════════════════
    //  Overlay helpers
    // ══════════════════════════════════════════════════════════

    /** Generic info card with a single Continue button. */
    private void showInfoOverlay(String title, String body, Runnable onConfirm) {
        overlayLayer.removeAll();
        overlayLayer.setVisible(true);

        JPanel dim = makeDim();
        dim.setLayout(new GridBagLayout());

        JPanel card = makeCard(420, 220);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JLabel titleLbl = cardTitle(title);
        JLabel bodyLbl  = cardBody(body);
        JButton btn     = makeGoldOverlayButton("Continue  →");
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addActionListener(e -> {
            overlayLayer.setVisible(false); overlayLayer.removeAll();
            if (onConfirm != null) onConfirm.run();
        });

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(14));
        card.add(bodyLbl);
        card.add(Box.createVerticalStrut(22));
        card.add(btn);
        dim.add(card);
        mountOverlay(dim);
    }

    /**
     * Ultimate skill draft overlay.
     *
     * Builds a randomized pool of exactly 3 "tier-3" ultimate skills from the
     * global character roster, strictly excluding the player's own native ultimate.
     * Player picks one; it is added as their 4th skill.
     */
    private void showUltimateDraftOverlay(Runnable onDone) {
        List<Skill> pool = buildUltimatePool();
        if (pool.isEmpty()) { onDone.run(); return; }

        overlayLayer.removeAll();
        overlayLayer.setVisible(true);

        JPanel dim = makeDim();
        dim.setLayout(new GridBagLayout());

        int cardH = 140 + pool.size() * 54;
        JPanel card = makeCard(490, cardH);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel titleLbl = new JLabel("🔥  ULTIMATE SKILL DRAFT", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 22));
        titleLbl.setForeground(new Color(0xFF, 0xAA, 0x30));
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("Choose one ultimate ability — choose wisely:", SwingConstants.CENTER);
        subLbl.setFont(new Font("Serif", Font.ITALIC, 13));
        subLbl.setForeground(LOG_FG);
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(subLbl);
        card.add(Box.createVerticalStrut(16));

        for (Skill skill : pool) {
            JButton btn = makeSkillPickButton(
                    skill.getName(),
                    skill.getMinDamage() + "–" + skill.getMaxDamage() + " dmg  |  CD: " + skill.getCooldown());
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.addActionListener(e -> {
                playerCharacter.addSkill(skill);
                ultimateUnlocked = true;        // ← ADD THIS LINE
                rebuildSkillSlots();
                log("🔥  Ultimate unlocked: " + skill.getName() + "!");
                overlayLayer.setVisible(false); overlayLayer.removeAll();
                onDone.run();
            });
            card.add(btn);
            card.add(Box.createVerticalStrut(6));
        }

        dim.add(card);
        mountOverlay(dim);
    }

    /**
     * Builds the randomized ultimate draft pool.
     *
     * Rules:
     *  1. Take the 3rd skill (index 2) of every character in the global roster.
     *  2. Remove any skill whose name matches the player's own 3rd skill.
     *  3. Shuffle and return the first 3 (or fewer if roster is small).
     */
    private List<Skill> buildUltimatePool() {
        // Global roster — same order as CharacterSelectionFrame
        Character[] globalRoster = {
                new Tyrone(), new MakelanShere(), new Mary(), new Dirk(),
                new Flamara(), new Dea(), new Adamus(), new Tera()
        };

        // Determine the player's own ultimate name (skill index 2)
        String playerUltimateName = "";
        List<Skill> mySkills = playerCharacter.getSkills();
        if (mySkills.size() >= 3) playerUltimateName = mySkills.get(2).getName();

        List<Skill> pool = new ArrayList<>();
        for (Character c : globalRoster) {
            // Skip player's own character class
            if (c.getName().equals(playerCharacter.getName())) continue;
            List<Skill> skills = c.getSkills();
            if (skills.size() >= 3) {
                Skill ultimate = skills.get(2);
                // Skip if it's the same skill as the player already owns
                if (!ultimate.getName().equals(playerUltimateName)) {
                    pool.add(ultimate);
                }
            }
        }

        Collections.shuffle(pool);
        return pool.subList(0, Math.min(3, pool.size()));
    }
    private void registerHotkeys() {
        JComponent root = (JComponent) getContentPane();
        InputMap  im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        // ── A / S / D — core three skills ────────────────────────────────────
        int[][] coreKeys = {
                { KeyEvent.VK_A, 0 },
                { KeyEvent.VK_S, 1 },
                { KeyEvent.VK_D, 2 },
        };
        for (int[] kb : coreKeys) {
            int keyCode = kb[0];
            int si      = kb[1];
            String id   = "arc_skill_" + si;
            im.put(KeyStroke.getKeyStroke(keyCode, 0, false), id);
            am.put(id, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (si < skillBtns.length && skillBtns[si] != null
                            && skillBtns[si].isEnabled()) {
                        onPlayerSkill(si);
                    }
                }
            });
        }

        // ── F — 4th skill slot (inactive until ultimate is unlocked) ─────────
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0, false), "arc_skill_3");
        am.put("arc_skill_3", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                // Guard: do nothing if ultimate not yet unlocked
                if (!ultimateUnlocked) return;
                if (skillBtns[3] != null && skillBtns[3].isEnabled()) {
                    onPlayerSkill(3);
                }
            }
        });
    }
    // ── Overlay widget builders ───────────────────────────────
    private JPanel makeDim() {
        JPanel dim = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        dim.setOpaque(false);
        return dim;
    }

    private JPanel makeCard(int w, int h) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0E, 0x09, 0x04, 248));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(GOLD);
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 20, 20);
                g2.dispose(); super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(w, h));
        card.setMaximumSize(new Dimension(w, h));
        return card;
    }

    private JLabel cardTitle(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 22));
        l.setForeground(GOLD); l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JLabel cardBody(String text) {
        String html = "<html><div style='text-align:center;color:#D4C5A0;font-size:13px;font-family:serif'>"
                + text.replace("\n", "<br>") + "</div></html>";
        JLabel l = new JLabel(html, SwingConstants.CENTER);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private void mountOverlay(JPanel dim) {
        overlayLayer.add(dim);
        dim.setBounds(0, 0, getWidth(), getHeight());
        overlayLayer.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                dim.setBounds(0, 0, overlayLayer.getWidth(), overlayLayer.getHeight());
            }
        });
        overlayLayer.revalidate(); overlayLayer.repaint();
    }

    // ══════════════════════════════════════════════════════════
    //  UI helpers
    // ══════════════════════════════════════════════════════════
    private void refreshUI() { if (battleCanvas != null) battleCanvas.repaint(); }

    private void refreshCdRow() {
        if (turnManager == null) return;
        List<Skill> skills = playerCharacter.getSkills();
        for (int i = 0; i < skillBtns.length; i++) {
            if (skillBtns[i] == null || i >= skills.size()) continue;
            int cd = turnManager.getCooldownManager().getRemainingCooldown(playerCharacter, i);
            if (cd > 0) {
                cdLabels[i].setText(cd + " turn(s)"); cdLabels[i].setForeground(ORANGE_LOW);
                skillBtns[i].setEnabled(false);
            } else {
                cdLabels[i].setText("READY"); cdLabels[i].setForeground(GREEN_RDY);
            }
        }
    }

    private void setPlayerEnabled(boolean on) {
        List<Skill> skills = playerCharacter.getSkills();
        for (int i = 0; i < skillBtns.length; i++)
            if (skillBtns[i] != null) skillBtns[i].setEnabled(on && i < skills.size());
        if (on) refreshCdRow();
    }

    private void flushResult(TurnResult r) { for (String m : r.getLogMessages()) log(m); }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (battleLog != null) {
                battleLog.append(msg + "\n");
                battleLog.setCaretPosition(battleLog.getDocument().getLength());
            }
        });
    }

    private String getFrameImg(String name) {
        for (int i = 0; i < CHAR_NAMES.length; i++)
            if (CHAR_NAMES[i].equals(name)) return FRAME_IMGS[i];
        return null;
    }

    // ══════════════════════════════════════════════════════════
    //  BattleCanvas — Graphics2D HUD
    // ══════════════════════════════════════════════════════════
    private class BattleCanvas extends JPanel {
        private final Image playerFrame, enemyFrame;
        private final boolean isBoss;
        private final int idx, total;
        private boolean playerActive = true;
        private float   glowTick    = 0f;
        private final Timer glowTimer;

        BattleCanvas(boolean isBoss, int idx, int total) {
            setOpaque(false);
            this.isBoss = isBoss; this.idx = idx; this.total = total;
            playerFrame = loadImage(getFrameImg(playerCharacter.getName()));
            enemyFrame  = loadImage(getFrameImg(enemyCharacter.getName()));
            glowTimer = new Timer(20, e -> { glowTick += 0.08f; repaint(); });
            glowTimer.start();
        }

        void setPlayerActive(boolean v) { playerActive = v; }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int W = getWidth(), H = getHeight();
            double sc = Math.min(W/1024.0, H/768.0);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Counter pill at top-center
            String ctTxt = isBoss ? "⚡  FINAL BOSS  ⚡" : "Enemy  " + idx + "  of  " + total;
            Color  ctClr = isBoss ? BOSS_CLR : GOLD;
            g2.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, Math.max(14, (int)(18*sc))));
            FontMetrics fm = g2.getFontMetrics();
            int ctw = fm.stringWidth(ctTxt)+24, cth = fm.getHeight()+10;
            int ctx = (W-ctw)/2, cty = (int)(10*sc);
            g2.setColor(new Color(0x08,0x04,0x02,200)); g2.fillRoundRect(ctx,cty,ctw,cth,cth,cth);
            g2.setColor(new Color(ctClr.getRed(),ctClr.getGreen(),ctClr.getBlue(),120));
            g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(ctx,cty,ctw,cth,cth,cth);
            drawShadow(g2,ctTxt,ctx+12,cty+fm.getAscent()+4,ctClr);

            // Measurements
            int portW=(int)(82*sc),portH=(int)(82*sc),hpW=(int)(230*sc),hpH=(int)(16*sc);
            int pillW=(int)(140*sc),pillH=(int)(24*sc),portY=cty+cth+(int)(8*sc);

            // Player
            int ppx=(int)(10*sc);
            drawPortrait(g2,playerFrame,ppx,portY,portW,portH,PLAYER_CLR,playerActive);
            int phx=ppx+portW+(int)(8*sc),phy=portY+(int)(8*sc);
            drawHPBar(g2,phx,phy,hpW,hpH,playerCharacter,PLAYER_CLR);
            g2.setFont(new Font("SansSerif",Font.PLAIN,Math.max(8,(int)(10*sc))));
            drawShadow(g2,"HP: "+playerCharacter.getCurrentHP()+" / "+playerCharacter.getMaxHP(),phx,phy+hpH+(int)(10*sc),new Color(0xFF,0xF5,0xDC,190));
            drawNamePill(g2,playerCharacter.getName(),phx,phy+hpH+(int)(14*sc),pillW,pillH,new Color(0x20,0x50,0x20,215),new Color(0x80,0xFF,0xAA));
            g2.setFont(new Font("SansSerif",Font.BOLD,Math.max(9,(int)(11*sc))));
            drawShadow(g2,"PLAYER",phx,phy-(int)(3*sc),new Color(0xEE,0xEE,0xEE));

            // Enemy
            Color ec = isBoss ? BOSS_CLR : ENEMY_CLR;
            int epx=W-(int)(10*sc)-portW;
            drawPortrait(g2,enemyFrame,epx,portY,portW,portH,ec,!playerActive);
            int ehx=epx-hpW-(int)(8*sc),ehy=portY+(int)(8*sc);
            drawHPBar(g2,ehx,ehy,hpW,hpH,enemyCharacter,ec);
            g2.setFont(new Font("SansSerif",Font.PLAIN,Math.max(8,(int)(10*sc))));
            drawShadow(g2,"HP: "+enemyCharacter.getCurrentHP()+" / "+enemyCharacter.getMaxHP(),ehx,ehy+hpH+(int)(10*sc),new Color(0xFF,0xF5,0xDC,190));
            Color pillBg=isBoss?new Color(0x30,0x00,0x40,215):new Color(0x50,0x10,0x10,215);
            Color pillFg=isBoss?new Color(0xDD,0x88,0xFF):new Color(0xFF,0x77,0x77);
            drawNamePill(g2,enemyCharacter.getName(),ehx,ehy+hpH+(int)(14*sc),pillW,pillH,pillBg,pillFg);
            g2.setFont(new Font("SansSerif",Font.BOLD,Math.max(9,(int)(11*sc))));
            drawShadow(g2,isBoss?"⚡ BOSS":"ENEMY",ehx,ehy-(int)(3*sc),new Color(0xEE,0xEE,0xEE));

            // Turn indicator
            String tt=playerActive?"▶ Your Turn":"⏳ "+enemyCharacter.getName()+" is acting...";
            g2.setFont(new Font("Serif",Font.BOLD|Font.ITALIC,Math.max(10,(int)(13*sc))));
            fm=g2.getFontMetrics();
            drawShadow(g2,tt,(W-fm.stringWidth(tt))/2,cty+cth+(int)(2*sc),GOLD);
            g2.dispose();
        }

        private void drawPortrait(Graphics2D g2,Image img,int x,int y,int w,int h,Color accent,boolean active){
            if(active){float a=0.22f+0.16f*(float)Math.sin(glowTick);for(int r=5;r>=1;r--){int sp=r*3;g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),Math.min(255,(int)(a*80/r))));g2.setStroke(new BasicStroke(sp));g2.drawRoundRect(x-sp/2,y-sp/2,w+sp,h+sp,10,10);}}
            g2.setColor(new Color(0x08,0x05,0x02,210));g2.fillRoundRect(x,y,w,h,8,8);
            if(img!=null)g2.drawImage(img,x,y,w,h,null);
            g2.setStroke(new BasicStroke(2));g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),active?220:90));g2.drawRoundRect(x,y,w,h,8,8);
        }
        private void drawHPBar(Graphics2D g2,int x,int y,int w,int h,Character c,Color base){
            double pct=Math.max(0,Math.min(1.0,(double)c.getCurrentHP()/c.getMaxHP()));
            Color bar=pct<=0.25?RED_CRIT:pct<=0.50?ORANGE_LOW:base;
            g2.setColor(new Color(0x08,0x04,0x02,220));g2.fillRoundRect(x,y,w,h,h,h);
            int fw=(int)(w*pct);if(fw>2){g2.setPaint(new GradientPaint(x,y,bar.brighter(),x,y+h,bar.darker()));g2.fillRoundRect(x,y,fw,h,h,h);}
            g2.setStroke(new BasicStroke(1));g2.setColor(new Color(0xFF,0xFF,0xFF,50));g2.drawRoundRect(x,y,w,h,h,h);
        }
        private void drawNamePill(Graphics2D g2,String t,int x,int y,int w,int h,Color bg,Color fg){
            g2.setColor(bg);g2.fillRoundRect(x,y,w,h,h,h);g2.setStroke(new BasicStroke(1));
            g2.setColor(new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),130));g2.drawRoundRect(x,y,w,h,h,h);
            g2.setFont(new Font("Serif",Font.BOLD,Math.max(9,h-6)));FontMetrics fm=g2.getFontMetrics();
            int tx=x+(w-fm.stringWidth(t))/2,ty=y+(h+fm.getAscent()-fm.getDescent())/2;
            g2.setColor(new Color(0,0,0,100));g2.drawString(t,tx+1,ty+1);g2.setColor(fg);g2.drawString(t,tx,ty);
        }
        private void drawShadow(Graphics2D g2,String t,int x,int y,Color c){
            g2.setColor(new Color(0,0,0,150));g2.drawString(t,x+1,y+1);g2.setColor(c);g2.drawString(t,x,y);
        }
    }

    // ── Widget factories ──────────────────────────────────────
    private JButton makePillButton(String label, Color bg, Color fg) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean h=getModel().isRollover(),en=isEnabled();
                Color bc=en?(h?bg.brighter():bg):new Color(0x28,0x20,0x18,140);
                g2.setColor(new Color(bc.getRed(),bc.getGreen(),bc.getBlue(),210));g2.fillRoundRect(0,0,getWidth(),getHeight(),getHeight(),getHeight());
                g2.setStroke(new BasicStroke(1.5f));g2.setColor(new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),en?(h?255:180):60));
                g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,getHeight()-2,getHeight()-2);
                g2.setFont(new Font("Serif",Font.BOLD,Math.max(9,getHeight()-8)));FontMetrics fm=g2.getFontMetrics();
                int tx=(getWidth()-fm.stringWidth(getText()))/2,ty=(getHeight()+fm.getAscent()-fm.getDescent())/2;
                g2.setColor(new Color(0,0,0,90));g2.drawString(getText(),tx+1,ty+1);
                g2.setColor(en?fg:new Color(0x60,0x50,0x40));g2.drawString(getText(),tx,ty);g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);btn.setBorderPainted(false);btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton makeGoldOverlayButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean h=getModel().isRollover();
                g2.setPaint(new GradientPaint(0,0,h?new Color(170,110,40):new Color(120,70,20),0,getHeight(),h?new Color(120,80,30):new Color(80,40,10)));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                if(h){g2.setColor(new Color(255,215,120,80));g2.setStroke(new BasicStroke(2.5f));g2.drawRoundRect(2,2,getWidth()-4,getHeight()-4,14,14);}
                g2.setColor(new Color(220,180,90));g2.setStroke(new BasicStroke(1.5f));g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,14,14);
                g2.setFont(new Font("Serif",Font.BOLD,14));FontMetrics fm=g2.getFontMetrics();
                int tx=(getWidth()-fm.stringWidth(getText()))/2,ty=(getHeight()+fm.getAscent())/2-2;
                g2.setColor(Color.BLACK);g2.drawString(getText(),tx+1,ty+1);
                g2.setColor(new Color(255,230,170));g2.drawString(getText(),tx,ty);g2.dispose();
            }
        };
        btn.setOpaque(false);btn.setContentAreaFilled(false);btn.setBorderPainted(false);btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));btn.setPreferredSize(new Dimension(200,46));
        return btn;
    }

    private JButton makeSkillPickButton(String name, String stat) {
        JButton btn = new JButton(name + "   [" + stat + "]") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean h=getModel().isRollover();
                g2.setColor(h?new Color(0x3C,0x18,0x04,240):new Color(0x22,0x0E,0x02,220));g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setStroke(new BasicStroke(1.5f));g2.setColor(h?new Color(0xFF,0xAA,0x30):new Color(0xC8,0x70,0x10,160));
                g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,10,10);
                g2.setFont(new Font("Serif",Font.BOLD,13));FontMetrics fm=g2.getFontMetrics();
                int tx=(getWidth()-fm.stringWidth(getText()))/2,ty=(getHeight()+fm.getAscent()-fm.getDescent())/2;
                g2.setColor(new Color(0,0,0,80));g2.drawString(getText(),tx+1,ty+1);
                g2.setColor(h?new Color(0xFF,0xE0,0x80):CREAM);g2.drawString(getText(),tx,ty);g2.dispose();
            }
        };
        btn.setOpaque(false);btn.setContentAreaFilled(false);btn.setBorderPainted(false);btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(400,42));btn.setMaximumSize(new Dimension(400,42));
        return btn;
    }

    private Image loadImage(String path) {
        if (path == null) return null;
        URL url = getClass().getResource(path);
        if (url == null) { System.err.println("Missing: " + path); return null; }
        return new ImageIcon(url).getImage();
    }

    private class BgPanel extends JPanel {
        private final Image img;
        BgPanel(String p){img=loadImage(p);setOpaque(true);setBackground(Color.BLACK);}
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);if(img!=null){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g2.drawImage(img,0,0,getWidth(),getHeight(),null);g2.dispose();}}
    }
}