package encantadia.ui.frames;

import encantadia.characters.Character;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * ResultDialogFrame — shown after each round and after the final match.
 *
 * Two modes:
 *   - ROUND_WIN  → shows round winner, score, "Ready for Round X" button
 *   - MATCH_WIN  → shows match winner, final score, "Back to Menu" button
 *
 * Usage from PVEBattleFrame:
 *
 *   // Round win:
 *   ResultDialogFrame.showRoundResult(
 *       this, winner, loser,
 *       playerRoundsWon, enemyRoundsWon,
 *       nextRound,
 *       () -> resetRound()     // called when player clicks "Ready"
 *   );
 *
 *   // Match win:
 *   ResultDialogFrame.showMatchResult(
 *       this, winner, loser,
 *       playerRoundsWon, enemyRoundsWon,
 *       () -> { dispose(); new MainMenuFrame(); }
 *   );
 */
public class ResultDialogFrame extends JDialog {

    // ── Constants ─────────────────────────────────────────────
    private static final int WIDTH        = 700;
    private static final int HEIGHT       = 520;
    private static final int IMAGE_SIZE   = 160;

    private static final Color BG_DARK    = new Color(30, 30, 40);
    private static final Color BG_CARD    = new Color(43, 45, 48);
    private static final Color GOLD       = new Color(255, 215, 80);
    private static final Color SILVER     = new Color(180, 180, 200);
    private static final Color GREEN_WIN  = new Color(80, 200, 120);
    private static final Color RED_LOSE   = new Color(220, 80, 80);
    private static final Color BTN_COLOR  = new Color(60, 130, 200);
    private static final Color BTN_HOVER  = new Color(80, 160, 240);

    // ── Static factory methods ────────────────────────────────

    /**
     * Shows a round result dialog.
     *
     * @param parent         The parent JFrame (PVEBattleFrame)
     * @param roundWinner    Character who won the round
     * @param roundLoser     Character who lost the round
     * @param winnerScore    Winner's current round count
     * @param loserScore     Loser's current round count
     * @param nextRoundNum   The upcoming round number
     * @param onReady        Callback — runs when "Ready" button is clicked
     */
    public static void showRoundResult(JFrame parent,
                                       Character roundWinner,
                                       Character roundLoser,
                                       int winnerScore,
                                       int loserScore,
                                       int nextRoundNum,
                                       Runnable onReady) {

        ResultDialogFrame dialog = new ResultDialogFrame(
                parent,
                roundWinner,
                roundLoser,
                winnerScore,
                loserScore,
                false,
                nextRoundNum,
                onReady
        );
        dialog.setVisible(true);

    }

    /**
     * Shows a match result dialog (final winner).
     *
     * @param parent       The parent JFrame
     * @param matchWinner  Character who won the match
     * @param matchLoser   Character who lost
     * @param winnerScore  Winner's final round count
     * @param loserScore   Loser's final round count
     * @param onFinish     Callback — runs when "Back to Menu" is clicked
     */
    public static void showMatchResult(JFrame parent,
                                       Character matchWinner,
                                       Character matchLoser,
                                       int winnerScore,
                                       int loserScore,
                                       Runnable onFinish) {

        ResultDialogFrame dialog = new ResultDialogFrame(
                parent,
                matchWinner,
                matchLoser,
                winnerScore,
                loserScore,
                true,
                -1,
                onFinish
        );
        dialog.setVisible(true);
    }

    // ── Constructor ───────────────────────────────────────────

    private ResultDialogFrame(JFrame parent,
                              Character winner,
                              Character loser,
                              int winnerScore,
                              int loserScore,
                              boolean isFinalMatch,
                              int nextRoundNum,
                              Runnable onAction) {

        super(parent, isFinalMatch ? "Match Result" : "Round Result", true);

        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // force button use

        setContentPane(buildUI(winner, loser, winnerScore, loserScore,
                isFinalMatch, nextRoundNum, onAction));
    }

    // ── UI Builder ────────────────────────────────────────────

    private JPanel buildUI(Character winner, Character loser,
                           int winnerScore, int loserScore,
                           boolean isFinalMatch, int nextRoundNum,
                           Runnable onAction) {

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);
        root.setBorder(BorderFactory.createLineBorder(GOLD, 2));

        // ── Top: title banner ─────────────────────────────────
        root.add(buildTitlePanel(winner, isFinalMatch), BorderLayout.NORTH);

        // ── Center: character portraits + score ───────────────
        root.add(buildCenterPanel(winner, loser, winnerScore, loserScore, isFinalMatch), BorderLayout.CENTER);

        // ── Bottom: action button ─────────────────────────────
        root.add(buildBottomPanel(isFinalMatch, nextRoundNum, onAction), BorderLayout.SOUTH);

