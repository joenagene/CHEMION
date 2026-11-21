
package battlefield;

import java.awt.*;
import java.util.Random;

public class EnvironmentManager {

    private static final int TREE = 1;
    private static final int CAR = 2;
    private static final int STREETLIGHT = 3;
    private static final int SMOKE = 4;

    private final int[][] objects =
        new int[TileManager.MAP_ROWS][TileManager.MAP_COLS];

    private int animationTick = 0;

    public EnvironmentManager(TileManager tileManager) {
        generateObjects(tileManager);
    }

    private void generateObjects(TileManager tileManager) {
        Random random = new Random(75);

        for (int row = 0; row < TileManager.MAP_ROWS; row++) {
            for (int col = 0; col < TileManager.MAP_COLS; col++) {
                if (row >= 45 && row <= 55 &&
                    col >= 55 && col <= 65) {
                    continue;
                }

                int tile = tileManager.getTile(row, col);
                int chance = random.nextInt(1000);

                if (tile == TileManager.GRASS) {
                    if (chance < 22) {
                        objects[row][col] = TREE;
                    } else if (chance < 30) {
                        objects[row][col] = SMOKE;
                    }
                } else if (tile == TileManager.ROAD) {
                    if (chance < 12) {
                        objects[row][col] = CAR;
                    }
                } else if (tile == TileManager.SIDEWALK) {
                    if (chance < 30) {
                        objects[row][col] = STREETLIGHT;
                    }
                }
            }
        }
    }

    public void update() {
        animationTick++;
    }

    public void draw(Graphics2D g2, int cameraX, int cameraY) {
        int size = TileManager.TILE_SIZE;
        Rectangle clip = g2.getClipBounds();

        int startCol = Math.max(0, cameraX / size);
        int startRow = Math.max(0, cameraY / size);

        int endCol = Math.min(TileManager.MAP_COLS,
            (cameraX + clip.width) / size + 2);
        int endRow = Math.min(TileManager.MAP_ROWS,
            (cameraY + clip.height) / size + 2);

        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                int x = col * size - cameraX;
                int y = row * size - cameraY;

                switch (objects[row][col]) {
                    case TREE:
                        drawTree(g2, x, y);
                        break;
                    case CAR:
                        drawCar(g2, x, y);
                        break;
                    case STREETLIGHT:
                        drawStreetlight(g2, x, y);
                        break;
                    case SMOKE:
                        drawSmoke(g2, x, y);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void drawTree(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(60, 42, 38));
        g2.fillRect(x + 20, y + 24, 8, 20);

        g2.setColor(new Color(25, 72, 48));
        g2.fillOval(x + 5, y + 3, 38, 34);

        g2.setColor(new Color(40, 100, 60));
        g2.fillOval(x + 13, y + 6, 22, 20);
    }

    private void drawCar(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(100, 47, 55));
        g2.fillRect(x + 6, y + 10, 36, 28);

        g2.setColor(new Color(32, 39, 50));
        g2.fillRect(x + 13, y + 15, 22, 15);

        g2.setColor(Color.BLACK);
        g2.fillRect(x + 8, y + 6, 8, 5);
        g2.fillRect(x + 32, y + 6, 8, 5);
        g2.fillRect(x + 8, y + 38, 8, 5);
        g2.fillRect(x + 32, y + 38, 8, 5);
    }

    private void drawStreetlight(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(80, 80, 90));
        g2.fillRect(x + 21, y + 10, 5, 34);

        boolean lightOn = (animationTick / 25) % 4 != 0;

        if (lightOn) {
            g2.setColor(new Color(255, 215, 100, 75));
            g2.fillOval(x + 5, y, 38, 38);
        }

        g2.setColor(lightOn
            ? new Color(255, 220, 120)
            : new Color(95, 85, 70));

        g2.fillRect(x + 15, y + 5, 18, 9);
    }

    private void drawSmoke(Graphics2D g2, int x, int y) {
        int offset = (animationTick / 5) % 24;

        g2.setColor(new Color(135, 135, 145, 85));
        g2.fillOval(x + 10, y + 26 - offset, 22, 18);

        g2.setColor(new Color(170, 170, 180, 60));
        g2.fillOval(x + 18, y + 35 - offset, 18, 14);
    }
}
