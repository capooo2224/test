package com.capocann.site12.ui;

import javax.swing.*;

import com.capocann.site12.Main;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.awt.*;
import java.io.File;

public class menuPanel extends JPanel {
    private static final String INTRO_VIDEO_PATH = "assets/menu/Intro.mp4";
    private static final String LOOP_VIDEO_PATH = "assets/menu/loop.mp4";
    private boolean introPlayed = false;
    private static final boolean SHOW_MENU_DEBUG_STATUS = true;

    private final JFXPanel videoHost = new JFXPanel();
    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JPanel overlayUi = new JPanel(new BorderLayout());
    private MediaPlayer introPlayer;
    private MediaPlayer loopPlayer;
    private MediaView mediaView;
    private boolean fxReady = false;
    private boolean menuVisibleRequested = false;
    private boolean loopStartedForCurrentMenuVisit = false;
    private JPanel actions;
    private JLabel debugStatusLabel;
    private JButton skipIntroButton;

    public menuPanel(Main main) {
        setPreferredSize(new Dimension(800, 600));
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Color.BLACK);

        layeredPane.setLayout(null);
        layeredPane.setOpaque(true);
        layeredPane.setBackground(Color.BLACK);

        overlayUi.setOpaque(false);

        layeredPane.add(videoHost, Integer.valueOf(0));
        layeredPane.add(overlayUi, Integer.valueOf(100));
        add(layeredPane, BorderLayout.CENTER);

        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                layoutLayers();
            }
        });
        SwingUtilities.invokeLater(this::layoutLayers);

        initializeVideoLayer();

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        if (SHOW_MENU_DEBUG_STATUS) {
            JPanel statusHost = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            statusHost.setOpaque(false);
            debugStatusLabel = new JLabel();
            debugStatusLabel.setForeground(Color.WHITE);
            debugStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            statusHost.add(debugStatusLabel);
            topBar.add(statusHost, BorderLayout.WEST);
            setDebugStatus("INIT", false);
        }

        JPanel skipHost = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        skipHost.setOpaque(false);
        skipIntroButton = new JButton("Skip Intro");
        skipIntroButton.setVisible(false);
        skipIntroButton.addActionListener(e -> Platform.runLater(() -> {
            if (introPlayed) {
                return;
            }
            introPlayed = true;
            startLoopPlayback();
        }));
        skipHost.add(skipIntroButton);
        topBar.add(skipHost, BorderLayout.EAST);
        overlayUi.add(topBar, BorderLayout.NORTH);

        actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));
        actions.setOpaque(false);
        actions.setVisible(false);

        JButton toRes = new JButton("Go to 60secs");
        JButton toTiles = new JButton("Go to Tiles");
        JButton toCombat = new JButton("Go to OMORI");
        JButton quit = new JButton("Quit");

        toRes.addActionListener(e -> main.showScreen("60secs"));
        toTiles.addActionListener(e -> main.showScreen("Tiles"));
        toCombat.addActionListener(e -> main.showScreen("OMORI"));
        quit.addActionListener(e -> {
            onMenuHidden();
            Platform.exit();
            Window window = SwingUtilities.getWindowAncestor(menuPanel.this);
            if (window != null) {
                window.dispose();
            }
            System.exit(0);
        });

        actions.add(toRes);
        actions.add(toTiles);
        actions.add(toCombat);
        actions.add(quit);
        overlayUi.add(actions, BorderLayout.SOUTH);
    }

    public void onMenuShown() {
        menuVisibleRequested = true;
        loopStartedForCurrentMenuVisit = false;
        setDebugStatus("MENU_SHOWN", actions != null && actions.isVisible());

        if (!fxReady) {
            setDebugStatus("WAITING_FOR_JAVAFX", false);
            return;
        }
        Platform.runLater(() -> {
            if (!introPlayed) {
                playIntroThenLoop();
            } else {
                playLoopOnly();
            }
        });
    }

    public void onMenuHidden() {
        menuVisibleRequested = false;
        SwingUtilities.invokeLater(() -> actions.setVisible(false));
        setSkipIntroVisible(false);
        setDebugStatus("MENU_HIDDEN", false);

        if (!fxReady) {
            return;
        }
        Platform.runLater(() -> {
            if (introPlayer != null) {
                introPlayer.stop();
            }
            if (loopPlayer != null) {
                loopPlayer.stop();
            }
        });
    }

    private void initializeVideoLayer() {
        try {
            Platform.setImplicitExit(false);
            Platform.runLater(this::setupVideoPlayers);

            videoHost.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    Platform.runLater(menuPanel.this::syncMediaViewSize);
                }
            });
        } catch (Exception ex) {
            fxReady = false;
            setDebugStatus("JAVAFX_INIT_FAILED", false);
        }
    }

    private void playIntroThenLoop() {
        SwingUtilities.invokeLater(() -> actions.setVisible(false));
        setSkipIntroVisible(true);
        setDebugStatus("PLAYING_INTRO", false);
        loopStartedForCurrentMenuVisit = false;

        if (introPlayer == null || loopPlayer == null) {
            playLoopOnly();
            introPlayed = true;
            return;
        }

        if (mediaView != null) {
            mediaView.setMediaPlayer(introPlayer);
        }
        loopPlayer.stop();
        introPlayer.seek(Duration.ZERO);
        introPlayer.play();
    }

    private void playLoopOnly() {
        SwingUtilities.invokeLater(() -> actions.setVisible(false));
        setSkipIntroVisible(false);
        setDebugStatus("REQUEST_LOOP", false);
        startLoopPlayback();
    }

    private Media buildMedia(String relativePath) {
        File file = new File(relativePath);
        if (!file.exists()) {
            return null;
        }

        return new Media(file.toURI().toString());
    }

    private void setupVideoPlayers() {
        try {
            Media introMedia = buildMedia(INTRO_VIDEO_PATH);
            Media loopMedia = buildMedia(LOOP_VIDEO_PATH);

            if (introMedia != null) {
                introPlayer = new MediaPlayer(introMedia);
                introPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                    Duration total = introPlayer.getTotalDuration();
                    if (total == null || total.isUnknown() || total.lessThanOrEqualTo(Duration.ZERO)) {
                        return;
                    }

                    // Fail-safe: some files/drivers miss onEndOfMedia, so switch near the end.
                    Duration threshold = total.subtract(Duration.millis(120));
                    if (newTime.greaterThanOrEqualTo(threshold)) {
                        introPlayed = true;
                        startLoopPlayback();
                    }
                });
            }
            if (loopMedia != null) {
                loopPlayer = new MediaPlayer(loopMedia);
                loopPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                loopPlayer.setOnPlaying(() -> SwingUtilities.invokeLater(() -> {
                    actions.setVisible(true);
                    setSkipIntroVisible(false);
                    setDebugStatus("LOOP_PLAYING", true);
                }));
                loopPlayer.setOnReady(() -> setDebugStatus("LOOP_READY", false));
                loopPlayer.setOnError(() -> {
                    setDebugStatus("LOOP_ERROR", false);
                });
            }

            mediaView = new MediaView();
            mediaView.setPreserveRatio(false);

            if (introPlayer != null) {
                mediaView.setMediaPlayer(introPlayer);
            } else if (loopPlayer != null) {
                mediaView.setMediaPlayer(loopPlayer);
            }

            if (introPlayer != null && loopPlayer != null) {
                introPlayer.setOnEndOfMedia(() -> {
                    introPlayed = true;
                    startLoopPlayback();
                });
                introPlayer.setOnPlaying(() -> setDebugStatus("PLAYING_INTRO", false));
                introPlayer.setOnError(() -> {
                    setDebugStatus("INTRO_ERROR", false);
                    introPlayed = true;
                    startLoopPlayback();
                });
            }

            StackPane root = new StackPane(mediaView);
            root.setStyle("-fx-background-color: black;");
            videoHost.setScene(new Scene(root));

            syncMediaViewSize();
            fxReady = true;
            setDebugStatus("JAVAFX_READY", false);

            if (menuVisibleRequested) {
                if (!introPlayed) {
                    playIntroThenLoop();
                } else {
                    playLoopOnly();
                }
            }
        } catch (Exception ex) {
            fxReady = false;
            setDebugStatus("VIDEO_SETUP_FAILED", false);
        }
    }

    private void setDebugStatus(String playbackState, boolean buttonsVisible) {
        if (!SHOW_MENU_DEBUG_STATUS || debugStatusLabel == null) {
            return;
        }

        SwingUtilities.invokeLater(() ->
            debugStatusLabel.setText("DEBUG | " + playbackState + " | buttonsVisible=" + buttonsVisible)
        );
    }

    private void syncMediaViewSize() {
        if (mediaView == null) {
            return;
        }

        int width = Math.max(1, videoHost.getWidth());
        int height = Math.max(1, videoHost.getHeight());
        mediaView.setFitWidth(width);
        mediaView.setFitHeight(height);
    }

    private void startLoopPlayback() {
        if (loopPlayer == null) {
            return;
        }
        if (loopStartedForCurrentMenuVisit) {
            return;
        }
        loopStartedForCurrentMenuVisit = true;

        if (introPlayer != null) {
            introPlayer.stop();
        }
        if (mediaView != null) {
            mediaView.setMediaPlayer(loopPlayer);
        }
        setSkipIntroVisible(false);

        setDebugStatus("STARTING_LOOP", false);
        loopPlayer.seek(Duration.ZERO);
        loopPlayer.play();
    }

    private void setSkipIntroVisible(boolean visible) {
        if (skipIntroButton == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> skipIntroButton.setVisible(visible));
    }

    private void layoutLayers() {
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        layeredPane.setBounds(0, 0, width, height);
        videoHost.setBounds(0, 0, width, height);
        overlayUi.setBounds(0, 0, width, height);
        syncMediaViewSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}
