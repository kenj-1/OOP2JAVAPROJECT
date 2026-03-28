package encantadia.ui.frames.battleModeFrames;

import encantadia.ScreenManager;
import encantadia.characters.Character;

import javax.swing.*;

/**
 * PVPBattleFrame
 *
 * Receives player 1's chosen character.
 * Player 2 selection happens inside this frame (second character select).
 */
public class PVPBattleFrame extends JFrame {

    private final Character player1Character;
    private Character player2Character; // set when P2 picks

    public PVPBattleFrame(Character player1Character) {
        this.player1Character = player1Character;

        setTitle("PVP Battle — Player 1: " + player1Character.getName());
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // TODO: prompt Player 2 to pick, then build battle UI
        JLabel placeholder = new JLabel(
                "<html><center><br><br>"
                        + "<b>PVP BATTLE</b><br><br>"
                        + "Player 1: " + player1Character.getName() + "<br>"
                        + "Player 2: (pending selection)"
                        + "</center></html>",
                SwingConstants.CENTER);
        add(placeholder);

        setVisible(true);
        ScreenManager.register(this);
    }
    @Override
    public void dispose() {
        ScreenManager.unregister(this);
        super.dispose();
    }
    public Character getPlayer1Character() { return player1Character; }
    public Character getPlayer2Character() { return player2Character; }
    public void setPlayer2Character(Character c) { this.player2Character = c; }
}