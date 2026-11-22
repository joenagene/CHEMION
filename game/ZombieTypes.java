package game;

/** Concrete subclasses provide inheritance + overridden polymorphic behavior. */
public final class ZombieTypes {
    private ZombieTypes() { }

    public static Zombie create(int level, int type, int currentHealth) {
        if (type < 0 || type > 3 || (level != 1 && level != 2 && level != 3))
            throw new IllegalArgumentException("Unsupported level or zombie type");
        Zombie z;
        if (level == 1) {
            switch (type) {
                case 0: z = new CommonZombie(); break;
                case 1: z = new RunnerZombie(); break;
                case 2: z = new CrawlerZombie(); break;
                default: z = new MutatedSurvivor(); break;
            }
        } else if (level == 2) {
            switch (type) {
                case 0: z = new AcidZombie(); break;
                case 1: z = new PatientZero(); break;
                case 2: z = new ToxicZombie(); break;
                default: z = new PlagueDoctor(); break;
            }
        } else {
            switch (type) {
                case 0: z = new ToxicBrute(); break;
                case 1: z = new VenomSpitter(); break;
                case 2: z = new ExplosiveZombie(); break;
                default: z = new ChemicalAbomination(); break;
            }
        }
        z.setHealth(currentHealth);
        return z;
    }
    private static class CommonZombie extends Zombie {
        CommonZombie() { super("Common Zombie", "Basic infection", "Low", "L1-0", 2); }
        @Override public int attack() { return 8; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 2); }
        @Override public Reward dropRewards() { return new Reward(10, 5, null, 0); }
    }
    private static class RunnerZombie extends Zombie {
        RunnerZombie() { super("Runner Zombie", "Rapid mutation", "Moderate", "L1-1", 3); }
        @Override public int attack() { return 8; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 2); }
        @Override public Reward dropRewards() { return new Reward(10, 5, null, 0); }
    }
    private static class CrawlerZombie extends Zombie {
        CrawlerZombie() { super("Crawler Zombie", "Low-profile mutation", "Moderate", "L1-2", 3); }
        @Override public int attack() { return 8; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 2); }
        @Override public Reward dropRewards() { return new Reward(10, 5, null, 0); }
    }
    private static class MutatedSurvivor extends Zombie {
        MutatedSurvivor() { super("Mutated Survivor", "Compound mutation", "High", "L1-3", 10); }
        @Override public int attack() { return 17; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 2); }
        @Override public Reward dropRewards() { return new Reward(40, 25, null, 0); }
    }
    private static class AcidZombie extends Zombie {
        AcidZombie() { super("Acid Zombie", "Acid barrier", "Strong acidity", "L2-0", 1); }
        @Override public int attack() { return 8; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 1); }
        @Override public Reward dropRewards() { return new Reward(20, 8, null, 0); }
    }
    private static class PatientZero extends Zombie {
        PatientZero() { super("Patient Zero", "Ionic contamination", "Metal ions", "L2-1", 2); }
        @Override public int attack() { return 8; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 1); }
        @Override public Reward dropRewards() { return new Reward(30, 11, null, 0); }
    }
    private static class ToxicZombie extends Zombie {
        ToxicZombie() { super("Toxic Zombie", "Airborne contamination", "Toxic residues", "L2-2", 2); }
        @Override public int attack() { return 11; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 1); }
        @Override public Reward dropRewards() { return new Reward(40, 14, null, 0); }
    }
    private static class PlagueDoctor extends Zombie {
        PlagueDoctor() { super("Plague Doctor", "Multi-barrier mutation", "Layered defenses", "L2-3", 4); }
        @Override public int attack() { return 15; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 1); }
        @Override public Reward dropRewards() { return new Reward(120, 50, null, 0); }
    }
    private static class ToxicBrute extends Zombie {
        ToxicBrute() { super("Toxic Brute", "Acidic industrial sludge", "Moderate", "L3-0", 1); }
        @Override public int attack() { return 12; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 1); }
        @Override public Reward dropRewards() { return new Reward(24, 14, null, 0); }
    }
    private static class VenomSpitter extends Zombie {
        VenomSpitter() { super("Venom Spitter", "Airborne corrosive droplets", "Challenging", "L3-1", 2); }
        @Override public int attack() { return 9; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 1); }
        @Override public Reward dropRewards() { return new Reward(35, 22, null, 0); }
    }
    private static class ExplosiveZombie extends Zombie {
        ExplosiveZombie() { super("Explosive Zombie", "Volatile industrial residue", "Hard", "L3-2", 2); }
        @Override public int attack() { return 15; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 1); }
        @Override public Reward dropRewards() { return new Reward(45, 28, null, 0); }
    }
    private static class ChemicalAbomination extends Zombie {
        ChemicalAbomination() { super("Chemical Abomination", "Layered reactor mutation", "Boss", "L3-3", 5); }
        @Override public int attack() { return 18; }
        @Override public int receiveTreatment(Potion p) { return applyMatchingTreatment(p, 1); }
        @Override public Reward dropRewards() { return new Reward(150, 65, null, 0); }
    }
}
