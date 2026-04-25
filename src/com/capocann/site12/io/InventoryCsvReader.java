package com.capocann.site12.io;

import com.capocann.site12.GameData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InventoryCsvReader {
    public List<GameData.InventoryEntry> readInventoryItems(String csvPath) {
        List<GameData.InventoryEntry> items = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (firstLine && trimmed.equalsIgnoreCase("id,quantity")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String[] columns = trimmed.split(",");
                if (columns.length >= 2) {
                    String itemId = columns[0].trim();
                    String quantityText = columns[1].trim();
                    if (!itemId.isEmpty()) {
                        int quantity = parseQuantity(quantityText);
                        items.add(new GameData.InventoryEntry(itemId, quantity));
                    }
                }
            }
        } catch (IOException ignored) {
            // Return an empty list when the CSV is missing or unreadable.
        }

        return items;
    }

    private int parseQuantity(String quantityText) {
        try {
            int parsed = Integer.parseInt(quantityText);
            return Math.max(parsed, 0);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
