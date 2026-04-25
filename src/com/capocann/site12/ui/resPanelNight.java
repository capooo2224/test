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
import java.io.File;
import java.io.IOException;
import java.util.List;

public class resPanelNight extends JPanel {
    // UI Dimensions
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private static final int TOP_LEFT_BUTTON_SIZE = 46;
    private static final int TOP_LEFT_BUTTON_PADDING = 12;

    // Camera
    private static final int MAX_PAN_X = 24;
    private static final int MAX_PAN_Y = 14;
    private static final double CAMERA_LERP = 0.12;

    // Colors
    private static final Color DARK_GRAY = new Color(30, 30, 30);
    private static final Color MEDIUM_GRAY = new Color(58, 58, 58);
    private static final Color BORDER_GRAY = new Color(120, 120, 120);
    private static final Color LIGHT_GRAY = new Color(230, 230, 230);
    private static final Color BORDER_DARK = new Color(70, 70, 70);
    private static final Color HIGHLIGHT_YELLOW = new Color(255, 230, 120);
    private static final Color SEMI_WHITE = new Color(255, 255, 255, 70);

    // Fonts
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font DIALOG_TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.BOLD, 12);

    // Timers
    private static final int TIMER_DELAY = 16;
    private static final int BORDER_WIDTH = 2;
    private static final int BORDER_WIDTH_THICK = 3;

    // Dialog UI
    private static final int DIALOG_H_GAP = 0;
    private static final int DIALOG_V_GAP = 12;
    private static final int DIALOG_PADDING = 16;
    private static final int DIALOG_BUTTON_GAP = 8;
    private static final int GRID_ROWS = 2;
    private static final int GRID_COLS = 3;
    private static final int GRID_GAP = 10;
    private static final int GRID_ITEM_WIDTH = 150;
    private static final int GRID_ITEM_HEIGHT = 90;

    // Inventory
    private static final int INVENTORY_CARD_WIDTH = 430;
    private static final int INVENTORY_CARD_HEIGHT = 52;
    private static final int INVENTORY_SCROLL_WIDTH = 470;
    private static final int INVENTORY_SCROLL_HEIGHT = 230;
    private static final int INVENTORY_ITEM_STRUT = 8;
    private static final int INVENTORY_PADDING = 12;
    private static final int ALPHA_THRESHOLD = 10;

    // Background
    private final Image backgroundImage = new ImageIcon("assets/res/Backgrounds/SublabDay.png").getImage();

    // Mouse tracking
    private double targetMouseX = PANEL_WIDTH / 2.0;
    private double targetMouseY = PANEL_HEIGHT / 2.0;
    private double smoothMouseX = PANEL_WIDTH / 2.0;
    private double smoothMouseY = PANEL_HEIGHT / 2.0;
    private int lastMouseX = (int) (PANEL_WIDTH / 2.0);
    private int lastMouseY = (int) (PANEL_HEIGHT / 2.0);
    private boolean isMouseInsidePanel = false;

    // EDIT HERE: Adjust x, y, width, height for each rectangle below.
    private final Rectangle[] overlayRects = {
        new Rectangle(158, 260, 370, 450),
        new Rectangle(459, 259, 370, 470),
        new Rectangle(1000, 150, 770, 1280),
        new Rectangle(759, 259, 370, 600),
        new Rectangle(939, 292, 370, 600),
        new Rectangle(1000, 285, 370, 600)
    };

    // EDIT HERE: Set image paths for each rectangle (must match the rectangles above).
    private final String[] overlayImagePaths = {
        "assets/res/Characters/Kriegs/AliveKriegs.png",
        "assets/res/Characters/Azrael/AliveAzrael.png",
        "assets/res/Characters/Gambit/AliveGambit.png",
        "assets/res/Characters/Lazarus/AliveLazarus.png",
        "assets/res/Characters/Raphaela/AliveRaphaela.png",
        "assets/res/Characters/Terry/AliveTerry.png"
    };

    private final GameData gameData = new GameData();
    private final String[] characterIds = {"kriegs", "azrael", "gambit", "lazarus", "raphaela", "terry"};
    private final BufferedImage[] overlayImages = loadOverlayImages();
    private int hoveredOverlayIndex = -1;

    public resPanelNight(Main main) {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setLayout(new BorderLayout());
        loadInventoryData();

        JPanel topLeftContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, TOP_LEFT_BUTTON_PADDING, TOP_LEFT_BUTTON_PADDING));
        topLeftContainer.setOpaque(false);

        JButton lowerHpTestButton = new JButton("Lower HP Test");
        lowerHpTestButton.setFocusPainted(false);
        lowerHpTestButton.addActionListener(e -> {
            lowerAllCharacterHpForTest();
            refreshOverlayImagesFromStats();
            repaint();
        });
        topLeftContainer.add(lowerHpTestButton);

        JButton healHpTestButton = new JButton("Heal HP Test");
        healHpTestButton.setFocusPainted(false);
        healHpTestButton.addActionListener(e -> {
            healAllCharacterHpForTest();
            refreshOverlayImagesFromStats();
            repaint();
        });
        topLeftContainer.add(healHpTestButton);
        add(topLeftContainer, BorderLayout.NORTH);

        Timer cameraEaseTimer = new Timer(TIMER_DELAY, e -> {
            smoothMouseX += (targetMouseX - smoothMouseX) * CAMERA_LERP;
            smoothMouseY += (targetMouseY - smoothMouseY) * CAMERA_LERP;
            if (isMouseInsidePanel) {
                updateHoveredOverlay(lastMouseX, lastMouseY);
            }
            repaint();
        });
        cameraEaseTimer.start();

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                targetMouseX = e.getX();
                targetMouseY = e.getY();
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                isMouseInsidePanel = true;
                updateHoveredOverlay(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                targetMouseX = getWidth() / 2.0;
                targetMouseY = getHeight() / 2.0;
                isMouseInsidePanel = false;
                hoveredOverlayIndex = -1;
                setCursor(Cursor.getDefaultCursor());
            }
        });

        JLabel label = new JLabel("60secs Placeholder Screen", SwingConstants.CENTER);
        label.setFont(TITLE_FONT);
        add(label, BorderLayout.CENTER);

        JButton back = new JButton("Back to Menu");
        back.addActionListener(e -> main.showScreen("Menu"));
        add(back, BorderLayout.SOUTH);
    }

    private void showPlaceholderUI() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog placeholderDialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        placeholderDialog.setUndecorated(true);
        placeholderDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel dialogContent = new JPanel(new BorderLayout(DIALOG_H_GAP, DIALOG_V_GAP));
        dialogContent.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_DARK, BORDER_WIDTH),
            BorderFactory.createEmptyBorder(DIALOG_PADDING, DIALOG_PADDING, DIALOG_PADDING, DIALOG_PADDING)
        ));
        dialogContent.setBackground(DARK_GRAY);

        JLabel title = new JLabel("Placeholder UI", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(DIALOG_TITLE_FONT);
        dialogContent.add(title, BorderLayout.NORTH);

        JPanel bodyPanel = new JPanel(new BorderLayout(0, 10));
        bodyPanel.setOpaque(false);

        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.add(createPlaceholderGrid("Tab 1 - Rectangle "), "tab1");
        cardPanel.add(createInventoryListPanel(), "tab2");

        JPanel tabButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, DIALOG_BUTTON_GAP, 0));
        tabButtons.setOpaque(false);
        JButton tab1Button = new JButton("Tab 1");
        JButton tab2Button = new JButton("Tab 2");
        tab1Button.addActionListener(e -> cardLayout.show(cardPanel, "tab1"));
        tab2Button.addActionListener(e -> cardLayout.show(cardPanel, "tab2"));
        tabButtons.add(tab1Button);
        tabButtons.add(tab2Button);

        bodyPanel.add(tabButtons, BorderLayout.NORTH);
        bodyPanel.add(cardPanel, BorderLayout.CENTER);
        dialogContent.add(bodyPanel, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> placeholderDialog.dispose());
        dialogContent.add(closeButton, BorderLayout.SOUTH);

        placeholderDialog.setContentPane(dialogContent);
        placeholderDialog.pack();
        placeholderDialog.setLocationRelativeTo(this);
        placeholderDialog.setVisible(true);
    }

    private void loadInventoryData() {
        InventoryCsvReader csvReader = new InventoryCsvReader();
        List<GameData.InventoryEntry> loadedItems = csvReader.readInventoryItems("data/inventory.csv");

        if (loadedItems.isEmpty()) {
            loadedItems = List.of(new GameData.InventoryEntry("no_data", 0));
        }
        gameData.setInventoryItems(loadedItems);
    }

    private void lowerAllCharacterHpForTest() {
        for (String characterId : characterIds) {
            GameData.CharacterStats stats = gameData.getCharacterStats(characterId);
            if (stats == null) {
                continue;
            }

            int targetHealth = (int) Math.floor(stats.getMaxHealth() * 0.30);
            stats.setCurrentHealth(targetHealth);
        }
    }

    private void healAllCharacterHpForTest() {
        for (String characterId : characterIds) {
            GameData.CharacterStats stats = gameData.getCharacterStats(characterId);
            if (stats == null) {
                continue;
            }

            stats.setCurrentHealth(stats.getMaxHealth());
        }
    }

    private void refreshOverlayImagesFromStats() {
        BufferedImage[] refreshed = loadOverlayImages();
        int copyLen = Math.min(overlayImages.length, refreshed.length);
        System.arraycopy(refreshed, 0, overlayImages, 0, copyLen);
    }

    private JPanel createPlaceholderGrid(String labelPrefix) {
        JPanel grid = new JPanel(new GridLayout(GRID_ROWS, GRID_COLS, GRID_GAP, GRID_GAP));
        grid.setOpaque(false);
        for (int i = 1; i <= 6; i++) {
            JPanel rect = new JPanel(new BorderLayout());
            rect.setPreferredSize(new Dimension(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            rect.setBackground(MEDIUM_GRAY);
            rect.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, BORDER_WIDTH));

            JLabel rectLabel = new JLabel(labelPrefix + i, SwingConstants.CENTER);
            rectLabel.setForeground(Color.WHITE);
            rect.add(rectLabel, BorderLayout.CENTER);

            grid.add(rect);
        }
        return grid;
    }

    private JPanel createInventoryListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout(8, 8));
        listPanel.setOpaque(false);

        JLabel inventoryTitle = new JLabel("Inventory Items", SwingConstants.LEFT);
        inventoryTitle.setForeground(Color.WHITE);
        inventoryTitle.setFont(LABEL_FONT);

        JPanel itemsContainer = new JPanel();
        itemsContainer.setOpaque(false);
        itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));

        for (GameData.InventoryEntry item : gameData.getInventoryItems()) {
            if (item.getQuantity() <= 0) {
                continue;
            }

            JPanel itemCard = new JPanel(new BorderLayout());
            itemCard.setBackground(MEDIUM_GRAY);
            itemCard.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, BORDER_WIDTH));
            itemCard.setPreferredSize(new Dimension(INVENTORY_CARD_WIDTH, INVENTORY_CARD_HEIGHT));
            itemCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, INVENTORY_CARD_HEIGHT));

            JLabel itemNameLabel = new JLabel(formatItemNameFromId(item.getItemId()), SwingConstants.LEFT);
            itemNameLabel.setForeground(Color.WHITE);
            itemNameLabel.setFont(LABEL_FONT);
            itemNameLabel.setBorder(BorderFactory.createEmptyBorder(0, INVENTORY_PADDING, 0, INVENTORY_PADDING));
            itemCard.add(itemNameLabel, BorderLayout.CENTER);

            JLabel quantityLabel = new JLabel("x" + item.getQuantity(), SwingConstants.RIGHT);
            quantityLabel.setForeground(LIGHT_GRAY);
            quantityLabel.setFont(LABEL_FONT);
            quantityLabel.setBorder(BorderFactory.createEmptyBorder(0, INVENTORY_PADDING, 0, INVENTORY_PADDING));
            itemCard.add(quantityLabel, BorderLayout.EAST);

            itemsContainer.add(itemCard);
            itemsContainer.add(Box.createVerticalStrut(INVENTORY_ITEM_STRUT));
        }

        if (itemsContainer.getComponentCount() == 0) {
            JLabel emptyLabel = new JLabel("No inventory data found", SwingConstants.CENTER);
            emptyLabel.setForeground(Color.WHITE);
            itemsContainer.setLayout(new BorderLayout());
            itemsContainer.add(emptyLabel, BorderLayout.CENTER);
        }

        JScrollPane listScrollPane = new JScrollPane(itemsContainer);
        listScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, BORDER_WIDTH));
        listScrollPane.setPreferredSize(new Dimension(INVENTORY_SCROLL_WIDTH, INVENTORY_SCROLL_HEIGHT));
        listScrollPane.getViewport().setOpaque(false);
        listScrollPane.setOpaque(false);

        listPanel.add(inventoryTitle, BorderLayout.NORTH);
        listPanel.add(listScrollPane, BorderLayout.CENTER);
        return listPanel;
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int panelW = getWidth();
        int panelH = getHeight();

        int cameraOffsetX = getCameraOffsetX();
        int cameraOffsetY = getCameraOffsetY();
        int drawW = panelW + (2 * MAX_PAN_X);
        int drawH = panelH + (2 * MAX_PAN_Y);

        g.drawImage(backgroundImage, cameraOffsetX, cameraOffsetY, drawW, drawH, this);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setStroke(new BasicStroke(BORDER_WIDTH_THICK));
        g2.setFont(SMALL_FONT);
        for (int i = 0; i < overlayRects.length; i++) {
            Rectangle rect = getCameraShiftedRect(overlayRects[i]);
            BufferedImage img = overlayImages[i];
            if (img != null) {
                Rectangle drawRect = getFittedImageRect(i);
                g2.drawImage(img, drawRect.x, drawRect.y, drawRect.width, drawRect.height, this);
            } else {
                g2.setColor(SEMI_WHITE);
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            }
            g2.setColor(i == hoveredOverlayIndex ? HIGHLIGHT_YELLOW : Color.WHITE);
            g2.drawRect(rect.x, rect.y, rect.width, rect.height);
            g2.drawString("Rect " + (i + 1), rect.x + 8, rect.y + 18);
        }
        g2.dispose();
    }

    private BufferedImage[] loadOverlayImages() {
        BufferedImage[] images = new BufferedImage[overlayImagePaths.length];
        for (int i = 0; i < overlayImagePaths.length; i++) {
            String imagePath = overlayImagePaths[i];
            if (i < characterIds.length) {
                GameData.CharacterStats stats = gameData.getCharacterStats(characterIds[i]);
                if (stats != null) {
                    imagePath = stats.getCurrentImagePath();
                }
            }

            try {
                images[i] = ImageIO.read(new File(imagePath));
            } catch (IOException e) {
                images[i] = null;
            }
        }
        return images;
    }

    private Rectangle getFittedImageRect(int index) {
        Rectangle rect = getCameraShiftedRect(overlayRects[index]);
        BufferedImage img = overlayImages[index];
        if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) {
            return new Rectangle(rect);
        }

        double scale = Math.min((double) rect.width / img.getWidth(), (double) rect.height / img.getHeight());
        int scaledW = (int) Math.round(img.getWidth() * scale);
        int scaledH = (int) Math.round(img.getHeight() * scale);
        int imageX = rect.x + (rect.width - scaledW) / 2;
        int imageY = rect.y + (rect.height - scaledH) / 2;
        return new Rectangle(imageX, imageY, scaledW, scaledH);
    }

    private Rectangle getCameraShiftedRect(Rectangle baseRect) {
        return new Rectangle(baseRect.x + getCameraOffsetX(), baseRect.y + getCameraOffsetY(), baseRect.width, baseRect.height);
    }

    private int getCameraOffsetX() {
        int panelW = getWidth();
        if (panelW <= 0) {
            return 0;
        }
        double xRatio = smoothMouseX / panelW;
        return (int) (MAX_PAN_X - (2 * MAX_PAN_X * xRatio));
    }

    private int getCameraOffsetY() {
        int panelH = getHeight();
        if (panelH <= 0) {
            return 0;
        }
        double yRatio = smoothMouseY / panelH;
        return (int) (MAX_PAN_Y - (2 * MAX_PAN_Y * yRatio));
    }

    private void updateHoveredOverlay(int mouseX, int mouseY) {
        int hitIndex = -1;
        for (int i = 0; i < overlayImages.length; i++) {
            if (isPointOnVisiblePixel(i, mouseX, mouseY)) {
                hitIndex = i;
                break;
            }
        }

        if (hitIndex != hoveredOverlayIndex) {
            hoveredOverlayIndex = hitIndex;
            setCursor(hitIndex >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            repaint();
        }
    }

    private boolean isPointOnVisiblePixel(int index, int mouseX, int mouseY) {
        BufferedImage img = overlayImages[index];
        if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) {
            return false;
        }

        Rectangle drawRect = getFittedImageRect(index);
        if (!drawRect.contains(mouseX, mouseY) || drawRect.width <= 0 || drawRect.height <= 0) {
            return false;
        }

        double normX = (mouseX - drawRect.x) / (double) drawRect.width;
        double normY = (mouseY - drawRect.y) / (double) drawRect.height;
        int srcX = Math.min(img.getWidth() - 1, Math.max(0, (int) (normX * img.getWidth())));
        int srcY = Math.min(img.getHeight() - 1, Math.max(0, (int) (normY * img.getHeight())));
        int alpha = (img.getRGB(srcX, srcY) >>> 24) & 0xFF;
        return alpha > ALPHA_THRESHOLD;
    }
}
