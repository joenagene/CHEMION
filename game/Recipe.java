package game;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Recipe {
    private final String name, treatmentKey, effect;
    private final int strength, difficulty;
    private final Map<String, Integer> ingredients;
    public Recipe(String name, String treatmentKey, String effect, int strength,
                  int difficulty, Map<String, Integer> ingredients) {
        this.name = Inventory.requireName(name);
        this.treatmentKey = Inventory.requireName(treatmentKey);
        this.effect = Inventory.requireName(effect);
        if (strength < 1 || difficulty < 1 || ingredients == null || ingredients.isEmpty())
            throw new IllegalArgumentException("Invalid recipe");
        this.strength = strength; this.difficulty = difficulty;
        this.ingredients = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            Inventory.requireName(entry.getKey());
            Inventory.requirePositive(entry.getValue());
            this.ingredients.put(entry.getKey(), entry.getValue());
        }
    }
    public String getName() { return name; }
    public String getTreatmentKey() { return treatmentKey; }
    public int getDifficulty() { return difficulty; }
    public Map<String, Integer> getIngredients() { return Collections.unmodifiableMap(ingredients); }
    public boolean checkIngredients(Inventory inventory, int quantity) {
        Inventory.requirePositive(quantity);
        if (inventory == null) throw new IllegalArgumentException("Inventory required");
        for (Map.Entry<String, Integer> e : ingredients.entrySet())
            if (inventory.getQuantity(e.getKey()) < (long) e.getValue() * quantity) return false;
        return true;
    }
    public int maxCraftable(Inventory inventory) {
        if (inventory == null) throw new IllegalArgumentException("Inventory required");
        int max = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : ingredients.entrySet())
            max = Math.min(max, inventory.getQuantity(e.getKey()) / e.getValue());
        return max;
    }
    public Potion craftPotion(Inventory inventory, int quantity) {
        if (!checkIngredients(inventory, quantity)) throw new IllegalStateException("Not enough ingredients");
        for (Map.Entry<String, Integer> e : ingredients.entrySet())
            inventory.removeItem(e.getKey(), e.getValue() * quantity);
        return new Potion(name, treatmentKey, effect, strength, ingredients);
    }
}
