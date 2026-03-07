package encantadia.ui.ux;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainMenuFrame extends JFrame {
    private JPanel MainMenuFramePanel;
    private JButton arcadeButton;
    private JButton PVPButton;
    private JButton PVEButton;


    public MainMenuFrame() {
        setContentPane(MainMenuFramePanel);
        setTitle("Encantadia: Echoes of the Gem - Main Menu");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setVisible(true);
        arcadeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(MainMenuFrame.this, "Hello all my niggas say what");
            }
        });
    }
}
