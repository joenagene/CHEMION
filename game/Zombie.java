package game;

public abstract class Zombie {
    private final String name, mutationType, resistance, treatmentKey;
    private int health;
    protected Zombie(String name, String mutationType, String resistance,
                     String treatmentKey, int health) {
        this.name = Inventory.requireName(name);
        this.mutationType = Inventory.requireName(mutationType);
        this.resistance = Inventory.requireName(resistance);
        this.treatmentKey = Inventory.requireName(treatmentKey);
        setHealth(health);
    }
    public String getName() { return name; }
    public String getMutationType() { return mutationType; }
    public String getResistance() { return resistance; }
    public String getTreatmentKey() { return treatmentKey; }
    public int getHealth() { return health; }
    public void setHealth(int value) {
        if (value < 0) throw new IllegalArgumentException("Negative zombie health");
        health = value;
    }
    public boolean isStabilized() { return health == 0; }
    protected int applyMatchingTreatment(Potion potion, int damage) {
        if (!treatmentKey.equals(potion.getTreatmentKey())) return 0;
        int applied = Math.min(health, damage);
        health -= applied;
        return applied;
    }
    public abstract int attack();
    public abstract int receiveTreatment(Potion potion);
    public abstract Reward dropRewards();
}
