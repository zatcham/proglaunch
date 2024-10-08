package com.zatcham.proglaunch;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class ProgramLauncherGUI extends JFrame {
    private List<String> availablePrograms = new ArrayList<>();
    private List<String> runningPrograms = new ArrayList<>();
    private String programFolder;
    private int launchInterval;
    private boolean darkMode;
    private boolean shortcutsEnabled;

    private JList<String> programList;
    private JButton launchButton;
    private JButton configButton;
    private JButton listAvailableButton;
    private JTextField intervalEdit;
    private JTextField folderEdit;
    private JFileChooser fileChooser;
    private Preferences prefs = Preferences.userNodeForPackage(getClass());
    private Timer timer;
    private JLabel timerLabel;

    public ProgramLauncherGUI() {
        setTitle("Program Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        programFolder = prefs.get("folder", System.getProperty("user.dir"));
        launchInterval = prefs.getInt("interval", 3600);
        darkMode = prefs.getBoolean("darkmode", true);
        shortcutsEnabled = prefs.getBoolean("shortcuts", false);

        System.out.println("Starting program - Settings: \n Folder: " + programFolder + "\n Timer Interval: " + launchInterval + "\n Dark Mode: " + darkMode + "\n Shortcuts Enabled: " + shortcutsEnabled);

        try {
            if (darkMode) {
                FlatDarkLaf.setup();
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                FlatLightLaf.setup();
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        // TODO Display timer wait
        programList = new JList<>();
        launchButton = new JButton("Start");
        configButton = new JButton("Configure");
        listAvailableButton = new JButton("List Available Programs");
        fileChooser = new JFileChooser();

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        DefaultListModel<String> programListModel = new DefaultListModel<>();
        programList.setModel(programListModel);

        availablePrograms = getAvailablePrograms(programFolder);
        configButton.addActionListener(e -> openConfigDialog());
        listAvailableButton.addActionListener(e -> showAvailableProgramsDialog());

        timer = new Timer(30000, e -> launchProgram());

        launchButton.addActionListener(e -> {
            if (timer.isRunning()) {
                System.out.println("Timer Stopped");
                launchButton.setText("Start");
                timer.stop();
            } else {
                launchProgram();
                timer.setDelay(launchInterval * 1000);
                launchButton.setText("Stop");
                System.out.println("Timer started - interval in ms " + launchInterval * 1000);
                timer.start();
            }
        });
//        timer.start();
        timerLabel = new JLabel("Until next exec: ");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(programList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(launchButton);
        buttonPanel.add(configButton);
        buttonPanel.add(listAvailableButton);
//        buttonPanel.add(timerLabel); TODO

        panel.add(buttonPanel, BorderLayout.SOUTH);

        getContentPane().add(panel);
        System.out.println("UI Started");
    }


    private void launchProgram() {
        if (!availablePrograms.isEmpty()) {
            String programToLaunch = availablePrograms.get(0);
            System.out.println("Launching " + programToLaunch);
            availablePrograms.remove(programToLaunch);
            runningPrograms.add(programToLaunch);
            updateList();

            File programFile = new File(programFolder, programToLaunch);
            if (programFile.exists()) {
                if (programFile.getName().endsWith(".lnk")) {
                    try {
                        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", programFile.getAbsolutePath());
                        processBuilder.start();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    try {
                        ProcessBuilder processBuilder = new ProcessBuilder(programFile.getAbsolutePath());
                        processBuilder.start();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "There are no more programs left to launch", "Prog Launcher", JOptionPane.INFORMATION_MESSAGE);
            timer.stop();
            launchButton.setText("Start");
        }
    }



    private void openConfigDialog() {
        JDialog configDialog = new JDialog(this, "Configuration", true);
        configDialog.setLocationRelativeTo(null);
        configDialog.setSize(700, 200);
        configDialog.setLayout(new GridLayout(4, 2));

        JLabel intervalLabel = new JLabel("Launch Interval (minutes):");
        intervalEdit = new JTextField(String.valueOf(launchInterval / 60));
        JLabel folderLabel = new JLabel("Program Folder:");
        folderEdit = new JTextField(programFolder);
        JButton browseButton = new JButton("Browse");
        JButton saveButton = new JButton("Save");

        JToggleButton darkModeToggle = new JToggleButton("Dark Mode");
        darkModeToggle.setSelected(darkMode);
        darkModeToggle.addActionListener(e -> darkMode = darkModeToggle.isSelected());
        JToggleButton shortcutsToggle = new JToggleButton("Shortcuts");
        shortcutsToggle.addActionListener(e -> shortcutsEnabled = shortcutsToggle.isSelected());
        shortcutsToggle.setSelected(shortcutsEnabled);

        browseButton.addActionListener(e -> browseFolder());
        saveButton.addActionListener(e -> saveConfig());

        configDialog.add(intervalLabel);
        configDialog.add(intervalEdit);
        configDialog.add(folderLabel);
        configDialog.add(folderEdit);
        configDialog.add(darkModeToggle);
        configDialog.add(shortcutsToggle);
        configDialog.add(browseButton);
        configDialog.add(new JLabel()); // Empty label for alignment
        configDialog.add(saveButton);

        configDialog.setVisible(true);
    }

    private void saveConfig() {
        System.out.println("Saving config.. "); // TODO add settings
        launchInterval = Integer.parseInt(intervalEdit.getText()) * 60;
        programFolder = folderEdit.getText();

        prefs.putBoolean("darkmode", darkMode);
        prefs.putBoolean("shortcuts", shortcutsEnabled);
        prefs.put("folder", programFolder);
        prefs.putInt("interval", launchInterval);
    }

    private void showAvailableProgramsDialog() {
        JDialog availableDialog = new JDialog(this, "Available Programs", true);
        availableDialog.setSize(300, 200);
        availableDialog.setLayout(new BorderLayout());
        availableDialog.setLocationRelativeTo(null);

        JList<String> availableList = new JList<>();
        DefaultListModel<String> availableListModel = new DefaultListModel<>();
        availableList.setModel(availableListModel);

        availablePrograms = getAvailablePrograms(programFolder);
        for (String program : availablePrograms) {
            availableListModel.addElement(program);
        }

        availableDialog.add(new JScrollPane(availableList), BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> availableDialog.dispose());
        availableDialog.add(closeButton, BorderLayout.SOUTH);

        availableDialog.setVisible(true);
    }

    private void updateList() {
        JList<String> runningList = new JList<>();
        DefaultListModel<String> listModel = (DefaultListModel<String>) programList.getModel();
        runningList.setModel(listModel);

        String program = runningPrograms.get(runningPrograms.size() - 1);
        listModel.addElement(program);
//        for (String program : runningPrograms) {
//            listModel.addElement(program);
//        }
    }

    private List<String> getAvailablePrograms(String directory) {
        List<String> availablePrograms = new ArrayList<>();
        File currentDir = new File(directory);
        File[] exeFiles = currentDir.listFiles((dir, name) -> name.endsWith(".exe"));
        if (exeFiles != null) {
            for (File file : exeFiles) {
                availablePrograms.add(file.getName());
            }
        }
        if (shortcutsEnabled) {
            File[] lnkFiles = currentDir.listFiles((dir, name) -> name.endsWith(".lnk"));
            if (lnkFiles != null) {
                for (File file : lnkFiles) {
                    availablePrograms.add(file.getName());
                }
            }
        }
        return availablePrograms;
    }

    private void browseFolder() {
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            folderEdit.setText(selectedFolder.getAbsolutePath());
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ProgramLauncherGUI gui = new ProgramLauncherGUI();
            gui.setVisible(true);
        });
    }
}
