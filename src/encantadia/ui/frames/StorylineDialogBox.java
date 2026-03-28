package encantadia.ui.frames;

import encantadia.ScreenManager;
import encantadia.characters.Character;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StorylineDialogBox extends JFrame {

    private JLabel playerPortrait;
    private JLabel enemyPortrait;

    private JTextPane storyBox;

    private JButton skipButton;
    private JButton actionButton;

    private Character player;
    private Character enemy;

    private Runnable onFinish;

    private boolean enemyStoryStarted = false;
    private volatile boolean skipAnimation = false;

    public StorylineDialogBox(Character player, Character enemy, Runnable onFinish){

        this.player = player;
        this.enemy = enemy;
        this.onFinish = onFinish;

        setTitle("Encantadia Story");
        setSize(1024,768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        BackgroundPanel root = new BackgroundPanel("src/assets/story_background.png");
        root.setLayout(new BorderLayout());
        setContentPane(root);

        JPanel portraits = new JPanel(new BorderLayout());
        portraits.setOpaque(false);
        portraits.setBorder(new EmptyBorder(40,80,20,80));

        playerPortrait = new JLabel(loadCharacterImage(player));
        enemyPortrait = new JLabel(loadCharacterImage(enemy));

        playerPortrait.setHorizontalAlignment(SwingConstants.CENTER);
        enemyPortrait.setHorizontalAlignment(SwingConstants.CENTER);

        portraits.add(playerPortrait, BorderLayout.WEST);
        portraits.add(enemyPortrait, BorderLayout.EAST);

        root.add(portraits, BorderLayout.CENTER);

        storyBox = new JTextPane();
        storyBox.setContentType("text/html");
        storyBox.setEditable(false);
        storyBox.setOpaque(false);
        storyBox.setFont(new Font("Serif",Font.PLAIN,18));

        JScrollPane scroll = new JScrollPane(storyBox);
        scroll.setBorder(new EmptyBorder(10,80,10,80));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        root.add(scroll, BorderLayout.SOUTH);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);

        skipButton = new JButton("Skip");
        actionButton = new JButton("Reveal Enemy");
        actionButton.setEnabled(false);

        buttons.add(skipButton);
        buttons.add(actionButton);

        root.add(buttons, BorderLayout.NORTH);

        skipButton.addActionListener(e -> skipAnimation = true);

        actionButton.addActionListener(e -> {

            if(!enemyStoryStarted){

                enemyStoryStarted = true;
                storyBox.setText("");

                actionButton.setEnabled(false);
                startEnemyStory();

            }else{

                dispose();

                if(onFinish != null){
                    onFinish.run();
                }
            }
        });

        setVisible(true);
        ScreenManager.register(this);
        startPlayerStory();
    }
    @Override
    public void dispose() {
        ScreenManager.unregister(this);
        super.dispose();
    }

    private ImageIcon loadCharacterImage(Character c){

        ImageIcon icon = new ImageIcon(
                "src/assets/" + c.getName().toLowerCase() + ".png"
        );

        Image img = icon.getImage().getScaledInstance(250,250,Image.SCALE_SMOOTH);

        return new ImageIcon(img);
    }

    private void typeText(String text) throws InterruptedException{

        if(skipAnimation){
            SwingUtilities.invokeLater(() -> storyBox.setText(text));
            return;
        }

        StringBuilder builder = new StringBuilder();

        for(char c : text.toCharArray()){

            if(skipAnimation){
                SwingUtilities.invokeLater(() -> storyBox.setText(text));
                return;
            }

            builder.append(c);
            String current = builder.toString();

            SwingUtilities.invokeLater(() ->
                    storyBox.setText("<html><div style='width:700px'>" + current + "</div></html>")
            );

            Thread.sleep(18);
        }
    }

    private void startPlayerStory(){

        new Thread(() -> {

            try{

                String story =
                        "<h2>" + player.getName() + "</h2>" +
                                "<h4>" + player.getTitle() + "</h4>" +
                                "<p>" + player.getBackstory() + "</p>";

                typeText(story);

                SwingUtilities.invokeLater(() ->
                        actionButton.setEnabled(true)
                );

            }catch(Exception ignored){}

        }).start();
    }

    private void startEnemyStory(){

        new Thread(() -> {

            try{

                String story =
                        "<h2>Enemy Appears</h2>" +
                                "<h3>" + enemy.getName() + "</h3>" +
                                "<h4>" + enemy.getTitle() + "</h4>" +
                                "<p>" + enemy.getBackstory() + "</p>";

                typeText(story);

                SwingUtilities.invokeLater(() -> {

                    actionButton.setText("Begin Battle");
                    actionButton.setEnabled(true);

                });

            }catch(Exception ignored){}

        }).start();
    }

    private static class BackgroundPanel extends JPanel{

        private Image background;

        public BackgroundPanel(String path){
            background = new ImageIcon(path).getImage();
        }

        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            g.drawImage(background,0,0,getWidth(),getHeight(),this);
        }
    }
}