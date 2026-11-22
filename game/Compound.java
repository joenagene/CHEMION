package game;

/** Chemistry journal compound model. */
public class Compound {
    private final String name, formula, properties;
    public Compound(String name, String formula, String properties) {
        this.name = Inventory.requireName(name);
        this.formula = Inventory.requireName(formula);
        this.properties = Inventory.requireName(properties);
    }
    public String getName() { return name; }
    public String getFormula() { return formula; }
    public String getDetails() { return name + " (" + formula + "): " + properties; }
}
