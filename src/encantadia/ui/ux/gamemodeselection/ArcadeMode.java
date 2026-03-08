package encantadia.ui.ux.gamemodeselection;

import encantadia.ui.ux.MainMenuFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ArcadeMode extends JFrame {

    private JPanel arcadeModePanel;
    private JButton backToMainMenuButton;
    private JLabel arcadeLabel;
    private JButton noButton;
    private JButton yesButton;

    public ArcadeMode() {

        setContentPane(arcadeModePanel);
        setTitle("Encantadia: Echoes of the Gem - Arcade Mode");
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Back to Main Menu
        backToMainMenuButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new MainMenuFrame();
                dispose();
            }
        });

        // YES button
        yesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(ArcadeMode.this,
                        "Arcade Mode Starting...");
            }
        });

        // NO button
        noButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(ArcadeMode.this,
                        "Arcade Mode Cancelled.");
            }
        });

        setVisible(true);
    }
}