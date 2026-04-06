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
import java.awt.event.*;
import java.net.URL;
import java.util.List;

public class PVEBattleFrame extends JFrame {

    // ── Resources ─────────────────────────────────────────────
    private static final String BATTLE_BG = "/resources/backgroundPve.png";
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
            "/resources/adamusFrame (1).png",  "/resources/teraFrame (1).png"
    };
    private static final String[] CHAR_NAMES = {
            "Tyrone","Elan","Claire","Dirk","Flamara","Dea","Adamus","Tera"
    };

    // ── Constants ─────────────────────────────────────────────
    private static final int ROUNDS_TO_WIN    = 2;
    private static final int ENEMY_TURN_DELAY = 1100;

    // ── Colors ────────────────────────────────────────────────
    private static final Color PLAYER_CLR = new Color(0x2E,0x8B,0x57);
    private static final Color ENEMY_CLR  = new Color(0xB0,0x2A,0x2A);
    private static final Color GOLD       = new Color(0xC8,0xA0,0x28);
    private static final Color CREAM      = new Color(0xFF,0xF5,0xDC);
    private static final Color LOG_FG     = new Color(0xD4,0xC5,0xA0);
    private static final Color ORANGE_LOW = new Color(0xCC,0x88,0x22);
    private static final Color RED_CRIT   = new Color(0xCC,0x22,0x22);
    private static final Color GREEN_RDY  = new Color(0x60,0xCC,0x60);
    private static final Color BG_DARK    = new Color(0x18,0x14,0x0E);
    private static final Color BORDER_CLR = new Color(0xC8,0xA0,0x28);

    // ── Characters & engine ───────────────────────────────────
    private final Character playerCharacter;
    private final Character enemyCharacter;
    private TurnManager     turnManager;

    // ── Match state ───────────────────────────────────────────
    private int     playerWins   = 0;
    private int     enemyWins    = 0;
    private int     currentRound = 1;
    private volatile boolean processingTurn = false;

    // ── Visual refs ───────────────────────────────────────────
    private BattleCanvas battleCanvas;
    private JPanel       skillsLayer;
    private RoundOverlay roundOverlay;
    private JTextArea    battleLog;
    private JButton[]    skillBtns = new JButton[3];
    private JLabel[]     cdLabels  = new JLabel[3];
    // kept for existing logic calls
    private JLabel playerWinsLabel, enemyWinsLabel, roundLabel, turnIndicator;

    // ══════════════════════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════════════════════
    public PVEBattleFrame(Character playerCharacter, Character enemyCharacter) {
        this.playerCharacter = playerCharacter;
        this.enemyCharacter  = enemyCharacter;
        this.turnManager     = new TurnManager(playerCharacter, enemyCharacter);

        setTitle("PVE — "+playerCharacter.getName()+" vs "+enemyCharacter.getName());
        setSize(1024, 768);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        buildUI();
        registerHotkeys();
        setVisible(true);
        ScreenManager.register(this);

        refreshUI(); refreshCdRow(); updateTurnState();
        showRoundAnnouncement(currentRound);
        log("⚔  Round "+currentRound+" — First to "+ROUNDS_TO_WIN+" wins!");
        log(playerCharacter.getName()+"  vs  "+enemyCharacter.getName());

    }

    private void registerHotkeys() {
        JComponent root = (JComponent) getContentPane();
        InputMap  im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        // Map key → skill index
        Object[][] bindings = {
                { KeyEvent.VK_A, 0, "pve_skill_0" },
                { KeyEvent.VK_S, 1, "pve_skill_1" },
                { KeyEvent.VK_D, 2, "pve_skill_2" },
        };

        for (Object[] b : bindings) {
            int     keyCode = (int) b[0];
            int     si      = (int) b[1];
            String  id      = (String) b[2];

            im.put(KeyStroke.getKeyStroke(keyCode, 0, false), id);
            am.put(id, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    // Only fire if the button is currently enabled (same guard as mouse click)
                    if (si < skillBtns.length && skillBtns[si] != null
                            && skillBtns[si].isEnabled()) {
                        onPlayerSkill(si);
                    }
                }
            });
        }
    }

    @Override
    public void dispose() { ScreenManager.unregister(this); super.dispose(); }

    // ══════════════════════════════════════════════════════════
    //  UI construction — JLayeredPane with full background
    // ══════════════════════════════════════════════════════════
    private void buildUI() {
        JLayeredPane lp = new JLayeredPane();
        lp.setLayout(null);
        setContentPane(lp);

        // Layer 0: background
        BgPanel bg = new BgPanel(BATTLE_BG);
        lp.add(bg, JLayeredPane.DEFAULT_LAYER);

        // Layer 1: custom HUD canvas
        battleCanvas = new BattleCanvas();
        lp.add(battleCanvas, JLayeredPane.PALETTE_LAYER);

        // Layer 2: skill buttons + mini log
        skillsLayer = new JPanel(null);
        skillsLayer.setOpaque(false);
        buildSkillsLayer();
        lp.add(skillsLayer, JLayeredPane.MODAL_LAYER);

        // Layer 3: round overlay
        roundOverlay = new RoundOverlay();
        roundOverlay.setVisible(false);
        lp.add(roundOverlay, JLayeredPane.POPUP_LAYER);

        // Dummy labels so existing calls don't NPE
        playerWinsLabel = new JLabel("0"); enemyWinsLabel = new JLabel("0");
        roundLabel = new JLabel("Round 1"); turnIndicator = new JLabel("");

        lp.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int W=lp.getWidth(),H=lp.getHeight();
                if(W==0||H==0) return;
                bg.setBounds(0,0,W,H); battleCanvas.setBounds(0,0,W,H);
                skillsLayer.setBounds(0,0,W,H); roundOverlay.setBounds(0,0,W,H);
                layoutSkillsLayer(W,H);
            }
        });
    }

    private void buildSkillsLayer() {
        List<Skill> skills = playerCharacter.getSkills();

        JPanel skillPanel = new JPanel();
        skillPanel.setOpaque(false);
        skillPanel.setLayout(new BoxLayout(skillPanel, BoxLayout.Y_AXIS));

        for (int i=0;i<3;i++) {
            final int si=i;
            String n=(i<skills.size())?skills.get(i).getName():"—";
            skillBtns[i]=makePillButton(n,new Color(0x70,0x14,0x14),new Color(0xFF,0x99,0x99));
            skillBtns[i].setEnabled(false);
            skillBtns[i].addActionListener(e->onPlayerSkill(si));
            cdLabels[i]=new JLabel("READY",SwingConstants.CENTER);
            cdLabels[i].setFont(new Font("SansSerif",Font.BOLD,9)); cdLabels[i].setForeground(GREEN_RDY);
            JPanel slot=new JPanel(new BorderLayout(0,2)); slot.setOpaque(false);
            slot.add(skillBtns[i],BorderLayout.CENTER); slot.add(cdLabels[i],BorderLayout.SOUTH);
            skillPanel.add(slot);
            if (i<2) skillPanel.add(Box.createVerticalStrut(4));
        }

        skillsLayer.add(skillPanel);
        skillsLayer.putClientProperty("skills", skillPanel);

        // Mini log
        battleLog = new JTextArea();
        battleLog.setFont(new Font("Monospaced",Font.PLAIN,10));
        battleLog.setForeground(LOG_FG); battleLog.setOpaque(false);
        battleLog.setEditable(false); battleLog.setLineWrap(true); battleLog.setWrapStyleWord(true);
        JScrollPane scroll=new JScrollPane(battleLog,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.setBorder(null);
        JPanel logHolder=new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setColor(new Color(0,0,0,145)); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(new Color(0xC8,0xA0,0x28,70)); g2.setStroke(new BasicStroke(1)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        logHolder.setOpaque(false); logHolder.add(scroll,BorderLayout.CENTER);
        skillsLayer.add(logHolder);
        skillsLayer.putClientProperty("log", logHolder);
    }

    private void layoutSkillsLayer(int W,int H) {
        JPanel sp=(JPanel)skillsLayer.getClientProperty("skills");
        JPanel lh=(JPanel)skillsLayer.getClientProperty("log");
        if (sp==null) return;
        double sc=Math.min(W/1024.0,H/768.0);
        int skillW=(int)(170*sc),skillH=(int)(112*sc);
        sp.setBounds((int)(10*sc),H-skillH-(int)(16*sc),skillW,skillH);
        if (lh!=null){int lw=(int)(340*sc),lhh=(int)(110*sc);lh.setBounds((W-lw)/2,H-lhh-(int)(16*sc),lw,lhh);}
        skillsLayer.revalidate(); skillsLayer.repaint();
    }

    // ── Turn logic ────────────────────────────────────────────
    private void onPlayerSkill(int si) {
        if (processingTurn||!turnManager.isPlayerTurn()) return;
        processingTurn=true; setPlayerEnabled(false);
        TurnResult res=turnManager.executeSkill(playerCharacter,enemyCharacter,si);
        flushResult(res); refreshUI();
        if (res.isTargetDefeated()){endRound(true);return;}
        if (res.isTurnStolen()){setPlayerEnabled(true);processingTurn=false;return;}
        turnManager.advanceTurn(); refreshCdRow(); updateTurnState();
        new Timer(ENEMY_TURN_DELAY,e->doEnemyTurn()){{setRepeats(false);start();}};
    }

    private void doEnemyTurn() {
        int si=EnemyAI.chooseSkill(enemyCharacter,turnManager.getCooldownManager());
        TurnResult res=turnManager.executeSkill(enemyCharacter,playerCharacter,si);
        flushResult(res); refreshUI();
        if (res.isTargetDefeated()){endRound(false);return;}
        if (res.isTurnStolen()){new Timer(ENEMY_TURN_DELAY,e->doEnemyTurn()){{setRepeats(false);start();}};return;}
        turnManager.advanceTurn(); refreshCdRow(); updateTurnState();
        setPlayerEnabled(true); processingTurn=false;
    }

    private void updateTurnState() {
        boolean pt=turnManager.isPlayerTurn();
        setPlayerEnabled(pt);
        if (battleCanvas!=null) battleCanvas.setPlayerActive(pt);
    }

    // ── Round / match management ──────────────────────────────
    private void endRound(boolean playerWon) {
        setPlayerEnabled(false);
        if (playerWon){playerWins++;log("🏆  "+playerCharacter.getName()+" wins Round "+currentRound+"!");}
        else          {enemyWins++;  log("💀  "+enemyCharacter.getName()+" wins Round "+currentRound+"!");}
        if (battleCanvas!=null) battleCanvas.repaint();
        if (playerWins>=ROUNDS_TO_WIN||enemyWins>=ROUNDS_TO_WIN) {
            new Timer(1600,e->{((Timer)e.getSource()).stop();showMatchResult(playerWins>=ROUNDS_TO_WIN);}){{setRepeats(false);start();}};
        } else {
            currentRound++;
            new Timer(2000,e->{((Timer)e.getSource()).stop();startNextRound();}){{setRepeats(false);start();}};
        }
    }

    private void startNextRound() {
        fullHeal(playerCharacter); fullHeal(enemyCharacter);
        turnManager=new TurnManager(playerCharacter,enemyCharacter);
        refreshUI(); refreshCdRow(); processingTurn=false; updateTurnState();
        showRoundAnnouncement(currentRound);
        log("⚔  Round "+currentRound+" begins!");
    }

    private void showRoundAnnouncement(int round) {
        if (roundOverlay!=null) roundOverlay.show(round);
    }

    private void showMatchResult(boolean playerWon) {
        String winner=playerWon?playerCharacter.getName():enemyCharacter.getName();
        log("★  MATCH OVER — "+winner+" wins "+Math.max(playerWins,enemyWins)+"–"+Math.min(playerWins,enemyWins)+"!");
        String msg=playerWon?"Victory!\n"+playerCharacter.getName()+" wins "+playerWins+"–"+enemyWins+"!"
                :"Defeat.\n"+enemyCharacter.getName()+" wins "+enemyWins+"–"+playerWins+".";
        String[] options={"Main Menu","Rematch"};
        int choice=JOptionPane.showOptionDialog(this,msg,playerWon?"Victory!":"Defeat",
                JOptionPane.DEFAULT_OPTION,playerWon?JOptionPane.INFORMATION_MESSAGE:JOptionPane.WARNING_MESSAGE,null,options,options[0]);
        dispose();
        if (choice==1){fullHeal(playerCharacter);fullHeal(enemyCharacter);new PVEBattleFrame(playerCharacter,enemyCharacter);}
        else new MainMenuFrame();
    }

    // ── UI helpers ────────────────────────────────────────────
    private void refreshUI(){if(battleCanvas!=null)battleCanvas.repaint();}

    private void refreshCdRow(){
        if(turnManager==null)return;
        List<Skill> skills=playerCharacter.getSkills();
        for(int i=0;i<skillBtns.length;i++){
            if(skillBtns[i]==null||i>=skills.size())continue;
            int cd=turnManager.getCooldownManager().getRemainingCooldown(playerCharacter,i);
            if(cd>0){cdLabels[i].setText(cd+" turn(s)");cdLabels[i].setForeground(ORANGE_LOW);skillBtns[i].setEnabled(false);}
            else    {cdLabels[i].setText("READY");      cdLabels[i].setForeground(GREEN_RDY);}
        }
    }

    private void setPlayerEnabled(boolean on){
        for(JButton b:skillBtns)if(b!=null)b.setEnabled(on);
        if(on)refreshCdRow();
    }
    private void flushResult(TurnResult r){for(String m:r.getLogMessages())log(m);}
    private void log(String msg){
        SwingUtilities.invokeLater(()->{if(battleLog!=null){battleLog.append(msg+"\n");battleLog.setCaretPosition(battleLog.getDocument().getLength());}});
    }
    private static void fullHeal(Character c){c.heal(c.getMaxHP());}
    private String getFrameImg(String name){
        for(int i=0;i<CHAR_NAMES.length;i++)if(CHAR_NAMES[i].equals(name))return FRAME_IMGS[i];
        return null;
    }

    // ══════════════════════════════════════════════════════════
    //  BattleCanvas
    // ══════════════════════════════════════════════════════════
    private class BattleCanvas extends JPanel {
        private final Image playerFrame, enemyFrame;
        private final Image[] tablets=new Image[3];
        private boolean playerActive=true;
        private float   glowTick=0f;
        private final Timer glowTimer;

        BattleCanvas(){
            setOpaque(false);
            playerFrame=loadImage(getFrameImg(playerCharacter.getName()));
            enemyFrame =loadImage(getFrameImg(enemyCharacter.getName()));
            for(int i=0;i<3;i++) tablets[i]=loadImage(ROUND_TABLETS[i]);
            glowTimer=new Timer(20,e->{glowTick+=0.08f;repaint();}); glowTimer.start();
        }

        void setPlayerActive(boolean v){playerActive=v;}

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int W=getWidth(),H=getHeight();
            double sc=Math.min(W/1024.0,H/768.0);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Round tablet
            int ri=Math.min(currentRound-1,2);
            Image tab=tablets[ri]; int tabW=(int)(200*sc),tabH=(int)(68*sc);
            if(tab!=null) g2.drawImage(tab,(W-tabW)/2,(int)(6*sc),tabW,tabH,null);

            int portW=(int)(82*sc),portH=(int)(82*sc);
            int hpW=(int)(230*sc),hpH=(int)(16*sc),pillW=(int)(140*sc),pillH=(int)(24*sc);
            int portY=(int)(6*sc)+tabH+(int)(6*sc);

            // Player
            int ppx=(int)(10*sc);
            drawPortrait(g2,playerFrame,ppx,portY,portW,portH,PLAYER_CLR,playerActive);
            int phx=ppx+portW+(int)(8*sc),phy=portY+(int)(8*sc);
            drawHPBar(g2,phx,phy,hpW,hpH,playerCharacter,PLAYER_CLR);
            g2.setFont(new Font("SansSerif",Font.PLAIN,Math.max(8,(int)(10*sc))));
            drawShadow(g2,"HP: "+playerCharacter.getCurrentHP()+" / "+playerCharacter.getMaxHP(),phx,phy+hpH+(int)(10*sc),new Color(0xFF,0xF5,0xDC,190));
            drawNamePill(g2,playerCharacter.getName(),phx,phy+hpH+(int)(14*sc),pillW,pillH,new Color(0x60,0x10,0x10,215),new Color(0xFF,0x99,0x99));
            g2.setFont(new Font("SansSerif",Font.BOLD,Math.max(9,(int)(11*sc))));
            drawShadow(g2,"PLAYER",phx,phy-(int)(3*sc),new Color(0xEE,0xEE,0xEE));
            g2.setFont(new Font("Serif",Font.BOLD,Math.max(16,(int)(22*sc))));
            drawShadow(g2,String.valueOf(playerWins),ppx,portY-(int)(4*sc),PLAYER_CLR);

            // Enemy
            int epx=W-(int)(10*sc)-portW;
            drawPortrait(g2,enemyFrame,epx,portY,portW,portH,ENEMY_CLR,!playerActive);
            int ehx=epx-hpW-(int)(8*sc),ehy=portY+(int)(8*sc);
            drawHPBar(g2,ehx,ehy,hpW,hpH,enemyCharacter,ENEMY_CLR);
            g2.setFont(new Font("SansSerif",Font.PLAIN,Math.max(8,(int)(10*sc))));
            drawShadow(g2,"HP: "+enemyCharacter.getCurrentHP()+" / "+enemyCharacter.getMaxHP(),ehx,ehy+hpH+(int)(10*sc),new Color(0xFF,0xF5,0xDC,190));
            drawNamePill(g2,enemyCharacter.getName(),ehx,ehy+hpH+(int)(14*sc),pillW,pillH,new Color(0x50,0x10,0x10,215),new Color(0xFF,0x77,0x77));
            g2.setFont(new Font("SansSerif",Font.BOLD,Math.max(9,(int)(11*sc))));
            drawShadow(g2,"ENEMY",ehx,ehy-(int)(3*sc),new Color(0xEE,0xEE,0xEE));
            g2.setFont(new Font("Serif",Font.BOLD,Math.max(16,(int)(22*sc))));
            drawShadow(g2,String.valueOf(enemyWins),epx+portW-(int)(24*sc),portY-(int)(4*sc),ENEMY_CLR);

            // Turn indicator
            String tt=playerActive?"▶ Your Turn":"⏳ "+enemyCharacter.getName()+" is acting...";
            g2.setFont(new Font("Serif",Font.BOLD|Font.ITALIC,Math.max(10,(int)(13*sc))));
            FontMetrics fm=g2.getFontMetrics();
            drawShadow(g2,tt,(W-fm.stringWidth(tt))/2,tabH+(int)(22*sc),GOLD);

            g2.dispose();
        }

        private void drawPortrait(Graphics2D g2,Image img,int x,int y,int w,int h,Color accent,boolean active){
            if(active){float a=0.25f+0.15f*(float)Math.sin(glowTick);for(int r=5;r>=1;r--){int sp=r*3;g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),Math.min(255,(int)(a*80/r))));g2.setStroke(new BasicStroke(sp));g2.drawRoundRect(x-sp/2,y-sp/2,w+sp,h+sp,10,10);}}
            g2.setColor(new Color(0x08,0x05,0x02,200));g2.fillRoundRect(x,y,w,h,8,8);
            if(img!=null)g2.drawImage(img,x,y,w,h,null);
            g2.setStroke(new BasicStroke(2));g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),active?220:100));g2.drawRoundRect(x,y,w,h,8,8);
        }
        private void drawHPBar(Graphics2D g2,int x,int y,int w,int h,Character c,Color base){
            double pct=Math.max(0,Math.min(1.0,(double)c.getCurrentHP()/c.getMaxHP()));
            Color bar=pct<=0.25?RED_CRIT:pct<=0.50?ORANGE_LOW:base;
            g2.setColor(new Color(0x08,0x04,0x02,220));g2.fillRoundRect(x,y,w,h,h,h);
            int fw=(int)(w*pct);if(fw>2){g2.setPaint(new GradientPaint(x,y,bar.brighter(),x,y+h,bar.darker()));g2.fillRoundRect(x,y,fw,h,h,h);}
            g2.setStroke(new BasicStroke(1));g2.setColor(new Color(0xFF,0xFF,0xFF,55));g2.drawRoundRect(x,y,w,h,h,h);
        }
        private void drawNamePill(Graphics2D g2,String text,int x,int y,int w,int h,Color bg,Color fg){
            g2.setColor(bg);g2.fillRoundRect(x,y,w,h,h,h);
            g2.setStroke(new BasicStroke(1));g2.setColor(new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),130));g2.drawRoundRect(x,y,w,h,h,h);
            g2.setFont(new Font("Serif",Font.BOLD,Math.max(9,h-6)));
            FontMetrics fm=g2.getFontMetrics();int tx=x+(w-fm.stringWidth(text))/2,ty=y+(h+fm.getAscent()-fm.getDescent())/2;
            g2.setColor(new Color(0,0,0,100));g2.drawString(text,tx+1,ty+1);g2.setColor(fg);g2.drawString(text,tx,ty);
        }
        private void drawShadow(Graphics2D g2,String t,int x,int y,Color c){
            g2.setColor(new Color(0,0,0,150));g2.drawString(t,x+1,y+1);g2.setColor(c);g2.drawString(t,x,y);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  RoundOverlay
    // ══════════════════════════════════════════════════════════
    private class RoundOverlay extends JPanel {
        private float alpha=0f; private Image textImg; private Timer fadeTimer;
        RoundOverlay(){setOpaque(false);}
        void show(int round){
            textImg=loadImage(ROUND_TEXTS[Math.min(round-1,2)]);
            alpha=0f;setVisible(true);repaint();
            if(fadeTimer!=null)fadeTimer.stop();
            final boolean[]in={true};final long[]hold={0};
            fadeTimer=new Timer(16,e->{
                if(in[0]){alpha=Math.min(1f,alpha+0.07f);if(alpha>=1f){in[0]=false;hold[0]=System.currentTimeMillis();}}
                else if(System.currentTimeMillis()-hold[0]<900){}
                else{alpha=Math.max(0f,alpha-0.05f);if(alpha<=0f){((Timer)e.getSource()).stop();setVisible(false);}}
                repaint();
            }); fadeTimer.start();
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);if(alpha<=0f)return;
            int W=getWidth(),H=getHeight();Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.min(1f,alpha*0.5f)));
            g2.setColor(Color.BLACK);g2.fillRect(0,0,W,H);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha));
            if(textImg!=null){int iw=textImg.getWidth(null),ih=textImg.getHeight(null);if(iw>0&&ih>0){double sc=Math.min(W*0.78/iw,H*0.40/ih);int dw=(int)(iw*sc),dh=(int)(ih*sc);g2.drawImage(textImg,(W-dw)/2,(H-dh)/2,dw,dh,null);}}
            g2.dispose();
        }
    }

    // ── Widget factory ────────────────────────────────────────
    private JButton makePillButton(String label,Color bg,Color fg){
        JButton btn=new JButton(label){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean h=getModel().isRollover(),en=isEnabled();
                Color bc=en?(h?bg.brighter():bg):new Color(0x28,0x20,0x18,140);
                g2.setColor(new Color(bc.getRed(),bc.getGreen(),bc.getBlue(),210));g2.fillRoundRect(0,0,getWidth(),getHeight(),getHeight(),getHeight());
                g2.setStroke(new BasicStroke(1.5f));g2.setColor(new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),en?(h?255:180):70));g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,getHeight()-2,getHeight()-2);
                g2.setFont(new Font("Serif",Font.BOLD,Math.max(9,getHeight()-8)));
                FontMetrics fm=g2.getFontMetrics();int tx=(getWidth()-fm.stringWidth(getText()))/2,ty=(getHeight()+fm.getAscent()-fm.getDescent())/2;
                g2.setColor(new Color(0,0,0,90));g2.drawString(getText(),tx+1,ty+1);
                g2.setColor(en?fg:new Color(0x60,0x50,0x40));g2.drawString(getText(),tx,ty);g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);btn.setBorderPainted(false);btn.setFocusPainted(false);btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return btn;
    }

    private Image loadImage(String path){
        if(path==null)return null;URL url=getClass().getResource(path);
        if(url==null){System.err.println("Missing: "+path);return null;}return new ImageIcon(url).getImage();
    }
    private class BgPanel extends JPanel{
        private final Image img;BgPanel(String p){img=loadImage(p);setOpaque(true);setBackground(Color.BLACK);}
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);if(img!=null){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g2.drawImage(img,0,0,getWidth(),getHeight(),null);g2.dispose();}}
    }
}