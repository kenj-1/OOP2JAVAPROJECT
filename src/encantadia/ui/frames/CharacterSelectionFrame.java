package encantadia.ui.frames;

import encantadia.BackstoryShowcase;
import encantadia.ScreenManager;
import encantadia.battle.EnemyFactory;
import encantadia.characters.*;
import encantadia.characters.Character;
import encantadia.gamemode.GameModeType;
import encantadia.story.CharacterStories;
import encantadia.gamemode.ArcadeMode;
import encantadia.gamemode.GameModeType;
import encantadia.gamemode.PVEMode;
import encantadia.gamemode.PVPMode;
import encantadia.ui.frames.battleModeFrames.ArcadeModeBattleFrame;
import encantadia.ui.frames.battleModeFrames.PVEBattleFrame;
import encantadia.ui.frames.battleModeFrames.PVPBattleFrame;

import javax.swing.*;
import java.awt.*;

/**
 * CharacterSelectionFrame
 *
 * Flow after a character is picked:
 *
 *   PVE  →  player backstory  →  enemy backstory  →  PVEBattleFrame
 *   PVP  →  player backstory  →  PVPBattleFrame
 *   ARCADE → player backstory → ArcadeModeBattleFrame
 *
 * The gameModeType is received from MainMenuFrame / PVEMode / etc. and is
 * never null — defaults to PVE if the no-arg constructor is used.
 */
public class CharacterSelectionFrame extends JFrame {

    private final GameModeType gameModeType;

    // ── IntelliJ form fields (bound by $$$setupUI$$$) ─────────────────
    private JPanel  characterSelectionFrame;
    private JButton selectDirk;
    private JButton selectMary;
    private JButton selectMakelanShere;
    private JButton selectTyrone;
    private JButton selectAdamus;
    private JButton selectTera;
    private JButton selectFlamara;
    private JButton selectDea;
    private JLabel  labelDirk;
    private JLabel  labelMary;
    private JLabel  labelMakelanShere;
    private JLabel  labelTyrone;
    private JLabel  labelDea;
    private JLabel  labelFlamara;
    private JLabel  labelTera;
    private JLabel  labelAdamus;
    private JLabel  dirkCharacter;
    private JLabel  maryCharacter;
    private JLabel  makelanShereCharacter;
    private JLabel  tyroneCharacter;
    private JLabel  deaCharacter;
    private JLabel  flamaraCharacter;
    private JLabel  teraCharacter;
    private JLabel  adamusCharacter;

    // ── Constructors ──────────────────────────────────────────────────

    public CharacterSelectionFrame(GameModeType gameModeType) {
        this.gameModeType = gameModeType;
        init();
    }

    public CharacterSelectionFrame() {
        this(GameModeType.PVE);
    }

    // ── Init ──────────────────────────────────────────────────────────
    private void init() {
        setContentPane(characterSelectionFrame);
        setTitle("Select Your Fighter  [" + gameModeType.name() + "]");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);

        // Portrait placeholder
        ImageIcon icon = new ImageIcon("src/assets/Portrait_Placeholder.png");
        Image img = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        ImageIcon portrait = new ImageIcon(img);

        // Character cards
        dirkCharacter.setText(        card("Dirk",         "src/assets/dirk.png",      "5000 HP", "Reduces enemy damage 65%"));
        maryCharacter.setText(        card("Claire",       "src/assets/mary.png",      "5000 HP", "50% chance to steal a turn"));
        makelanShereCharacter.setText(card("Makelan Shere","src/assets/makelan.png",   "5000 HP", "Recoil + self-debuff"));
        tyroneCharacter.setText(      card("Tyrone",       "src/assets/tyrone.png",    "5000 HP", "+250 bonus damage"));
        deaCharacter.setText(         card("Dea",          "src/assets/dea.png",       "5000 HP", "45% extend enemy CDs"));
        flamaraCharacter.setText(     card("Flamara",      "src/assets/flamara.png",   "5000 HP", "+300 bonus damage"));
        teraCharacter.setText(        card("Tera",         "src/assets/tera.png",      "5000 HP", "Heals 300 HP"));
        adamusCharacter.setText(      card("Adamus",       "src/assets/adamus.png",    "5000 HP", "Resets all cooldowns"));