        return root;
    }

    // ── Title banner ──────────────────────────────────────────

    private JPanel buildTitlePanel(Character winner, boolean isFinalMatch) {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(isFinalMatch ? new Color(60, 20, 20) : new Color(20, 50, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));

        String titleHtml = isFinalMatch
                ? "<html><center>"
                + "<span style='font-size:22px; color:#FFD750;'><b>⚔ MATCH OVER ⚔</b></span><br>"
                + "<span style='font-size:14px; color:#DDDDDD;'>"
                + winner.getName() + " claims victory!</span>"
                + "</center></html>"
                : "<html><center>"
                + "<span style='font-size:20px; color:#50C878;'><b>ROUND COMPLETE</b></span><br>"
                + "<span style='font-size:13px; color:#DDDDDD;'>"
                + winner.getName() + " wins this round!</span>"
                + "</center></html>";

        JLabel title = new JLabel(titleHtml, SwingConstants.CENTER);
        panel.add(title, BorderLayout.CENTER);

        return panel;
    }


    // ── Center: portraits + scoreboard ───────────────────────

    private JPanel buildCenterPanel(Character winner, Character loser,
                                    int winnerScore, int loserScore,
                                    boolean isFinalMatch) {

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG_DARK);
        center.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill   = GridBagConstraints.BOTH;

        // Winner card (left)
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1;
        center.add(buildCharacterCard(winner, true, isFinalMatch), gbc);

        // VS / scoreboard (middle)
        gbc.gridx = 1; gbc.weightx = 0.6;
        center.add(buildScorePanel(winner, loser, winnerScore, loserScore), gbc);

        // Loser card (right)
        gbc.gridx = 2; gbc.weightx = 1;
        center.add(buildCharacterCard(loser, false, false), gbc);

        return center;
    }

    private JPanel buildCharacterCard(Character character, boolean isWinner, boolean isMatchWinner) {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isWinner ? GOLD : new Color(70, 70, 80), 2),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Crown / defeat indicator
        String badge = isMatchWinner ? "👑 CHAMPION" : isWinner ? "🏆 WINNER" : "💀 DEFEATED";
        Color badgeColor = isMatchWinner ? GOLD : isWinner ? GREEN_WIN : RED_LOSE;

        JLabel badgeLabel = new JLabel(badge, SwingConstants.CENTER);
        badgeLabel.setForeground(badgeColor);
        badgeLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        badgeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Character image placeholder
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(IMAGE_SIZE, IMAGE_SIZE));
        imageLabel.setMaximumSize(new Dimension(IMAGE_SIZE, IMAGE_SIZE));
        imageLabel.setMinimumSize(new Dimension(IMAGE_SIZE, IMAGE_SIZE));
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadCharacterImage(imageLabel, character);

        // Character name
        JLabel nameLabel = new JLabel(
                "<html><center><b>" + character.getName() + "</b><br>"
                        + "<small><i>" + character.getTitle() + "</i></small></center></html>",
                SwingConstants.CENTER
        );
        nameLabel.setForeground(isWinner ? GOLD : SILVER);
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(badgeLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(imageLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(nameLabel);

        return card;
    }

    private JPanel buildScorePanel(Character winner, Character loser,
                                   int winnerScore, int loserScore) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_DARK);

        // VS label
        JLabel vsLabel = new JLabel("VS", SwingConstants.CENTER);
        vsLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        vsLabel.setForeground(new Color(150, 150, 170));
        vsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Score display
        String scoreHtml = "<html><center>"
                + "<span style='color:#FFD750; font-size:36px;'><b>"
                + winnerScore + " - " + loserScore
                + "</b></span><br>"
                + "<span style='color:#AAAAAA; font-size:10px;'>ROUNDS</span>"
                + "</center></html>";

        JLabel scoreLabel = new JLabel(scoreHtml, SwingConstants.CENTER);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Best of 3 reminder
        JLabel bestOf = new JLabel(
                "<html><center><span style='color:#888888; font-size:9px;'>BEST OF 3</span></center></html>",
                SwingConstants.CENTER
        );
        bestOf.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalGlue());
        panel.add(vsLabel);
        panel.add(Box.createVerticalStrut(12));
        panel.add(scoreLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(bestOf);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ── Bottom: action button ─────────────────────────────────

    private JPanel buildBottomPanel(boolean isFinalMatch, int nextRoundNum, Runnable onAction) {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 14));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 70)));

        String btnText = isFinalMatch
                ? "  Back to Main Menu  "
                : "  Ready for Round " + nextRoundNum + "  ";

        JButton actionButton = createStyledButton(btnText);
        actionButton.addActionListener(e -> {
            dispose();
            onAction.run();
        });

        panel.add(actionButton);
        return panel;
    }

    // ── Image loader ──────────────────────────────────────────

    /**
     * Loads the character's image into the label.
     * Falls back to a colored placeholder if no image is found.
     * Replace the placeholder path with actual character image paths when ready.
     */
    private void loadCharacterImage(JLabel label, Character character) {

        // TODO: Replace with actual per-character image paths when assets are ready
        // e.g. "/images/" + character.getName().toLowerCase() + ".png"
        String imagePath = "/images/placeholder.png";

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));
            Image scaled   = icon.getImage().getScaledInstance(IMAGE_SIZE, IMAGE_SIZE, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            // Placeholder: colored panel with character initial
            label.setText("<html><center>"
                    + "<span style='font-size:48px;'>🧙</span><br>"
                    + "<span style='color:#888888; font-size:10px;'>[No Image]</span>"
                    + "</center></html>");
            label.setOpaque(true);
            label.setBackground(new Color(55, 57, 65));
            label.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 90)));
        }
    }

    // ── Styled button ─────────────────────────────────────────

    private JButton createStyledButton(String text) {

        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BTN_HOVER : BTN_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(240, 44));

        return btn;
    }
}