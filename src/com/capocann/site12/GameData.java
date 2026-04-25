package com.capocann.site12;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameData {
    // TUNING LABELS: adjust these globally as needed.
    public static final int BASE_MAX_THIRST = 100;
    public static final int BASE_MAX_HUNGER = 100;
    public static final int BASE_MAX_SANITY = 100;
    public static final int BLEED_DOT_PERCENT = 20;
    public static final int FIRE_DOT_PERCENT = 5;
    public static final int FIRE_DAMAGE_BOOST_PERCENT = 10;
    public static final int MORALE_STACK_MAX = 6;
    public static final int MORALE_STACK_DEBUFF_PERCENT = 5;

    public enum StatusEffect {
        BLEED,
        CRIPPLE,
        FIRE,
        MORALE
    }

    public static class InventoryEntry {
        private String itemId;
        private int quantity;

        public InventoryEntry(String itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }

        public String getItemId() {
            return itemId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = Math.max(0, quantity);
        }
    }

    public static class CharacterStats {
        private final String characterName;
        private int maxHealth;
        private int currentHealth;
        private int maxThirst = BASE_MAX_THIRST;
        private int currentThirst = BASE_MAX_THIRST;
        private int maxHunger = BASE_MAX_HUNGER;
        private int currentHunger = BASE_MAX_HUNGER;
        private int maxSanity = BASE_MAX_SANITY;
        private int currentSanity = BASE_MAX_SANITY;
        private final String aliveImagePath;
        private final String almostdeadImagePath;
        private static final int HEALTH_THRESHOLD = 30; // Percentage threshold to show almostdead sprite
        private final EnumMap<StatusEffect, Integer> statusStacks = new EnumMap<>(StatusEffect.class);

        public CharacterStats(String characterName, int maxHealth) {
            this.characterName = characterName;
            this.maxHealth = maxHealth;
            this.currentHealth = maxHealth;

            String normalizedName = characterName.toLowerCase();
            String characterFolder = getCharacterFolder(normalizedName);

            String alivePrimary = "assets/res/Characters/" + characterFolder + "/" + normalizedName + "-AliveDay.png";
            String almostdeadPrimary = "assets/res/Characters/" + characterFolder + "/" + normalizedName + "-AlmostdeadDay.png";

            String legacyAlive = "assets/res/Characters/" + characterFolder + "/Alive" + capitalizeFirst(normalizedName) + ".png";
            String legacyAlmostdead = "assets/res/Characters/" + characterFolder + "/Almostdead" + capitalizeFirst(normalizedName) + ".png";

            if ("terry".equals(normalizedName)) {
                this.aliveImagePath = resolveExistingPath(
                    alivePrimary,
                    "assets/res/Characters/terry/raphaela-AliveDay.png",
                    legacyAlive
                );
                this.almostdeadImagePath = resolveExistingPath(
                    almostdeadPrimary,
                    "assets/res/Characters/terry/raphaela-AlmostdeadDay.png",
                    legacyAlmostdead
                );
            } else {
                this.aliveImagePath = resolveExistingPath(alivePrimary, legacyAlive);
                this.almostdeadImagePath = resolveExistingPath(almostdeadPrimary, legacyAlmostdead);
            }
        }

        private String capitalizeFirst(String str) {
            if (str == null || str.isEmpty()) return str;
            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }

        private String getCharacterFolder(String normalizedName) {
            return switch (normalizedName) {
                case "kriegs" -> "Kriegs";
                case "azrael" -> "Azrael";
                case "gambit" -> "Gambit";
                case "lazarus" -> "Lazarus";
                case "raphaela" -> "raphaela";
                case "terry" -> "terry";
                default -> capitalizeFirst(normalizedName);
            };
        }

        private String resolveExistingPath(String primaryPath, String... fallbackPaths) {
            if (new File(primaryPath).exists()) {
                return primaryPath;
            }

            for (String fallback : fallbackPaths) {
                if (fallback != null && !fallback.isBlank() && new File(fallback).exists()) {
                    return fallback;
                }
            }

            return primaryPath;
        }

        public String getCharacterName() {
            return characterName;
        }

        public int getMaxHealth() {
            return maxHealth;
        }

        public int getCurrentHealth() {
            return currentHealth;
        }

        public int getMaxThirst() {
            return maxThirst;
        }

        public int getCurrentThirst() {
            return currentThirst;
        }

        public int getMaxHunger() {
            return maxHunger;
        }

        public int getCurrentHunger() {
            return currentHunger;
        }

        public int getMaxSanity() {
            return maxSanity;
        }

        public int getCurrentSanity() {
            return currentSanity;
        }

        public void setCurrentHealth(int health) {
            this.currentHealth = Math.max(0, Math.min(health, maxHealth));
        }

        public void setCurrentThirst(int thirst) {
            this.currentThirst = Math.max(0, Math.min(thirst, maxThirst));
        }

        public void setCurrentHunger(int hunger) {
            this.currentHunger = Math.max(0, Math.min(hunger, maxHunger));
        }

        public void setCurrentSanity(int sanity) {
            this.currentSanity = Math.max(0, Math.min(sanity, maxSanity));
            if (this.currentSanity == 0) {
                this.currentHealth = 0;
            }
        }

        public void takeDamage(int damage) {
            setCurrentHealth(currentHealth - damage);
        }

        public void heal(int amount) {
            setCurrentHealth(currentHealth + amount);
        }

        public int getHealthPercentage() {
            return (int) ((currentHealth / (double) maxHealth) * 100);
        }

        public String getCurrentImagePath() {
            if (getHealthPercentage() <= HEALTH_THRESHOLD) {
                return almostdeadImagePath;
            }
            return aliveImagePath;
        }

        public String getAliveImagePath() {
            return aliveImagePath;
        }

        public String getAlmostdeadImagePath() {
            return almostdeadImagePath;
        }

        public boolean isAlive() {
            return currentHealth > 0;
        }

        public boolean isAlmostDead() {
            return getHealthPercentage() <= HEALTH_THRESHOLD;
        }

        public int getStatusStacks(StatusEffect effect) {
            return statusStacks.getOrDefault(effect, 0);
        }

        public void addStatusStacks(StatusEffect effect, int amount) {
            if (amount <= 0) {
                return;
            }

            int current = statusStacks.getOrDefault(effect, 0);
            int max = (effect == StatusEffect.MORALE) ? MORALE_STACK_MAX : 99;
            statusStacks.put(effect, Math.min(max, current + amount));
        }

        public void clearStatus(StatusEffect effect) {
            statusStacks.remove(effect);
        }

        public void applyEndTurnEffects() {
            if (getStatusStacks(StatusEffect.BLEED) > 0) {
                int bleedDamage = Math.max(1, (maxHealth * BLEED_DOT_PERCENT) / 100);
                takeDamage(bleedDamage);
                changeStatusStacks(StatusEffect.BLEED, -1);
            }

            if (getStatusStacks(StatusEffect.FIRE) > 0) {
                int fireDamage = Math.max(1, (maxHealth * FIRE_DOT_PERCENT) / 100);
                takeDamage(fireDamage);
                changeStatusStacks(StatusEffect.FIRE, -1);
            }

            if (getStatusStacks(StatusEffect.CRIPPLE) > 0) {
                changeStatusStacks(StatusEffect.CRIPPLE, -1);
            }
        }

        public double getMoraleMultiplier() {
            int moraleStacks = getStatusStacks(StatusEffect.MORALE);
            return Math.max(0.1, 1.0 - (moraleStacks * (MORALE_STACK_DEBUFF_PERCENT / 100.0)));
        }

        private void changeStatusStacks(StatusEffect effect, int signedDelta, boolean internal) {
            int current = statusStacks.getOrDefault(effect, 0);
            int next = current + signedDelta;
            if (next <= 0) {
                statusStacks.remove(effect);
            } else {
                int max = (effect == StatusEffect.MORALE) ? MORALE_STACK_MAX : 99;
                statusStacks.put(effect, Math.min(max, next));
            }
        }

        private void changeStatusStacks(StatusEffect effect, int signedDelta) {
            changeStatusStacks(effect, signedDelta, true);
        }
    }

    private static final GameData INSTANCE = new GameData(true);

    public static GameData getInstance() {
        return INSTANCE;
    }

    private final List<InventoryEntry> inventoryItems = new ArrayList<>();
    private final Map<String, CharacterStats> characterStats = new HashMap<>();

    public GameData() {
        this(false);
    }

    private GameData(boolean singletonInit) {
        // Initialize default characters
        initializeCharacters();
    }

    private void initializeCharacters() {
        characterStats.put("kriegs", new CharacterStats("kriegs", 100));
        characterStats.put("azrael", new CharacterStats("azrael", 120));
        characterStats.put("gambit", new CharacterStats("gambit", 110));
        characterStats.put("lazarus", new CharacterStats("lazarus", 95));
        characterStats.put("raphaela", new CharacterStats("raphaela", 115));
        characterStats.put("terry", new CharacterStats("terry", 105));
    }

    // Inventory methods
    public List<InventoryEntry> getInventoryItems() {
        return Collections.unmodifiableList(inventoryItems);
    }

    public void setInventoryItems(List<InventoryEntry> items) {
        inventoryItems.clear();
        if (items != null) {
            inventoryItems.addAll(items);
        }
    }

    public void addInventoryItem(String itemId, int quantity) {
        if (itemId == null || itemId.isBlank() || quantity <= 0) {
            return;
        }

        for (InventoryEntry entry : inventoryItems) {
            if (entry.getItemId().equalsIgnoreCase(itemId)) {
                entry.setQuantity(entry.getQuantity() + quantity);
                return;
            }
        }

        inventoryItems.add(new InventoryEntry(itemId, quantity));
    }

    // Character stats methods
    public CharacterStats getCharacterStats(String characterName) {
        return characterStats.get(characterName.toLowerCase());
    }

    public Map<String, CharacterStats> getAllCharacterStats() {
        return Collections.unmodifiableMap(characterStats);
    }

    public void addCharacterStats(String characterName, CharacterStats stats) {
        characterStats.put(characterName.toLowerCase(), stats);
    }

    public boolean reduceCharacterSanity(String characterName, int amount, boolean whileScavenging) {
        CharacterStats stats = getCharacterStats(characterName);
        if (stats == null || amount <= 0) {
            return false;
        }

        int beforeSanity = stats.getCurrentSanity();
        stats.setCurrentSanity(beforeSanity - amount);

        if (beforeSanity > 0 && stats.getCurrentSanity() == 0) {
            if (whileScavenging) {
                stats.addStatusStacks(StatusEffect.MORALE, 1);
            }
            addInventoryItem("itm_suicide_note", 1);
            handleCharacterDeath(characterName);
            return true;
        }

        return false;
    }

    public void handleCharacterDeath(String characterName) {
        String deadName = characterName == null ? "" : characterName.toLowerCase();
        CharacterStats dead = getCharacterStats(deadName);
        if (dead != null) {
            dead.setCurrentHealth(0);
        }

        int moraleGain = "terry".equals(deadName) ? 4 : 1;
        for (Map.Entry<String, CharacterStats> entry : characterStats.entrySet()) {
            if (entry.getKey().equals(deadName)) {
                continue;
            }
            if (entry.getValue().isAlive()) {
                entry.getValue().addStatusStacks(StatusEffect.MORALE, moraleGain);
            }
        }
    }
}