        // Button labels
        selectDirk.setText("Select Dirk");
        selectMary.setText("Select Claire");
        selectMakelanShere.setText("Select Makelan");
        selectTyrone.setText("Select Tyrone");
        selectDea.setText("Select Dea");
        selectFlamara.setText("Select Flamara");
        selectTera.setText("Select Tera");
        selectAdamus.setText("Select Adamus");

        // Portraits
        for (JLabel lbl : new JLabel[]{
                labelDirk, labelMary, labelMakelanShere, labelTyrone,
                labelDea, labelFlamara, labelTera, labelAdamus}) {
            lbl.setIcon(portrait);
        }

        // ── Listeners — ALL route through onCharacterSelected ──────────
        // This is the ONLY place where action listeners are set.
        // onCharacterSelected() always shows the character backstory first.
        selectDirk.addActionListener(        e -> onCharacterSelected(new Dirk()));
        selectMary.addActionListener(        e -> onCharacterSelected(new Mary()));
        selectMakelanShere.addActionListener(e -> onCharacterSelected(new MakelanShere()));
        selectTyrone.addActionListener(      e -> onCharacterSelected(new Tyrone()));
        selectAdamus.addActionListener(      e -> onCharacterSelected(new Adamus()));
        selectTera.addActionListener(        e -> onCharacterSelected(new Tera()));
        selectFlamara.addActionListener(     e -> onCharacterSelected(new Flamara()));
        selectDea.addActionListener(         e -> onCharacterSelected(new Dea()));

        setVisible(true);
        ScreenManager.register(this);
    }
    @Override
    public void dispose() {
        ScreenManager.unregister(this);
        super.dispose();
    }


    // ══════════════════════════════════════════════════════════════════
    //  Central dispatch
    //
    //  Every character button routes here.  This is where the backstory
    //  chain starts — nothing goes directly to a battle frame.
    // ══════════════════════════════════════════════════════════════════
    private void onCharacterSelected(Character character) {
        dispose();   // close character selection immediately

        switch (gameModeType) {
            case PVE:    startPVEFlow(character);    break;
            case PVP:    startPVPFlow(character);    break;
            case ARCADE: startArcadeFlow(character); break;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PVE:
    //    1. Player character backstory  (BackstoryShowcase)
    //    2. Enemy reveal backstory      (BackstoryShowcase)
    //    3. PVEBattleFrame(player, enemy)
    // ══════════════════════════════════════════════════════════════════
    private void startPVEFlow(Character character) {
        Character enemy = EnemyFactory.getRandomEnemy(character);

        // Step 3 — battle (created last, called last)
        Runnable launchBattle = () ->
                new PVEBattleFrame(character, enemy);

        // Step 2 — enemy backstory, then battle
        Runnable showEnemyStory = () ->
                new BackstoryShowcase(
                        CharacterStories.getEnemyStory(enemy),
                        CharacterStories.getEnemyTitle(enemy),
                        launchBattle);

        // Step 1 — character backstory, then enemy story (opens immediately)
        new BackstoryShowcase(
                CharacterStories.getCharacterStory(character),
                CharacterStories.getCharacterTitle(character),
                showEnemyStory);
    }

    // ══════════════════════════════════════════════════════════════════
    //  PVP:
    //    1. Player character backstory
    //    2. PVPBattleFrame(player)
    // ══════════════════════════════════════════════════════════════════
    private void startPVPFlow(Character character) {
        Runnable launchBattle = () -> new PVPBattleFrame(character);

        new BackstoryShowcase(
                CharacterStories.getCharacterStory(character),
                CharacterStories.getCharacterTitle(character),
                launchBattle);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Arcade:
    //    1. Player character backstory
    //    2. ArcadeModeBattleFrame(player)
    // ══════════════════════════════════════════════════════════════════
    private void startArcadeFlow(Character character) {
        Runnable launchBattle = () -> new ArcadeModeBattleFrame(character);

        new BackstoryShowcase(
                CharacterStories.getCharacterStory(character),
                CharacterStories.getCharacterTitle(character),
                launchBattle);
    }

    // ── HTML card ─────────────────────────────────────────────────────
    private String card(String name, String imgPath, String hp, String ult) {
        return "<html><div style='text-align:center;width:170px;'>"
                + "<h3>" + name + "</h3>"
                + "<img src='file:" + imgPath + "' width='110' height='110'><br>"
                + "<b>HP:</b> " + hp + "<br>"
                + "<b>Ultimate:</b> " + ult
                + "</div></html>";
    }

    private void createUIComponents() { /* IntelliJ form stub */ }
}