package encantadia.ui.ux;

import encantadia.characters.Character;

import javax.swing.*;

public class BattleFrame extends JFrame {

    private JButton skill1Button;
    private JButton skill2Button;
    private JButton skill3Button;

    private JLabel enemyIcon;
    private JLabel playerIcon;

    private JLabel playerName;
    private JLabel enemyName;

    private JLabel playerHP;
    private JLabel enemyHP;

    private JLabel bestOf3RoundsCounter;
    private JLabel roundTitle;

    private JPanel battleFramePanel;

    private Character playerCharacter;
    private Character enemyCharacter;

    public BattleFrame(Character player){

        this.playerCharacter = player;

        // Temporary enemy until enemy selection or AI is added
        this.enemyCharacter = player; // placeholder (replace later)

        setContentPane(battleFramePanel);
        setTitle("Encantadia: Echoes of the Gem - Battle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);

        initializeBattleUI();

        setVisible(true);
    }

    private void initializeBattleUI(){

        // Player name
        playerName.setText(playerCharacter.getName());

        // Enemy name
        enemyName.setText(enemyCharacter.getName());

        // Round title
        roundTitle.setText("Battle Start!");

        // HP display
        playerHP.setText(playerCharacter.getCurrentHP() + " / " + playerCharacter.getMaxHP());
        enemyHP.setText(enemyCharacter.getCurrentHP() + " / " + enemyCharacter.getMaxHP());

        // Skill button names
        skill1Button.setText(playerCharacter.getSkills().get(0).getName());
        skill2Button.setText(playerCharacter.getSkills().get(1).getName());
        skill3Button.setText(playerCharacter.getSkills().get(2).getName());

        // Skill actions
        skill1Button.addActionListener(e -> useSkill(0));
        skill2Button.addActionListener(e -> useSkill(1));
        skill3Button.addActionListener(e -> useSkill(2));
    }

    private void useSkill(int skillIndex){

        int damage = playerCharacter.getSkills().get(skillIndex).rollValue();

        enemyCharacter.takeDamage(damage);

        roundTitle.setText(
                playerCharacter.getName() +
                        " used " +
                        playerCharacter.getSkills().get(skillIndex).getName() +
                        "!"
        );

        updateHealthBars();

        if(!enemyCharacter.isAlive()){
            JOptionPane.showMessageDialog(this,
                    playerCharacter.getName() + " Wins!");
        }
    }

    private void updateHealthBars(){

        playerHP.setText(
                playerCharacter.getCurrentHP() +
                        " / " +
                        playerCharacter.getMaxHP()
        );

        enemyHP.setText(
                enemyCharacter.getCurrentHP() +
                        " / " +
                        enemyCharacter.getMaxHP()
        );
    }
}