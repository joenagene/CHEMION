package game;

public final class ChemistryInfo {
    private ChemistryInfo() {}
    public static String[] get(String name) {
        switch (name) {
            case "Sodium Bicarbonate": return new String[] {"NaHCO3", "Na(11) H(1) C(6) O(8)", "Ionic/covalent", "Mildly basic", "Compound"};
            case "Distilled Water": return new String[] {"H2O", "H(1) O(8)", "Polar covalent", "Neutral*", "Compound"};
            case "Sodium Chloride": return new String[] {"NaCl", "Na(11) Cl(17)", "Ionic", "Near neutral", "Compound"};
            case "Citric Acid": return new String[] {"C6H8O7", "C(6) H(1) O(8)", "Covalent", "Acidic", "Compound"};
            case "Ascorbic Acid": return new String[] {"C6H8O6", "C(6) H(1) O(8)", "Covalent", "Acidic", "Compound"};
            case "Sodium Citrate": return new String[] {"Na3C6H5O7", "Na(11) C(6) H(1) O(8)", "Ionic/covalent", "Mildly basic", "Compound"};
            case "Activated Carbon": return new String[] {"C (main)", "C(6)", "Covalent network", "Variable", "Carbon material"};
            case "Crystal Sample": return new String[] {"Unknown", "Unknown", "Unknown", "Unknown", "Unidentified"};
            case "Calcium Carbonate": return new String[] {"CaCO3", "Ca(20) C(6) O(8)", "Ionic/covalent", "Basic suspension", "Compound"};
            case "Disodium EDTA": return new String[] {"C10H14N2Na2O8", "C(6) H(1) N(7) Na(11) O(8)", "Ionic/covalent", "Often acidic", "Compound"};
            case "Sodium Thiosulfate": return new String[] {"Na2S2O3", "Na(11) S(16) O(8)", "Ionic/covalent", "Near neutral", "Compound"};
            case "Potassium Iodide": return new String[] {"KI", "K(19) I(53)", "Ionic", "Near neutral", "Compound"};
            case "Magnesium Sulfate": return new String[] {"MgSO4", "Mg(12) S(16) O(8)", "Ionic/covalent", "Often mildly acidic", "Compound"};
            case "Zeolite": return new String[] {"Variable", "Si(14) Al(13) O(8) + ions", "Network/ionic", "Variable", "Mineral material"};
            case "Silica Gel": return new String[] {"SiO2 (main)", "Si(14) O(8)", "Covalent network", "Variable", "Porous material"};
            default: return new String[] {"Unknown", "Unknown", "Unknown", "Unknown", "Unidentified"};
        }
    }
}
