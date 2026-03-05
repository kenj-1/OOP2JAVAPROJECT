package encantadia.ui.ux;

import javax.swing.*;
import java.awt.*;

public class MainMenuFrame {

    public void showMainMenu() {

        JFrame frame = new JFrame("Main Menu");

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(3, 1, 10, 10));

        JButton start = new JButton("Start Game");
        JButton settings = new JButton("Settings");
        JButton exit = new JButton("Exit");

        menuPanel.add(start);
        menuPanel.add(settings);
        menuPanel.add(exit);

        panel.add(menuPanel);

        frame.add(panel);
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}