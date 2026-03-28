package encantadia.ui.frames.battleModeFrames;

import encantadia.ScreenManager;
import encantadia.battle.ai.EnemyAI;
import encantadia.battle.engine.TurnManager;
import encantadia.battle.result.TurnResult;
import encantadia.battle.skill.Skill;
import encantadia.characters.Character;
import encantadia.ui.frames.MainMenuFrame;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

/**
 * PVEBattleFrame — Best-of-3 turn-based battle
 *
 * Flow per round:
 *   1. Player clicks a skill button → TurnManager.executeSkill(player, enemy)
 *   2. TurnResult is consumed → HP bars + log updated
 *   3. If enemy alive and no turn steal → enemy acts after 1 s delay (EnemyAI)
 *   4. When one side reaches 0 HP → round ends, wins counter updated
 *   5. First to ROUNDS_TO_WIN (2) wins the match → result dialog
 *
 * HP is fully restored and a fresh TurnManager (= fresh CooldownManager)
 * is created at the start of each new round.
 */
public class PVEBattleFrame extends JFrame {

    // ── Constants ─────────────────────────────────────────────
    private static final int ROUNDS_TO_WIN    = 2;
    private static final int ENEMY_TURN_DELAY = 1100; // ms before enemy acts

    // ── Colour palette ────────────────────────────────────────
    private static final Color BG_DARK    = new Color(0x18, 0x14, 0x0E);
    private static final Color BG_PANEL   = new Color(0x22, 0x1C, 0x14);
    private static final Color BG_LOG     = new Color(0x10, 0x0D, 0x07);
    private static final Color BORDER_CLR = new Color(0xC8, 0xA0, 0x28);
    private static final Color PLAYER_CLR = new Color(0x2E, 0x8B, 0x57); // sea-green
    private static final Color ENEMY_CLR  = new Color(0xB0, 0x2A, 0x2A); // crimson
    private static final Color GOLD       = new Color(0xC8, 0xA0, 0x28);
    private static final Color CREAM      = new Color(0xFF, 0xF5, 0xDC);
    private static final Color LOG_FG     = new Color(0xD4, 0xC5, 0xA0);
    private static final Color ORANGE_LOW = new Color(0xCC, 0x88, 0x22);
    private static final Color RED_CRIT   = new Color(0xCC, 0x22, 0x22);
    private static final Color GREEN_RDY  = new Color(0x60, 0xCC, 0x60);

    // ── Characters & engine ───────────────────────────────────
    private final Character playerCharacter;
    private final Character enemyCharacter;
    private TurnManager     turnManager;

    // ── Round/match tracking ──────────────────────────────────
    private int playerWins   = 0;
    private int enemyWins    = 0;
    private int currentRound = 1;

    // ── UI references ─────────────────────────────────────────
    private JLabel       roundLabel;
    private JLabel       playerWinsLabel;
    private JLabel       enemyWinsLabel;
    private JLabel       playerHPLabel;
    private JLabel       enemyHPLabel;
    private JProgressBar playerHPBar;
    private JProgressBar enemyHPBar;
    private JTextArea    battleLog;
    private JButton[]    skillButtons   = new JButton[3];
    private JLabel[]     cooldownLabels = new JLabel[3];
    private JLabel       turnIndicator;

    // ── Guard: prevents re-entry while processing ─────────────
    private volatile boolean processingTurn = false;

    // ══════════════════════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════════════════════
    public PVEBattleFrame(Character playerCharacter, Character enemyCharacter) {
        this.playerCharacter = playerCharacter;
        this.enemyCharacter  = enemyCharacter;
        this.turnManager     = new TurnManager(playerCharacter, enemyCharacter);

        setTitle("PVE — " + playerCharacter.getName() + " vs " + enemyCharacter.getName());
        setSize(1024, 768);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        buildUI();

        setVisible(true);
        ScreenManager.register(this);

        // Opening log
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log("⚔  Round " + currentRound + " — First to " + ROUNDS_TO_WIN + " wins!");
        log(playerCharacter.getName() + "  vs  " + enemyCharacter.getName());
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    @Override
    public void dispose() {
        ScreenManager.unregister(this);
        super.dispose();
    }

    // ══════════════════════════════════════════════════════════
    //  UI construction
    // ══════════════════════════════════════════════════════════
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        setContentPane(root);

        root.add(buildTopBar(),     BorderLayout.NORTH);
        root.add(buildMainArea(),   BorderLayout.CENTER);
        root.add(buildSkillPanel(), BorderLayout.SOUTH);
    }

