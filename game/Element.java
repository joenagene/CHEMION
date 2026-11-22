package game;

/** Chemistry journal element model. */
public class Element {
    private final String name, symbol;
    private final int atomicNumber;
    private final double atomicMass;
    public Element(String name, String symbol, int atomicNumber, double atomicMass) {
        this.name = Inventory.requireName(name);
        this.symbol = Inventory.requireName(symbol);
        if (atomicNumber < 1 || atomicMass <= 0 || !Double.isFinite(atomicMass))
            throw new IllegalArgumentException("Invalid atomic properties");
        this.atomicNumber = atomicNumber; this.atomicMass = atomicMass;
    }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public int getAtomicNumber() { return atomicNumber; }
    public double getAtomicMass() { return atomicMass; }
    public String getDetails() {
        return name + " (" + symbol + "), atomic number " + atomicNumber + ", mass " + atomicMass;
    }
    public int compareProperties(Element other) {
        if (other == null) throw new IllegalArgumentException("Other element required");
        return Double.compare(atomicMass, other.atomicMass);
    }
}
