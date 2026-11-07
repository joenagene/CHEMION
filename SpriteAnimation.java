
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SpriteAnimation {

    private final BufferedImage[] frames;
    private int currentFrame = 0;
    private long lastUpdate = 0;

    public SpriteAnimation(String path,
                           int frameWidth,
                           int frameHeight,
                           int frameCount) throws IOException {

        BufferedImage sheet = ImageIO.read(new File(path));

        if (sheet == null) {
            throw new IOException("Cannot read: " + path);
        }

        if (frameWidth <= 0 || frameHeight <= 0
                || frameCount <= 0
                || sheet.getWidth() < frameWidth * frameCount
                || sheet.getHeight() < frameHeight) {
            throw new IOException("Invalid sprite dimensions: " + path);
        }

        frames = new BufferedImage[frameCount];

        for (int i = 0; i < frameCount; i++) {
            frames[i] = sheet.getSubimage(
                i * frameWidth, 0, frameWidth, frameHeight
            );
        }
    }

    public void update() {
        long now = System.currentTimeMillis();

        if (now - lastUpdate >= 150) {
            currentFrame = (currentFrame + 1) % frames.length;
            lastUpdate = now;
        }
    }

    public void reset() {
        currentFrame = 0;
    }

    public void draw(Graphics g, int x, int y,
                     int width, int height) {
        g.drawImage(frames[currentFrame],
                    x, y, width, height, null);
    }
}
