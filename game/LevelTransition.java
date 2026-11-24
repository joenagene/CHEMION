package game;

import java.awt.*;
import javax.swing.*;

public class LevelTransition extends JPanel {

    private final String levelNumber;
    private final String levelName;
    private final Runnable onComplete;

    private Timer timer;
    private long startTime;
    private boolean soundPlayed;
    private boolean gameplayMusicStarted;

    private static final int SOUND_DELAY = 800;
    private static final int FADE_START = 3500;
    private static final int DURATION = 5500;

    public LevelTransition(int level, String name,
                           Runnable onComplete) {

        this.levelNumber = String.format("LEVEL %02d", level);
        this.levelName = name.toUpperCase();
        this.onComplete = onComplete;

        setOpaque(false);
        setFocusable(true);
    }

    public void start() {
        if (timer != null) {
            timer.stop();
        }

        soundPlayed = false;
        gameplayMusicStarted = false;
        startTime = System.currentTimeMillis();

        timer = new Timer(16, e -> {

            long elapsed =
                System.currentTimeMillis() - startTime;

            if (!soundPlayed && elapsed >= SOUND_DELAY) {
                soundPlayed = true;
                AudioManager.playCinematic();
            }

            if (!gameplayMusicStarted && elapsed >= FADE_START) {
                gameplayMusicStarted = true;
                AudioManager.playGameplayMusic();
            }
            repaint();

            if (elapsed >= DURATION) {
                timer.stop();

                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });

        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }

        AudioManager.stopCinematic();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        long elapsed = startTime == 0
            ? 0
            : System.currentTimeMillis() - startTime;

        // Black overlay fade-out
        float backgroundAlpha = 1f;

        if (elapsed >= FADE_START) {
            backgroundAlpha = 1f -
                (elapsed - FADE_START) /
                (float) (DURATION - FADE_START);
        }

        backgroundAlpha = Math.max(
            0f, Math.min(1f, backgroundAlpha)
        );

        g2.setComposite(
            AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER,
                backgroundAlpha
            )
        );

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Title fade-in and fade-out
        float alpha;

        if (elapsed < 800) {
            alpha = 0f;
        } else if (elapsed < 1800) {
            alpha = (elapsed - 800) / 1000f;
        } else if (elapsed < FADE_START) {
            alpha = 1f;
        } else {
            alpha = 1f -
                (elapsed - FADE_START) /
                (float) (DURATION - FADE_START);
        }

        alpha = Math.max(0f, Math.min(1f, alpha));

        g2.setComposite(
            AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, alpha
            )
        );

        int centerY = getHeight() / 2;

        g2.setFont(new Font("Monospaced", Font.BOLD, 24));
        g2.setColor(new Color(150, 150, 150));
        drawCentered(g2, levelNumber, centerY - 65);

        int titleSize = Math.min(48,
            Math.max(20, getWidth() / 22));

        g2.setFont(new Font("Serif", Font.BOLD, titleSize));
        g2.setColor(new Color(220, 220, 220));
        drawCentered(g2, levelName, centerY);

        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2.setColor(new Color(160, 45, 45));
        drawCentered(g2, "THREAT DETECTED", centerY + 55);

        g2.dispose();
    }

    private void drawCentered(Graphics2D g2,
                              String text, int y) {

        FontMetrics fm = g2.getFontMetrics();

        int x = (getWidth() - fm.stringWidth(text)) / 2;

        g2.drawString(text, x, y);
    }
}