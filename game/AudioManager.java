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
    private static Clip ambient, click, coin, cinematic, gameplay;

    private static javax.swing.Timer gameplayFade;
    private static int gameplayLevel = 0;

    private static Clip medkitSound;
    private static Clip heartbeatLow;
    private static Clip heartbeatCritical;

private static Clip activeHeartbeat;

    private static final float GAMEPLAY_VOLUME = 0.60f;
    private static final Clip[] zombieGrowls = new Clip[16];
    private static Clip zombieDamage;

    private static final String[] ZOMBIE_SOUNDS = {
        "assets/audio/zombie_common.wav",
        "assets/audio/zombie_runner.wav",
        "assets/audio/zombie_crawler.wav",
        "assets/audio/zombie_boss.wav",

        // Level 2
        "assets/audio/zombie_acid.wav",
        "assets/audio/zombie_patient_zero.wav",
        "assets/audio/zombie_toxic.wav",
        "assets/audio/zombie_plague_doctor.wav",

            // Level 3
        "assets/audio/zombie_toxic_brute.wav",
        "assets/audio/zombie_venom_spitter.wav",
        "assets/audio/zombie_explosive.wav",
        "assets/audio/zombie_chemical_abomination.wav",

        // Level 4
        "assets/audio/zombie_experiment_01.wav",
        "assets/audio/zombie_mutant_stalker.wav",
        "assets/audio/zombie_project_chimera.wav",
        "assets/audio/zombie_regenerator.wav"
    };

    private static final float ZOMBIE_VOLUME = 0.45f;
    private static final float BOSS_VOLUME = 0.50f;
    private static final float DAMAGE_VOLUME = 0.60f;

    private static final float MEDKIT_VOLUME = 0.90f;
    private static final float HEARTBEAT_LOW_VOLUME = 1.00f;
    private static final float HEARTBEAT_CRITICAL_VOLUME = 1.00f;

    private static long nextGrowlTime = 0;

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
    
    public static synchronized void playCinematic() {
    if (cinematic == null) {
        cinematic = load("assets/audio/level_title_reveal.wav");
    }

    if (cinematic == null) return;

    cinematic.stop();
    cinematic.setFramePosition(0);
    cinematic.start();
    }

    public static synchronized void stopCinematic() {
        if (cinematic != null) {
            cinematic.stop();
            }
        }

        private static void setVolume(Clip clip, float volume) {

        if (clip == null) return;

        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }

        FloatControl gain = (FloatControl)
            clip.getControl(FloatControl.Type.MASTER_GAIN);

        float dB = volume <= 0f
            ? gain.getMinimum()
            : (float) (20.0 * Math.log10(volume));

        gain.setValue(
            Math.max(gain.getMinimum(),
                Math.min(gain.getMaximum(), dB))
        );
    }
    
    public static synchronized void playZombieGrowl(int type) {

    if (type < 0 || type >= zombieGrowls.length) return;

    long now = System.currentTimeMillis();

    if (now < nextGrowlTime) return;

    for (Clip growl : zombieGrowls) {
        if (growl != null && growl.isRunning()) return;
    }

    if (zombieGrowls[type] == null) {
        zombieGrowls[type] = load(ZOMBIE_SOUNDS[type]);
    }

    Clip growl = zombieGrowls[type];

    if (growl == null) {
        nextGrowlTime = now + 5000;
        return;
    }

    setVolume(growl,
    (type == 3 || type == 7 || type == 11 || type == 15)
    ? BOSS_VOLUME : ZOMBIE_VOLUME);

    growl.setFramePosition(0);
    growl.start();

    nextGrowlTime = now + 6000;
    }

    public static synchronized void playLevel2ZombieGrowl(int type) {
        if (type < 0 || type > 3) return;

        playZombieGrowl(type + 4);
    }
    public static synchronized void playLevel3ZombieGrowl(int type) {
    if (type < 0 || type > 3) return;

    playZombieGrowl(type + 8);
    }

    public static synchronized void playLevel4ZombieGrowl(int type) {
        if (type < 0 || type > 3) return;

        playZombieGrowl(type + 12);
    }
    public static synchronized void playMedkitSound() {
    if (medkitSound == null) {
        medkitSound = load("assets/audio/medkit_use.wav");
    }

    if (medkitSound == null) return;

    medkitSound.stop();
    medkitSound.setFramePosition(0);
    setVolume(medkitSound, MEDKIT_VOLUME);
    medkitSound.start();
    }
    public static synchronized void updateHeartbeat(
        double hp, double maxHp) {

    if (hp <= 0 || maxHp <= 0) {
        stopHeartbeat();
        return;
    }

    double healthPercent = (hp / maxHp) * 100.0;

    if (healthPercent > 25) {
        stopHeartbeat();
        return;
    }

    Clip target;
    float volume;

    if (healthPercent <= 10) {

        if (heartbeatCritical == null) {
            heartbeatCritical = load(
                "assets/audio/heartbeat_critical.wav"
            );
        }

        target = heartbeatCritical;
        volume = HEARTBEAT_CRITICAL_VOLUME;

    } else {

        if (heartbeatLow == null) {
            heartbeatLow = load(
                "assets/audio/heartbeat_low.wav"
            );
        }

        target = heartbeatLow;
        volume = HEARTBEAT_LOW_VOLUME;
    }

    if (target == null) {
        stopHeartbeat();
        return;
    }

    if (activeHeartbeat == target && target.isRunning()) {
        return;
    }

    stopHeartbeat();

    activeHeartbeat = target;
    activeHeartbeat.setFramePosition(0);
    setVolume(activeHeartbeat, volume);
    activeHeartbeat.loop(Clip.LOOP_CONTINUOUSLY);
}

