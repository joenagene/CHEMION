package game;

/** Abstract player base: name, health, level, inventory. */
public abstract class Character {
    private final String name;
    private int health = 100;
    private int level = 1;
    private final Inventory inventory = new Inventory();
    private double x, y;

    protected Character(String name) { this.name = Inventory.requireName(name); }
    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getLevel() { return level; }
    public Inventory getInventory() { return inventory; }
    public double getX() { return x; }
    public double getY() { return y; }
    public void setPosition(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("Invalid position");
        this.x = x; this.y = y;
    }
    public void setLevel(int level) {
        if (level < 1 || level > 5) throw new IllegalArgumentException("Level must be 1 through 5");
        this.level = level;
    }
    public abstract void move(double dx, double dy);
    public void takeDamage(int damage) {
        if (damage < 0) throw new IllegalArgumentException("Damage cannot be negative");
        health = Math.max(0, health - damage);
    }
    public void restoreHealth(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Heal amount cannot be negative");
        health = Math.min(100, health + amount);
    }
    public void setHealth(int value) {
        if (value < 0 || value > 100) throw new IllegalArgumentException("Health must be 0..100");
        health = value;
    }
    public boolean useItem(String item) { return inventory.useItem(item); }
}
