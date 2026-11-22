import game.*;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

/** Run with: java OOPComplianceTest (does not open the game UI or touch saved progress). */
public class OOPComplianceTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void expectInvalid(Runnable task) {
        boolean rejected = false;
        try { task.run(); }
        catch (IllegalArgumentException | IllegalStateException e) { rejected = true; }
        check(rejected, "Invalid input was accepted");
    }
    public static void main(String[] args) throws Exception {
        check(Modifier.isAbstract(game.Character.class.getModifiers()), "Character must be abstract");
        check(Modifier.isAbstract(Zombie.class.getModifiers()), "Zombie must be abstract");
        check(game.Character.class.isAssignableFrom(Scientist.class), "Scientist must inherit Character");
        Scientist nicole = Scientist.forKey("nicole");
        nicole.setPosition(10, 20); nicole.move(3, -2);
        check(nicole.getX() == 13 && nicole.getY() == 18, "move() not functional");
        nicole.takeDamage(20); check(nicole.getHealth() == 80, "takeDamage() not functional");
        nicole.getInventory().addItem("Medkit", 1);
        check(nicole.useItem("Medkit") && nicole.getHealth() == 100, "useItem() not functional");
        check(nicole.scanEnemy(ZombieTypes.create(1, 0, 2)).contains("Common Zombie"), "scanEnemy() not functional");
        check(nicole.getResearchPoints() == 10, "Nicole research ability missing");
        Scientist dutch = Scientist.forKey("dutch");
        Scientist gene = Scientist.forKey("gene");
        Scientist sebastian = Scientist.forKey("sebastian");
        Scientist elena = Scientist.forKey("elena");
        check(dutch.craftingBonusSeconds() == 6 && elena.healingBonus() == 12, "Distinct character abilities missing");
        gene.collectReward(new Reward(10, 0, null, 0));
        sebastian.collectReward(new Reward(0, 5, null, 0));
        check(gene.getExperience() == 15 && sebastian.getCoins() == 7, "Reward abilities missing");

        Inventory inventory = new Inventory(10);
        check(inventory.addItem("Water", 4), "addItem failed");
        check(inventory.removeItem("Water", 2), "removeItem failed");
        check(inventory.useItem("Water") && inventory.getQuantity("Water") == 1, "useItem failed");
        check(!inventory.removeItem("Water", 2), "Insufficient item removal accepted");
        expectInvalid(() -> inventory.addItem("Water", -1));
        expectInvalid(() -> inventory.setQuantity("Water", -1));
        check(!inventory.addItem("Water", 10), "Capacity not enforced");

        Element hydrogen = new Element("Hydrogen", "H", 1, 1.008);
        Element oxygen = new Element("Oxygen", "O", 8, 15.999);
        Compound water = new Compound("Distilled Water", "H2O", "Liquid carrier");
        check(hydrogen.compareProperties(oxygen) < 0 && water.getFormula().equals("H2O"), "Element/Compound incorrect");
        LabSystem lab = new LabSystem();
        lab.registerElement(hydrogen); lab.registerCompound(water);
        check(lab.analyzeSample("Distilled Water").contains("H2O"), "analyzeSample() not functional");
        Map<String, Integer> ingredients = new LinkedHashMap<>();
        ingredients.put("Water", 1); ingredients.put("Powder", 2);
        Recipe recipe = new Recipe("Test Treatment", "TEST", "Fictional effect", 1, 2, ingredients);
        lab.registerRecipe(recipe);
        Inventory supplies = new Inventory();
        supplies.addItem("Water", 3); supplies.addItem("Powder", 5);
        check(recipe.checkIngredients(supplies, 2), "checkIngredients() failed");
        check(recipe.maxCraftable(supplies) == 2, "Stoichiometric quantity check failed");
        check(lab.getKnownRecipes().isEmpty(), "Recipe revealed before crafting");
        Potion potion = lab.mixIngredients(new LinkedHashSet<>(ingredients.keySet()), supplies, 2);
        check(potion.getDescription().contains("Test Treatment"), "Potion not crafted");
        check(supplies.getQuantity("Water") == 1 && supplies.getQuantity("Powder") == 1, "Inventory not consumed");
        check(lab.getKnownRecipes().containsKey("TEST"), "unlockRecipe() failed");
        expectInvalid(() -> lab.mixIngredients(Collections.singleton("Wrong"), supplies, 1));

        Zombie acid = ZombieTypes.create(2, 0, 1);
        Zombie doctor = ZombieTypes.create(2, 3, 4);
        check(acid.attack() != doctor.attack(), "Polymorphic attack() missing");
        BattleSystem battle = new BattleSystem();
        battle.startBattle(nicole, doctor);
        Potion wrong = new Potion("Wrong", "L2-0", "Fictional", 1, Collections.emptyMap());
        Potion right = new Potion("Right", "L2-3", "Fictional", 1, Collections.emptyMap());
        check(battle.usePotion(wrong) == 0 && doctor.getHealth() == 4, "Wrong treatment must not work");
        check(battle.usePotion(right) == 1 && doctor.getHealth() == 3, "Polymorphic receiveTreatment() failed");
        battle.endBattle();
        check(!battle.isActive(), "endBattle() failed");
        check(doctor.dropRewards().getCoins() == 50, "dropRewards() failed");
        check(GameplayBridge.applyTreatment(1, 0, 2, 0) == 0, "Level 1 treatment bridge failed");
        check(GameplayBridge.applyTreatment(2, 0, 1, 1) == 1, "Wrong Level 2 treatment accepted");
        check(GameplayBridge.applyTreatment(2, 0, 1, 0) == 0, "Correct Level 2 treatment failed");

        GameManager manager = GameManager.getInstance();
        manager.resetProgress();
        manager.startGame(nicole);
        check(manager.getCurrentLevel() == 1, "startGame() failed");
        expectInvalid(() -> manager.changeLevel(2));
        manager.completeCurrentLevel();
        check(manager.getLevel(2).isUnlocked(), "unlockLevel() failed");
        manager.changeLevel(2);
        check(manager.getCurrentLevel() == 2, "changeLevel() failed");
        final boolean[] saved = {false};
        manager.saveProgress(() -> saved[0] = true);
        check(saved[0], "saveProgress() failed");
        manager.endGame();
        check("MENU".equals(manager.getGameState()), "endGame() failed");

        manager.resetProgress();
        manager.startGame(Scientist.forKey("elena"));
        GameplayBridge.recordMovement(10, 10, 13, 9);
        check(manager.getPlayer().getX() == 13 && manager.getPlayer().getY() == 9,
            "Battlefield movement did not use Character.move()");
        check(GameplayBridge.damagePlayer(80, 8) == 72,
            "Battlefield damage did not use Character.takeDamage()");
        check(GameplayBridge.useMedkit(30, "elena") == 77,
            "Battlefield medkit did not use Scientist.useItem()");
        GameplayBridge.restoreScientistProgress(44, 20, 7, 80, 25, 30);
        check(manager.getPlayer().getExperience() == 44
            && manager.getPlayer().getCoins() == 20
            && manager.getPlayer().getResearchPoints() == 7
            && manager.getPlayer().getHealth() == 80,
            "Save model restoration failed");
        System.out.println("PASS: " + checks + " checks - proposed classes, OOP, gameplay, validation, and progression");
    }
}
