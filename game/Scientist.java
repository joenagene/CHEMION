package game;

/** Scientist inherits Character; encapsulates XP, coins, research points. */
public class Scientist extends Character {
    private final String specialty;
    private int experience, coins, researchPoints;

    public Scientist(String name, String specialty) {
        super(name);
        this.specialty = Inventory.requireName(specialty);
    }
    public static Scientist forKey(String key) {
        String normalized = key == null ? "nicole" : key.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("sebastian")) return new Scientist("Dr. Sebastian", "HEAD SCIENTIST");
        if (normalized.contains("dutch")) return new Scientist("Dr. Dutch", "CHEMIST");
        if (normalized.contains("gene")) return new Scientist("Dr. Gene", "BIOLOGIST");
        if (normalized.contains("elena")) return new Scientist("Dr. Elena", "MEDICAL SCIENTIST");
        return new Scientist("Dr. Nicole", "RESEARCH SCIENTIST");
    }
    public String getSpecialty() { return specialty; }
    public int craftingBonusSeconds() { return "CHEMIST".equals(specialty) ? 6 : 0; }
    public int healingBonus() { return "MEDICAL SCIENTIST".equals(specialty) ? 12 : 0; }
    public String getSpecialAbility() {
        switch (specialty) {
            case "HEAD SCIENTIST": return "Leadership: +2 coins per reward";
            case "CHEMIST": return "Formulation: +6 seconds to craft";
            case "RESEARCH SCIENTIST": return "Investigation: +5 research points per scan";
            case "BIOLOGIST": return "Biology: +5 XP per reward";
            case "MEDICAL SCIENTIST": return "Medical care: +12 HP per medkit";
            default: return "General research";
        }
    }
    public int getExperience() { return experience; }
    public int getCoins() { return coins; }
    public int getResearchPoints() { return researchPoints; }
    @Override public void move(double dx, double dy) {
        setPosition(getX() + dx, getY() + dy);
    }
    @Override public boolean useItem(String item) {
        if (!super.useItem(item)) return false;
        if ("Medkit".equalsIgnoreCase(item)) restoreHealth(35);
        return true;
    }
    public String scanEnemy(Zombie zombie) {
        if (zombie == null) throw new IllegalArgumentException("Zombie required");
        researchPoints += "RESEARCH SCIENTIST".equals(specialty) ? 10 : 5;
        return zombie.getName() + " | mutation: " + zombie.getMutationType()
                + " | resistance: " + zombie.getResistance();
    }
    public void gainXP(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative XP");
        experience = Math.addExact(experience, amount);
    }
    public void collectReward(Reward reward) {
        if (reward == null) throw new IllegalArgumentException("Reward required");
        gainXP(reward.getXp() + (reward.getXp() > 0 && "BIOLOGIST".equals(specialty) ? 5 : 0));
        coins = Math.addExact(coins, reward.getCoins()
                + (reward.getCoins() > 0 && "HEAD SCIENTIST".equals(specialty) ? 2 : 0));
        if (reward.getItemQuantity() > 0 && !getInventory().addItem(reward.getItem(), reward.getItemQuantity()))
            throw new IllegalStateException("Inventory full");
    }
    public boolean spendCoins(int cost) {
        if (cost < 0) throw new IllegalArgumentException("Negative price");
        if (coins < cost) return false;
        coins -= cost;
        return true;
    }
    public void restoreProgress(int experience, int coins, int researchPoints) {
        if (experience < 0 || coins < 0 || researchPoints < 0) throw new IllegalArgumentException("Negative progress");
        this.experience = experience;
        this.coins = coins;
        this.researchPoints = researchPoints;
    }
}
