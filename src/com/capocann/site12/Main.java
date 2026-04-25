package com.capocann.site12;

import javax.swing.*;

import com.capocann.site12.ui.combatPanel;
import com.capocann.site12.ui.menuPanel;
import com.capocann.site12.ui.resPanel;
import com.capocann.site12.ui.roamPanel;

import java.awt.*;

public class Main extends JFrame {
    //switches between panels(scenes)
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContainer  = new JPanel(cardLayout);
    private menuPanel menu;
    private combatPanel combat;
    private resPanel manage;
    private roamPanel roam;
    private String currentScreen = "Menu";

    public Main() {
        setTitle("Site 12");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setUndecorated(true);
        setAlwaysOnTop(false);

        addWindowStateListener(e -> {
            if ((e.getNewState() & Frame.ICONIFIED) != 0) {
                SwingUtilities.invokeLater(() -> {
                    setState(Frame.NORMAL);
                    toFront();
                    requestFocus();
                });
            }
        });

        // Initialize panels
        menu = new menuPanel(this);
        manage = new resPanel(this);
        combat = new combatPanel(this);
        roam = new roamPanel(this);
        

        // Add panels to the main container
        mainContainer.add(menu, "Menu");
        mainContainer.add(manage, "60secs");
        mainContainer.add(roam, "Tiles");
        mainContainer.add(combat, "OMORI");

        add(mainContainer);
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isFullScreenSupported()) {
            device.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        setVisible(true);
        showScreen("Menu");
    }

    public void showScreen(String screenName) {
        if ("Menu".equals(currentScreen) && menu != null && !"Menu".equals(screenName)) {
            menu.onMenuHidden();
        }

        if ("60secs".equals(screenName) && manage != null) {
            manage.refreshFromGameState();
        }
        if ("Tiles".equals(screenName) && roam != null) {
            roam.startNewScavenge();
        }
        if ("OMORI".equals(screenName) && combat != null) {
            combat.startNewEncounter();
        }

        cardLayout.show(mainContainer, screenName);
        currentScreen = screenName;

        if ("Menu".equals(screenName) && menu != null) {
            menu.onMenuShown();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}

