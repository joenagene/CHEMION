package game;

public final class Reward {
    private final int xp, coins;
    private final String item;
    private final int itemQuantity;
    public Reward(int xp, int coins, String item, int itemQuantity) {
        if (xp < 0 || coins < 0 || itemQuantity < 0) throw new IllegalArgumentException("Negative reward");
        this.xp = xp; this.coins = coins; this.item = item; this.itemQuantity = itemQuantity;
        if (itemQuantity > 0) Inventory.requireName(item);
    }
    public int getXp() { return xp; }
    public int getCoins() { return coins; }
    public String getItem() { return item; }
    public int getItemQuantity() { return itemQuantity; }
}
