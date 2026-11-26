import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class OutbreakEnding extends JPanel {

    private final Runnable onFinish;
    private final Timer timer;
    private final String scientist;

    private int scene = 0;
    private int line = 0;
    private int typed = 0;
    private int tick = 0;
    private int animationTick = 0;
    private boolean finished = false;

    private final String[] scientists = {
        "Dr. Sebastian",
        "Dr. Dutch",
        "Dr. Nicole",
        "Dr. Gene",
        "Dr. Elena"
    };

    private final String[] dialogue = {
        "It's over... The original strain has finally been neutralized.",
        "Wait... These records weren't included in the Genesis files.",
        "The Genesis Project wasn't an attempt to develop a cure.",
        "They were deliberately engineering human mutations.",
        "Multiple research facilities... Hundreds of test subjects...",
        "If these records are correct, then Patient Zero wasn't the first.",
        "He was just the first to escape.",
        "You've uncovered more than you were supposed to.",
        "Who are you? What do you--"
    };

    private final String[] secretDialogue = {
        "Genesis has fallen.",
        "And the scientist?",
        "They found the records.",
        "Then we proceed with the next phase."
    };

    public OutbreakEnding(int scientistIndex, Runnable onFinish) {

        this.onFinish = onFinish;

        scientist = scientists[
            Math.max(0, Math.min(scientistIndex, scientists.length - 1))
        ];

        setBackground(Color.BLACK);
        setFocusable(true);

        timer = new Timer(25, e -> updateScene());

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            "continue"
        );

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
            "continue"
        );

        getActionMap().put("continue", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                advance();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                advance();
            }
        });
    }

    public void start() {
        scene = 0;
        line = 0;
        typed = 0;
        tick = 0;
        animationTick = 0;
        finished = false;
        timer.start();
        requestFocusInWindow();
    }

    public void stop() {
        timer.stop();
    }

    private void changeScene(int next) {
        scene = next;
        tick = 0;
        line = 0;
        typed = 0;
    }

    private void updateScene() {

        tick++;
        animationTick++;

        if (scene == 0) {

            if (tick % 2 == 0 &&
                typed < dialogue[line].length()) {
                typed++;
            }

            if (line == dialogue.length - 1 &&
                typed == dialogue[line].length() &&
                tick > dialogue[line].length() * 2 + 30) {

                changeScene(1);
            }

        } else if (scene == 1) {

            if (tick >= 120) {
                changeScene(2);
            }

        } else if (scene == 2) {

            if (tick > 100 && tick % 2 == 0 &&
                typed < secretDialogue[line].length()) {
                typed++;
            }

        } else if (scene == 3) {

            if (tick >= 150) {
                changeScene(4);
            }

        } else if (scene == 4) {

            if (tick >= 100) {
                changeScene(5);
            }

        } else if (scene == 5) {

            if (tick >= 320) {
                finish();
            }
        }

        repaint();
    }

    private void advance() {

        if (finished) return;

        if (scene == 0) {

            if (typed < dialogue[line].length()) {
                typed = dialogue[line].length();
                return;
            }

            if (line < dialogue.length - 1) {
                line++;
                typed = 0;
                tick = 0;
            } else {
                changeScene(1);
            }

        } else if (scene == 2 && tick > 100) {

            if (typed < secretDialogue[line].length()) {
                typed = secretDialogue[line].length();
                return;
            }

            if (line < secretDialogue.length - 1) {
                line++;
                typed = 0;
            } else {
                changeScene(3);
            }

        } else if (scene == 5 && tick > 100) {
            finish();
        }

        repaint();
    }

    private void finish() {

        if (finished) return;

        finished = true;
        stop();

        if (onFinish != null) {
            SwingUtilities.invokeLater(onFinish);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.scale(
            getWidth() / 1100.0,
            getHeight() / 620.0
        );

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, 1100, 620);

        switch (scene) {

            case 0:
                drawGenesisTerminal(g2);

                drawDialogue(
                    g2,
                    line == 7 ? "UNKNOWN VOICE" : scientist,
                    dialogue[line],
                    typed
                );
                break;

            case 1:
                break;

            case 2:
                drawUnknownLaboratory(g2, false);

                if (tick > 100) {

                    drawDialogue(
                        g2,
                        line % 2 == 0 ? "UNKNOWN 01" : "UNKNOWN 02",
                        secretDialogue[line],
                        typed
                    );
                }

                drawFade(g2, Math.max(0, 255 - tick * 3));
                break;

            case 3:
                drawUnknownLaboratory(g2, true);

                if (tick > 90 && tick % 12 < 5) {
                    drawFade(g2, 210);
                }
                break;

            case 4:
                break;

            case 5:
                drawFinalTitle(g2);
                break;
        }

        g2.dispose();
    }

    private void drawGenesisTerminal(Graphics2D g) {

        g.setPaint(new GradientPaint(
            0, 0, new Color(8, 12, 20),
            0, 620, new Color(25, 12, 35)
        ));

        g.fillRect(0, 0, 1100, 620);

        g.setColor(new Color(20, 24, 35));

        for (int x = 0; x < 1100; x += 100) {
            g.drawLine(x, 0, x, 620);
        }

        g.setColor(new Color(35, 38, 48));
        g.fillRect(100, 370, 900, 65);

        g.setColor(new Color(10, 14, 22));
        g.fillRoundRect(300, 55, 500, 300, 12, 12);

        g.setColor(new Color(65, 110, 105));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(300, 55, 500, 300, 12, 12);

        g.setColor(new Color(5, 30, 27));
        g.fillRect(320, 75, 460, 260);

        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(new Color(100, 255, 190));

        g.drawString("GENESIS INTERNAL DATABASE", 340, 110);

        g.setColor(new Color(220, 65, 75));
        g.drawString("CLASSIFIED - LEVEL 05", 340, 145);

        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(new Color(80, 190, 150));

        g.drawString("PROJECT STATUS: TERMINATED", 340, 180);

        if (line >= 1) {
            g.drawString("ACCESSING RESTRICTED FILES...", 340, 210);
        }

        if (line >= 3) {
            g.drawString("HUMAN MUTATION PROGRAM", 340, 240);
        }

        if (line >= 4) {
            g.drawString("EXTERNAL FACILITIES: DETECTED", 340, 270);
        }

        if (line >= 5) {
            g.setColor(new Color(255, 70, 80));
            g.drawString("ORIGIN SUBJECT: NOT THE FIRST", 340, 300);
        }

        if (animationTick % 30 < 15) {
            g.fillRect(340, 315, 12, 4);
        }

        for (int y = 75; y < 335; y += 5) {
            g.setColor(new Color(0, 0, 0, 35));
            g.drawLine(320, y, 780, y);
        }

        if (line >= 7) {

            g.setColor(new Color(150, 0, 0, 35));
            g.fillRect(0, 0, 1100, 620);

            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(Color.RED);
            g.drawString("UNAUTHORIZED CONNECTION", 20, 30);
        }
    }

    private void drawUnknownLaboratory(
        Graphics2D g, boolean specimenMoves
    ) {

        g.setPaint(new GradientPaint(
            0, 0, new Color(4, 9, 14),
            0, 620, new Color(16, 30, 32)
        ));

        g.fillRect(0, 0, 1100, 620);

        g.setColor(new Color(23, 32, 38));

        for (int x = 0; x < 1100; x += 110) {
            g.fillRect(x, 0, 5, 620);
        }

        g.setColor(new Color(12, 20, 25));
        g.fillRect(0, 440, 1100, 180);

        g.setColor(new Color(35, 45, 50));

        for (int x = -400; x < 1500; x += 120) {
            g.drawLine(550, 400, x, 620);
        }

        for (int y = 450; y < 620; y += 30) {
            g.drawLine(0, y, 1100, y);
        }

        for (int i = 10; i > 0; i--) {

            g.setColor(new Color(20, 170, 125, 4));

            g.fillRoundRect(
                405 - i * 5,
                60 - i * 5,
                290 + i * 10,
                390 + i * 10,
                70, 70
            );
        }

        g.setColor(new Color(35, 45, 50));
        g.fillRoundRect(405, 60, 290, 390, 55, 55);

        g.setColor(new Color(5, 42, 35));
        g.fillRoundRect(425, 90, 250, 320, 45, 45);

        g.setColor(new Color(20, 120, 85, 75));
        g.fillRoundRect(425, 90, 250, 320, 45, 45);

        g.setColor(new Color(70, 140, 120));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(425, 90, 250, 320, 45, 45);

        drawSpecimen(g, specimenMoves);

        for (int i = 0; i < 22; i++) {

            int x = 440 + (i * 47) % 220;

            int y = 390 -
                (animationTick * (1 + i % 3) + i * 29) % 280;

            int size = 3 + i % 6;

            g.setColor(new Color(100, 255, 190, 70));
            g.drawOval(x, y, size, size);
        }

        g.setColor(new Color(45, 55, 60));
        g.fillRoundRect(395, 45, 310, 55, 15, 15);
        g.fillRoundRect(395, 400, 310, 55, 15, 15);

        g.setColor(new Color(15, 25, 30));
        g.fillRect(450, 420, 200, 20);

        g.setColor(new Color(100, 210, 170));
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("SUBJECT: UNKNOWN", 480, 434);

        drawMonitor(g, 100, 130);
        drawMonitor(g, 780, 130);

        drawSilhouette(g, 280, 270, false);
        drawSilhouette(g, 820, 280, true);

        if (animationTick % 90 < 5) {
            drawFade(g, 120);
        }

        g.setColor(new Color(120, 140, 140));
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("LOCATION: CLASSIFIED", 25, 30);
    }

    private void drawSpecimen(Graphics2D g, boolean moving) {

        int floating = (int) (
            Math.sin(animationTick * 0.035) * 5
        );

        int y = 140 + floating;

        g.setColor(new Color(3, 17, 15));

        g.fillOval(523, y, 54, 65);
        g.fillRoundRect(510, y + 60, 80, 115, 30, 30);

        g.setStroke(new BasicStroke(
            20,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        ));

        g.drawLine(530, y + 165, 520, y + 245);
        g.drawLine(570, y + 165, 580, y + 245);

        g.drawLine(515, y + 80, 490, y + 145);

        if (moving) {

            int movement = Math.min(65, tick);

            g.drawLine(
                585, y + 80,
                610, y + 130 - movement
            );

            g.drawLine(
                610, y + 130 - movement,
                625, y + 165 - movement
            );

            g.setStroke(new BasicStroke(4));

            for (int i = 0; i < 4; i++) {

                g.drawLine(
                    620 + i * 4,
                    y + 165 - movement,
                    617 + i * 6,
                    y + 180 - movement
                );
            }

            if (tick > 75) {

                g.setColor(new Color(210, 40, 50));
                g.fillOval(535, y + 25, 6, 3);
                g.fillOval(559, y + 25, 6, 3);
            }

        } else {
            g.drawLine(585, y + 80, 610, y + 145);
        }
    }

    private void drawMonitor(Graphics2D g, int x, int y) {

        g.setColor(new Color(30, 40, 45));
        g.fillRoundRect(x, y, 220, 125, 12, 12);

        g.setColor(new Color(3, 20, 18));
        g.fillRect(x + 10, y + 10, 200, 105);

        g.setColor(new Color(40, 180, 130));

        int previousX = x + 15;
        int previousY = y + 60;

        for (int i = 0; i < 190; i++) {

            int wave = (i + animationTick * 2) % 65;
            int offset = 0;

            if (wave > 25 && wave < 32) {
                offset = -25;
            } else if (wave >= 32 && wave < 39) {
                offset = 20;
            }

            int nextX = x + 15 + i;
            int nextY = y + 60 + offset;

            g.drawLine(previousX, previousY, nextX, nextY);

            previousX = nextX;
            previousY = nextY;
        }

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("VITAL SIGNS: ACTIVE", x + 15, y + 25);
    }

    private void drawSilhouette(
        Graphics2D g, int x, int y, boolean facingLeft
    ) {

        g.setColor(new Color(3, 5, 8));

        g.fillOval(x - 28, y - 65, 56, 65);

        Polygon body = new Polygon();

        body.addPoint(x - 25, y);
        body.addPoint(x + 25, y);
        body.addPoint(x + 55, y + 55);
        body.addPoint(x + 45, y + 170);
        body.addPoint(x - 45, y + 170);
        body.addPoint(x - 55, y + 55);

        g.fillPolygon(body);

        g.fillRect(x - 33, y + 150, 27, 100);
        g.fillRect(x + 6, y + 150, 27, 100);

        g.setColor(new Color(30, 55, 50));
        g.setStroke(new BasicStroke(2));

        if (facingLeft) {
            g.drawArc(x - 28, y - 65, 56, 65, 90, 180);
        } else {
            g.drawArc(x - 28, y - 65, 56, 65, -90, 180);
        }
    }

    private void drawDialogue(
        Graphics2D g,
        String speaker,
        String message,
        int visible
    ) {

        g.setColor(new Color(8, 6, 16, 235));
        g.fillRoundRect(70, 455, 960, 135, 15, 15);

        g.setColor(new Color(145, 80, 200));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(70, 455, 960, 135, 15, 15);

        g.setFont(new Font("Monospaced", Font.BOLD, 17));

        g.setColor(
            speaker.startsWith("UNKNOWN")
                ? new Color(235, 85, 90)
                : new Color(190, 130, 255)
        );

        g.drawString(speaker, 95, 485);

        g.setColor(new Color(225, 225, 235));
        g.setFont(new Font("Monospaced", Font.PLAIN, 16));

        String displayed = message.substring(
            0, Math.min(visible, message.length())
        );

        drawWrappedText(g, displayed, 95, 520, 900);

        if (visible >= message.length() &&
            animationTick % 40 < 25) {

            g.setColor(new Color(160, 120, 200));
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.drawString("ENTER TO CONTINUE", 850, 570);
        }
    }

    private void drawWrappedText(
        Graphics2D g, String text, int x, int y, int width
    ) {

        FontMetrics fm = g.getFontMetrics();

        StringBuilder row = new StringBuilder();

        for (String word : text.split(" ")) {

            String next = row.length() == 0
                ? word
                : row + " " + word;

            if (fm.stringWidth(next) > width) {

                g.drawString(row.toString(), x, y);

                row = new StringBuilder(word);
                y += 25;

            } else {
                row = new StringBuilder(next);
            }
        }

        g.drawString(row.toString(), x, y);
    }

    private void drawFinalTitle(Graphics2D g) {

        int alpha = Math.min(255, tick * 3);

        g.setFont(new Font("Monospaced", Font.BOLD, 85));

        for (int i = 8; i > 0; i--) {

            g.setColor(new Color(
                110, 35, 170, Math.min(alpha, 12)
            ));

            g.setStroke(new BasicStroke(i * 3f));

            FontMetrics fm = g.getFontMetrics();

            int x = (1100 - fm.stringWidth("CHEMION")) / 2;

            Shape outline = g.getFont()
                .createGlyphVector(
                    g.getFontRenderContext(), "CHEMION"
                )
                .getOutline(x, 285);

            g.draw(outline);
        }

        g.setColor(new Color(185, 120, 245, alpha));
        drawCentered(g, "CHEMION", 285);

        g.setFont(new Font("Monospaced", Font.BOLD, 23));
        g.setColor(new Color(210, 210, 225, alpha));
        drawCentered(g, "THE LAST CURE", 335);

        if (tick > 120) {

            g.setFont(new Font("Monospaced", Font.PLAIN, 14));
            g.setColor(new Color(125, 125, 140));

            drawCentered(g, "THANK YOU FOR PLAYING", 440);
        }

        if (tick > 150 && animationTick % 50 < 30) {

            g.setFont(new Font("Monospaced", Font.PLAIN, 12));

            drawCentered(g, "PRESS ENTER TO CONTINUE", 550);
        }
    }

    private void drawCentered(
        Graphics2D g, String text, int y
    ) {

        int x = (
            1100 - g.getFontMetrics().stringWidth(text)
        ) / 2;

        g.drawString(text, x, y);
    }

    private void drawFade(Graphics2D g, int alpha) {

        g.setColor(new Color(
            0, 0, 0,
            Math.max(0, Math.min(255, alpha))
        ));

        g.fillRect(0, 0, 1100, 620);
    }
}