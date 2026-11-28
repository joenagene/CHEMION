package game;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


//items, capacity; addItem(), removeItem(), useItem().
public class Inventory {
    private final Map<String, Integer> items = new LinkedHashMap<>();
    private final int capacity;

    public Inventory() { this(Integer.MAX_VALUE); }
    public Inventory(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
    }
    public int getCapacity() { return capacity; }
    public int getQuantity(String item) { return items.getOrDefault(requireName(item), 0); }
    public Map<String, Integer> getItems() { return Collections.unmodifiableMap(items); }
    public int totalItems() { return items.values().stream().mapToInt(Integer::intValue).sum(); }
    public boolean addItem(String item, int quantity) {
        item = requireName(item);
        requirePositive(quantity);
        if ((long) totalItems() + quantity > capacity) return false;
        items.put(item, Math.addExact(getQuantity(item), quantity));
        return true;
    }
    public boolean removeItem(String item, int quantity) {
        item = requireName(item);
        requirePositive(quantity);
        int current = getQuantity(item);
        if (current < quantity) return false;
        if (current == quantity) items.remove(item);
        else items.put(item, current - quantity);
        return true;
    }
    public boolean useItem(String item) { return removeItem(item, 1); }
    public void setQuantity(String item, int quantity) {
        item = requireName(item);
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        int old = getQuantity(item);
        if ((long) totalItems() - old + quantity > capacity) throw new IllegalArgumentException("Inventory is full");
        if (quantity == 0) items.remove(item); else items.put(item, quantity);
    }
    static String requireName(String item) {
        if (item == null || item.trim().isEmpty()) throw new IllegalArgumentException("Item name required");
        return item.trim();
    }
    static void requirePositive(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1");
    }
}
