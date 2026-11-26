import game.AudioManager;

public class AudioTest {
    public static void main(String[] args) throws Exception {

        System.out.println("Testing medkit sound...");
        AudioManager.playMedkitSound();
        Thread.sleep(3000);

        System.out.println("Testing low heartbeat (20 HP)...");
        AudioManager.updateHeartbeat(20, 100);
        Thread.sleep(10000);

        AudioManager.stopHeartbeat();
        Thread.sleep(2000);

        System.out.println("Testing critical heartbeat (5 HP)...");
        AudioManager.updateHeartbeat(5, 100);
        Thread.sleep(10000);

        AudioManager.stopHeartbeat();
        AudioManager.shutdown();

        System.out.println("Audio testing complete.");
    }
}