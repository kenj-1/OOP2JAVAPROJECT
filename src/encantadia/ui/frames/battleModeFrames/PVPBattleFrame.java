package encantadia.ui.frames.battleModeFrames;

import encantadia.ScreenManager;
import encantadia.battle.engine.TurnManager;
import encantadia.battle.result.TurnResult;
import encantadia.battle.skill.Skill;
import encantadia.characters.Character;
import encantadia.characters.Tyrone;
import encantadia.characters.MakelanShere;
import encantadia.characters.Mary;
import encantadia.characters.Dirk;
import encantadia.characters.Flamara;
import encantadia.characters.Dea;
import encantadia.characters.Adamus;
import encantadia.characters.Tera;
import encantadia.ui.frames.MainMenuFrame;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.List;

public class PVPBattleFrame extends JFrame {

    // ── Resource paths ────────────────────────────────────────
    private static final String BATTLE_BG  = "/resources/backgroundPvP.png";
    private static final String SELECT_BG  = "/resources/background (4).png";
    private static final String TITLE_PATH = "/resources/chooseSangreTitle.png";
    private static final String[] ROUND_TABLETS = {
            "/resources/round1.png", "/resources/round2.png", "/resources/round3.png"
    };
    private static final String[] ROUND_TEXTS = {
            "/resources/round1Text.png", "/resources/round2Text.png", "/resources/round3Text.png"
    };
    private static final String[] FRAME_IMGS = {
            "/resources/tyroneFrame (1).png", "/resources/elanFrame (1).png",
            "/resources/claireFrame (1).png", "/resources/dirkFrame (1).png",
            "/resources/flamaraFrame (1).png", "/resources/deaFrame (1).png",
            "/resources/adamusFrame (1).png", "/resources/teraFrame (1).png"
    };
    private static final String[] CHAR_NAMES = {
            "Tyrone","Elan","Claire","Dirk","Flamara","Dea","Adamus","Tera"
    };
    private static final Color[] GLOW_COLORS = {
            new Color(0xFF,0x60,0x20), new Color(0x40,0xA0,0xFF),
            new Color(0x40,0xE0,0x60), new Color(0xFF,0xCC,0x30),
            new Color(0xFF,0x40,0x20), new Color(0x60,0xA0,0xFF),
            new Color(0x30,0xDD,0x88), new Color(0xFF,0xCC,0x00),
    };
    private static final Character[] ROSTER = {
            new Tyrone(), new MakelanShere(), new Mary(), new Dirk(),
            new Flamara(), new Dea(), new Adamus(), new Tera()
    };

    // ── Constants ─────────────────────────────────────────────
    private static final int    ROUNDS_TO_WIN      = 2;
    private static final double WINNER_VOTE_WEIGHT = 0.70;

    // ── Colors ────────────────────────────────────────────────
    private static final Color P1_CLR     = new Color(0x2E,0x8B,0x57);
    private static final Color P2_CLR     = new Color(0x2A,0x6B,0xB0);
    private static final Color GOLD       = new Color(0xC8,0xA0,0x28);
    private static final Color CREAM      = new Color(0xFF,0xF5,0xDC);
    private static final Color LOG_FG     = new Color(0xD4,0xC5,0xA0);
    private static final Color ORANGE_LOW = new Color(0xCC,0x88,0x22);
    private static final Color RED_CRIT   = new Color(0xCC,0x22,0x22);
    private static final Color GREEN_RDY  = new Color(0x60,0xCC,0x60);
    private static final Color BORDER_CLR = new Color(0xC8,0xA0,0x28);
    private static final Color BG_DARK    = new Color(0x18,0x14,0x0E);
    private static final Color BG_LOG     = new Color(0x08,0x06,0x03,200);

    // ── Card names ────────────────────────────────────────────
    private static final String CARD_SELECT = "SELECT_P2";
    private static final String CARD_COIN   = "COIN_TOSS";
    private static final String CARD_BATTLE = "BATTLE";

    // ── State ─────────────────────────────────────────────────
    private final Character player1;
    private Character       player2;
    private TurnManager     turnManager;
    private int     p1Wins       = 0;
    private int     p2Wins       = 0;
    private int     currentRound = 1;
    private boolean p1GoesFirst  = true;
    private volatile boolean processingTurn = false;

    private CardLayout cardLayout;
    private JPanel     mainPanel;

    // Coin toss refs
    private JLabel  coinFaceLabel, coinStatusLabel, coinPlayerInfoLabel;
    private JButton headsBtn, tailsBtn;
    private String  p1CoinChoice = null;

    // Battle visual refs
    private BattleCanvas battleCanvas;
    private JPanel       skillsLayer;
    private RoundOverlay roundOverlay;
    private JTextArea    battleLog;
    private JButton[]    p1SkillBtns = new JButton[3];
    private JButton[]    p2SkillBtns = new JButton[3];
    private JLabel[]     p1CdLabels  = new JLabel[3];
    private JLabel[]     p2CdLabels  = new JLabel[3];
    // kept for backward compat with existing logic calls
    private JLabel p1WinsLabel, p2WinsLabel, roundLabel, turnIndicator;

    // P2 selection refs
    private JLayeredPane   selectPane;
    private BgPanel        selectBg;
    private ScaledImgPanel titleImg;
    private PlayerBanner   bannerPanel;
    private StatusRibbon   statusRibbon;
    private CharCard[]     cards = new CharCard[8];
    private final int[]    baseY = new int[8];
    private double         animTick = 0;
    private Timer          bobTimer;

    // ══════════════════════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════════════════════
    public PVPBattleFrame(Character player1character) {
        this.player1 = player1character;
        setTitle("PVP Battle");
        setSize(1024, 768);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel  = new JPanel(cardLayout);
        mainPanel.add(buildP2SelectPanel(), CARD_SELECT);
        mainPanel.add(buildCoinTossPanel(), CARD_COIN);

        setContentPane(mainPanel);
        cardLayout.show(mainPanel, CARD_SELECT);
        setVisible(true);
        ScreenManager.register(this);
    }