    // ── Top bar ───────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x0C, 0x09, 0x05));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_CLR));
        bar.setPreferredSize(new Dimension(0, 62));

        // Player win dots (left)
        playerWinsLabel = makeWinLabel("0", PLAYER_CLR);
        playerWinsLabel.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));

        // Round info (centre)
        JPanel centre = new JPanel();
        centre.setOpaque(false);
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));

        roundLabel = new JLabel("Round " + currentRound, SwingConstants.CENTER);
        roundLabel.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 22));
        roundLabel.setForeground(GOLD);
        roundLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("First to " + ROUNDS_TO_WIN + " Wins", SwingConstants.CENTER);
        sub.setFont(new Font("Serif", Font.ITALIC, 12));
        sub.setForeground(new Color(0xA0, 0x88, 0x50));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        centre.add(Box.createVerticalGlue());
        centre.add(roundLabel);
        centre.add(sub);
        centre.add(Box.createVerticalGlue());

        // Enemy win dots (right)
        enemyWinsLabel = makeWinLabel("0", ENEMY_CLR);
        enemyWinsLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24));

        bar.add(playerWinsLabel, BorderLayout.WEST);
        bar.add(centre,          BorderLayout.CENTER);
        bar.add(enemyWinsLabel,  BorderLayout.EAST);
        return bar;
    }

    private JLabel makeWinLabel(String text, Color color) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Serif", Font.BOLD, 30));
        lbl.setForeground(color);
        lbl.setPreferredSize(new Dimension(70, 0));
        return lbl;
    }

    // ── Main area: player | log | enemy ──────────────────────
    private JPanel buildMainArea() {
        JPanel area = new JPanel(new GridBagLayout());
        area.setBackground(BG_DARK);

        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.BOTH;
        g.weighty = 1.0;
        g.insets  = new Insets(10, 10, 6, 10);

        // Player panel (left, 26 %)
        g.gridx   = 0;
        g.weightx = 0.26;
        area.add(buildCharacterPanel(true), g);

        // Battle log (centre, 48 %)
        g.gridx   = 1;
        g.weightx = 0.48;
        g.insets  = new Insets(10, 0, 6, 0);
        area.add(buildLogPanel(), g);

        // Enemy panel (right, 26 %)
        g.gridx   = 2;
        g.weightx = 0.26;
        g.insets  = new Insets(10, 10, 6, 10);
        area.add(buildCharacterPanel(false), g);

        return area;
    }

    private JPanel buildCharacterPanel(boolean isPlayer) {
        Character ch = isPlayer ? playerCharacter : enemyCharacter;
        Color     cl = isPlayer ? PLAYER_CLR       : ENEMY_CLR;

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(cl, 1),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)));

        // Portrait placeholder
        JPanel portrait = new JPanel();
        portrait.setBackground(new Color(0x18, 0x14, 0x0E));
        portrait.setBorder(new LineBorder(new Color(0x40, 0x32, 0x18), 1));
        portrait.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        portrait.setPreferredSize(new Dimension(0, 110));
        JLabel pLabel = new JLabel(ch.getName(), SwingConstants.CENTER);
        pLabel.setFont(new Font("Serif", Font.ITALIC, 11));
        pLabel.setForeground(new Color(0x70, 0x60, 0x40));
        portrait.setLayout(new BorderLayout());
        portrait.add(pLabel, BorderLayout.CENTER);

        // Name
        JLabel name = new JLabel(ch.getName(), SwingConstants.CENTER);
        name.setFont(new Font("Serif", Font.BOLD, 15));
        name.setForeground(CREAM);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Title
        JLabel title = new JLabel(ch.getTitle(), SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.ITALIC, 10));
        title.setForeground(new Color(0xA0, 0x88, 0x50));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // HP bar
        JProgressBar bar = new JProgressBar(0, ch.getMaxHP());
        bar.setValue(ch.getCurrentHP());
        bar.setStringPainted(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 13));
        bar.setForeground(cl);
        bar.setBackground(new Color(0x2A, 0x1A, 0x10));
        bar.setBorder(BorderFactory.createLineBorder(new Color(0x50, 0x40, 0x20)));

        // HP text
        JLabel hpText = new JLabel(
                "HP: " + ch.getCurrentHP() + " / " + ch.getMaxHP(),
                SwingConstants.CENTER);
        hpText.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hpText.setForeground(LOG_FG);
        hpText.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (isPlayer) {
            playerHPBar   = bar;
            playerHPLabel = hpText;

            // Turn indicator only on player side
            turnIndicator = new JLabel("▶ Your turn", SwingConstants.CENTER);
            turnIndicator.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 12));
            turnIndicator.setForeground(GOLD);
            turnIndicator.setAlignmentX(Component.CENTER_ALIGNMENT);
        } else {
            enemyHPBar   = bar;
            enemyHPLabel = hpText;
        }

        panel.add(portrait);
        panel.add(Box.createVerticalStrut(8));
        panel.add(name);
        panel.add(Box.createVerticalStrut(2));
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));
        panel.add(bar);
        panel.add(Box.createVerticalStrut(4));
        panel.add(hpText);
        if (isPlayer) {
            panel.add(Box.createVerticalStrut(10));
            panel.add(turnIndicator);
        }
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildLogPanel() {
        battleLog = new JTextArea();
        battleLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        battleLog.setForeground(LOG_FG);
        battleLog.setBackground(BG_LOG);
        battleLog.setEditable(false);
        battleLog.setLineWrap(true);
        battleLog.setWrapStyleWord(true);
        battleLog.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane scroll = new JScrollPane(battleLog,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(new LineBorder(BORDER_CLR, 1));
        scroll.getViewport().setBackground(BG_LOG);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Skill panel ───────────────────────────────────────────
    private JPanel buildSkillPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(0x0C, 0x09, 0x05));
        wrapper.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, BORDER_CLR));
        wrapper.setPreferredSize(new Dimension(0, 84));

        JPanel inner = new JPanel(new GridLayout(1, 3, 12, 0));
        inner.setBackground(new Color(0x0C, 0x09, 0x05));
        inner.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        List<Skill> skills = playerCharacter.getSkills();
        for (int i = 0; i < 3; i++) {
            String name = (i < skills.size()) ? skills.get(i).getName() : "—";

            JPanel slot = new JPanel(new BorderLayout(0, 3));
            slot.setOpaque(false);

            skillButtons[i]   = makeSkillButton(name, i);
            cooldownLabels[i] = new JLabel("READY", SwingConstants.CENTER);
            cooldownLabels[i].setFont(new Font("SansSerif", Font.BOLD, 10));
            cooldownLabels[i].setForeground(GREEN_RDY);

            slot.add(skillButtons[i],   BorderLayout.CENTER);
            slot.add(cooldownLabels[i], BorderLayout.SOUTH);
            inner.add(slot);
        }

        wrapper.add(inner, BorderLayout.CENTER);
        return wrapper;
    }

    private JButton makeSkillButton(String label, int index) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover   = getModel().isRollover();
                boolean enabled = isEnabled();

                Color t = hover   ? new Color(0x3C, 0x2C, 0x10)
                        : new Color(0x28, 0x1E, 0x0C);
                Color b = hover   ? new Color(0x28, 0x1A, 0x08)
                        : new Color(0x18, 0x10, 0x06);
                g2.setPaint(new GradientPaint(0, 0, t, 0, getHeight(), b));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                g2.setStroke(new java.awt.BasicStroke(1.5f));
                g2.setColor(enabled
                        ? new Color(0xC8, 0xA0, 0x28, hover ? 255 : 180)
                        : new Color(0x50, 0x40, 0x20, 100));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);

                g2.setFont(new Font("Serif", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(new Color(0, 0, 0, 100));
                g2.drawString(getText(), tx+1, ty+1);
                g2.setColor(enabled
                        ? (hover ? new Color(0xFF, 0xE0, 0x80) : CREAM)
                        : new Color(0x60, 0x50, 0x30));
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final int si = index;
        btn.addActionListener(e -> onPlayerSkill(si));
        return btn;
    }

    // ══════════════════════════════════════════════════════════
    //  Turn logic
    // ══════════════════════════════════════════════════════════

    /** Called when the player clicks a skill button. */
    private void onPlayerSkill(int skillIndex) {
        if (processingTurn) return;
        if (!turnManager.isPlayerTurn()) return;

        processingTurn = true;
        setSkillsEnabled(false);

        TurnResult result = turnManager.executeSkill(
                playerCharacter, enemyCharacter, skillIndex);
        flushResult(result);
        refreshUI();

        if (result.isTargetDefeated()) {
            endRound(true);
            return;
        }

        if (result.isTurnStolen()) {
            // Player acts again — no advanceTurn
            setTurnHint(true, false);
            setSkillsEnabled(true);
            processingTurn = false;
            return;
        }

        // Hand off to enemy
        turnManager.advanceTurn();
        refreshCooldownLabels();
        setTurnHint(false, true);

        new Timer(ENEMY_TURN_DELAY, e -> doEnemyTurn()) {{
            setRepeats(false); start();
        }};
    }

    /** Enemy AI executes one action. */
    private void doEnemyTurn() {
        int si = EnemyAI.chooseSkill(enemyCharacter, turnManager.getCooldownManager());

        TurnResult result = turnManager.executeSkill(
                enemyCharacter, playerCharacter, si);
        flushResult(result);
        refreshUI();

        if (result.isTargetDefeated()) {
            endRound(false);
            return;
        }

        if (result.isTurnStolen()) {
            // Enemy acts again
            new Timer(ENEMY_TURN_DELAY, e -> doEnemyTurn()) {{
                setRepeats(false); start();
            }};
            return;
        }

        // Return to player
        turnManager.advanceTurn();
        refreshCooldownLabels();
        setTurnHint(true, false);
        setSkillsEnabled(true);
        processingTurn = false;
    }

    // ══════════════════════════════════════════════════════════
    //  Round / match management
    // ══════════════════════════════════════════════════════════

    private void endRound(boolean playerWonRound) {
        setSkillsEnabled(false);

        if (playerWonRound) {
            playerWins++;
            playerWinsLabel.setText(String.valueOf(playerWins));
            log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log("🏆  " + playerCharacter.getName() + " wins Round " + currentRound + "!");
        } else {
            enemyWins++;
            enemyWinsLabel.setText(String.valueOf(enemyWins));
            log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log("💀  " + enemyCharacter.getName() + " wins Round " + currentRound + "!");
        }

        if (playerWins >= ROUNDS_TO_WIN || enemyWins >= ROUNDS_TO_WIN) {
            // Match over
            new Timer(1600, e -> showMatchResult(playerWins >= ROUNDS_TO_WIN)) {{
                setRepeats(false); start();
            }};
        } else {
            // Start next round after a pause
            currentRound++;
            new Timer(2000, e -> startNextRound()) {{
                setRepeats(false); start();
            }};
        }
    }

    private void startNextRound() {
        // Fully restore both characters
        fullHeal(playerCharacter);
        fullHeal(enemyCharacter);

        // Fresh TurnManager resets ALL cooldowns automatically
        turnManager = new TurnManager(playerCharacter, enemyCharacter);

        roundLabel.setText("Round " + currentRound);
        refreshUI();
        refreshCooldownLabels();
        setTurnHint(true, false);
        setSkillsEnabled(true);
        processingTurn = false;

        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log("⚔  Round " + currentRound + " begins!");
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showMatchResult(boolean playerWon) {
        String winner = playerWon ? playerCharacter.getName() : enemyCharacter.getName();
        log("★  MATCH OVER — " + winner + " wins " +
                Math.max(playerWins, enemyWins) + "–" + Math.min(playerWins, enemyWins) + "!");

        String msg = playerWon
                ? "Victory!\n" + playerCharacter.getName() + " wins "
                + playerWins + "–" + enemyWins + "!"
                : "Defeat.\n" + enemyCharacter.getName() + " wins "
                + enemyWins + "–" + playerWins + ".";

        String[] options = {"Main Menu", "Rematch"};
        int choice = JOptionPane.showOptionDialog(
                this, msg,
                playerWon ? "Victory!" : "Defeat",
                JOptionPane.DEFAULT_OPTION,
                playerWon ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);

        dispose();
        if (choice == 1) {
            // Rematch: fresh characters (full HP, fresh cooldowns via new TurnManager)
            fullHeal(playerCharacter);
            fullHeal(enemyCharacter);
            new PVEBattleFrame(playerCharacter, enemyCharacter);
        } else {
            new MainMenuFrame();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  UI helpers
    // ══════════════════════════════════════════════════════════

    private void refreshUI() {
        // HP bars
        playerHPBar.setValue(playerCharacter.getCurrentHP());
        enemyHPBar.setValue(enemyCharacter.getCurrentHP());

        // HP labels
        playerHPLabel.setText(
                "HP: " + playerCharacter.getCurrentHP() + " / " + playerCharacter.getMaxHP());
        enemyHPLabel.setText(
                "HP: " + enemyCharacter.getCurrentHP() + " / " + enemyCharacter.getMaxHP());

        // Bar colours by percentage
        tintBar(playerHPBar, playerCharacter, PLAYER_CLR);
        tintBar(enemyHPBar,  enemyCharacter,  ENEMY_CLR);

        repaint();
    }

    private void tintBar(JProgressBar bar, Character c, Color base) {
        double pct = (double) c.getCurrentHP() / c.getMaxHP();
        if      (pct <= 0.25) bar.setForeground(RED_CRIT);
        else if (pct <= 0.50) bar.setForeground(ORANGE_LOW);
        else                  bar.setForeground(base);
    }

    private void refreshCooldownLabels() {
        List<Skill> skills = playerCharacter.getSkills();
        for (int i = 0; i < skillButtons.length; i++) {
            if (i >= skills.size()) continue;
            int cd = turnManager.getCooldownManager()
                    .getRemainingCooldown(playerCharacter, i);
            if (cd > 0) {
                skillButtons[i].setEnabled(false);
                cooldownLabels[i].setText(cd + " turn(s)");
                cooldownLabels[i].setForeground(ORANGE_LOW);
            } else {
                skillButtons[i].setEnabled(true);
                cooldownLabels[i].setText("READY");
                cooldownLabels[i].setForeground(GREEN_RDY);
            }
        }
    }

    private void setSkillsEnabled(boolean enabled) {
        for (JButton b : skillButtons) b.setEnabled(enabled);
        if (enabled) refreshCooldownLabels(); // re-check which are actually on CD
    }

    private void setTurnHint(boolean isPlayerTurn, boolean enemyThinking) {
        if (isPlayerTurn) {
            turnIndicator.setText("▶ Your turn");
            turnIndicator.setForeground(GOLD);
        } else {
            turnIndicator.setText("⏳ Enemy is acting...");
            turnIndicator.setForeground(ORANGE_LOW);
        }
    }

    private void flushResult(TurnResult result) {
        for (String msg : result.getLogMessages()) log(msg);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            battleLog.append(message + "\n");
            // Auto-scroll to bottom
            battleLog.setCaretPosition(battleLog.getDocument().getLength());
        });
    }

    /**
     * Fully restores a character to max HP.
     *
     * Character.heal() clamps at maxHP, so heal(maxHP) always gives full HP
     * regardless of current HP (including 0 after defeat).
     */
    private static void fullHeal(Character c) {
        c.heal(c.getMaxHP());
    }
}