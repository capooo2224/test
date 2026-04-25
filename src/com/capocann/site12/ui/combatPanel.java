package com.capocann.site12.ui;

import javax.swing.*;
import javax.imageio.ImageIO;

import com.capocann.site12.GameData;
import com.capocann.site12.Main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class combatPanel extends JPanel {
    private static final String LABEL_BATTLE_LOG = "Battle Text";
    private static final String LABEL_ATTACK_BUTTON = "Attack";
    private static final String LABEL_RUN_BUTTON = "Run";
    private static final int BASE_DAMAGE = 12;
    private static final Dimension SIDE_PORTRAIT_BLOCK = new Dimension(290, 350);
    private static final Dimension ENEMY_TRIANGLE_BLOCK = new Dimension(500, 650);
    private static final Dimension BATTLE_LOG_BLOCK = new Dimension(750, 250);
    private static final Dimension ACTION_BLOCK = new Dimension(750, 150);

    private static final String[] ALLY_IDS = {"kriegs", "azrael", "gambit"};
    private static final String[] SCAVENGE_ORDER = {"kriegs", "azrael", "gambit", "lazarus", "raphaela", "terry"};
    private static final String[] ENEMY_IDS = {"enemy_raider_1", "enemy_raider_2", "enemy_raider_3"};

    private final Image backgroundImage = new ImageIcon("assets/Backgrounds/background.png").getImage();
    private final Main main;
    private final GameData gameData = GameData.getInstance();
    private final Random random = new Random();

    private final JTextArea battleLogArea = new JTextArea();
    private final JPanel allyPanel = new JPanel(new GridLayout(3, 1, 8, 8));
    private final JPanel reserveAllyPanel = new JPanel(new GridLayout(3, 1, 8, 8));
    private final JPanel enemyTrianglePanel = new JPanel(null);
    private final JLabel[] enemyTrianglePortraits = new JLabel[3];
    private final CardLayout actionCardLayout = new CardLayout();
    private final JPanel actionCardHost = new JPanel(actionCardLayout);
    private final DefaultListModel<String> attackOverlayListModel = new DefaultListModel<>();
    private final JList<String> attackOverlayList = new JList<>(attackOverlayListModel);
    private List<MoveDef> attackOverlayMoves = List.of();
    private List<String> attackOverlayActors = List.of();
    private String pendingMoveActorId;
    private final Map<String, GameData.CharacterStats> enemyStats = new HashMap<>();
    private final Map<String, List<MoveDef>> movesByCharacter = new HashMap<>();
    private final Map<String, Integer> moveCooldowns = new HashMap<>();
    private int allyTurnIndex = 0;

    private static class MoveDef {
        private final String moveName;
        private final String description;
        private final int hitMultiplier;
        private final int cooldown;
        private final GameData.StatusEffect appliesStatus;

        private MoveDef(String moveName, String description, int hitMultiplier, int cooldown,
                GameData.StatusEffect appliesStatus) {
            this.moveName = moveName;
            this.description = description;
            this.hitMultiplier = hitMultiplier;
            this.cooldown = cooldown;
            this.appliesStatus = appliesStatus;
        }
    }

    public combatPanel(Main main) {
        this.main = main;
        setPreferredSize(new Dimension(800, 600));
        setLayout(new GridBagLayout());

        initializeEnemyRoster();
        loadMovesFromTxt();

        allyPanel.setOpaque(false);
        reserveAllyPanel.setOpaque(false);

        JPanel allySlotsBlock = wrapPanel("Ally Portrait Slots", allyPanel);
        allySlotsBlock.setPreferredSize(SIDE_PORTRAIT_BLOCK);

        JPanel middleColumnBlock = createMiddleColumnBlock();
        middleColumnBlock.setPreferredSize(ENEMY_TRIANGLE_BLOCK);

        JPanel reserveSlotsBlock = wrapPanel("Ally Portrait Slots", reserveAllyPanel);
        reserveSlotsBlock.setPreferredSize(SIDE_PORTRAIT_BLOCK);

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 0.18;
        left.weighty = 1.0;
        left.insets = new Insets(10, 10, 10, 6);
        left.fill = GridBagConstraints.BOTH;
        add(allySlotsBlock, left);

        GridBagConstraints middle = new GridBagConstraints();
        middle.gridx = 1;
        middle.gridy = 0;
        middle.weightx = 0.64;
        middle.weighty = 1.0;
        middle.insets = new Insets(10, 6, 10, 6);
        middle.fill = GridBagConstraints.BOTH;
        add(middleColumnBlock, middle);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 2;
        right.gridy = 0;
        right.weightx = 0.18;
        right.weighty = 1.0;
        right.insets = new Insets(10, 6, 10, 10);
        right.fill = GridBagConstraints.BOTH;
        add(reserveSlotsBlock, right);

        startNewEncounter();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }

    private JPanel wrapPanel(String title, Component component) {
        JPanel wrapper = new JPanel(new BorderLayout(6, 6));
        wrapper.setOpaque(true);
        wrapper.setBackground(new Color(15, 15, 15, 210));
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(72, 72, 72), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JLabel label = new JLabel(title);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
        wrapper.add(label, BorderLayout.NORTH);
        wrapper.add(component, BorderLayout.CENTER);
        return wrapper;
    }

    private JScrollPane createBattleLogPane() {
        battleLogArea.setEditable(false);
        battleLogArea.setLineWrap(true);
        battleLogArea.setWrapStyleWord(true);
        battleLogArea.setForeground(new Color(240, 240, 240));
        battleLogArea.setOpaque(false);
        battleLogArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        battleLogArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JScrollPane logScroll = new JScrollPane(battleLogArea);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 1));
        return logScroll;
    }

    private JPanel createMiddleColumnBlock() {
        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);

        JPanel logPanel = wrapPanel(LABEL_BATTLE_LOG, createBattleLogPane());
        logPanel.setPreferredSize(BATTLE_LOG_BLOCK);

        JPanel enemyTrianglesBlock = wrapPanel("Enemy Triangle Placeholder", createEnemyTrianglePanel());
        enemyTrianglesBlock.setPreferredSize(ENEMY_TRIANGLE_BLOCK);

        JPanel actionButtons = new JPanel(new GridLayout(1, 2, 14, 0));
        actionButtons.setOpaque(false);
        actionButtons.setPreferredSize(ACTION_BLOCK);

        JButton attackButton = new JButton(LABEL_ATTACK_BUTTON);
        attackButton.setFont(new Font("SansSerif", Font.BOLD, 30));
        attackButton.addActionListener(e -> onAttackPressed());

        JButton runButton = new JButton(LABEL_RUN_BUTTON);
        runButton.setFont(new Font("SansSerif", Font.BOLD, 30));
        runButton.addActionListener(e -> onRunPressed());

        actionButtons.add(attackButton);
        actionButtons.add(runButton);

        JPanel actionOverlay = createAttackOverlayPanel();
        actionCardHost.setOpaque(false);
        actionCardHost.add(actionButtons, "buttons");
        actionCardHost.add(actionOverlay, "overlay");
        actionCardLayout.show(actionCardHost, "buttons");

        GridBagConstraints top = new GridBagConstraints();
        top.gridx = 0;
        top.gridy = 0;
        top.weightx = 1.0;
        top.weighty = 0.26;
        top.insets = new Insets(0, 0, 8, 0);
        top.fill = GridBagConstraints.BOTH;
        middle.add(logPanel, top);

        GridBagConstraints center = new GridBagConstraints();
        center.gridx = 0;
        center.gridy = 1;
        center.weightx = 1.0;
        center.weighty = 0.54;
        center.insets = new Insets(0, 0, 8, 0);
        center.fill = GridBagConstraints.BOTH;
        middle.add(enemyTrianglesBlock, center);

        GridBagConstraints bottom = new GridBagConstraints();
        bottom.gridx = 0;
        bottom.gridy = 2;
        bottom.weightx = 1.0;
        bottom.weighty = 0.20;
        bottom.fill = GridBagConstraints.BOTH;
        middle.add(actionCardHost, bottom);

        return middle;
    }

    private JPanel createEnemyTrianglePanel() {
        enemyTrianglePanel.setOpaque(false);
        for (int i = 0; i < enemyTrianglePortraits.length; i++) {
            JLabel portrait = new JLabel();
            portrait.setHorizontalAlignment(SwingConstants.CENTER);
            portrait.setVerticalAlignment(SwingConstants.CENTER);
            portrait.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 2));
            portrait.setOpaque(true);
            portrait.setBackground(new Color(22, 22, 22, 210));
            enemyTrianglePortraits[i] = portrait;
            enemyTrianglePanel.add(portrait);
        }

        enemyTrianglePanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                layoutEnemyTrianglePortraits();
            }
        });

        layoutEnemyTrianglePortraits();
        return enemyTrianglePanel;
    }

    private void layoutEnemyTrianglePortraits() {
        int w = enemyTrianglePanel.getWidth();
        int h = enemyTrianglePanel.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        int slotW = Math.max(70, (int) (w * 0.30));
        int slotH = Math.max(88, (int) (h * 0.24));

        int topX = (w - slotW) / 2;
        int topY = (int) (h * 0.18);
        int botLeftX = (int) (w * 0.18);
        int botRightX = w - botLeftX - slotW;
        int botY = (int) (h * 0.56);

        enemyTrianglePortraits[0].setBounds(topX, topY, slotW, slotH);
        enemyTrianglePortraits[1].setBounds(botLeftX, botY, slotW, slotH);
        enemyTrianglePortraits[2].setBounds(botRightX, botY, slotW, slotH);
    }

    private void initializeEnemyRoster() {
        for (String enemyId : ENEMY_IDS) {
            enemyStats.put(enemyId, new GameData.CharacterStats(enemyId, 140));
        }
    }

    private void refreshBattlePanels() {
        allyPanel.removeAll();
        reserveAllyPanel.removeAll();

        String[] activeScavengeParty = buildScavengePartySlots();

        for (int i = 0; i < 3; i++) {
            String allyId = activeScavengeParty[i];
            allyPanel.add(createAllySlotCard(allyId));
        }

        for (int i = 3; i < 6; i++) {
            String allyId = activeScavengeParty[i];
            reserveAllyPanel.add(createAllySlotCard(allyId));
        }

        for (int i = 0; i < enemyTrianglePortraits.length && i < ENEMY_IDS.length; i++) {
            enemyTrianglePortraits[i].setIcon(loadPortraitIcon(ENEMY_IDS[i], 96, 96));
            enemyTrianglePortraits[i].setText("<html><center><b>" + formatName(ENEMY_IDS[i]) + "</b></center></html>");
            enemyTrianglePortraits[i].setForeground(Color.WHITE);
        }

        allyPanel.revalidate();
        reserveAllyPanel.revalidate();
        allyPanel.repaint();
        reserveAllyPanel.repaint();
        enemyTrianglePanel.repaint();
    }

    private JPanel createAllySlotCard(String allyId) {
        if (allyId == null || allyId.isBlank()) {
            JPanel blank = new JPanel(new BorderLayout());
            blank.setBackground(new Color(22, 22, 22, 190));
            blank.setBorder(BorderFactory.createDashedBorder(new Color(96, 96, 96), 2, 4));
            JLabel text = new JLabel("EMPTY", SwingConstants.CENTER);
            text.setForeground(new Color(180, 180, 180));
            text.setFont(new Font("SansSerif", Font.BOLD, 14));
            blank.add(text, BorderLayout.CENTER);
            return blank;
        }

        return createUnitCard(allyId, gameData.getCharacterStats(allyId), true);
    }

    private String[] buildScavengePartySlots() {
        String[] slots = new String[6];
        int next = 0;
        for (String id : SCAVENGE_ORDER) {
            GameData.CharacterStats stats = gameData.getCharacterStats(id);
            if (stats == null || !stats.isAlive()) {
                continue;
            }
            if (next < slots.length) {
                slots[next++] = id;
            }
        }
        return slots;
    }

    private JPanel createUnitCard(String id, GameData.CharacterStats stats, boolean allySide) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setBackground(new Color(28, 28, 28, 225));
        card.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 1));

        JLabel portrait = new JLabel(loadPortraitIcon(id), SwingConstants.CENTER);
        portrait.setPreferredSize(new Dimension(86, 86));
        card.add(portrait, BorderLayout.WEST);

        String role = allySide ? "ALLY" : "ENEMY";
        String unitName = formatName(id);
        int hp = stats == null ? 0 : stats.getCurrentHealth();
        int max = stats == null ? 1 : Math.max(1, stats.getMaxHealth());
        int moraleStacks = stats == null ? 0 : stats.getStatusStacks(GameData.StatusEffect.MORALE);
        String statsText = String.format("%s  HP %d/%d  MOR %d", role, hp, max, moraleStacks);

        JLabel text = new JLabel("<html><b>" + unitName + "</b><br/>" + statsText + "</html>");
        text.setForeground(Color.WHITE);
        text.setFont(new Font("SansSerif", Font.PLAIN, 12));
        card.add(text, BorderLayout.CENTER);

        return card;
    }

    private void onAttackPressed() {
        List<String> livingAllies = getLivingAlliesInCombatOrder();
        if (livingAllies.isEmpty()) {
            appendBattleLog("All allies are down. Returning to exploration.");
            main.showScreen("Tiles");
            return;
        }

        showActorSelectionOverlay(livingAllies);
    }

    private JPanel createAttackOverlayPanel() {
        JPanel overlay = new JPanel(new BorderLayout(10, 10));
        overlay.setOpaque(true);
        overlay.setBackground(new Color(15, 15, 15, 235));
        overlay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        attackOverlayList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        attackOverlayList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        attackOverlayList.setBackground(new Color(24, 24, 24));
        attackOverlayList.setForeground(Color.WHITE);
        attackOverlayList.setSelectionBackground(new Color(76, 94, 128));
        attackOverlayList.setSelectionForeground(Color.WHITE);

        JScrollPane listPane = new JScrollPane(attackOverlayList);
        listPane.setBorder(BorderFactory.createLineBorder(new Color(78, 78, 78), 1));
        overlay.add(listPane, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(1, 2, 10, 0));
        controls.setOpaque(false);

        JButton confirm = new JButton("Use Move");
        confirm.setFont(new Font("SansSerif", Font.BOLD, 16));
        confirm.addActionListener(e -> confirmAttackOverlayMove());

        JButton cancel = new JButton("Back");
        cancel.setFont(new Font("SansSerif", Font.BOLD, 16));
        cancel.addActionListener(e -> hideAttackOverlay());

        controls.add(confirm);
        controls.add(cancel);
        overlay.add(controls, BorderLayout.SOUTH);
        return overlay;
    }

    private void showAttackOverlay(String actorId, List<MoveDef> moveList) {
        pendingMoveActorId = actorId;
        attackOverlayActors = List.of();
        attackOverlayMoves = moveList;
        attackOverlayListModel.clear();
        for (MoveDef move : moveList) {
            attackOverlayListModel.addElement(move.moveName + " - " + move.description);
        }
        if (!moveList.isEmpty()) {
            attackOverlayList.setSelectedIndex(0);
        }
        actionCardLayout.show(actionCardHost, "overlay");
    }

    private void showActorSelectionOverlay(List<String> actorIds) {
        pendingMoveActorId = null;
        attackOverlayMoves = List.of();
        attackOverlayActors = new ArrayList<>(actorIds);
        attackOverlayListModel.clear();

        for (String actorId : actorIds) {
            GameData.CharacterStats stats = gameData.getCharacterStats(actorId);
            int hp = stats == null ? 0 : stats.getCurrentHealth();
            int max = stats == null ? 1 : Math.max(1, stats.getMaxHealth());
            attackOverlayListModel.addElement(formatName(actorId) + "  (HP " + hp + "/" + max + ")");
        }

        if (!actorIds.isEmpty()) {
            attackOverlayList.setSelectedIndex(0);
        }
        actionCardLayout.show(actionCardHost, "overlay");
    }

    private void hideAttackOverlay() {
        actionCardLayout.show(actionCardHost, "buttons");
        pendingMoveActorId = null;
        attackOverlayMoves = List.of();
        attackOverlayActors = List.of();
        attackOverlayListModel.clear();
    }

    private void confirmAttackOverlayMove() {
        int selected = attackOverlayList.getSelectedIndex();
        if (selected < 0) {
            appendBattleLog("Choose a move first.");
            return;
        }

        if (pendingMoveActorId == null) {
            if (selected >= attackOverlayActors.size()) {
                appendBattleLog("Choose a character first.");
                return;
            }

            String actorId = attackOverlayActors.get(selected);
            List<MoveDef> moveList = movesByCharacter.getOrDefault(actorId, List.of());
            if (moveList.isEmpty()) {
                appendBattleLog(formatName(actorId) + " has no loaded moves. Using basic attack.");
                executeMove(actorId, new MoveDef("Basic Strike", "Fallback attack", 1, 0, null));
                hideAttackOverlay();
                return;
            }

            showAttackOverlay(actorId, moveList);
            return;
        }

        if (selected >= attackOverlayMoves.size()) {
            appendBattleLog("Choose a move first.");
            return;
        }

        MoveDef chosen = attackOverlayMoves.get(selected);
        String cooldownKey = pendingMoveActorId + ":" + chosen.moveName;
        int remainingCd = moveCooldowns.getOrDefault(cooldownKey, 0);
        if (remainingCd > 0) {
            appendBattleLog(chosen.moveName + " is on cooldown for " + remainingCd + " more turn(s).");
            return;
        }

        executeMove(pendingMoveActorId, chosen);
        if (chosen.cooldown > 0) {
            moveCooldowns.put(cooldownKey, chosen.cooldown);
        }
        hideAttackOverlay();
    }

    private void executeMove(String actorId, MoveDef move) {
        GameData.CharacterStats actor = gameData.getCharacterStats(actorId);
        String enemyId = getFirstLivingEnemy();
        if (actor == null || enemyId == null) {
            appendBattleLog("No valid target found.");
            return;
        }

        GameData.CharacterStats target = enemyStats.get(enemyId);
        double damageMultiplier = move.hitMultiplier;
        if (actor.getStatusStacks(GameData.StatusEffect.FIRE) > 0) {
            damageMultiplier *= 1.0 + (GameData.FIRE_DAMAGE_BOOST_PERCENT / 100.0);
        }
        damageMultiplier *= actor.getMoraleMultiplier();

        int damage = Math.max(1, (int) Math.round(BASE_DAMAGE * damageMultiplier));
        target.takeDamage(damage);
        appendBattleLog(formatName(actorId) + " used " + move.moveName + " on " + formatName(enemyId)
                + " for " + damage + " damage.");

        if (move.appliesStatus != null) {
            target.addStatusStacks(move.appliesStatus, 1);
            appendBattleLog(formatName(enemyId) + " gained " + move.appliesStatus + ".");
        }

        handleDeathsAndDots();
        performEnemyTurn();
        tickCooldowns();
        advanceTurnOrder();
        refreshBattlePanels();

        if (allEnemiesDefeated()) {
            appendBattleLog("Enemies defeated. Returning to exploration.");
            JOptionPane.showMessageDialog(this, "Combat won!", "Victory", JOptionPane.INFORMATION_MESSAGE);
            main.showScreen("Tiles");
        }
    }

    private void performEnemyTurn() {
        String enemyId = pickEnemyByCrippleOrder();
        String allyId = getFirstLivingAlly();
        if (enemyId == null || allyId == null) {
            return;
        }

        GameData.CharacterStats enemy = enemyStats.get(enemyId);
        GameData.CharacterStats ally = gameData.getCharacterStats(allyId);
        if (enemy == null || ally == null) {
            return;
        }

        int damage = Math.max(1, (int) Math.round(10 * enemy.getMoraleMultiplier()));
        if (enemy.getStatusStacks(GameData.StatusEffect.FIRE) > 0) {
            damage = Math.max(1, (int) Math.round(damage * 1.1));
        }

        ally.takeDamage(damage);
        appendBattleLog(formatName(enemyId) + " attacks " + formatName(allyId) + " for " + damage + " damage.");
        if (!ally.isAlive()) {
            gameData.handleCharacterDeath(allyId);
            appendBattleLog(formatName(allyId) + " was defeated. Morale penalty applied.");
        }

        handleDeathsAndDots();
    }

    private void handleDeathsAndDots() {
        for (String allyId : ALLY_IDS) {
            GameData.CharacterStats ally = gameData.getCharacterStats(allyId);
            if (ally == null) {
                continue;
            }

            if (ally.isAlive()) {
                ally.applyEndTurnEffects();
                if (!ally.isAlive()) {
                    gameData.handleCharacterDeath(allyId);
                    appendBattleLog(formatName(allyId) + " fell from status effects.");
                }
            }
        }

        for (String enemyId : ENEMY_IDS) {
            GameData.CharacterStats enemy = enemyStats.get(enemyId);
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            enemy.applyEndTurnEffects();
            if (!enemy.isAlive()) {
                appendBattleLog(formatName(enemyId) + " was finished by status effects.");
            }
        }
    }

    private void onRunPressed() {
        boolean escaped = random.nextBoolean();
        if (escaped) {
            appendBattleLog("Run attempt succeeded (50/50). Returning to exploration.");
            main.showScreen("Tiles");
        } else {
            appendBattleLog("Run failed. Turn passed.");
            performEnemyTurn();
            tickCooldowns();
            advanceTurnOrder();
            refreshBattlePanels();
        }
    }

    public void startNewEncounter() {
        initializeEnemyRoster();
        moveCooldowns.clear();
        allyTurnIndex = 0;
        hideAttackOverlay();
        battleLogArea.setText("");
        refreshBattlePanels();
        appendBattleLog("Combat started. " + getCurrentActorDisplayName() + " takes the first action.");
    }

    private List<String> getLivingAlliesInCombatOrder() {
        List<String> living = new ArrayList<>();
        for (String allyId : SCAVENGE_ORDER) {
            GameData.CharacterStats stats = gameData.getCharacterStats(allyId);
            if (stats != null && stats.isAlive()) {
                living.add(allyId);
            }
        }
        return living;
    }

    private void tickCooldowns() {
        List<String> keys = new ArrayList<>(moveCooldowns.keySet());
        for (String key : keys) {
            int left = moveCooldowns.getOrDefault(key, 0) - 1;
            if (left <= 0) {
                moveCooldowns.remove(key);
            } else {
                moveCooldowns.put(key, left);
            }
        }
    }

    private void advanceTurnOrder() {
        allyTurnIndex = (allyTurnIndex + 1) % ALLY_IDS.length;
    }

    private String getCurrentLivingAlly() {
        for (int i = 0; i < ALLY_IDS.length; i++) {
            String allyId = ALLY_IDS[(allyTurnIndex + i) % ALLY_IDS.length];
            GameData.CharacterStats stats = gameData.getCharacterStats(allyId);
            if (stats != null && stats.isAlive()) {
                allyTurnIndex = (allyTurnIndex + i) % ALLY_IDS.length;
                return allyId;
            }
        }
        return null;
    }

    private String getFirstLivingAlly() {
        for (String allyId : ALLY_IDS) {
            GameData.CharacterStats stats = gameData.getCharacterStats(allyId);
            if (stats != null && stats.isAlive()) {
                return allyId;
            }
        }
        return null;
    }

    private String getFirstLivingEnemy() {
        for (String enemyId : ENEMY_IDS) {
            GameData.CharacterStats stats = enemyStats.get(enemyId);
            if (stats != null && stats.isAlive()) {
                return enemyId;
            }
        }
        return null;
    }

    private String pickEnemyByCrippleOrder() {
        String crippledCandidate = null;
        for (String enemyId : ENEMY_IDS) {
            GameData.CharacterStats stats = enemyStats.get(enemyId);
            if (stats == null || !stats.isAlive()) {
                continue;
            }
            if (stats.getStatusStacks(GameData.StatusEffect.CRIPPLE) > 0) {
                if (crippledCandidate == null) {
                    crippledCandidate = enemyId;
                }
            } else {
                return enemyId;
            }
        }
        return crippledCandidate;
    }

    private boolean allEnemiesDefeated() {
        for (String enemyId : ENEMY_IDS) {
            GameData.CharacterStats stats = enemyStats.get(enemyId);
            if (stats != null && stats.isAlive()) {
                return false;
            }
        }
        return true;
    }

    private void appendBattleLog(String message) {
        battleLogArea.append(message + "\n");
        battleLogArea.setCaretPosition(battleLogArea.getDocument().getLength());
    }

    private String getCurrentActorDisplayName() {
        String actorId = getCurrentLivingAlly();
        return actorId == null ? "No one" : formatName(actorId);
    }

    private void loadMovesFromTxt() {
        movesByCharacter.clear();
        File moveFile = new File("moves.txt");
        if (!moveFile.exists()) {
            return;
        }

        String currentOwner = null;
        Pattern numberedMovePattern = Pattern.compile("^\\d+\\.\\s*(.+)$");
        Pattern cooldownPattern = Pattern.compile("(\\d+)\\s*turn");
        Pattern hitsPatternA = Pattern.compile("(\\d+)x", Pattern.CASE_INSENSITIVE);
        Pattern hitsPatternB = Pattern.compile("x(\\d+)", Pattern.CASE_INSENSITIVE);

        try (BufferedReader reader = new BufferedReader(new FileReader(moveFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (!trimmed.contains(":") && !trimmed.matches("^\\d+\\..*") && !"Moves".equalsIgnoreCase(trimmed)) {
                    currentOwner = normalizeOwner(trimmed);
                    continue;
                }

                Matcher moveMatcher = numberedMovePattern.matcher(trimmed);
                if (!moveMatcher.find() || currentOwner == null) {
                    continue;
                }

                String body = moveMatcher.group(1);
                String[] pieces = body.split("-", 2);
                String moveName = pieces[0].trim();
                String description = (pieces.length > 1) ? pieces[1].trim() : "";

                int cooldown = 0;
                Matcher cdMatcher = cooldownPattern.matcher(description.toLowerCase());
                if (cdMatcher.find()) {
                    cooldown = parseIntOrDefault(cdMatcher.group(1), 0);
                }

                int hits = 1;
                Matcher hitMatcherA = hitsPatternA.matcher(description.toLowerCase());
                Matcher hitMatcherB = hitsPatternB.matcher(description.toLowerCase());
                if (hitMatcherA.find()) {
                    hits = Math.max(1, parseIntOrDefault(hitMatcherA.group(1), 1));
                } else if (hitMatcherB.find()) {
                    hits = Math.max(1, parseIntOrDefault(hitMatcherB.group(1), 1));
                }

                GameData.StatusEffect status = null;
                String lowered = (moveName + " " + description).toLowerCase();
                if (lowered.contains("bleed")) {
                    status = GameData.StatusEffect.BLEED;
                } else if (lowered.contains("fire")) {
                    status = GameData.StatusEffect.FIRE;
                } else if (lowered.contains("cripple") || lowered.contains("low blow")) {
                    status = GameData.StatusEffect.CRIPPLE;
                } else if (lowered.contains("morale")) {
                    status = GameData.StatusEffect.MORALE;
                }

                movesByCharacter.computeIfAbsent(currentOwner, key -> new ArrayList<>())
                    .add(new MoveDef(moveName, description, hits, cooldown, status));
            }
        } catch (IOException ignored) {
            // Keep empty move table if parsing fails.
        }
    }

    private String normalizeOwner(String rawOwner) {
        String lowered = rawOwner.toLowerCase();
        if (lowered.startsWith("terry")) {
            return "terry";
        }
        if (lowered.contains("kriegs")) {
            return "kriegs";
        }
        if (lowered.contains("azrael")) {
            return "azrael";
        }
        if (lowered.contains("gambit")) {
            return "gambit";
        }
        if (lowered.contains("lazarus")) {
            return "lazarus";
        }
        if (lowered.contains("raphaela")) {
            return "raphaela";
        }
        return lowered;
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String formatName(String id) {
        if (id == null || id.isBlank()) {
            return "Unknown";
        }
        if (id.startsWith("enemy_")) {
            return id.replace("enemy_", "").replace('_', ' ').toUpperCase();
        }
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    private Icon loadPortraitIcon(String id) {
        return loadPortraitIcon(id, 52, 52);
    }

    private Icon loadPortraitIcon(String id, int width, int height) {
        String path;
        if (id.startsWith("enemy_")) {
            path = "assets/res/Characters/terry/PFPTerry.png";
        } else {
            path = switch (id.toLowerCase()) {
                case "kriegs" -> "assets/res/Characters/Kriegs/PFPKriegs.png";
                case "azrael" -> "assets/res/Characters/Azrael/PFPAzrael.png";
                case "gambit" -> "assets/res/Characters/Gambit/PFPGambit.png";
                case "lazarus" -> "assets/res/Characters/Lazarus/PFPLazarus.png";
                case "raphaela" -> "assets/res/Characters/raphaela/PFPRaphaela.png";
                case "terry" -> "assets/res/Characters/terry/PFPTerry.png";
                default -> null;
            };
        }

        if (path == null) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(new File(path));
            if (image == null) {
                return null;
            }
            Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException ignored) {
            return null;
        }
    }
}
