package encantadia.ui.frames;

import encantadia.ScreenManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;

public class ExitConfirmDialog extends JFrame {

    private static final String BG_PATH       = "/welcomeScreen_JAVA.png";
    private static final String HOLDER_PATH   = "/optionsHolder.png";
    private static final String BANNER_PATH   = "/exitButton (1).png";
    private static final String QUESTION_PATH = "/exitGame.png";
    private static final String CANCEL_PATH   = "/cancelButton.png";
    private static final String EXITGAME_PATH = "/exitButton1.png";
    private static final String TITLE_PATH    = "/gameTitle.png";

    private ImagePanel holderPanel;
    private ImagePanel bannerPanel;
    private ImagePanel questionPanel;
    private FloatingImagePanel titlePanel;
    private JButton cancelButton;
    private JButton exitGameButton;
    private JPanel btnRow;

    private float time = 0f;

    public ExitConfirmDialog() {
        setTitle("Encantadia — Exit");
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);

        JLayeredPane lp = new JLayeredPane();
        lp.setLayout(null);
        setContentPane(lp);

        // Background
        ImagePanel bg = new ImagePanel(BG_PATH);
        lp.add(bg, JLayeredPane.DEFAULT_LAYER);

        // Title
        titlePanel = new FloatingImagePanel(TITLE_PATH);
        lp.add(titlePanel, JLayeredPane.PALETTE_LAYER);

        // Holder
        holderPanel = new ImagePanel(HOLDER_PATH);
        lp.add(holderPanel, JLayeredPane.PALETTE_LAYER);

        // Banner
        bannerPanel = new ImagePanel(BANNER_PATH);
        lp.add(bannerPanel, JLayeredPane.MODAL_LAYER);

        // Question
        questionPanel = new ImagePanel(QUESTION_PATH);
        lp.add(questionPanel, JLayeredPane.MODAL_LAYER);

        // Buttons
        cancelButton   = makeImgButton(CANCEL_PATH);
        exitGameButton = makeImgButton(EXITGAME_PATH);

        btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(cancelButton);
        btnRow.add(exitGameButton);
        lp.add(btnRow, JLayeredPane.POPUP_LAYER);

        // Actions
        cancelButton.addActionListener(e -> {
            dispose();
            new WelcomeScreenPage();
        });

        exitGameButton.addActionListener(e -> System.exit(0));

        // Resize listener
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                reposition(lp, bg);
            }
        });

        // Animation Timer
        new Timer(16, e -> {
            time += 0.016f;
            repaint();
        }).start();

        if (ScreenManager.isFullscreen()) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        setVisible(true);
        ScreenManager.register(this);
        SwingUtilities.invokeLater(() -> reposition(lp, bg));
    }

    @Override
    public void dispose() {
        ScreenManager.unregister(this);
        super.dispose();
    }

    private void reposition(JLayeredPane pane, ImagePanel bg) {
        int W = pane.getWidth();
        int H = pane.getHeight();
        if (W == 0 || H == 0) return;

        bg.setBounds(0, 0, W, H);

        double scale = Math.min(W / 1024.0, H / 768.0);
        scale = Math.min(scale, 1.4);

        // Title position
        int titleW = (int)(500 * scale);
        int titleH = (int)(titleW * 0.30);
        int titleY = (int)(40 * scale);
        int titleX = (W - titleW) / 2;
        titlePanel.setBounds(titleX, titleY, titleW, titleH);

        // Holder
        int holderW = (int)(600 * scale);
        int holderH = (int)(holderW / 2.0);
        int holderX = (W - holderW) / 2;
        int holderY = (H - holderH) / 2 ;
        holderPanel.setBounds(holderX, holderY, holderW, holderH);

        // Banner
        int bannerW = (int)(200 * scale);
        int bannerH = (int)(bannerW / 2.5);
        int bannerX = holderX + (holderW - bannerW) / 2;
        int bannerY = holderY - (bannerH / 3);
        bannerPanel.setBounds(bannerX, bannerY, bannerW, bannerH);

        // Question
        int qW = (int)(370 * scale);
        int qH = (int)(qW / 3.0);
        int qY = holderY + (int)(holderH * 0.20);
        int qX = holderX + (holderW - qW) / 2;
        questionPanel.setBounds(qX, qY, qW, qH);

        // Buttons
        int btnH = (int)(60 * scale);
        int cancelW = (int)(btnH * 3.0);
        int exitGW  = (int)(btnH * 4.5);
        int gap = (int)(20 * scale);

        setFull(cancelButton, cancelW, btnH);
        setFull(exitGameButton, exitGW, btnH);

        int rowW = cancelW + exitGW + gap;
        int rowX = holderX + (holderW - rowW) / 2;
        int rowY = holderY + (int)(holderH * 0.65);

        btnRow.removeAll();
        btnRow.add(cancelButton);
        btnRow.add(Box.createHorizontalStrut(gap));
        btnRow.add(exitGameButton);
        btnRow.setBounds(rowX, rowY, rowW, btnH + 4);

        pane.setLayer(titlePanel,   JLayeredPane.PALETTE_LAYER);
        pane.setLayer(holderPanel,  JLayeredPane.PALETTE_LAYER);
        pane.setLayer(bannerPanel,  JLayeredPane.MODAL_LAYER);
        pane.setLayer(questionPanel,JLayeredPane.MODAL_LAYER);
        pane.setLayer(btnRow,       JLayeredPane.POPUP_LAYER);

        pane.revalidate();
        pane.repaint();
    }

    private static void setFull(JButton b, int w, int h) {
        Dimension d = new Dimension(w, h);
        b.setPreferredSize(d);
        b.setMinimumSize(d);
        b.setMaximumSize(d);
    }

    // FLOATING TITLE PANEL
    private class FloatingImagePanel extends JPanel {
        private final Image img;

        FloatingImagePanel(String path) {
            img = loadImage(path);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (img != null) {
                Graphics2D g2 = (Graphics2D) g.create();

                float floatY = (float)Math.sin(time * 1.2) * 10f;

                g2.drawImage(img,
                        0,
                        (int)floatY,
                        getWidth(),
                        getHeight(),
                        null);

                g2.dispose();
            }
        }
    }

    private JButton makeImgButton(String path) {
        Image img = loadImage(path);
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                if (img == null) { super.paintComponent(g); return; }
                Graphics2D g2 = (Graphics2D) g.create();
                int iw = img.getWidth(null), ih = img.getHeight(null);
                double s = Math.min((double)getWidth()/iw, (double)getHeight()/ih);
                int dw = (int)(iw*s), dh = (int)(ih*s);
                int x  = (getWidth()-dw)/2, y = (getHeight()-dh)/2;
                g2.drawImage(img, x, y, dw, dh, null);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        if (url == null) { System.err.println("Missing: " + path); return null; }
        return new ImageIcon(url).getImage();
    }

    private class ImagePanel extends JPanel {
        private final Image img;
        ImagePanel(String path) { img = loadImage(path); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img == null) return;
            g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        }
    }
}
