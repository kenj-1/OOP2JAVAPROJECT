package encantadia.ui.frames.battleModeFrames;

import encantadia.ScreenManager;
import encantadia.characters.Character;

import javax.swing.*;

/**
 * ArcadeModeBattleFrame
 *
 * Receives the player's chosen character.
 * Arcade mode queues opponents one-by-one; this frame manages the queue.
 */
public class ArcadeModeBattleFrame extends JFrame {

    private final Character playerCharacter;
    private int roundNumber = 1;

    public ArcadeModeBattleFrame(Character playerCharacter) {
        this.playerCharacter = playerCharacter;

        setTitle("Arcade — " + playerCharacter.getName() + " | Round " + roundNumber);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // TODO: build arcade opponent queue and battle UI
        JLabel placeholder = new JLabel(
                "<html><center><br><br>"
                        + "<b>ARCADE MODE</b><br><br>"
                        + "Player: " + playerCharacter.getName() + "<br>"
                        + "Round:  " + roundNumber
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

    public Character getPlayerCharacter() { return playerCharacter; }
    public int getRoundNumber()           { return roundNumber; }
    public void nextRound()               { roundNumber++; }
}