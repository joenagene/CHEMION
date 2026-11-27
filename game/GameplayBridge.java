package game;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Adapter from the existing Level 1/2 Swing arrays to the proposed OOP models.
 * Keeps the approved UI, save-file keys, and combat pacing unchanged.
 * The real validation, crafting, and treatment rules run in domain classes.
 */
public final class GameplayBridge {
    private GameplayBridge() { }

    private static Inventory inventory(String[] names, int[] quantities) {
        if (names.length != quantities.length) throw new IllegalArgumentException("Inventory length mismatch");
        Inventory inventory = new Inventory();
        for (int i = 0; i < names.length; i++) inventory.setQuantity(names[i], quantities[i]);
        return inventory;
    }
    private static void copyStock(Inventory inventory, String[] names, int[] quantities) {
        for (int i = 0; i < names.length; i++) quantities[i] = inventory.getQuantity(names[i]);
    }
    private static LabSystem lab(int level, String[] names, int[][] requirements, String[] treatmentNames) {
        if (requirements.length != treatmentNames.length) throw new IllegalArgumentException("Recipe count mismatch");
        LabSystem lab = new LabSystem();
        lab.registerElement(new Element("Hydrogen", "H", 1, 1.008));
        lab.registerElement(new Element("Oxygen", "O", 8, 15.999));
        lab.registerElement(new Element("Sodium", "Na", 11, 22.990));
        lab.registerElement(new Element("Calcium", "Ca", 20, 40.078));
        lab.registerCompound(new Compound("Distilled Water", "H2O", "Liquid carrier"));
        lab.registerCompound(new Compound("Sodium Bicarbonate", "NaHCO3", "Mild base used in the fictional Level 1 chemistry puzzle"));
        lab.registerCompound(new Compound("Calcium Carbonate", "CaCO3", "Carbonate mineral used in the fictional Level 2 chemistry puzzle"));
        for (int i = 0; i < requirements.length; i++) {
            Map<String, Integer> needed = new LinkedHashMap<>();
            for (int index : requirements[i]) {
                if (index < 0 || index >= names.length) throw new IllegalArgumentException("Invalid ingredient index");
                needed.merge(names[index], 1, Integer::sum);
            }
            lab.registerRecipe(new Recipe(treatmentNames[i] + " Treatment", "L" + level + "-" + i,
                    "Fictional stabilization treatment", 1, i + 1, needed));
        }
        return lab;
    }
    private static Set<String> selectedNames(String[] names, boolean[] selected) {
        if (names.length != selected.length) throw new IllegalArgumentException("Selection length mismatch");
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i < selected.length; i++) if (selected[i]) result.add(names[i]);
        return result;
    }
    public static int detectRecipe(int level, String[] names, int[][] recipes,
                                   String[] treatmentNames, boolean[] selected) {
        LabSystem lab = lab(level, names, recipes, treatmentNames);
        Recipe found = lab.detectRecipe(selectedNames(names, selected));
        if (found == null) return -1;
        for (int i = 0; i < recipes.length; i++)
            if (found.getTreatmentKey().equals("L" + level + "-" + i)) return i;
        return -1;
    }
    public static int maxBatch(int level, String[] names, int[][] recipes,
                               String[] treatmentNames, int[] stock, int recipe) {
        if (recipe < 0 || recipe >= recipes.length) return 0;
        LabSystem lab = lab(level, names, recipes, treatmentNames);
        return lab.getRecipe("L" + level + "-" + recipe).maxCraftable(inventory(names, stock));
    }
    public static boolean craft(int level, String[] names, int[][] recipes,
                                String[] treatmentNames, int[] stock, int[] vials,
                                boolean[] unlocked, int recipe, int quantity) {
        if (recipe < 0 || recipe >= recipes.length || recipe >= vials.length || recipe >= unlocked.length) return false;
        LabSystem lab = lab(level, names, recipes, treatmentNames);
        Inventory inventory = inventory(names, stock);
        Set<String> selection = new LinkedHashSet<>(lab.getRecipe("L" + level + "-" + recipe).getIngredients().keySet());
        try {
            Potion result = lab.mixIngredients(selection, inventory, quantity);
            if (!result.getTreatmentKey().equals("L" + level + "-" + recipe)) return false;
            copyStock(inventory, names, stock);
            vials[recipe] = Math.addExact(vials[recipe], quantity);
            unlocked[recipe] = lab.getKnownRecipes().containsKey(result.getTreatmentKey());
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return false;
        }
    }
    public static int attackDamage(int level, int type) {
        return ZombieTypes.create(level, type, 1).attack();
    }
    public static int applyTreatment(int level, int enemyType, int hp, int potionType) {
        Zombie zombie = ZombieTypes.create(level, enemyType, hp);
        BattleSystem battle = new BattleSystem();
        battle.startBattle(new Scientist("Scientist", "Research"), zombie);
        Potion potion = new Potion("Treatment " + potionType, "L" + level + "-" + potionType,
                "Fictional cure", 1, Collections.emptyMap());
        battle.usePotion(potion);
        battle.endBattle();
        return zombie.getHealth();
    }
    public static Reward rewards(int level, int type) {
        return ZombieTypes.create(level, type, 1).dropRewards();
    }
        public static int[] award(int oldXp, int oldCoins, Reward reward) {
    Scientist scientist = GameManager.getInstance().getPlayer();

    if (scientist == null) {
        scientist = Scientist.forKey("nicole");
    }

    scientist.restoreProgress(
        oldXp,
        oldCoins,
        scientist.getResearchPoints()   
    );

    scientist.collectReward(reward);

    int newCoins = scientist.getCoins();

    System.out.println("[REWARD] Level reward executed!");
    System.out.println("[REWARD] Old coins: " + oldCoins);
    System.out.println("[REWARD] New coins: " + newCoins);

    if (newCoins > oldCoins) {
        System.out.println("[REWARD] Playing coin sound!");
        AudioManager.playCoinGain();
    }

    return new int[] {
        scientist.getExperience(),
        newCoins
        };
    }
    public static int buy(int coins, String[] names, int[] stock, int ingredient, int price) {
        if (ingredient < 0 || ingredient >= names.length) return -1;
        Scientist scientist = new Scientist("Scientist", "Research");
        scientist.restoreProgress(0, coins, 0);
        if (!scientist.spendCoins(price)) return -1;
        Inventory inventory = inventory(names, stock);
        if (!inventory.addItem(names[ingredient], 1)) return -1;
        copyStock(inventory, names, stock);
        return scientist.getCoins();
    }
    public static String scan(int level, int type, String sample) {
        Scientist scientist = GameManager.getInstance().getPlayer();
        if (scientist == null) scientist = Scientist.forKey("nicole");
        String observation = scientist.scanEnemy(ZombieTypes.create(level, type, 1));
        LabSystem lab = new LabSystem();
        lab.registerCompound(new Compound("Distilled Water", "H2O", "Liquid carrier"));
        lab.registerCompound(new Compound("Calcium Carbonate", "CaCO3", "Carbonate mineral"));
        return observation + " | " + lab.analyzeSample(sample);
    }
    /** Synchronize the existing battlefield coordinates with Character.move(). */
    public static void recordMovement(double oldX, double oldY, double newX, double newY) {
        Scientist player = GameManager.getInstance().getPlayer();
        if (player == null) return;
        player.setPosition(oldX, oldY);
        player.move(newX - oldX, newY - oldY);
    }
    /** Apply damage through the proposed Character.takeDamage() method. */
    public static double damagePlayer(double currentHealth, int damage) {
        Scientist player = GameManager.getInstance().getPlayer();
        if (player == null) return Math.max(0, currentHealth - damage);
        player.setHealth((int) Math.round(Math.max(0, Math.min(100, currentHealth))));
        player.takeDamage(damage);
        return player.getHealth();
    }
    /** Consume a Medkit via Inventory.useItem() and Scientist.useItem(). */
    public static double useMedkit(double currentHealth, String scientistName) {
        Scientist player = GameManager.getInstance().getPlayer();
        if (player == null) player = Scientist.forKey(scientistName);
        player.setHealth((int) Math.round(Math.max(0, Math.min(100, currentHealth))));
        player.getInventory().setQuantity("Medkit", 1);
        if (!player.useItem("Medkit")) return currentHealth;
        player.restoreHealth(healingBonus(scientistName));
        return player.getHealth();
    }
    /** Restore model state from existing save keys without changing the save format. */
    public static void restoreScientistProgress(int xp, int coins, int researchPoints,
                                                double health, double x, double y) {
        Scientist player = GameManager.getInstance().getPlayer();
        if (player == null) return;
        player.restoreProgress(xp, coins, researchPoints);
        player.setHealth((int) Math.round(Math.max(0, Math.min(100, health))));
        player.setPosition(x, y);
    }
    public static int craftingBonus(String scientistName) {
        return Scientist.forKey(scientistName).craftingBonusSeconds();
    }
    public static int healingBonus(String scientistName) {
        return Scientist.forKey(scientistName).healingBonus();
    }
    public static void startLevel(int level, String scientistName) {
        GameManager manager = GameManager.getInstance();
        manager.restoreUnlocks(level);
        Scientist chosen = Scientist.forKey(scientistName);
        if (manager.getPlayer() == null || !manager.getPlayer().getName().equals(chosen.getName()))
            manager.startGame(chosen);
        manager.changeLevel(level);
    }
    public static void completeLevel(int level) {
        GameManager manager = GameManager.getInstance();
        manager.restoreUnlocks(level);
        if (manager.getPlayer() == null) manager.startGame(Scientist.forKey("nicole"));
        manager.changeLevel(level);
        manager.completeCurrentLevel();
    }
}
