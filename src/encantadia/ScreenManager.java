package encantadia;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ScreenManager {

    private static final List<JFrame> frames = new ArrayList<>();
    private static boolean fullscreen = false;

    private ScreenManager() {}

    public static void register(JFrame frame) {
        frames.add(frame);

        // ✅ If already fullscreen when this frame opens, apply it immediately
        if (fullscreen) {
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    toggle();
                }
            }
        });

        frame.setFocusable(true);
    }

    public static void unregister(JFrame frame) {
        frames.remove(frame);
    }

    public static void toggle() {
        fullscreen = !fullscreen;
        for (JFrame frame : new ArrayList<>(frames)) {
            if (frame.isDisplayable()) {
                apply(frame);
            }
        }
    }

    private static void apply(JFrame frame) {
        if (fullscreen) {
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            frame.setExtendedState(JFrame.NORMAL);
            frame.setSize(1024, 768);
            frame.setLocationRelativeTo(null);
        }
        frame.revalidate();
        frame.repaint();
    }

    public static void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    public static boolean isFullscreen() {
        return fullscreen;
    }
}