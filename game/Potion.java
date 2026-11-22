package game;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fictional in-game treatment; not a real medical formula. */
public class Potion {
    private final String name, treatmentKey, effect;
    private final int strength;
    private final Map<String, Integer> ingredients;
    public Potion(String name, String treatmentKey, String effect, int strength,
                  Map<String, Integer> ingredients) {
        this.name = Inventory.requireName(name);
        this.treatmentKey = Inventory.requireName(treatmentKey);
        this.effect = Inventory.requireName(effect);
        if (strength < 1) throw new IllegalArgumentException("Strength must be positive");
        this.strength = strength;
        this.ingredients = Collections.unmodifiableMap(new LinkedHashMap<>(ingredients));
    }
    public String getName() { return name; }
    public String getTreatmentKey() { return treatmentKey; }
    public String getEffect() { return effect; }
    public int getStrength() { return strength; }
    public Map<String, Integer> getIngredients() { return ingredients; }
    public int use(Zombie target) { return applyEffect(target); }
    public int applyEffect(Zombie target) {
        if (target == null) throw new IllegalArgumentException("Target required");
        return target.receiveTreatment(this);
    }
    public String getDescription() { return name + ": " + effect + " (strength " + strength + ")"; }
}
