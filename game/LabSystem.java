package game;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

//knownRecipes, equipmentLevel, researchData; analyzeSample(), mixIngredients(), unlockRecipe(). 
public class LabSystem {
    private final Map<String, Recipe> recipes = new LinkedHashMap<>();
    private final Set<String> knownRecipes = new LinkedHashSet<>();
    private final Map<String, String> researchData = new LinkedHashMap<>();
    private final Map<String, Element> elements = new LinkedHashMap<>();
    private final Map<String, Compound> compounds = new LinkedHashMap<>();
    private int equipmentLevel = 1;
    public int getEquipmentLevel() { return equipmentLevel; }
    public void upgradeEquipment() { equipmentLevel++; }
    public void registerRecipe(Recipe recipe) {
        if (recipe == null) throw new IllegalArgumentException("Recipe required");
        recipes.put(recipe.getTreatmentKey(), recipe);
    }
    public Recipe getRecipe(String key) { return recipes.get(key); }
    public Map<String, Recipe> getKnownRecipes() {
        Map<String, Recipe> known = new LinkedHashMap<>();
        for (String key : knownRecipes) known.put(key, recipes.get(key));
        return Collections.unmodifiableMap(known);
    }
    public void registerElement(Element element) { elements.put(element.getName(), element); }
    public void registerCompound(Compound compound) { compounds.put(compound.getName(), compound); }
    public String analyzeSample(String sample) {
        sample = Inventory.requireName(sample);
        Element e = elements.get(sample);
        Compound c = compounds.get(sample);
        String result = e != null ? e.getDetails() : c != null ? c.getDetails()
            : "Sample recorded: " + sample + ". Review chemistry clues before crafting.";
        researchData.put(sample, result);
        return result;
    }
    public Map<String, String> getResearchData() { return Collections.unmodifiableMap(researchData); }
    public Recipe detectRecipe(Set<String> selectedIngredients) {
        if (selectedIngredients == null) throw new IllegalArgumentException("Selection required");
        for (Recipe recipe : recipes.values())
            if (recipe.getIngredients().keySet().equals(selectedIngredients)) return recipe;
        return null;
    }
    public Potion mixIngredients(Set<String> selectedIngredients, Inventory inventory, int quantity) {
        Recipe recipe = detectRecipe(selectedIngredients);
        if (recipe == null) throw new IllegalArgumentException("Invalid ingredient combination");
        Potion potion = recipe.craftPotion(inventory, quantity);
        unlockRecipe(recipe.getTreatmentKey()); // Reveal only after successful crafting.
        return potion;
    }
    public void unlockRecipe(String treatmentKey) {
        if (!recipes.containsKey(treatmentKey)) throw new IllegalArgumentException("Unknown recipe: " + treatmentKey);
        knownRecipes.add(treatmentKey);
    }
}