public static synchronized void stopHeartbeat() {
    if (activeHeartbeat != null) {
        activeHeartbeat.stop();
        activeHeartbeat.setFramePosition(0);
        activeHeartbeat = null;
    }
}
    public static synchronized void playZombieDamage() {

        if (zombieDamage == null) {
            zombieDamage = load("assets/audio/zombie_damage.wav");
        }

        if (zombieDamage == null) return;

        setVolume(zombieDamage, DAMAGE_VOLUME);

        zombieDamage.stop();
        zombieDamage.setFramePosition(0);
        zombieDamage.start();
    }

    public static synchronized void stopZombieSounds() {

        for (Clip growl : zombieGrowls) {
            if (growl != null) {
                growl.stop();
            }
        }

        if (zombieDamage != null) {
            zombieDamage.stop();
        }

        nextGrowlTime = 0;
    }

    public static synchronized void playGameplayMusic() {
    playGameplayMusic(1);
    }

    public static synchronized void playGameplayMusic(int level) {

       if (level < 1 || level > 5) return;

        if (gameplayLevel != level) {

            stopGameplayMusic();

            if (gameplay != null) {
                gameplay.close();
                gameplay = null;
            }

            gameplayLevel = level;
        }

        if (gameplay == null) {
            gameplay = load(
                "assets/audio/level" + level + "_background.wav"
            );
        }

        if (gameplay == null || gameplay.isRunning()) return;

        if (gameplayFade != null) {
            gameplayFade.stop();
        }

        setVolume(gameplay, 0f);

        gameplay.loop(Clip.LOOP_CONTINUOUSLY);

        long fadeStart = System.currentTimeMillis();

        gameplayFade = new javax.swing.Timer(40, e -> {

            float progress = Math.min(
                1f,
                (System.currentTimeMillis() - fadeStart) / 2000f
            );

            setVolume(gameplay, GAMEPLAY_VOLUME * progress);

            if (progress >= 1f) {
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });

        gameplayFade.start();
    }

    public static synchronized void stopGameplayMusic() {
    stopHeartbeat();
    stopZombieSounds();

    if (gameplayFade != null) {
        gameplayFade.stop();
        gameplayFade = null;
    }

    if (gameplay != null) {
        gameplay.stop();
    }
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
        stopGameplayMusic();
        for (int i = 0; i < zombieGrowls.length; i++) {
        if (zombieGrowls[i] != null) {
        zombieGrowls[i].close();
        zombieGrowls[i] = null;
                }
            }

        if (zombieDamage != null) {
            zombieDamage.close();
            zombieDamage = null;
        }
        if (gameplay != null) {
            gameplay.close();
            gameplay = null;
        }
        if (installed) Toolkit.getDefaultToolkit().removeAWTEventListener(BUTTON_LISTENER);
        installed = false;
        if (ambient != null) { ambient.stop(); ambient.close(); ambient = null; }
        if (click != null) { click.stop(); click.close(); click = null; }
        if (coin != null) { coin.stop(); coin.close(); coin = null; }
        if (cinematic != null) { cinematic.stop(); cinematic.close(); cinematic = null; }

        stopHeartbeat();

        if (medkitSound != null) {
            medkitSound.close();
            medkitSound = null;
        }

        if (heartbeatLow != null) {
            heartbeatLow.close();
            heartbeatLow = null;
        }

        if (heartbeatCritical != null) {
            heartbeatCritical.close();
            heartbeatCritical = null;
        }
    }
    public static void main(String[] args) throws Exception {
    playCinematic();
    Thread.sleep(6000);
    shutdown();
    }
}
