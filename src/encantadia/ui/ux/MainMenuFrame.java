package encantadia.ui.ux;

import encantadia.ui.ux.gamemodeselection.ArcadeMode;
import encantadia.ui.ux.gamemodeselection.PVEMode;
import encantadia.ui.ux.gamemodeselection.PVPMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainMenuFrame extends JFrame {
    private JPanel MainMenuFramePanel;
    private JButton arcadeButton;
    private JButton PVPButton;
    private JButton PVEButton;
    private JButton exitGameButton;


    public MainMenuFrame() {
        setContentPane(MainMenuFramePanel);
        setTitle("Encantadia: Echoes of the Gem - Main Menu");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);



        // Button Sides
        arcadeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Arcade clicked");
                new ArcadeMode();
                dispose();
            }
        });


        PVPButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                new PVPMode();
                dispose();
            }
        });


        PVEButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                new PVEMode();
                dispose();
            }
        });


        exitGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });



        setVisible(true);
    }

}
