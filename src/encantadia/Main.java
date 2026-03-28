package encantadia;

import encantadia.ui.frames.WelcomeScreenPage;
import javax.swing.*;

public class Main {
    public static void main(String[] x) {
        ScreenManager.init();
        SwingUtilities.invokeLater(() -> {
            new WelcomeScreenPage();
            System.out.println("[ScreenManager] F11 = toggle fullscreen");
        });
    }
}