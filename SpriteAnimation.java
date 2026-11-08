
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SpriteAnimation {

    private BufferedImage spriteSheet;

    private int frameWidth;
    private int frameHeight;
    private int frameCount;

    private int currentFrame = 0;

    private long lastFrameTime;
    private final long frameDelay = 120;

    private boolean finished = false;

    public SpriteAnimation(
            String filePath,
            int frameWidth,
            int frameHeight,
            int frameCount
    ) throws IOException {

        spriteSheet = ImageIO.read(new File(filePath));

        if (spriteSheet == null) {
            throw new IOException("Invalid image: " + filePath);
        }

        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameCount = frameCount;

        if (spriteSheet.getWidth() < frameWidth * frameCount ||
            spriteSheet.getHeight() < frameHeight) {

            throw new IOException(
                "Sprite sheet dimensions are incorrect: " + filePath
            );
        }

        lastFrameTime = System.currentTimeMillis();
    }

    // Looping animation for walking
    public void update() {

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastFrameTime >= frameDelay) {

            currentFrame++;

            if (currentFrame >= frameCount) {
                currentFrame = 0;
            }

            lastFrameTime = currentTime;
        }
    }

    // Play animation once for hurt
    public void updateOnce() {

        if (finished) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastFrameTime >= frameDelay) {

            if (currentFrame < frameCount - 1) {
                currentFrame++;
            } else {
                finished = true;
            }

            lastFrameTime = currentTime;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public void reset() {

        currentFrame = 0;
        finished = false;

        lastFrameTime = System.currentTimeMillis();
    }

    // Draw normal sprite
    public void draw(
            Graphics g,
            int x,
            int y,
            int width,
            int height
    ) {

        draw(g, x, y, width, height, false);
    }

    // Draw sprite with optional red flash
    public void draw(
            Graphics g,
            int x,
            int y,
            int width,
            int height,
            boolean redFlash
    ) {

        int sourceX = currentFrame * frameWidth;

        BufferedImage frame = spriteSheet.getSubimage(
            sourceX,
            0,
            frameWidth,
            frameHeight
        );

        if (redFlash) {

            BufferedImage tinted = new BufferedImage(
                frameWidth,
                frameHeight,
                BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D tg = tinted.createGraphics();

            tg.drawImage(frame, 0, 0, null);

            tg.setComposite(
                AlphaComposite.SrcAtop.derive(0.65f)
            );

            tg.setColor(Color.RED);
            tg.fillRect(0, 0, frameWidth, frameHeight);

            tg.dispose();

            frame = tinted;
        }

        g.drawImage(
            frame,
            x, y,
            width, height,
            null
        );
    }
}
