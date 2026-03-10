package encantadia.ui.ux;


import encantadia.characters.*;
import encantadia.characters.Character;


import javax.swing.*;
import java.awt.*;        // ← add this

public class CharacterSelectionFrame extends JFrame {

    private JPanel characterSelectionFrame;
    private JButton button1;
    private JButton button3;
    private JButton button4;
    private JButton button2;
    private JButton button5;
    private JButton button6;
    private JButton button7;
    private JButton button8;
    private JLabel labelDirk;
    private JLabel labelMary;
    private JLabel labelMakelanShere;
    private JLabel labelTyrone;
    private JLabel labelDea;
    private JLabel labelFlamara;
    private JLabel labelTera;
    private JLabel labelAdamus;


    public CharacterSelectionFrame(){
        setContentPane(characterSelectionFrame);
        setTitle("Encantadia: Echoes of the Gem - Select your Fighter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);


        ImageIcon icon = new ImageIcon("src/assets/Portrait_Placeholder.png");

        Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);

        labelDirk.setIcon(new ImageIcon(img));
        labelMary.setIcon(new ImageIcon(img));
        labelMakelanShere.setIcon(new ImageIcon(img));
        labelTyrone.setIcon(new ImageIcon(img));
        labelDea.setIcon(new ImageIcon(img));
        labelFlamara.setIcon(new ImageIcon(img));
        labelTera.setIcon(new ImageIcon(img));
        labelAdamus.setIcon(new ImageIcon(img));


        button5.addActionListener(e -> {
            Character selectedCharacter = new Dea();
            showCharacterBackstory(selectedCharacter);
        });



        setVisible(true);
    }

    private void showCharacterBackstory(Character character) {

        String message =
                character.getName() + "\n" +
                        character.getTitle() + "\n\n" +
                        character.getBackstory();

        int choice = JOptionPane.showConfirmDialog(
                this,
                message,
                "Chosen Fighter",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if(choice == JOptionPane.OK_OPTION){

            new BattleFrame(character);

            dispose();
        }
    }
    private void createUIComponents() {
        // TODO: place custom component creation code here
    }


}
