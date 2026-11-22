package game;

/** Proposal: player, enemy, battleState; startBattle(), usePotion(), endBattle(). */
public class BattleSystem {
    private Scientist player;
    private Zombie enemy;
    private boolean battleState;
    public void startBattle(Scientist player, Zombie enemy) {
        if (player == null || enemy == null) throw new IllegalArgumentException("Battle participants required");
        this.player = player; this.enemy = enemy; battleState = true;
    }
    public int usePotion(Potion potion) {
        if (!battleState || potion == null) throw new IllegalStateException("Start a battle with a potion first");
        int damage = potion.use(enemy); // Dynamic dispatch to the concrete Zombie subclass.
        if (enemy.isStabilized()) endBattle();
        return damage;
    }
    public void endBattle() { battleState = false; }
    public boolean isActive() { return battleState; }
    public Scientist getPlayer() { return player; }
    public Zombie getEnemy() { return enemy; }
}
