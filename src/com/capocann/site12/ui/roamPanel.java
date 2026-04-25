package com.capocann.site12.ui;

import javax.swing.*;
import javax.imageio.ImageIO;

import com.capocann.site12.GameData;
import com.capocann.site12.Main;
import com.capocann.site12.io.InventoryCsvReader;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class roamPanel extends JPanel {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private static final int TILE_CELL_WIDTH = 78;
    private static final int TILE_CELL_HEIGHT = 72;
    private static final int TILE_GRID_PADDING = 10;
    private static final Color PANEL_BORDER = new Color(66, 66, 66);
    private static final Color PANEL_FILL = new Color(18, 18, 18);
    private static final Color CARD_FILL = new Color(35, 35, 35);
    private static final Color TILE_UNEXPLORED = new Color(45, 45, 45);
    private static final Color TILE_EXPLORED = new Color(80, 95, 120);
    private static final Color TILE_BORDER = new Color(25, 25, 25);
    private static final Color PLAYER_MARKER = new Color(255, 214, 102);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_SECONDARY = new Color(220, 220, 220);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 22);
    private static final Font CARD_TITLE_FONT = new Font("SansSerif", Font.BOLD, 15);
    private static final Font CARD_BODY_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final String[] CHARACTER_ORDER = {"kriegs", "azrael", "gambit", "lazarus", "raphaela", "terry"};
    private static final int SCAVENGE_THIRST_LOSS = 2;
    private static final int SCAVENGE_HUNGER_LOSS = 2;
    private static final int SCAVENGE_SANITY_LOSS_MIN = 1;
    private static final int SCAVENGE_SANITY_LOSS_MAX = 4;
    private static final Dimension BACKPACK_PANEL_SIZE = new Dimension(430, 230);

    private final Image backgroundImage = new ImageIcon("assets/Backgrounds/background.png").getImage();
    private final GameData gameData = GameData.getInstance();
    private final Main main;

    private final JTextArea backpackSummaryArea = new JTextArea();
    private final JPanel portraitRosterPanel = new JPanel();
    private final JLabel infoPanelTitle = new JLabel("Backpack Summary", SwingConstants.LEFT);
    private final CardLayout infoCardLayout = new CardLayout();
    private final JPanel infoCardHost = new JPanel(infoCardLayout);
    private final JPanel infoCardWrapper = new JPanel(new GridBagLayout());
    private final JPanel backpackListPanel = new JPanel();
    private final JPanel tileExplorerPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintTileExplorer((Graphics2D) g);
        }
    };
    private final Map<String, Integer> inventoryCounts = new HashMap<>();
    private final List<String> eventLogs = new ArrayList<>();
    private final Map<String, Integer> lootRarityWeights = new LinkedHashMap<>();
    private final Map<String, List<LootEntry>> lootByRarity = new LinkedHashMap<>();
    private final Map<String, LootEntry> lootById = new HashMap<>();
    private final Random random = new Random();

    private boolean[][] exploredTiles;
    private int mapRows = 6;
    private int mapCols = 8;
    private int playerRow = 0;
    private int playerCol = 0;
    private int exitRow = mapRows - 1;
    private int exitCol = mapCols - 1;
    private double combatChance = 0.35;
    private double lootChance = 0.40;
    private int noLootStreak = 0;

    private enum InfoMode { BACKPACK, LOGS }
    private InfoMode infoMode = InfoMode.BACKPACK;

    private static class LootEntry {
        private final String rarity;
        private final String displayName;
        private final String itemId;
        private final int minQty;
        private final int maxQty;
        private final String description;
        private final String effectText;

        private LootEntry(String rarity, String displayName, String itemId, int minQty, int maxQty, String description) {
            this.rarity = rarity;
            this.displayName = displayName;
            this.itemId = itemId;
            this.minQty = minQty;
            this.maxQty = maxQty;
            this.description = description;
            String extracted = extractEffectText(description);
            this.effectText = extracted.isBlank() ? description : extracted;
        }
    }

    public roamPanel(Main main) {
        this.main = main;
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setLayout(new GridBagLayout());
        loadMapConfig();
        loadInventoryData();
        loadLootTableFromCsv();
        initializeExplorationState();

        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        GridBagConstraints leftConstraints = new GridBagConstraints();
        leftConstraints.gridx = 0;
        leftConstraints.gridy = 0;
        leftConstraints.weightx = 0.42;
        leftConstraints.weighty = 1.0;
        leftConstraints.fill = GridBagConstraints.BOTH;
        leftConstraints.insets = new Insets(16, 16, 16, 8);
        add(leftPanel, leftConstraints);

        GridBagConstraints rightConstraints = new GridBagConstraints();
        rightConstraints.gridx = 1;
        rightConstraints.gridy = 0;
        rightConstraints.weightx = 0.58;
        rightConstraints.weighty = 1.0;
        rightConstraints.fill = GridBagConstraints.BOTH;
        rightConstraints.insets = new Insets(16, 8, 16, 16);
        add(rightPanel, rightConstraints);

        refreshPartyList();
        refreshBackpackSummary();
        refreshInfoPanel();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = createPanelContainer();
        leftPanel.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Party List", SwingConstants.LEFT);
        title.setForeground(TEXT_PRIMARY);
        title.setFont(TITLE_FONT);
        title.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 10));
        leftPanel.add(title, BorderLayout.NORTH);

        portraitRosterPanel.setOpaque(false);
        portraitRosterPanel.setLayout(new BoxLayout(portraitRosterPanel, BoxLayout.Y_AXIS));
        portraitRosterPanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        refreshPartyList();

        JScrollPane rosterScroll = new JScrollPane(portraitRosterPanel);
        rosterScroll.setBorder(BorderFactory.createEmptyBorder());
        rosterScroll.getVerticalScrollBar().setUnitIncrement(16);
        rosterScroll.getViewport().setOpaque(false);
        rosterScroll.setOpaque(false);
        leftPanel.add(rosterScroll, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new GridLayout(1, 3, 10, 0));
        buttonRow.setOpaque(false);
        buttonRow.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JButton backpackButton = new JButton("Backpack");
        backpackButton.setFont(BUTTON_FONT);
        backpackButton.addActionListener(e -> {
            infoMode = InfoMode.BACKPACK;
            refreshInfoPanel();
        });
        buttonRow.add(backpackButton);

        JButton logsButton = new JButton("Logs");
        logsButton.setFont(BUTTON_FONT);
        logsButton.addActionListener(e -> {
            infoMode = InfoMode.LOGS;
            refreshInfoPanel();
        });
        buttonRow.add(logsButton);

        JButton returnButton = new JButton("Return to Res Panel");
        returnButton.setFont(BUTTON_FONT);
        returnButton.addActionListener(e -> main.showScreen("60secs"));
        buttonRow.add(returnButton);

        leftPanel.add(buttonRow, BorderLayout.SOUTH);
        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);

        JPanel topRightPanel = createPanelContainer();
        topRightPanel.setLayout(new BorderLayout(12, 12));
        topRightPanel.add(createTopRightHeader(), BorderLayout.NORTH);
        topRightPanel.add(createTileExplorerPanel(), BorderLayout.CENTER);

        JPanel bottomRightPanel = createPanelContainer();
        bottomRightPanel.setLayout(new BorderLayout(10, 10));
        infoPanelTitle.setForeground(TEXT_PRIMARY);
        infoPanelTitle.setFont(TITLE_FONT);
        infoPanelTitle.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        bottomRightPanel.add(infoPanelTitle, BorderLayout.NORTH);

        backpackSummaryArea.setEditable(false);
        backpackSummaryArea.setOpaque(false);
        backpackSummaryArea.setForeground(TEXT_SECONDARY);
        backpackSummaryArea.setFont(CARD_BODY_FONT);
        backpackSummaryArea.setLineWrap(true);
        backpackSummaryArea.setWrapStyleWord(true);
        backpackSummaryArea.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        infoCardHost.setOpaque(false);
        backpackListPanel.setOpaque(false);
        backpackListPanel.setLayout(new BoxLayout(backpackListPanel, BoxLayout.Y_AXIS));

        JScrollPane backpackScroll = new JScrollPane(backpackListPanel);
        backpackScroll.setBorder(BorderFactory.createEmptyBorder());
        backpackScroll.setOpaque(false);
        backpackScroll.getViewport().setOpaque(false);
        backpackScroll.getVerticalScrollBar().setUnitIncrement(16);

        JScrollPane infoScroll = new JScrollPane(backpackSummaryArea);
        infoScroll.setBorder(BorderFactory.createEmptyBorder());
        infoScroll.setOpaque(false);
        infoScroll.getViewport().setOpaque(false);
        infoScroll.getVerticalScrollBar().setUnitIncrement(16);

        backpackScroll.setPreferredSize(BACKPACK_PANEL_SIZE);
        backpackScroll.setMinimumSize(BACKPACK_PANEL_SIZE);
        backpackScroll.setMaximumSize(BACKPACK_PANEL_SIZE);
        infoScroll.setPreferredSize(BACKPACK_PANEL_SIZE);
        infoScroll.setMinimumSize(BACKPACK_PANEL_SIZE);
        infoScroll.setMaximumSize(BACKPACK_PANEL_SIZE);
        infoCardHost.setPreferredSize(BACKPACK_PANEL_SIZE);
        infoCardHost.setMinimumSize(BACKPACK_PANEL_SIZE);
        infoCardHost.setMaximumSize(BACKPACK_PANEL_SIZE);

        infoCardWrapper.setOpaque(false);
        GridBagConstraints wrapperConstraints = new GridBagConstraints();
        wrapperConstraints.gridx = 0;
        wrapperConstraints.gridy = 0;
        wrapperConstraints.anchor = GridBagConstraints.CENTER;
        infoCardWrapper.add(infoCardHost, wrapperConstraints);

        infoCardHost.add(backpackScroll, "backpack");
        infoCardHost.add(infoScroll, "logs");
        bottomRightPanel.add(infoCardWrapper, BorderLayout.CENTER);

        GridBagConstraints topConstraints = new GridBagConstraints();
        topConstraints.gridx = 0;
        topConstraints.gridy = 0;
        topConstraints.weightx = 1.0;
        topConstraints.weighty = 0.74;
        topConstraints.fill = GridBagConstraints.BOTH;
        topConstraints.insets = new Insets(0, 0, 8, 0);
        rightPanel.add(topRightPanel, topConstraints);

        GridBagConstraints bottomConstraints = new GridBagConstraints();
        bottomConstraints.gridx = 0;
        bottomConstraints.gridy = 1;
        bottomConstraints.weightx = 1.0;
        bottomConstraints.weighty = 0.26;
        bottomConstraints.fill = GridBagConstraints.BOTH;
        rightPanel.add(bottomRightPanel, bottomConstraints);

        return rightPanel;
    }

    private JPanel createTopRightHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Exploration Tiles", SwingConstants.LEFT);
        title.setForeground(TEXT_PRIMARY);
        title.setFont(TITLE_FONT);
        title.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        header.add(title, BorderLayout.WEST);

        JLabel subtitle = new JLabel("Click adjacent tiles (1 step)", SwingConstants.RIGHT);
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setFont(CARD_BODY_FONT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        header.add(subtitle, BorderLayout.EAST);

        return header;
    }

    private JPanel createTileExplorerPanel() {
        tileExplorerPanel.setOpaque(false);
        tileExplorerPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        tileExplorerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleTileClick(e.getX(), e.getY());
            }
        });
        return tileExplorerPanel;
    }

    private JPanel createPartyCard(String characterId) {
        GameData.CharacterStats stats = gameData.getCharacterStats(characterId);
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setOpaque(true);
        card.setBackground(CARD_FILL);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PANEL_BORDER, 2),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

        JLabel portrait = new JLabel(loadPortraitIcon(characterId, 64, 64));
        portrait.setPreferredSize(new Dimension(64, 64));
        portrait.setBorder(BorderFactory.createLineBorder(new Color(85, 85, 85), 1));
        card.add(portrait, BorderLayout.WEST);

        JPanel statsPanel = new JPanel();
        statsPanel.setOpaque(false);
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(formatCharacterName(characterId));
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(CARD_TITLE_FONT);

        JLabel healthLabel = new JLabel(buildHealthText(stats));
        healthLabel.setForeground(TEXT_SECONDARY);
        healthLabel.setFont(CARD_BODY_FONT);

        JLabel survivalLabel = new JLabel(buildSurvivalText(stats));
        survivalLabel.setForeground(TEXT_SECONDARY);
        survivalLabel.setFont(CARD_BODY_FONT);

        JLabel stateLabel = new JLabel(buildStateText(stats));
        stateLabel.setForeground(TEXT_SECONDARY);
        stateLabel.setFont(CARD_BODY_FONT);

        statsPanel.add(nameLabel);
        statsPanel.add(Box.createVerticalStrut(6));
        statsPanel.add(healthLabel);
        statsPanel.add(survivalLabel);
        statsPanel.add(stateLabel);

        card.add(statsPanel, BorderLayout.CENTER);

        return card;
    }

    private void refreshPartyList() {
        portraitRosterPanel.removeAll();

        for (String characterId : CHARACTER_ORDER) {
            portraitRosterPanel.add(createPartyCard(characterId));
            portraitRosterPanel.add(Box.createVerticalStrut(10));
        }

        portraitRosterPanel.revalidate();
        portraitRosterPanel.repaint();
    }

    private void loadInventoryData() {
        InventoryCsvReader csvReader = new InventoryCsvReader();
        List<GameData.InventoryEntry> loadedItems = csvReader.readInventoryItems("data/inventory.csv");

        if (loadedItems.isEmpty()) {
            loadedItems = List.of(new GameData.InventoryEntry("no_data", 0));
        }

        gameData.setInventoryItems(loadedItems);
        inventoryCounts.clear();
        for (GameData.InventoryEntry item : loadedItems) {
            if (item.getQuantity() > 0) {
                inventoryCounts.put(item.getItemId(), item.getQuantity());
            }
        }
    }

    private JPanel createInventoryListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout(8, 8));
        listPanel.setOpaque(false);

        JLabel inventoryTitle = new JLabel("Inventory Items", SwingConstants.LEFT);
        inventoryTitle.setForeground(TEXT_PRIMARY);
        inventoryTitle.setFont(CARD_TITLE_FONT);

        JPanel itemsContainer = new JPanel();
        itemsContainer.setOpaque(false);
        itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));

        for (GameData.InventoryEntry item : gameData.getInventoryItems()) {
            if (item.getQuantity() <= 0) {
                continue;
            }

            LootEntry definition = lootById.get(item.getItemId());
            String itemLabel = definition == null ? formatItemNameFromId(item.getItemId()) : definition.displayName;
            String effectLabel = definition == null ? "No known use effect." : buildLootEffectText(definition);

            JPanel itemCard = new JPanel(new BorderLayout(10, 6));
            itemCard.setBackground(CARD_FILL);
            itemCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PANEL_BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            itemCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));

            JPanel textBlock = new JPanel();
            textBlock.setOpaque(false);
            textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

            JLabel itemNameLabel = new JLabel(itemLabel + " x" + item.getQuantity(), SwingConstants.LEFT);
            itemNameLabel.setForeground(TEXT_PRIMARY);
            itemNameLabel.setFont(CARD_TITLE_FONT);

            JLabel effectLabelView = new JLabel(effectLabel, SwingConstants.LEFT);
            effectLabelView.setForeground(TEXT_SECONDARY);
            effectLabelView.setFont(CARD_BODY_FONT);

            textBlock.add(itemNameLabel);
            textBlock.add(Box.createVerticalStrut(4));
            textBlock.add(effectLabelView);
            itemCard.add(textBlock, BorderLayout.CENTER);

            JButton useButton = new JButton("Use");
            useButton.setFont(BUTTON_FONT);
            useButton.addActionListener(e -> useInventoryItem(item.getItemId()));
            itemCard.add(useButton, BorderLayout.EAST);

            itemsContainer.add(itemCard);
            itemsContainer.add(Box.createVerticalStrut(8));
        }

        if (itemsContainer.getComponentCount() == 0) {
            JLabel emptyLabel = new JLabel("No inventory data found", SwingConstants.CENTER);
            emptyLabel.setForeground(TEXT_PRIMARY);
            itemsContainer.setLayout(new BorderLayout());
            itemsContainer.add(emptyLabel, BorderLayout.CENTER);
        }

        JScrollPane listScrollPane = new JScrollPane(itemsContainer);
        listScrollPane.setBorder(BorderFactory.createLineBorder(PANEL_BORDER, 1));
        listScrollPane.getViewport().setOpaque(false);
        listScrollPane.setOpaque(false);

        listPanel.add(inventoryTitle, BorderLayout.NORTH);
        listPanel.add(listScrollPane, BorderLayout.CENTER);
        return listPanel;
    }

    private void loadMapConfig() {
        File config = new File("data/roam_tiles.csv");
        if (!config.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(config))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.equalsIgnoreCase("key,value")) {
                    continue;
                }

                String[] parts = trimmed.split(",", 2);
                if (parts.length < 2) {
                    continue;
                }
                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();

                switch (key) {
                    case "rows" -> mapRows = Math.max(3, parseIntOrDefault(value, mapRows));
                    case "cols" -> mapCols = Math.max(3, parseIntOrDefault(value, mapCols));
                    case "start_row" -> playerRow = Math.max(0, parseIntOrDefault(value, playerRow));
                    case "start_col" -> playerCol = Math.max(0, parseIntOrDefault(value, playerCol));
                    case "exit_row" -> exitRow = Math.max(0, parseIntOrDefault(value, exitRow));
                    case "exit_col" -> exitCol = Math.max(0, parseIntOrDefault(value, exitCol));
                    case "combat_chance" -> combatChance = clamp01(parseDoubleOrDefault(value, combatChance));
                    case "loot_chance" -> lootChance = clamp01(parseDoubleOrDefault(value, lootChance));
                    default -> {
                        // ignore unknown keys
                    }
                }
            }
        } catch (IOException ignored) {
            // Keep defaults when config is unreadable.
        }

        playerRow = Math.min(playerRow, mapRows - 1);
        playerCol = Math.min(playerCol, mapCols - 1);
        exitRow = Math.min(exitRow, mapRows - 1);
        exitCol = Math.min(exitCol, mapCols - 1);
    }

    private void loadLootTableFromCsv() {
        lootRarityWeights.clear();
        lootByRarity.clear();
        lootById.clear();

        // Preserve existing rarity distribution used by scavenging rolls.
        lootRarityWeights.put("Common", 60);
        lootRarityWeights.put("Rare", 20);
        lootRarityWeights.put("Epic", 10);
        lootRarityWeights.put("Legendary", 5);

        File csvFile = new File("data/loot-table.csv");
        if (!csvFile.exists()) {
            loadFallbackLootTable();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean skippedHeader = false;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (!skippedHeader) {
                    skippedHeader = true;
                    if (trimmed.toLowerCase().startsWith("rarity,")) {
                        continue;
                    }
                }

                List<String> cols = parseCsvLine(trimmed);
                if (cols.size() < 3) {
                    continue;
                }

                String rarityRaw = cols.get(0).trim();
                String displayName = cols.get(1).trim();
                String effectOrDescription = cols.get(2).trim();
                String notes = cols.size() > 3 ? cols.get(3).trim() : "";

                if (rarityRaw.isEmpty() || displayName.isEmpty()) {
                    continue;
                }

                String rarity = rarityRaw.substring(0, 1).toUpperCase() + rarityRaw.substring(1).toLowerCase();
                if (!(rarity.equals("Common")
                        || rarity.equals("Rare")
                        || rarity.equals("Epic")
                        || rarity.equals("Legendary"))) {
                    continue;
                }

                String description = effectOrDescription;
                if (!notes.isEmpty()) {
                    description = effectOrDescription + " (" + notes + ")";
                }

                registerLootEntry(rarity, displayName, description);
            }
        } catch (IOException ignored) {
            loadFallbackLootTable();
        }

        if (lootById.isEmpty()) {
            loadFallbackLootTable();
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        values.add(current.toString());
        return values;
    }

    private void loadFallbackLootTable() {
        lootRarityWeights.clear();
        lootByRarity.clear();
        lootById.clear();

        lootRarityWeights.put("Common", 60);
        lootRarityWeights.put("Rare", 20);
        lootRarityWeights.put("Epic", 10);
        lootRarityWeights.put("Legendary", 5);

        registerLootEntry("Common", "Bandage", "can heal but not a lot (20% heal)");
        registerLootEntry("Common", "Rock", "it’s a rock");
        registerLootEntry("Common", "Wooden Plank", "it’s wood, might be useful.");
        registerLootEntry("Common", "String", "it’s string");
        registerLootEntry("Common", "Dust", "How did you even collect this???");
        registerLootEntry("Common", "Paper", "another tree.. GONE..");
        registerLootEntry("Common", "Folders", "What a nice folder! It’s all filled with some cool research…");
        registerLootEntry("Common", "Unknown Meat", "No one knows where this is from (???)");
        registerLootEntry("Common", "Lizard", "you sure you really want to eat that? (???)");
        registerLootEntry("Common", "Rat", "Bro you’re not that desperate to eat are you? (???)");
        registerLootEntry("Common", "Still Water", "are you sure about that? (-20% health, Death after day)");
        registerLootEntry("Common", "Cans", "Made of tin.");
        registerLootEntry("Common", "Ammo", "it is what it is.");
        registerLootEntry("Common", "Rope", "it’s a rope");

        registerLootEntry("Rare", "Bone", "uh oh");
        registerLootEntry("Rare", "Scissors", "who knows what you’re gonna do with this (adds bleed to physical damage) (only lootable one time)");
        registerLootEntry("Rare", "Tape", "might be useful (prevents bleeds)");
        registerLootEntry("Rare", "Medkit", "can heal anyone. ( +35% heal)");
        registerLootEntry("Rare", "Tourniquet", "stops bleeding (12% heal, removes bleed and arterial bleed)");
        registerLootEntry("Rare", "A Flask of Who Knows What’s Inside", "it was found in one of the sub laboratories (Full heal)");
        registerLootEntry("Rare", "A Flask with Unknown Contents", "it was found in one of the sub laboratories ( -35% health)");
        registerLootEntry("Rare", "A Flask with Questionable Contents", "it was found in one of the sub laboratories ( -3 sanity)");
        registerLootEntry("Rare", "Apple", "it’s an apple a day keeps you alive. (+10% hunger, +15% thirst, Sanity +1)");
        registerLootEntry("Rare", "Crackers", "it’s better than nothing. (+12% hunger)");
        registerLootEntry("Rare", "Water Bottle", "Water is lloyd, Water is good. (+30% hydration)");
        registerLootEntry("Rare", "Canned Rations (Rusty, Old)", "It’s better than nothing at all. ( +30% hunger)");
        registerLootEntry("Rare", "Batteries", "to get the flashlight and radio to work. (Can extend flashlight’s use, no effect on radio yet)");

        registerLootEntry("Epic", "Scientist’s Guitar", "Owner took really good care of it, even has a capo… (2 use per day, 1+ sanity per use)");
        registerLootEntry("Epic", "Water Jug", "This is for EXTREME thirst. (+80% hydration, 3 usage)");
        registerLootEntry("Epic", "Canned Rations (Rusty, Old)", "It’s better than nothing at all. ( +60% hunger)");
        registerLootEntry("Epic", "Notebook", "can write down thoughts to at least be sane (+5 sanity)");
        registerLootEntry("Epic", "Sanity Pill", "makes you not go crazy (+4 sanity)");
        registerLootEntry("Epic", "Bread", "woah you sure this is still good to eat? (+)");
        registerLootEntry("Epic", "Vodka", "it’s russian water (-5% Hydration, +Full sanity, gets rid of morale debuff)");
        registerLootEntry("Epic", "Flashlight", "( only good for 3 tiles) - Makes it easier to see in the dark (can go explore tile without triggering combat or loot, when “explored” depending if the tile was a combat encounter or not it will change color. Red for combat encounter and yellow for loot. If no event then normal.)");
        registerLootEntry("Epic", "Radio", "To get updates (Unfinished item)");
        registerLootEntry("Epic", "Soda Can", "Dang where did you get this? (+35% hydration)");

        registerLootEntry("Legendary", "Rice", "struggle meal essentials! (+80% hunger)");
        registerLootEntry("Legendary", "Funny Bone", "How’d this get here?");
        registerLootEntry("Legendary", "Trauma Kit", "cheesy ahh item, lucky you.. (Full heal, clear status effects)");
        registerLootEntry("Legendary", "MRE", "A full course meal ready to eat! (full hunger)");
        registerLootEntry("Legendary", "Incendiary Ammo", "things are about to get heated! (adds fire dmg to bullet damage)");
        registerLootEntry("Legendary", "Hollow-Point Ammo", "Are these even legal here…? (adds bleed and cripple status to bullet damage)");
    }

    private void registerLootEntry(String rarity, String displayName, String description) {
        String itemId = buildLootItemId(rarity, displayName);
        LootEntry entry = new LootEntry(rarity, displayName, itemId, 1, 1, description);
        lootByRarity.computeIfAbsent(rarity, key -> new ArrayList<>()).add(entry);
        lootById.put(itemId, entry);
    }

    private void initializeExplorationState() {
        regenerateScavengeMap();
    }

    public void startNewScavenge() {
        regenerateScavengeMap();
        refreshPartyList();
        refreshBackpackSummary();
        refreshInfoPanel();
        tileExplorerPanel.repaint();
    }

    private void regenerateScavengeMap() {
        exploredTiles = new boolean[mapRows][mapCols];
        playerRow = Math.min(playerRow, mapRows - 1);
        playerCol = Math.min(playerCol, mapCols - 1);

        do {
            exitRow = random.nextInt(mapRows);
            exitCol = random.nextInt(mapCols);
        } while (exitRow == playerRow && exitCol == playerCol);

        exploredTiles[playerRow][playerCol] = true;
        noLootStreak = 0;
        eventLogs.clear();
        addLog("Exploration started at tile (" + (playerCol + 1) + ", " + (playerRow + 1) + ").");
        addLog("Find the exit to leave scavenging.");
    }

    private void paintTileExplorer(Graphics2D g2) {
        int width = tileExplorerPanel.getWidth();
        int height = tileExplorerPanel.getHeight();
        if (width <= 0 || height <= 0 || mapCols <= 0 || mapRows <= 0) {
            return;
        }

        int gridW = mapCols * TILE_CELL_WIDTH;
        int gridH = mapRows * TILE_CELL_HEIGHT;
        int gridX = Math.max(TILE_GRID_PADDING, (width - gridW) / 2);
        int gridY = Math.max(TILE_GRID_PADDING, (height - gridH) / 2);

        for (int row = 0; row < mapRows; row++) {
            for (int col = 0; col < mapCols; col++) {
                int x = gridX + (col * TILE_CELL_WIDTH);
                int y = gridY + (row * TILE_CELL_HEIGHT);
                g2.setColor(exploredTiles[row][col] ? TILE_EXPLORED : TILE_UNEXPLORED);
                g2.fillRect(x, y, TILE_CELL_WIDTH, TILE_CELL_HEIGHT);
                g2.setColor(TILE_BORDER);
                g2.drawRect(x, y, TILE_CELL_WIDTH, TILE_CELL_HEIGHT);
            }
        }

        int markerX = gridX + (playerCol * TILE_CELL_WIDTH) + (TILE_CELL_WIDTH / 2);
        int markerY = gridY + (playerRow * TILE_CELL_HEIGHT) + (TILE_CELL_HEIGHT / 2);
        int markerSize = Math.max(12, Math.min(TILE_CELL_WIDTH, TILE_CELL_HEIGHT) / 2);
        g2.setColor(PLAYER_MARKER);
        g2.fillOval(markerX - markerSize / 2, markerY - markerSize / 2, markerSize, markerSize);
        g2.setColor(new Color(70, 55, 20));
        g2.drawOval(markerX - markerSize / 2, markerY - markerSize / 2, markerSize, markerSize);
    }

    private void handleTileClick(int mouseX, int mouseY) {
        int width = tileExplorerPanel.getWidth();
        int height = tileExplorerPanel.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        int gridW = mapCols * TILE_CELL_WIDTH;
        int gridH = mapRows * TILE_CELL_HEIGHT;
        int gridX = Math.max(TILE_GRID_PADDING, (width - gridW) / 2);
        int gridY = Math.max(TILE_GRID_PADDING, (height - gridH) / 2);
        if (mouseX < gridX || mouseY < gridY || mouseX >= gridX + gridW || mouseY >= gridY + gridH) {
            return;
        }

        int clickedCol = (mouseX - gridX) / TILE_CELL_WIDTH;
        int clickedRow = (mouseY - gridY) / TILE_CELL_HEIGHT;

        int distance = Math.abs(clickedCol - playerCol) + Math.abs(clickedRow - playerRow);
        if (distance != 1) {
            addLog("Move blocked: you can only move one tile up/down/left/right.");
            refreshInfoPanel();
            return;
        }

        playerCol = clickedCol;
        playerRow = clickedRow;
        tileExplorerPanel.repaint();

        handleTileEvent(playerRow, playerCol);
        refreshInfoPanel();
    }

    private void handleTileEvent(int row, int col) {
        if (row == exitRow && col == exitCol) {
            promptExitScavenge();
            return;
        }

        if (exploredTiles[row][col]) {
            addLog("Moved to explored tile (" + (col + 1) + ", " + (row + 1) + "). No new event.");
            applyScavengeUpkeep(false);
            return;
        }

        exploredTiles[row][col] = true;
        addLog("Entered unexplored tile (" + (col + 1) + ", " + (row + 1) + ").");
        applyScavengeUpkeep(true);

        boolean guaranteedLoot = noLootStreak >= 4;
        double roll = random.nextDouble();
        if (!guaranteedLoot && roll < combatChance) {
            addLog("Combat encountered. Party moved to combat panel.");
            noLootStreak++;
            SwingUtilities.invokeLater(() -> main.showScreen("OMORI"));
            return;
        }

        if (guaranteedLoot || roll < combatChance + lootChance) {
            grantRandomLoot();
            return;
        }

        addLog("No loot found and no enemies encountered.");
        noLootStreak++;
    }

    private void grantRandomLoot() {
        LootEntry picked = pickWeightedLoot();
        if (picked == null) {
            addLog("Loot event triggered, but loot table is empty.");
            return;
        }

        int qty = picked.minQty;
        if (picked.maxQty > picked.minQty) {
            qty += random.nextInt((picked.maxQty - picked.minQty) + 1);
        }

        inventoryCounts.merge(picked.itemId, qty, Integer::sum);
        syncInventoryToGameData();
        noLootStreak = 0;
        addLog("Loot found: " + picked.displayName + " x" + qty + ".");
        refreshBackpackSummary();
    }

    private LootEntry pickWeightedLoot() {
        if (lootByRarity.isEmpty() || lootRarityWeights.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (int weight : lootRarityWeights.values()) {
            totalWeight += weight;
        }

        int roll = random.nextInt(Math.max(1, totalWeight));
        int cumulative = 0;
        String chosenRarity = null;
        for (Map.Entry<String, Integer> entry : lootRarityWeights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                chosenRarity = entry.getKey();
                break;
            }
        }

        if (chosenRarity == null) {
            chosenRarity = lootRarityWeights.keySet().iterator().next();
        }

        List<LootEntry> rarityEntries = lootByRarity.get(chosenRarity);
        if (rarityEntries == null || rarityEntries.isEmpty()) {
            return null;
        }

        return rarityEntries.get(random.nextInt(rarityEntries.size()));
    }

    private void syncInventoryToGameData() {
        List<GameData.InventoryEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : inventoryCounts.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new GameData.InventoryEntry(entry.getKey(), entry.getValue()));
            }
        }
        gameData.setInventoryItems(entries);
    }

    private void syncInventoryFromGameData() {
        inventoryCounts.clear();
        for (GameData.InventoryEntry entry : gameData.getInventoryItems()) {
            if (entry.getQuantity() > 0) {
                inventoryCounts.put(entry.getItemId(), entry.getQuantity());
            }
        }
    }

    private void applyScavengeUpkeep(boolean unexploredTile) {
        for (String characterId : CHARACTER_ORDER) {
            GameData.CharacterStats stats = gameData.getCharacterStats(characterId);
            if (stats == null || !stats.isAlive()) {
                continue;
            }

            stats.setCurrentThirst(stats.getCurrentThirst() - SCAVENGE_THIRST_LOSS);
            stats.setCurrentHunger(stats.getCurrentHunger() - SCAVENGE_HUNGER_LOSS);
        }

        if (!unexploredTile) {
            refreshPartyList();
            return;
        }

        String sanityTarget = pickRandomLivingCharacter();
        if (sanityTarget != null) {
            int sanityLoss = SCAVENGE_SANITY_LOSS_MIN
                    + random.nextInt((SCAVENGE_SANITY_LOSS_MAX - SCAVENGE_SANITY_LOSS_MIN) + 1);
            boolean diedFromSanity = gameData.reduceCharacterSanity(sanityTarget, sanityLoss, true);
            if (diedFromSanity) {
                addLog(formatCharacterName(sanityTarget)
                        + " sanity reached 0. They died and left a suicide note. Morale dropped.");
                syncInventoryFromGameData();
            } else {
                addLog(formatCharacterName(sanityTarget) + " lost " + sanityLoss + " sanity while scavenging.");
            }
        }

        refreshPartyList();
    }

    private String pickRandomLivingCharacter() {
        List<String> living = new ArrayList<>();
        for (String characterId : CHARACTER_ORDER) {
            GameData.CharacterStats stats = gameData.getCharacterStats(characterId);
            if (stats != null && stats.isAlive()) {
                living.add(characterId);
            }
        }
        if (living.isEmpty()) {
            return null;
        }
        return living.get(random.nextInt(living.size()));
    }

    private void promptExitScavenge() {
        int option = JOptionPane.showConfirmDialog(
                this,
                "Exit tile reached. Quit scavenging and return to res panel?",
                "Exit Scavenging",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            main.showScreen("60secs");
        } else {
            addLog("Stayed in scavenging mode.");
        }
    }

    private void refreshInfoPanel() {
        if (infoMode == InfoMode.LOGS) {
            infoPanelTitle.setText("Exploration Logs");
            infoCardLayout.show(infoCardHost, "logs");
            StringBuilder sb = new StringBuilder();
            if (eventLogs.isEmpty()) {
                sb.append("No logs yet.");
            } else {
                for (String log : eventLogs) {
                    sb.append("- ").append(log).append('\n');
                }
            }
            backpackSummaryArea.setText(sb.toString());
        } else {
            infoPanelTitle.setText("Backpack Summary");
            infoCardLayout.show(infoCardHost, "backpack");
            refreshBackpackListPanel();
        }
        backpackSummaryArea.setCaretPosition(0);
    }

    private void addLog(String message) {
        eventLogs.add(0, message);
        if (eventLogs.size() > 50) {
            eventLogs.remove(eventLogs.size() - 1);
        }
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double parseDoubleOrDefault(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String formatItemNameFromId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "Unknown Item";
        }

        String normalized = itemId;
        if (normalized.startsWith("itm_")) {
            normalized = normalized.substring(4);
        }

        String[] parts = normalized.split("_");
        StringBuilder nameBuilder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (nameBuilder.length() > 0) {
                nameBuilder.append(' ');
            }
            nameBuilder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                nameBuilder.append(part.substring(1));
            }
        }

        return nameBuilder.length() == 0 ? "Unknown Item" : nameBuilder.toString();
    }

    private String formatCharacterName(String characterId) {
        if (characterId == null || characterId.isBlank()) {
            return "Unknown";
        }

        return switch (characterId.toLowerCase()) {
            case "kriegs" -> "Kriegs";
            case "azrael" -> "Azrael";
            case "gambit" -> "Gambit";
            case "lazarus" -> "Lazarus";
            case "raphaela" -> "Raphaela";
            case "terry" -> "Terry";
            default -> Character.toUpperCase(characterId.charAt(0)) + characterId.substring(1);
        };
    }

    private String buildHealthText(GameData.CharacterStats stats) {
        if (stats == null) {
            return "HP: unknown";
        }

        return "HP: " + stats.getCurrentHealth() + " / " + stats.getMaxHealth() +
            " (" + stats.getHealthPercentage() + "%)";
    }

    private String buildStateText(GameData.CharacterStats stats) {
        if (stats == null) {
            return "State: unknown";
        }

        int moraleStacks = stats.getStatusStacks(GameData.StatusEffect.MORALE);
        String state = stats.isAlmostDead() ? "almost dead" : "alive";
        return "State: " + state + " | Morale: " + moraleStacks;
    }

    private String buildSurvivalText(GameData.CharacterStats stats) {
        if (stats == null) {
            return "Thirst/Hunger/Sanity: unknown";
        }
        return "Thirst: " + stats.getCurrentThirst()
                + " Hunger: " + stats.getCurrentHunger()
                + " Sanity: " + stats.getCurrentSanity();
    }

    private void refreshBackpackSummary() {
        refreshBackpackListPanel();
    }

    private void refreshBackpackListPanel() {
        backpackListPanel.removeAll();
        JPanel inventoryList = createInventoryListPanel();
        backpackListPanel.add(inventoryList);
        backpackListPanel.revalidate();
        backpackListPanel.repaint();
        if (infoMode == InfoMode.BACKPACK) {
            infoCardLayout.show(infoCardHost, "backpack");
        }
    }

    private String buildLootItemId(String rarity, String displayName) {
        String normalizedName = displayName == null ? "" : displayName.toLowerCase()
                .replace("&", " and ")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        String normalizedRarity = rarity == null ? "item" : rarity.toLowerCase();
        return "itm_" + normalizedRarity + "_" + normalizedName;
    }

    private String buildLootEffectText(LootEntry entry) {
        if (entry.effectText == null || entry.effectText.isBlank()) {
            return entry.rarity + " loot: " + entry.description;
        }
        return entry.rarity + " loot: " + entry.effectText;
    }

    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.toLowerCase()
                .replace('’', '\'')
                .replace('“', '"')
                .replace('”', '"')
                .replace('&', ' ')
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private static String extractEffectText(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }

        Matcher matcher = Pattern.compile("\\(([^()]*)\\)").matcher(description);
        List<String> parts = new ArrayList<>();
        while (matcher.find()) {
            String text = matcher.group(1).trim();
            if (!text.isBlank()) {
                parts.add(text);
            }
        }
        return String.join("; ", parts);
    }

    private void useInventoryItem(String itemId) {
        GameData.InventoryEntry inventoryEntry = null;
        for (GameData.InventoryEntry entry : gameData.getInventoryItems()) {
            if (entry.getItemId().equalsIgnoreCase(itemId) && entry.getQuantity() > 0) {
                inventoryEntry = entry;
                break;
            }
        }

        if (inventoryEntry == null) {
            addLog("That item is no longer in your backpack.");
            refreshBackpackSummary();
            return;
        }

        LootEntry definition = lootById.get(inventoryEntry.getItemId());
        if (definition == null) {
            addLog(formatItemNameFromId(inventoryEntry.getItemId()) + " has no implemented use yet.");
            return;
        }

        if (!applyLootEffect(definition)) {
            addLog(definition.displayName + " cannot be used right now.");
            return;
        }

        inventoryCounts.put(inventoryEntry.getItemId(), Math.max(0, inventoryEntry.getQuantity() - 1));
        inventoryCounts.entrySet().removeIf(entry -> entry.getValue() <= 0);
        syncInventoryToGameData();
        refreshPartyList();
        refreshBackpackSummary();
        addLog("Used " + definition.displayName + ".");
    }

    private boolean applyLootEffect(LootEntry entry) {
        String itemKey = normalizeText(entry.displayName);
        String effectKey = normalizeText(entry.effectText);

        if (itemKey.equals("bandage") && effectKey.contains("20_heal")) {
            return healMostInjuredLivingCharacter(20);
        }
        if (itemKey.equals("medkit") && effectKey.contains("35_heal")) {
            return healMostInjuredLivingCharacter(35);
        }
        if (itemKey.equals("tourniquet") && effectKey.contains("12_heal")) {
            GameData.CharacterStats target = getMostInjuredLivingCharacter();
            if (target == null) {
                return false;
            }
            target.clearStatus(GameData.StatusEffect.BLEED);
            healCharacterByPercent(target, 12);
            return true;
        }
        if ((itemKey.equals("a_flask_of_who_knows_what_s_inside")
            || itemKey.equals("flask_of_who_knows_what_s_inside")
            || itemKey.equals("flask_unknown_inside"))
            && effectKey.contains("full_heal")) {
            return healMostInjuredLivingCharacter(100);
        }
        if ((itemKey.equals("a_flask_with_unknown_contents")
            || itemKey.equals("flask_with_unknown_contents")
            || itemKey.equals("flask_unknown_contents"))
            && effectKey.contains("35_health")) {
            return damageMostHealthyLivingCharacter(35);
        }
        if ((itemKey.equals("a_flask_with_questionable_contents")
            || itemKey.equals("flask_with_questionable_contents")
            || itemKey.equals("flask_questionable_contents"))
            && effectKey.contains("3_sanity")) {
            return reduceSanityOnLivingCharacter(3);
        }
        if (itemKey.equals("apple") && effectKey.contains("10_hunger") && effectKey.contains("15_thirst")) {
            return applyHungerThirstSanity("Apple", 10, 15, 1);
        }
        if (itemKey.equals("crackers") && effectKey.contains("12_hunger")) {
            return applyHungerToLivingCharacter(12);
        }
        if (itemKey.equals("water_bottle") && effectKey.contains("30_hydration")) {
            return applyThirstToLivingCharacter(30);
        }
        if (itemKey.equals("canned_rations_rusty_old") && effectKey.contains("30_hunger")) {
            return applyHungerToLivingCharacter(30);
        }
        if (itemKey.equals("batteries")) {
            addLog("Batteries stored. Flashlight and radio support is not fully modeled yet.");
            return true;
        }
        if (itemKey.equals("scientist_s_guitar") && effectKey.contains("1_sanity_per_use")) {
            return addSanityToLivingCharacter(1);
        }
        if (itemKey.equals("water_jug") && effectKey.contains("80_hydration")) {
            return applyThirstToLivingCharacter(80);
        }
        if (itemKey.equals("canned_rations_rusty_old") && effectKey.contains("60_hunger")) {
            return applyHungerToLivingCharacter(60);
        }
        if (itemKey.equals("notebook") && effectKey.contains("5_sanity")) {
            return addSanityToLivingCharacter(5);
        }
        if (itemKey.equals("sanity_pill") && effectKey.contains("4_sanity")) {
            return addSanityToLivingCharacter(4);
        }
        if (itemKey.equals("bread")) {
            return applyHungerToLivingCharacter(20);
        }
        if (itemKey.equals("vodka")) {
            boolean sanityRestored = addSanityToLivingCharacter(999);
            GameData.CharacterStats target = getMostInjuredLivingCharacter();
            if (target != null) {
                target.setCurrentThirst(Math.max(0, target.getCurrentThirst() - Math.max(1, target.getMaxThirst() / 20)));
                target.clearStatus(GameData.StatusEffect.MORALE);
            }
            return sanityRestored || target != null;
        }
        if (itemKey.equals("flashlight")) {
            addLog("Flashlight added. Tile vision behavior is not fully modeled yet.");
            return true;
        }
        if (itemKey.equals("radio")) {
            addLog("Radio added. Updates are unfinished, so it only logs for now.");
            return true;
        }
        if (itemKey.equals("soda_can") && effectKey.contains("35_hydration")) {
            return applyThirstToLivingCharacter(35);
        }
        if (itemKey.equals("rice") && effectKey.contains("80_hunger")) {
            return applyHungerToLivingCharacter(80);
        }
        if (itemKey.equals("funny_bone")) {
            addLog("Funny bone found. No active effect is modeled yet.");
            return true;
        }
        if (itemKey.equals("trauma_kit")) {
            GameData.CharacterStats target = getMostInjuredLivingCharacter();
            if (target == null) {
                return false;
            }
            target.heal(target.getMaxHealth());
            target.clearStatus(GameData.StatusEffect.BLEED);
            target.clearStatus(GameData.StatusEffect.CRIPPLE);
            target.clearStatus(GameData.StatusEffect.FIRE);
            target.clearStatus(GameData.StatusEffect.MORALE);
            return true;
        }
        if (itemKey.equals("mre") && effectKey.contains("full_hunger")) {
            return restoreFullHungerOnLivingCharacter();
        }
        if (itemKey.equals("incendiary_ammo")) {
            addLog("Incendiary ammo stored. Combat ammo modifiers are not linked yet.");
            return true;
        }
        if (itemKey.equals("hollow_point_ammo")) {
            addLog("Hollow-point ammo stored. Combat ammo modifiers are not linked yet.");
            return true;
        }

        if (itemKey.equals("rock") || itemKey.equals("wooden_plank") || itemKey.equals("string")
                || itemKey.equals("dust") || itemKey.equals("paper") || itemKey.equals("folders")
                || itemKey.equals("unknown_meat") || itemKey.equals("lizard") || itemKey.equals("rat")
                || itemKey.equals("still_water") || itemKey.equals("cans") || itemKey.equals("ammo")
                || itemKey.equals("rope") || itemKey.equals("bone") || itemKey.equals("scissors")
                || itemKey.equals("tape")) {
            addLog(entry.displayName + " is kept as loot but has no active use effect yet.");
            return true;
        }

        return false;
    }

    private GameData.CharacterStats getMostInjuredLivingCharacter() {
        GameData.CharacterStats chosen = null;
        double lowestRatio = 1.1;
        for (String characterId : CHARACTER_ORDER) {
            GameData.CharacterStats stats = gameData.getCharacterStats(characterId);
            if (stats == null || !stats.isAlive()) {
                continue;
            }
            double ratio = stats.getCurrentHealth() / (double) Math.max(1, stats.getMaxHealth());
            if (ratio < lowestRatio) {
                lowestRatio = ratio;
                chosen = stats;
            }
        }
        return chosen;
    }

    private boolean healMostInjuredLivingCharacter(int percent) {
        GameData.CharacterStats target = getMostInjuredLivingCharacter();
        if (target == null) {
            return false;
        }
        healCharacterByPercent(target, percent);
        return true;
    }

    private void healCharacterByPercent(GameData.CharacterStats target, int percent) {
        int amount = Math.max(1, (int) Math.round(target.getMaxHealth() * (percent / 100.0)));
        target.heal(amount);
    }

    private boolean damageMostHealthyLivingCharacter(int percent) {
        GameData.CharacterStats target = getMostInjuredLivingCharacter();
        if (target == null) {
            return false;
        }
        int amount = Math.max(1, (int) Math.round(target.getMaxHealth() * (percent / 100.0)));
        target.takeDamage(amount);
        return true;
    }

    private boolean reduceSanityOnLivingCharacter(int amount) {
        GameData.CharacterStats target = getMostInjuredLivingCharacter();
        if (target == null) {
            return false;
        }
        target.setCurrentSanity(target.getCurrentSanity() - amount);
        return true;
    }

    private boolean addSanityToLivingCharacter(int amount) {
        GameData.CharacterStats target = getMostInjuredLivingCharacter();
        if (target == null) {
            return false;
        }
        target.setCurrentSanity(target.getCurrentSanity() + amount);
        return true;
    }

    private boolean applyHungerToLivingCharacter(int percent) {
        GameData.CharacterStats target = getMostInjuredLivingCharacter();
        if (target == null) {
            return false;
        }
        int gain = Math.max(1, (int) Math.round(target.getMaxHunger() * (percent / 100.0)));
        target.setCurrentHunger(target.getCurrentHunger() + gain);
        return true;
    }

    private boolean applyThirstToLivingCharacter(int percent) {
        GameData.CharacterStats target = getMostInjuredLivingCharacter();
        if (target == null) {
            return false;
        }
        int gain = Math.max(1, (int) Math.round(target.getMaxThirst() * (percent / 100.0)));
        target.setCurrentThirst(target.getCurrentThirst() + gain);
        return true;
    }

    private boolean applyHungerThirstSanity(String itemName, int hungerPercent, int thirstPercent, int sanityBoost) {
        GameData.CharacterStats target = getMostInjuredLivingCharacter();
        if (target == null) {
            return false;
        }
        int hungerGain = Math.max(1, (int) Math.round(target.getMaxHunger() * (hungerPercent / 100.0)));
        int thirstGain = Math.max(1, (int) Math.round(target.getMaxThirst() * (thirstPercent / 100.0)));
        target.setCurrentHunger(target.getCurrentHunger() + hungerGain);
        target.setCurrentThirst(target.getCurrentThirst() + thirstGain);
        target.setCurrentSanity(target.getCurrentSanity() + sanityBoost);
        addLog(itemName + " used on " + formatCharacterName(target.getCharacterName()) + ".");
        return true;
    }

    private boolean restoreFullHungerOnLivingCharacter() {
        GameData.CharacterStats target = getMostInjuredLivingCharacter();
        if (target == null) {
            return false;
        }
        target.setCurrentHunger(target.getMaxHunger());
        return true;
    }

    private ImageIcon loadPortraitIcon(String characterId, int width, int height) {
        String path = getPortraitPath(characterId);
        BufferedImage image = loadImage(path);
        if (image == null) {
            return null;
        }

        Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private String getPortraitPath(String characterId) {
        return switch (characterId.toLowerCase()) {
            case "kriegs" -> "assets/res/Characters/Kriegs/PFPKriegs.png";
            case "azrael" -> "assets/res/Characters/Azrael/PFPAzrael.png";
            case "gambit" -> "assets/res/Characters/Gambit/PFPGambit.png";
            case "lazarus" -> "assets/res/Characters/Lazarus/PFPLazarus.png";
            case "raphaela" -> "assets/res/Characters/raphaela/PFPRaphaela.png";
            case "terry" -> "assets/res/Characters/terry/PFPTerry.png";
            default -> null;
        };
    }

    private BufferedImage loadImage(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            return null;
        }
    }

    private JPanel createPanelContainer() {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(PANEL_FILL);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PANEL_BORDER, 2),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        return panel;
    }
}