package com.stephanmeesters.quicktab;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorsSplitters;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;

public class QuickTab extends AnAction {

    private DefaultListModel<String> model;
    private JBList<String> list;
    private JBPopup popup;
    private VirtualFile[] openFiles;
    private VirtualFile currentFile;
    private Project project;
    private static final int padding = 30;

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public QuickTab() {
        super();
    }

    public QuickTab(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    private void Init()
    {
        model = new DefaultListModel<>();

        list = new JBList<>(model);
        list.setCellRenderer((list1, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value);
            if (openFiles[index].equals(currentFile)) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            label.setBorder(BorderFactory.createEmptyBorder(0, convertDPI(padding), 0, 0));
            return label;
        });

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(list, list)  // Passing list instead of scrollPane
                .setTitle("Open Tabs")
                .setFocusable(true)
                .setRequestFocus(true)
                .createPopup();

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                int index = list.locationToIndex(evt.getPoint());
                if(project != null)
                    FileEditorManager.getInstance(project).openFile(openFiles[index], true);
                popup.dispose();
            }
        });

        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char keyChar = Character.toLowerCase(e.getKeyChar());
                int index = getIndexForValue(keyChar);
                if (index >= 0 && index < Math.min(openFiles.length, 31)) {
                    if (project != null) {
                        if (e.isControlDown()) {
                            FileEditorManager.getInstance(project).closeFile(openFiles[index]);
                            popup.dispose();
                            if (openFiles.length - 1 >= 2) {
                                Init();
                                RefreshContents();
                            }
                        } else {
                            FileEditorManager.getInstance(project).openFile(openFiles[index], true);
                            popup.dispose();
                        }
                    }
                }
                else if(e.getKeyCode() != KeyEvent.VK_CONTROL)
                {
                    popup.dispose();
                }
            }
        });
    }

    private void RefreshContents()
    {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditorManagerEx fileEditorManagerEx = FileEditorManagerEx.getInstanceEx(project);
        EditorsSplitters splitters = fileEditorManagerEx.getSplitters();

        // Get open files in the active tab group
        openFiles = splitters.getCurrentWindow().getFiles();
        if(openFiles.length <= 1)
            return;

        // Count occurrences of each filename
        Map<String, Integer> nameCount = new HashMap<>();
        for (VirtualFile file : openFiles) {
            nameCount.put(file.getName(), nameCount.getOrDefault(file.getName(), 0) + 1);
        }

        currentFile = fileEditorManager.getSelectedFiles()[0];

        for (int i = 0; i < Math.min(openFiles.length, 31); i++) {
            String name = openFiles[i].getName();
            if (nameCount.get(name) > 1) {
                name = openFiles[i].getPath();
            }
            model.addElement(getValueAtIndex(i) + ".   " + name);
        }

        list.setPreferredSize(new Dimension(calculateWidth(), model.getSize() * convertDPI(20)));

        popup.showInFocusCenter();
        list.hasFocus();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        project = e.getProject();
        if (project == null)
            return;

        Init();
        RefreshContents();
    }

    @Override
    public void update(AnActionEvent e) {
        // Set the availability based on whether a project is open
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    private static String getValueAtIndex(int index) {
        if (index >= 0 && index <= 8) {
            return Integer.toString(index + 1); // 1 to 9
        } else if (index >= 9 && index <= 31) {
            return Character.toString((char) ('a' + (index - 9))); // a to w
        } else {
            return ""; // Invalid index
        }
    }

    private static int getIndexForValue(char ch) {
        if (ch >= '1' && ch <= '9') {
            return ch - '1';  // 0 to 8
        } else if (ch >= 'a' && ch <= 'w') {
            return ch - 'a' + 9;  // 9 to 31
        } else {
            return -1;  // Invalid input
        }
    }

    private int calculateWidth()
    {
        int maxWidth = 0;
        FontMetrics fm = list.getFontMetrics(list.getFont());
        for (int i = 0; i < model.size(); i++) {
            String item = model.get(i);
            int width = fm.stringWidth(item);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        return maxWidth + 2*convertDPI(padding);
    }

    private int convertDPI(int value)
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = defaultScreen.getDefaultConfiguration();

        AffineTransform at = gc.getDefaultTransform();
        double scaleX = at.getScaleX();
        return (int)(value * scaleX);
    }

}