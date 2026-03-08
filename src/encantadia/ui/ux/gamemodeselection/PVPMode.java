package encantadia.ui.ux.gamemodeselection;

import javax.swing.*;
import java.awt.*;

public class PVPMode extends JFrame {

    private JPanel pvpModePanel;
    private JButton button1;
    private JButton button2;
    private JButton button3;


    public PVPMode() {


        setContentPane(pvpModePanel);
        setTitle("Encantadia: Echoes of the Gem - PVP Mode");
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);





        setVisible(true);
    }
}