    @Override
    public void dispose() {
        if (bobTimer != null) bobTimer.stop();
        ScreenManager.unregister(this);
        super.dispose();
    }

    // ══════════════════════════════════════════════════════════
    //  Phase 1 — P2 Selection (unchanged)
    // ══════════════════════════════════════════════════════════
    private JPanel buildP2SelectPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.BLACK);
        selectPane = new JLayeredPane(); selectPane.setLayout(null);
        selectBg = new BgPanel(SELECT_BG); selectPane.add(selectBg, JLayeredPane.DEFAULT_LAYER);
        titleImg = new ScaledImgPanel(TITLE_PATH); selectPane.add(titleImg, JLayeredPane.PALETTE_LAYER);
        bannerPanel = new PlayerBanner(); selectPane.add(bannerPanel, JLayeredPane.PALETTE_LAYER);
        for (int i = 0; i < 8; i++) {
            final int idx = i;
            cards[i] = new CharCard(FRAME_IMGS[i], CHAR_NAMES[i], GLOW_COLORS[i]);
            cards[i].addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onP2Selected(ROSTER[idx]); }
                @Override public void mouseEntered(MouseEvent e) { cards[idx].setHovered(true); }
                @Override public void mouseExited(MouseEvent e)  { cards[idx].setHovered(false); }
            });
            cards[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            selectPane.add(cards[i], JLayeredPane.MODAL_LAYER);
        }
        statusRibbon = new StatusRibbon(); selectPane.add(statusRibbon, JLayeredPane.POPUP_LAYER);
        selectPane.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { layoutSelectPane(); }
        });
        bobTimer = new Timer(16, e -> {
            animTick += 0.06;
            for (int i = 0; i < 8; i++) {
                double phase = animTick + i * (Math.PI / 4);
                int bob = (int)(Math.sin(phase) * 5);
                if (baseY[i] > 0) cards[i].setLocation(cards[i].getX(), baseY[i] + bob);
                cards[i].repaint();
            }
        });
        bobTimer.start();
        wrapper.add(selectPane, BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::layoutSelectPane);
        return wrapper;
    }

    private void layoutSelectPane() {
        int W = selectPane.getWidth(), H = selectPane.getHeight();
        if (W == 0 || H == 0) return;
        selectBg.setBounds(0, 0, W, H);
        double scale = Math.min(Math.min(W/1024.0, H/768.0), 1.5);
        int bannerH = (int)(42*scale), bannerY = (int)(10*scale);
        bannerPanel.setBounds(0, bannerY, W, bannerH);
        int titleW = (int)(560*scale), titleH = (int)(titleW/3.5);
        titleImg.setBounds((W-titleW)/2, bannerY+bannerH+(int)(6*scale), titleW, titleH);
        int cellW=(int)(150*scale),cellH=(int)(150*scale),gapX=(int)(36*scale),gapY=(int)(32*scale);
        int gridW=4*cellW+3*gapX, gridX=(W-gridW)/2, gridY=bannerY+bannerH+(int)(6*scale)+titleH+(int)(28*scale);
        for (int i=0;i<8;i++) {
            int fx=gridX+(i%4)*(cellW+gapX), fy=gridY+(i/4)*(cellH+gapY);
            cards[i].setBounds(fx, fy, cellW, cellH); baseY[i]=fy;
        }
        statusRibbon.setBounds(0, H-(int)(36*scale), W, (int)(36*scale));
        selectPane.revalidate(); selectPane.repaint();
    }

    private void onP2Selected(Character selected) {
        if (selected.getName().equals(player1.getName())) {
            statusRibbon.setWarning(player1.getName()+" is already chosen by Player 1 — pick another warrior.");
            new Timer(2000, e -> { ((Timer)e.getSource()).stop(); statusRibbon.clearWarning(); })
            {{ setRepeats(false); start(); }};
            return;
        }
        this.player2 = selected;
        setTitle("PVP Battle — "+player1.getName()+" vs "+player2.getName());
        if (coinPlayerInfoLabel != null)
            coinPlayerInfoLabel.setText(player1.getName()+"  (P1)   vs   "+player2.getName()+"  (P2)");
        p1CoinChoice = null;
        coinFaceLabel.setText("?"); coinFaceLabel.setForeground(GOLD);
        coinStatusLabel.setText("Player 1 — pick your side:");
        headsBtn.setEnabled(true); tailsBtn.setEnabled(true);
        cardLayout.show(mainPanel, CARD_COIN);
    }

    // ══════════════════════════════════════════════════════════
    //  Phase 2 — Coin Toss (unchanged)
    // ══════════════════════════════════════════════════════════
    private JPanel buildCoinTossPanel() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setBackground(BG_DARK);
        JLabel title = new JLabel("COIN TOSS — Who Goes First?", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD|Font.ITALIC, 26)); title.setForeground(GOLD);
        title.setBorder(BorderFactory.createEmptyBorder(30,0,0,0)); panel.add(title, BorderLayout.NORTH);
        JPanel center = new JPanel(); center.setBackground(BG_DARK);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        coinFaceLabel = new JLabel("?", SwingConstants.CENTER);
        coinFaceLabel.setFont(new Font("Serif", Font.BOLD, 90)); coinFaceLabel.setForeground(GOLD);
        coinFaceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        coinFaceLabel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_CLR,3), BorderFactory.createEmptyBorder(16,44,16,44)));
        coinStatusLabel = new JLabel("Player 1 — pick your side:", SwingConstants.CENTER);
        coinStatusLabel.setFont(new Font("Serif", Font.ITALIC, 16)); coinStatusLabel.setForeground(CREAM);
        coinStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0)); btnRow.setBackground(BG_DARK);
        headsBtn = makeDarkButton("HEADS"); tailsBtn = makeDarkButton("TAILS");
        headsBtn.setPreferredSize(new Dimension(140,50)); tailsBtn.setPreferredSize(new Dimension(140,50));
        headsBtn.addActionListener(e -> onP1Picks("HEADS")); tailsBtn.addActionListener(e -> onP1Picks("TAILS"));
        btnRow.add(headsBtn); btnRow.add(tailsBtn);
        center.add(Box.createVerticalGlue()); center.add(coinFaceLabel);
        center.add(Box.createVerticalStrut(22)); center.add(coinStatusLabel);
        center.add(Box.createVerticalStrut(18)); center.add(btnRow); center.add(Box.createVerticalGlue());
        panel.add(center, BorderLayout.CENTER);
        coinPlayerInfoLabel = new JLabel("Waiting for Player 2 selection...", SwingConstants.CENTER);
        coinPlayerInfoLabel.setFont(new Font("Serif", Font.ITALIC, 13)); coinPlayerInfoLabel.setForeground(LOG_FG);
        coinPlayerInfoLabel.setBorder(BorderFactory.createEmptyBorder(0,0,18,0));
        panel.add(coinPlayerInfoLabel, BorderLayout.SOUTH);
        return panel;
    }

    private void onP1Picks(String choice) {
        p1CoinChoice = choice; headsBtn.setEnabled(false); tailsBtn.setEnabled(false);
        coinStatusLabel.setText("Player 1 chose "+choice+". Flipping coin..."); animateCoin();
    }

    private void animateCoin() {
        String[] sl = {"H","T","H","T","H","T","H","T","H","T","H","T"};
        final int[] idx = {0};
        final String result = Math.random()<0.5?"HEADS":"TAILS";
        Timer anim = new Timer(110, null);
        anim.addActionListener(ev -> {
            if (idx[0] < sl.length) {
                coinFaceLabel.setText(sl[idx[0]]);
                coinFaceLabel.setForeground(idx[0]%2==0?GOLD:P2_CLR); idx[0]++;
            } else {
                anim.stop();
                coinFaceLabel.setText(result.equals("HEADS")?"H":"T"); coinFaceLabel.setForeground(GOLD);
                p1GoesFirst = p1CoinChoice.equals(result);
                String first = p1GoesFirst?player1.getName()+" (P1)":player2.getName()+" (P2)";
                coinStatusLabel.setText("Result: "+result+"!  "+first+" goes FIRST!");
                new Timer(2000, done -> { ((Timer)done.getSource()).stop(); launchBattle(); })
                {{ setRepeats(false); start(); }};
            }
        });
        anim.start();
    }

    // ══════════════════════════════════════════════════════════
    //  Phase 3 — Battle (NEW visual architecture)
    // ══════════════════════════════════════════════════════════
    private void launchBattle() {
        mainPanel.add(buildBattlePanel(), CARD_BATTLE);
        cardLayout.show(mainPanel, CARD_BATTLE);
        fullHeal(player1); fullHeal(player2);
        turnManager = new TurnManager(player1, player2);
        if (!p1GoesFirst) turnManager.advanceTurn();
        refreshUI(); refreshAllCdLabels(); updateTurnState();
        showRoundAnnouncement(currentRound);
        log("⚔  Round "+currentRound+" — First to "+ROUNDS_TO_WIN+" round wins!");
        log(player1.getName()+" [P1]  vs  "+player2.getName()+" [P2]");
        log("🪙  "+(p1GoesFirst?player1.getName()+" (P1)":player2.getName()+" (P2)")+" goes first!");
    }

    private JPanel buildBattlePanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.BLACK);

        JLayeredPane lp = new JLayeredPane();
        lp.setLayout(null);
        wrapper.add(lp, BorderLayout.CENTER);

        // Layer 0: background
        BgPanel bg = new BgPanel(BATTLE_BG);
        lp.add(bg, JLayeredPane.DEFAULT_LAYER);

        // Layer 1: custom-painted HUD
        battleCanvas = new BattleCanvas();
        lp.add(battleCanvas, JLayeredPane.PALETTE_LAYER);

        // Layer 2: skill buttons + mini log
        skillsLayer = new JPanel(null);
        skillsLayer.setOpaque(false);
        buildSkillsLayer();
        registerHotkeys(lp);
        lp.add(skillsLayer, JLayeredPane.MODAL_LAYER);

        // Layer 3: round announcement overlay
        roundOverlay = new RoundOverlay();
        roundOverlay.setVisible(false);
        lp.add(roundOverlay, JLayeredPane.POPUP_LAYER);

        // Dummy labels so existing logic doesn't NPE
        p1WinsLabel = new JLabel("0"); p2WinsLabel = new JLabel("0");
        roundLabel  = new JLabel("Round 1"); turnIndicator = new JLabel("");

        lp.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int W = lp.getWidth(), H = lp.getHeight();
                if (W == 0 || H == 0) return;
                bg.setBounds(0, 0, W, H);
                battleCanvas.setBounds(0, 0, W, H);
                skillsLayer.setBounds(0, 0, W, H);
                roundOverlay.setBounds(0, 0, W, H);
                layoutSkillsLayer(W, H);
            }
        });
        return wrapper;
    }
    private void registerHotkeys(JComponent battleRoot) {
        InputMap  im = battleRoot.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = battleRoot.getActionMap();

        // ── Player 1 bindings (A / S / D) ────────────────────────────────────
        int[][] p1Keys = {
                { KeyEvent.VK_A, 0 },
                { KeyEvent.VK_S, 1 },
                { KeyEvent.VK_D, 2 },
        };
        for (int[] kb : p1Keys) {
            int keyCode = kb[0];
            int si      = kb[1];
            String id   = "pvp_p1_skill_" + si;
            im.put(KeyStroke.getKeyStroke(keyCode, 0, false), id);
            am.put(id, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (si < p1SkillBtns.length && p1SkillBtns[si] != null
                            && p1SkillBtns[si].isEnabled()) {
                        onP1Skill(si);
                    }
                }
            });
        }

        // ── Player 2 bindings (Numpad 1 / 2 / 3) ─────────────────────────────
        int[][] p2Keys = {
                { KeyEvent.VK_NUMPAD1, 0 },
                { KeyEvent.VK_NUMPAD2, 1 },
                { KeyEvent.VK_NUMPAD3, 2 },
        };
        for (int[] kb : p2Keys) {
            int keyCode = kb[0];
            int si      = kb[1];
            String id   = "pvp_p2_skill_" + si;
            im.put(KeyStroke.getKeyStroke(keyCode, 0, false), id);
            am.put(id, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (si < p2SkillBtns.length && p2SkillBtns[si] != null
                            && p2SkillBtns[si].isEnabled()) {
                        onP2Skill(si);
                    }
                }
            });
        }
    }
    private void buildSkillsLayer() {
        List<Skill> s1 = player1.getSkills();
        List<Skill> s2 = player2 != null ? player2.getSkills() : List.of();

        JPanel p1Panel = new JPanel();
        p1Panel.setOpaque(false);
        p1Panel.setLayout(new BoxLayout(p1Panel, BoxLayout.Y_AXIS));

        JPanel p2Panel = new JPanel();
        p2Panel.setOpaque(false);
        p2Panel.setLayout(new BoxLayout(p2Panel, BoxLayout.Y_AXIS));

        for (int i = 0; i < 3; i++) {
            final int si = i;
            String n1 = (i<s1.size())?s1.get(i).getName():"—";
            String n2 = (i<s2.size())?s2.get(i).getName():"—";

            p1SkillBtns[i] = makePillButton(n1, new Color(0x70,0x14,0x14), new Color(0xFF,0x99,0x99));
            p1SkillBtns[i].setEnabled(false);
            p1SkillBtns[i].addActionListener(e -> onP1Skill(si));
            p1CdLabels[i]  = makeCdLabel("READY", GREEN_RDY);
            JPanel sl1 = makeSkillSlot(p1SkillBtns[i], p1CdLabels[i]);
            p1Panel.add(sl1);
            if (i<2) p1Panel.add(Box.createVerticalStrut(4));

            p2SkillBtns[i] = makePillButton(n2, new Color(0x14,0x40,0x28), new Color(0x99,0xFF,0xBB));
            p2SkillBtns[i].setEnabled(false);
            p2SkillBtns[i].addActionListener(e -> onP2Skill(si));
            p2CdLabels[i]  = makeCdLabel("READY", GREEN_RDY);
            JPanel sl2 = makeSkillSlot(p2SkillBtns[i], p2CdLabels[i]);
            p2Panel.add(sl2);
            if (i<2) p2Panel.add(Box.createVerticalStrut(4));
        }

        skillsLayer.add(p1Panel);
        skillsLayer.add(p2Panel);
        skillsLayer.putClientProperty("p1", p1Panel);
        skillsLayer.putClientProperty("p2", p2Panel);

        // Mini battle log
        battleLog = new JTextArea();
        battleLog.setFont(new Font("Monospaced", Font.PLAIN, 10));
        battleLog.setForeground(LOG_FG); battleLog.setOpaque(false);
        battleLog.setEditable(false); battleLog.setLineWrap(true); battleLog.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(battleLog,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.setBorder(null);

        JPanel logHolder = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0,0,0,145));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(new Color(0xC8,0xA0,0x28,70));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        logHolder.setOpaque(false); logHolder.add(scroll, BorderLayout.CENTER);
        skillsLayer.add(logHolder);
        skillsLayer.putClientProperty("log", logHolder);
    }

    private void layoutSkillsLayer(int W, int H) {
        JPanel p1Panel = (JPanel) skillsLayer.getClientProperty("p1");
        JPanel p2Panel = (JPanel) skillsLayer.getClientProperty("p2");
        JPanel logHolder = (JPanel) skillsLayer.getClientProperty("log");
        if (p1Panel == null) return;

        double sc = Math.min(W/1024.0, H/768.0);
        int skillW = (int)(170*sc);
        int skillH = (int)(112*sc);
        int skillY = H - skillH - (int)(16*sc);

        p1Panel.setBounds((int)(10*sc), skillY, skillW, skillH);
        p2Panel.setBounds(W-(int)(10*sc)-skillW, skillY, skillW, skillH);

        if (logHolder != null) {
            int lw = (int)(340*sc), lh = (int)(110*sc);
            logHolder.setBounds((W-lw)/2, H-lh-(int)(16*sc), lw, lh);
        }
        skillsLayer.revalidate(); skillsLayer.repaint();
    }

    // ── Turn logic ────────────────────────────────────────────
    private void onP1Skill(int si) {
        if (processingTurn || !turnManager.isPlayerTurn()) return;
        processingTurn = true; setP1Enabled(false); setP2Enabled(false);
        TurnResult res = turnManager.executeSkill(player1, player2, si);
        flushResult(res); refreshUI();
        if (res.isTargetDefeated()) { endRound(true); return; }
        if (res.isTurnStolen())     { setP1Enabled(true); processingTurn=false; return; }
        turnManager.advanceTurn(); refreshAllCdLabels(); updateTurnState();
        processingTurn = false;
    }

    private void onP2Skill(int si) {
        if (processingTurn || turnManager.isPlayerTurn()) return;
        processingTurn = true; setP1Enabled(false); setP2Enabled(false);
        TurnResult res = turnManager.executeSkill(player2, player1, si);
        flushResult(res); refreshUI();
        if (res.isTargetDefeated()) { endRound(false); return; }
        if (res.isTurnStolen())     { setP2Enabled(true); processingTurn=false; return; }
        turnManager.advanceTurn(); refreshAllCdLabels(); updateTurnState();
        processingTurn = false;
    }

    private void updateTurnState() {
        boolean p1Turn = turnManager.isPlayerTurn();
        setP1Enabled(p1Turn); setP2Enabled(!p1Turn);
        if (battleCanvas != null) battleCanvas.setP1Active(p1Turn);
    }

    // ── Round management ──────────────────────────────────────
    private void endRound(boolean p1Won) {
        setP1Enabled(false); setP2Enabled(false);
        if (p1Won) { p1Wins++; log("🏆  "+player1.getName()+" (P1) wins Round "+currentRound+"!"); }
        else       { p2Wins++; log("🏆  "+player2.getName()+" (P2) wins Round "+currentRound+"!"); }
        if (battleCanvas != null) battleCanvas.repaint();

        if (p1Wins >= ROUNDS_TO_WIN || p2Wins >= ROUNDS_TO_WIN) {
            new Timer(1600, e -> { ((Timer)e.getSource()).stop(); showMatchResult(p1Wins>=ROUNDS_TO_WIN); })
            {{ setRepeats(false); start(); }};
        } else {
            currentRound++;
            new Timer(2000, e -> { ((Timer)e.getSource()).stop(); startNextRound(); })
            {{ setRepeats(false); start(); }};
        }
    }

    private void startNextRound() {
        fullHeal(player1); fullHeal(player2);
        p1GoesFirst = !p1GoesFirst;
        turnManager = new TurnManager(player1, player2);
        if (!p1GoesFirst) turnManager.advanceTurn();
        refreshUI(); refreshAllCdLabels(); processingTurn = false; updateTurnState();
        showRoundAnnouncement(currentRound);
        log("⚔  Round "+currentRound+" begins!  "+(p1GoesFirst?player1.getName():player2.getName())+" goes first.");
    }

    private void showRoundAnnouncement(int round) {
        if (roundOverlay != null) roundOverlay.show(round);
    }

    private void showMatchResult(boolean p1Won) {
        Character winner = p1Won?player1:player2, loser = p1Won?player2:player1;
        int wW=p1Won?p1Wins:p2Wins, lW=p1Won?p2Wins:p1Wins;
        String wTag=p1Won?"(P1)":"(P2)", lTag=p1Won?"(P2)":"(P1)";
        log("★  MATCH OVER — "+winner.getName()+" "+wTag+" wins "+wW+"–"+lW+"!");
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, winner.getName()+" "+wTag+" wins "+wW+"–"+lW+"!", "Match Over", JOptionPane.INFORMATION_MESSAGE);
            String[] opts = {"Rematch","End Match"};
            int winVote  = JOptionPane.showOptionDialog(this, winner.getName()+" "+wTag+" — Rematch or End?", "Winner's Vote", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
            int loseVote = JOptionPane.showOptionDialog(this, loser.getName()+" "+lTag+" — Rematch or End?",  "Loser's Vote",  JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
            boolean rematch;
            if (winVote==loseVote) { rematch=(winVote==0); }
            else {
                boolean wWants=(winVote==0);
                rematch = Math.random()<WINNER_VOTE_WEIGHT ? wWants : !wWants;
                JOptionPane.showMessageDialog(this, "Players disagreed — fate decides!\nResult: "+(rematch?"REMATCH":"END MATCH"), "Decision", JOptionPane.INFORMATION_MESSAGE);
            }
            dispose();
            if (rematch) { p1Wins=0; p2Wins=0; currentRound=1; new PVPBattleFrame(player1); }
            else new MainMenuFrame();
        });
    }

    // ── UI helpers ────────────────────────────────────────────
    private void refreshUI() { if (battleCanvas!=null) battleCanvas.repaint(); }

    private void refreshAllCdLabels() {
        if (turnManager==null) return;
        refreshCdRow(player1, p1SkillBtns, p1CdLabels);
        if (player2!=null) refreshCdRow(player2, p2SkillBtns, p2CdLabels);
    }

    private void refreshCdRow(Character ch, JButton[] btns, JLabel[] lbls) {
        if (btns==null||lbls==null||turnManager==null) return;
        List<Skill> skills = ch.getSkills();
        for (int i=0;i<btns.length;i++) {
            if (btns[i]==null||i>=skills.size()) continue;
            int cd = turnManager.getCooldownManager().getRemainingCooldown(ch, i);
            if (cd>0) { lbls[i].setText(cd+" turn(s)"); lbls[i].setForeground(ORANGE_LOW); btns[i].setEnabled(false); }
            else      { lbls[i].setText("READY");        lbls[i].setForeground(GREEN_RDY); }
        }
    }

    private void setP1Enabled(boolean on) {
        for (JButton b:p1SkillBtns) if(b!=null) b.setEnabled(on);
        if (on) refreshCdRow(player1,p1SkillBtns,p1CdLabels);
    }
    private void setP2Enabled(boolean on) {
        for (JButton b:p2SkillBtns) if(b!=null) b.setEnabled(on);
        if (on&&player2!=null) refreshCdRow(player2,p2SkillBtns,p2CdLabels);
    }
    private void flushResult(TurnResult r) { for (String m:r.getLogMessages()) log(m); }
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (battleLog!=null) { battleLog.append(msg+"\n"); battleLog.setCaretPosition(battleLog.getDocument().getLength()); }
        });
    }
    private static void fullHeal(Character c) { c.heal(c.getMaxHP()); }
    private String getFrameImg(String name) {
        for (int i=0;i<CHAR_NAMES.length;i++) if (CHAR_NAMES[i].equals(name)) return FRAME_IMGS[i];
        return null;
    }

    // ══════════════════════════════════════════════════════════
    //  BattleCanvas — paints the full HUD
    // ══════════════════════════════════════════════════════════
    private class BattleCanvas extends JPanel {
        private final Image p1Frame, p2Frame;
        private final Image[] tablets = new Image[3];
        private boolean p1Active = true;
        private float   glowTick = 0f;
        private final Timer glowTimer;

        BattleCanvas() {
            setOpaque(false);
            p1Frame = loadImage(getFrameImg(player1.getName()));
            p2Frame = player2!=null ? loadImage(getFrameImg(player2.getName())) : null;
            for (int i=0;i<3;i++) tablets[i] = loadImage(ROUND_TABLETS[i]);
            glowTimer = new Timer(20, e -> { glowTick+=0.08f; repaint(); });
            glowTimer.start();
        }

        void setP1Active(boolean v) { p1Active=v; }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (player2==null) return;
            int W=getWidth(), H=getHeight();
            double sc = Math.min(W/1024.0, H/768.0);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // ── Round tablet (top center) ─────────────────────
            int ri = Math.min(currentRound-1,2);
            Image tab = tablets[ri];
            int tabW=(int)(200*sc), tabH=(int)(68*sc);
            int tabX=(W-tabW)/2, tabY=(int)(6*sc);
            if (tab!=null) g2.drawImage(tab, tabX, tabY, tabW, tabH, null);

            // ── Shared measurements ───────────────────────────
            int portW=(int)(82*sc), portH=(int)(82*sc);
            int hpW=(int)(230*sc), hpH=(int)(16*sc);
            int pillW=(int)(140*sc), pillH=(int)(24*sc);
            int topY=(int)(12*sc);
            int portY=tabY+tabH+(int)(6*sc);

            // ── P1 ────────────────────────────────────────────
            int p1x=(int)(10*sc);
            drawPortrait(g2, p1Frame, p1x, portY, portW, portH, P1_CLR, p1Active);
            int p1hpx=p1x+portW+(int)(8*sc), p1hpy=portY+(int)(8*sc);
            drawHPBar(g2,p1hpx,p1hpy,hpW,hpH,player1,P1_CLR);
            String p1hp="HP: "+player1.getCurrentHP()+" / "+player1.getMaxHP();
            g2.setFont(new Font("SansSerif",Font.PLAIN,Math.max(8,(int)(10*sc))));
            drawShadow(g2,p1hp,p1hpx,p1hpy+hpH+(int)(10*sc),new Color(0xFF,0xF5,0xDC,190));
            drawNamePill(g2,player1.getName(),p1hpx,p1hpy+hpH+(int)(14*sc),pillW,pillH,
                    new Color(0x60,0x10,0x10,215),new Color(0xFF,0x99,0x99));
            g2.setFont(new Font("SansSerif",Font.BOLD,Math.max(9,(int)(11*sc))));
            drawShadow(g2,"PLAYER 1",p1hpx,p1hpy-(int)(3*sc),new Color(0xEE,0xEE,0xEE));
            g2.setFont(new Font("Serif",Font.BOLD,Math.max(16,(int)(22*sc))));
            drawShadow(g2,String.valueOf(p1Wins),p1x,portY-(int)(4*sc),P1_CLR);

            // ── P2 ────────────────────────────────────────────
            int p2px=W-(int)(10*sc)-portW;
            drawPortrait(g2, p2Frame, p2px, portY, portW, portH, P2_CLR, !p1Active);
            int p2hpx=p2px-hpW-(int)(8*sc), p2hpy=portY+(int)(8*sc);
            drawHPBar(g2,p2hpx,p2hpy,hpW,hpH,player2,P2_CLR);
            String p2hp="HP: "+player2.getCurrentHP()+" / "+player2.getMaxHP();
            g2.setFont(new Font("SansSerif",Font.PLAIN,Math.max(8,(int)(10*sc))));
            drawShadow(g2,p2hp,p2hpx,p2hpy+hpH+(int)(10*sc),new Color(0xFF,0xF5,0xDC,190));
            drawNamePill(g2,player2.getName(),p2hpx,p2hpy+hpH+(int)(14*sc),pillW,pillH,
                    new Color(0x10,0x28,0x60,215),new Color(0x99,0xBB,0xFF));
            g2.setFont(new Font("SansSerif",Font.BOLD,Math.max(9,(int)(11*sc))));
            drawShadow(g2,"PLAYER 2",p2hpx,p2hpy-(int)(3*sc),new Color(0xEE,0xEE,0xEE));
            g2.setFont(new Font("Serif",Font.BOLD,Math.max(16,(int)(22*sc))));
            drawShadow(g2,String.valueOf(p2Wins),p2px+portW-(int)(24*sc),portY-(int)(4*sc),P2_CLR);

            // ── Turn indicator (below tablet) ─────────────────
            if (turnManager!=null) {
                String tt = p1Active?"▶ "+player1.getName()+"'s Turn":"▶ "+player2.getName()+"'s Turn";
                g2.setFont(new Font("Serif",Font.BOLD|Font.ITALIC,Math.max(10,(int)(13*sc))));
                FontMetrics fm=g2.getFontMetrics();
                drawShadow(g2,tt,(W-fm.stringWidth(tt))/2,tabY+tabH+(int)(4*sc),GOLD);
            }
            g2.dispose();
        }

        private void drawPortrait(Graphics2D g2,Image img,int x,int y,int w,int h,Color accent,boolean active) {
            if (active) {
                float a=0.25f+0.15f*(float)Math.sin(glowTick);
                for (int r=5;r>=1;r--) {
                    int sp=r*3;
                    g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),Math.min(255,(int)(a*80/r))));
                    g2.setStroke(new BasicStroke(sp));
                    g2.drawRoundRect(x-sp/2,y-sp/2,w+sp,h+sp,10,10);
                }
            }
            g2.setColor(new Color(0x08,0x05,0x02,200)); g2.fillRoundRect(x,y,w,h,8,8);
            if (img!=null) g2.drawImage(img,x,y,w,h,null);
            g2.setStroke(new BasicStroke(2));
            g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),active?220:100));
            g2.drawRoundRect(x,y,w,h,8,8);
        }

        private void drawHPBar(Graphics2D g2,int x,int y,int w,int h,Character c,Color base) {
            double pct=Math.max(0,Math.min(1.0,(double)c.getCurrentHP()/c.getMaxHP()));
            Color bar=pct<=0.25?RED_CRIT:pct<=0.50?ORANGE_LOW:base;
            g2.setColor(new Color(0x08,0x04,0x02,220)); g2.fillRoundRect(x,y,w,h,h,h);
            int fw=(int)(w*pct);
            if (fw>2) { g2.setPaint(new GradientPaint(x,y,bar.brighter(),x,y+h,bar.darker())); g2.fillRoundRect(x,y,fw,h,h,h); }
            g2.setStroke(new BasicStroke(1)); g2.setColor(new Color(0xFF,0xFF,0xFF,55)); g2.drawRoundRect(x,y,w,h,h,h);
        }

        private void drawNamePill(Graphics2D g2,String text,int x,int y,int w,int h,Color bg,Color fg) {
            g2.setColor(bg); g2.fillRoundRect(x,y,w,h,h,h);
            g2.setStroke(new BasicStroke(1)); g2.setColor(new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),130)); g2.drawRoundRect(x,y,w,h,h,h);
            g2.setFont(new Font("Serif",Font.BOLD,Math.max(9,h-6)));
            FontMetrics fm=g2.getFontMetrics(); int tx=x+(w-fm.stringWidth(text))/2, ty=y+(h+fm.getAscent()-fm.getDescent())/2;
            g2.setColor(new Color(0,0,0,100)); g2.drawString(text,tx+1,ty+1);
            g2.setColor(fg); g2.drawString(text,tx,ty);
        }

        private void drawShadow(Graphics2D g2,String t,int x,int y,Color c) {
            g2.setColor(new Color(0,0,0,150)); g2.drawString(t,x+1,y+1);
            g2.setColor(c); g2.drawString(t,x,y);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  RoundOverlay — animated round text announcement
    // ══════════════════════════════════════════════════════════
    private class RoundOverlay extends JPanel {
        private float alpha = 0f;
        private Image textImg;
        private Timer fadeTimer;

        RoundOverlay() { setOpaque(false); }

        void show(int round) {
            int ri = Math.min(round-1,2);
            textImg = loadImage(ROUND_TEXTS[ri]);
            alpha = 0f; setVisible(true); repaint();
            if (fadeTimer!=null) fadeTimer.stop();
            final boolean[] in={true};
            final long[]   hold={0};
            fadeTimer = new Timer(16, e -> {
                if (in[0]) { alpha=Math.min(1f,alpha+0.07f); if(alpha>=1f){in[0]=false;hold[0]=System.currentTimeMillis();} }
                else if (System.currentTimeMillis()-hold[0]<900) { /* hold */ }
                else { alpha=Math.max(0f,alpha-0.05f); if(alpha<=0f){((Timer)e.getSource()).stop();setVisible(false);} }
                repaint();
            });
            fadeTimer.start();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (alpha<=0f) return;
            int W=getWidth(),H=getHeight();
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.min(1f,alpha*0.5f)));
            g2.setColor(Color.BLACK); g2.fillRect(0,0,W,H);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha));
            if (textImg!=null) {
                int iw=textImg.getWidth(null),ih=textImg.getHeight(null);
                if (iw>0&&ih>0) {
                    double sc=Math.min(W*0.78/iw,H*0.40/ih);
                    int dw=(int)(iw*sc),dh=(int)(ih*sc);
                    g2.drawImage(textImg,(W-dw)/2,(H-dh)/2,dw,dh,null);
                }
            }
            g2.dispose();
        }
    }

    // ── Widget factories ──────────────────────────────────────
    private JButton makePillButton(String label, Color bg, Color fg) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean h=getModel().isRollover(),en=isEnabled();
                Color bc=en?(h?bg.brighter():bg):new Color(0x28,0x20,0x18,140);
                g2.setColor(new Color(bc.getRed(),bc.getGreen(),bc.getBlue(),210)); g2.fillRoundRect(0,0,getWidth(),getHeight(),getHeight(),getHeight());
                g2.setStroke(new BasicStroke(1.5f)); g2.setColor(new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),en?(h?255:180):70)); g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,getHeight()-2,getHeight()-2);
                g2.setFont(new Font("Serif",Font.BOLD,Math.max(9,getHeight()-8)));
                FontMetrics fm=g2.getFontMetrics(); int tx=(getWidth()-fm.stringWidth(getText()))/2,ty=(getHeight()+fm.getAscent()-fm.getDescent())/2;
                g2.setColor(new Color(0,0,0,90)); g2.drawString(getText(),tx+1,ty+1);
                g2.setColor(en?fg:new Color(0x60,0x50,0x40)); g2.drawString(getText(),tx,ty);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel makeCdLabel(String text, Color color) {
        JLabel l=new JLabel(text,SwingConstants.CENTER); l.setFont(new Font("SansSerif",Font.BOLD,9)); l.setForeground(color); return l;
    }

    private JPanel makeSkillSlot(JButton btn, JLabel cd) {
        JPanel slot=new JPanel(new BorderLayout(0,2)); slot.setOpaque(false);
        slot.add(btn,BorderLayout.CENTER); slot.add(cd,BorderLayout.SOUTH); return slot;
    }

    private JButton makeDarkButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()?new Color(0x3C,0x2C,0x10):new Color(0x28,0x1E,0x0C)); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(BORDER_CLR); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,10,10);
                g2.setFont(new Font("Serif",Font.BOLD,14)); g2.setColor(CREAM);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); btn.setPreferredSize(new Dimension(120,40));
        return btn;
    }

    // ── Image panels & helpers ────────────────────────────────
    private Image loadImage(String path) {
        if (path==null) return null;
        URL url=getClass().getResource(path);
        if (url==null){System.err.println("Missing: "+path);return null;}
        return new ImageIcon(url).getImage();
    }

    private class BgPanel extends JPanel {
        private final Image img;
        BgPanel(String p){img=loadImage(p);setOpaque(true);setBackground(Color.BLACK);}
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(img!=null){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g2.drawImage(img,0,0,getWidth(),getHeight(),null);g2.dispose();}
        }
    }
    private class ScaledImgPanel extends JPanel {
        private final Image img;
        ScaledImgPanel(String p){img=loadImage(p);setOpaque(false);}
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(img==null)return;Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g2.drawImage(img,0,0,getWidth(),getHeight(),null);g2.dispose();
        }
    }

    // ── P2 Selection inner panels (unchanged) ─────────────────
    private class PlayerBanner extends JPanel {
        PlayerBanner(){setOpaque(false);}
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int W=getWidth(),H=getHeight();
            g2.setColor(new Color(0x0A,0x06,0x02,200)); g2.fillRoundRect(W/8,2,W*6/8,H-4,12,12);
            g2.setStroke(new BasicStroke(1.5f)); g2.setColor(new Color(0xC8,0xA0,0x28,180)); g2.drawRoundRect(W/8,2,W*6/8,H-4,12,12);
            g2.setFont(new Font("Serif",Font.BOLD|Font.ITALIC,Math.max(14,H/2)));
            FontMetrics fm=g2.getFontMetrics(); String main="\u2726  The Second Warrior Rises  \u2726";
            g2.setColor(new Color(0,0,0,120)); g2.drawString(main,(W-fm.stringWidth(main))/2+1,H/2);
            g2.setColor(new Color(0x80,0xC4,0xFF)); g2.drawString(main,(W-fm.stringWidth(main))/2,H/2-1);
            g2.setFont(new Font("Serif",Font.ITALIC,Math.max(10,H/3)));
            fm=g2.getFontMetrics(); String sub="Player 2 \u2014 step forth and claim your element";
            g2.setColor(new Color(0xD4,0xC5,0xA0,200)); g2.drawString(sub,(W-fm.stringWidth(sub))/2,H-4);
            g2.dispose();
        }
    }

    private class StatusRibbon extends JPanel {
        private String warningText=null;
        StatusRibbon(){setOpaque(false);}
        void setWarning(String t){warningText=t;repaint();}
        void clearWarning(){warningText=null;repaint();}
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int W=getWidth(),H=getHeight();
            if (warningText!=null) {
                g2.setColor(new Color(0x60,0x10,0x10,210)); g2.fillRect(0,0,W,H);
                g2.setColor(new Color(0xFF,0x88,0x88)); g2.setFont(new Font("Serif",Font.BOLD|Font.ITALIC,Math.max(11,H/3)));
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(warningText,(W-fm.stringWidth(warningText))/2,(H+fm.getAscent()-fm.getDescent())/2);
            } else {
                g2.setColor(new Color(0x0A,0x06,0x02,200)); g2.fillRect(0,0,W,H);
                g2.setColor(new Color(0xC8,0xA0,0x28,120)); g2.setStroke(new BasicStroke(1f)); g2.drawLine(0,0,W,0);
                String p1Txt=player1==null?"Awaiting Player 1's resonance...":"\u2605  Player 1 has chosen:  "+player1.getName()+"  \u2014  "+player1.getTitle()+"  \u2605";
                g2.setFont(new Font("Serif",Font.ITALIC,Math.max(11,H/3)));
                FontMetrics fm=g2.getFontMetrics();
                g2.setColor(new Color(0,0,0,100)); g2.drawString(p1Txt,(W-fm.stringWidth(p1Txt))/2+1,(H+fm.getAscent()-fm.getDescent())/2+1);
                g2.setColor(new Color(0x60,0xCC,0x90)); g2.drawString(p1Txt,(W-fm.stringWidth(p1Txt))/2,(H+fm.getAscent()-fm.getDescent())/2);
            }
            g2.dispose();
        }
    }

    private class CharCard extends JPanel {
        private final Image img; private final String name; private final Color glow;
        private boolean hovered=false; private float glowAlpha=0f; private final Timer glowTimer;
        CharCard(String path,String name,Color glow){this.img=loadImage(path);this.name=name;this.glow=glow;setOpaque(false);
            glowTimer=new Timer(20,e->{if(hovered&&glowAlpha<1f){glowAlpha=Math.min(1f,glowAlpha+0.08f);repaint();}else if(!hovered&&glowAlpha>0f){glowAlpha=Math.max(0f,glowAlpha-0.06f);repaint();}});glowTimer.start();}
        void setHovered(boolean h){this.hovered=h;}
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int W=getWidth(),H=getHeight();
            if(glowAlpha>0f){for(int r=4;r>=1;r--){int sp=r*6;float a=(glowAlpha*0.25f)/r;g2.setColor(new Color(glow.getRed(),glow.getGreen(),glow.getBlue(),Math.min(255,(int)(a*255))));g2.setStroke(new BasicStroke(sp));g2.drawRoundRect(sp/2,sp/2,W-sp,H-sp,16,16);}}
            float sc=hovered?1.08f:1.0f; int dw=(int)(W*sc),dh=(int)(H*sc),dx=(W-dw)/2,dy=(H-dh)/2;
            if(img!=null)g2.drawImage(img,dx,dy,dw,dh,null);else{g2.setColor(new Color(60,50,30));g2.fillRoundRect(dx,dy,dw,dh,12,12);}
            if(glowAlpha>0.05f){int fs=Math.max(10,(int)(H*0.13));g2.setFont(new Font("SansSerif",Font.BOLD,fs));FontMetrics fm=g2.getFontMetrics();int tx=(W-fm.stringWidth(name))/2,ty=H-(int)(H*0.06);int pp=6,pw=fm.stringWidth(name)+pp*2,ph=fm.getHeight()+2;g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,glowAlpha*0.85f));g2.setColor(glow.darker());g2.fillRoundRect((W-pw)/2,ty-fm.getAscent()-2,pw,ph,ph,ph);g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,glowAlpha));g2.setColor(new Color(0,0,0,160));g2.drawString(name,tx+1,ty+1);g2.setColor(Color.WHITE);g2.drawString(name,tx,ty);}
            g2.dispose();
        }
    }

    public Character getPlayer1Character(){return player1;}
    public Character getPlayer2Character(){return player2;}
    public void setPlayer2Character(Character c){this.player2=c;}
}