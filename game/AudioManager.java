package game;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.sound.sampled.*;
import javax.swing.AbstractButton;

/** One ambient soundtrack across narration, dialogues and Levels 1-3. */
public final class AudioManager {
    private static Clip ambient, click, coin;
    private static boolean installed;
    private static final AWTEventListener BUTTON_LISTENER = event -> {
        if (!(event instanceof MouseEvent)) return;
        MouseEvent e = (MouseEvent) event;
        if (e.getID() != MouseEvent.MOUSE_RELEASED || e.getButton() != MouseEvent.BUTTON1
                || !(e.getSource() instanceof AbstractButton)) return;
        AbstractButton button = (AbstractButton) e.getSource();
        if (button.isEnabled() && button.contains(e.getPoint())) playClick();
    };
    private AudioManager() {}
    private static Clip load(String path) {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(new File(path))) {
            Clip result = AudioSystem.getClip();
            result.open(stream);
            return result;
        } catch (Exception e) {
            System.err.println("CHEMION audio unavailable: " + path + " (" + e.getMessage() + ")");
            return null;
        }
    }
    public static synchronized void installButtonClicks() {
        if (installed) return;
        click = load("assets/audio/button_click.wav");
        Toolkit.getDefaultToolkit().addAWTEventListener(BUTTON_LISTENER, AWTEvent.MOUSE_EVENT_MASK);
        installed = true;
    }
    public static synchronized void playClick() {
        if (click == null) return;
        click.stop();
        click.setFramePosition(0);
        click.start();
    }
    
    public static synchronized void playCoinGain() {
    File file = new File("assets/audio/coin_gain.wav");

    System.out.println("[COIN] Working directory: "
            + System.getProperty("user.dir"));
    System.out.println("[COIN] File exists: " + file.exists());
    System.out.println("[COIN] File path: " + file.getAbsolutePath());

    if (coin == null) {
        coin = load(file.getPath());
    }

    if (coin == null) {
        System.out.println("[COIN] Failed to load audio.");
        return;
    }

    coin.stop();
    coin.setFramePosition(0);
    coin.start();

    System.out.println("[COIN] Playback started.");
    }

    public static synchronized void setAmbientEnabled(boolean enabled) {
        if (!enabled) {
            if (ambient != null) ambient.stop();
            return;
        }
        if (ambient == null) ambient = load("assets/audio/intro_background.wav");
        if (ambient != null && !ambient.isRunning()) ambient.loop(Clip.LOOP_CONTINUOUSLY);
    }
    public static synchronized void shutdown() {
        if (installed) Toolkit.getDefaultToolkit().removeAWTEventListener(BUTTON_LISTENER);
        installed = false;
        if (ambient != null) { ambient.stop(); ambient.close(); ambient = null; }
        if (click != null) { click.stop(); click.close(); click = null; }
        if (coin != null) { coin.stop(); coin.close(); coin = null; }
    }
    public static void main(String[] args) throws Exception {
    playCoinGain();
    Thread.sleep(3000);
    shutdown();
}
}
