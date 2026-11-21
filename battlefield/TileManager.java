
package battlefield;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

public class TileManager {

    public static final int TILE_SIZE = 48;
    public static final int MAP_COLS = 120;
    public static final int MAP_ROWS = 100;

    public static final int GRASS = 0;
    public static final int ROAD = 1;
    public static final int BUILDING = 2;
    public static final int SIDEWALK = 3;
    public static final int DEBRIS = 4;
    public static final int TOXIC = 5;

    private final int[][] map = new int[MAP_ROWS][MAP_COLS];

    private BufferedImage groundImage;
    private BufferedImage roadImage;
    private BufferedImage sidewalkImage;
    private BufferedImage toxicImage;

    public TileManager() {
        loadTiles();
        generateMap();
    }

    private BufferedImage loadImage(String name) {
        try {
            return ImageIO.read(new File(
                "assets/battlefield/level1/" + name
            ));
        } catch (IOException e) {
            System.err.println("Cannot load tile: " + name);
            return null;
        }
    }

    private void loadTiles() {
        groundImage = loadImage("ground.png");
        roadImage = loadImage("road.png");
        sidewalkImage = loadImage("sidewalk.png");
        toxicImage = loadImage("toxic_ground.png");
    }

    private void generateMap() {
        for (int row = 0; row < MAP_ROWS; row++) {
            for (int col = 0; col < MAP_COLS; col++) {
                map[row][col] = GRASS;
            }
        }

        // Horizontal streets
        for (int row = 15; row < MAP_ROWS; row += 25) {
            fillArea(row, 0, row + 5, MAP_COLS - 1, ROAD);
        }

        // Vertical streets
        for (int col = 15; col < MAP_COLS; col += 25) {
            fillArea(0, col, MAP_ROWS - 1, col + 5, ROAD);
        }

        // Sidewalks
        for (int row = 0; row < MAP_ROWS; row++) {
            for (int col = 0; col < MAP_COLS; col++) {
                if (map[row][col] == GRASS && isNextToRoad(row, col)) {
                    map[row][col] = SIDEWALK;
                }
            }
        }

        // Abandoned buildings
        for (int blockRow = 0; blockRow < 4; blockRow++) {
            for (int blockCol = 0; blockCol < 5; blockCol++) {
                int row = 3 + blockRow * 25;
                int col = 3 + blockCol * 25;
                fillArea(row, col, row + 9, col + 9, BUILDING);
            }
        }

        Random random = new Random(42);

        // Toxic chemical spills
        for (int i = 0; i < 65; i++) {
            int row = random.nextInt(MAP_ROWS);
            int col = random.nextInt(MAP_COLS);

            if (map[row][col] == GRASS) {
                map[row][col] = TOXIC;
            }
        }

        // Scattered debris
        for (int i = 0; i < 160; i++) {
            int row = random.nextInt(MAP_ROWS);
            int col = random.nextInt(MAP_COLS);

            if (map[row][col] == GRASS) {
                map[row][col] = DEBRIS;
            }
        }
    }

    private void fillArea(int startRow, int startCol,
                          int endRow, int endCol, int type) {
        for (int row = Math.max(0, startRow);
             row <= Math.min(endRow, MAP_ROWS - 1); row++) {

            for (int col = Math.max(0, startCol);
                 col <= Math.min(endCol, MAP_COLS - 1); col++) {

                map[row][col] = type;
            }
        }
    }

    private boolean isNextToRoad(int row, int col) {
        int[][] directions = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
        };

        for (int[] direction : directions) {
            int r = row + direction[0];
            int c = col + direction[1];

            if (r >= 0 && r < MAP_ROWS &&
                c >= 0 && c < MAP_COLS &&
                map[r][c] == ROAD) {
                return true;
            }
        }
        return false;
    }

    public int getTile(int row, int col) {
        if (row < 0 || row >= MAP_ROWS ||
            col < 0 || col >= MAP_COLS) {
            return -1;
        }
        return map[row][col];
    }

    public boolean isWalkable(int row, int col) {
        int tile = getTile(row, col);

        return tile == GRASS ||
               tile == ROAD ||
               tile == SIDEWALK;
    }

    public void draw(Graphics2D g2, int cameraX, int cameraY) {
        Rectangle clip = g2.getClipBounds();

        int startCol = Math.max(0, cameraX / TILE_SIZE);
        int startRow = Math.max(0, cameraY / TILE_SIZE);

        int endCol = Math.min(MAP_COLS,
            (cameraX + clip.width) / TILE_SIZE + 2);

        int endRow = Math.min(MAP_ROWS,
            (cameraY + clip.height) / TILE_SIZE + 2);

        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                int x = col * TILE_SIZE - cameraX;
                int y = row * TILE_SIZE - cameraY;

                switch (map[row][col]) {
                    case ROAD:
                        drawImage(g2, roadImage, x, y, new Color(50, 50, 60));
                        break;

                    case SIDEWALK:
                        drawImage(g2, sidewalkImage, x, y, Color.GRAY);
                        break;

                    case TOXIC:
                        drawImage(g2, toxicImage, x, y, new Color(35, 95, 55));
                        break;

                    case BUILDING:
                        drawBuilding(g2, x, y, row, col);
                        break;

                    case DEBRIS:
                        drawImage(g2, groundImage, x, y, new Color(35, 57, 43));
                        drawDebris(g2, x, y);
                        break;

                    default:
                        drawImage(g2, groundImage, x, y, new Color(35, 57, 43));
                        break;
                }
            }
        }
    }

    private void drawImage(Graphics2D g2, BufferedImage image,
                           int x, int y, Color fallback) {
        if (image != null) {
            g2.drawImage(image, x, y, TILE_SIZE, TILE_SIZE, null);
        } else {
            g2.setColor(fallback);
            g2.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        }
    }

    private void drawBuilding(Graphics2D g2, int x, int y,
                              int row, int col) {
        g2.setColor(new Color(58, 46, 73));
        g2.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        g2.setColor(new Color(92, 72, 108));
        g2.fillRect(x + 3, y + 3, 42, 42);

        if ((row + col) % 4 == 0) {
            g2.setColor(new Color(30, 90, 92));
            g2.fillRect(x + 10, y + 10, 28, 22);
        }
    }

    private void drawDebris(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(95, 85, 80));
        g2.fillRect(x + 8, y + 18, 20, 12);

        g2.setColor(new Color(60, 56, 62));
        g2.fillRect(x + 25, y + 10, 12, 15);
    }

    public int getMapWidth() {
        return MAP_COLS * TILE_SIZE;
    }

    public int getMapHeight() {
        return MAP_ROWS * TILE_SIZE;
    }
}
