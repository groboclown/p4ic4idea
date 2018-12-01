package net.groboclown.idea.p4ic.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

public class RevertedFilesDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JPanel myRevertedFilesPanel;
    private JList myRevertedFilesList;
    private DefaultListModel/*<String>*/ myRevertedFilesModel; // jdk 1.6: this is not generic
    private JPanel myErrorsListPanel;
    private JList myErrorsList;
    private DefaultListModel/*<String>*/ myErrorsModel; // jdk 1.6: this is not generic
    private JPanel myNothingDonePanel;

    public RevertedFilesDialog(@NotNull Project project, @NotNull Collection<FilePath> reverted,
            @NotNull Collection<P4StatusMessage> errors) {
        super(WindowManager.getInstance().suggestParentWindow(project),
                P4Bundle.message("reverted.files.title"));
        $$$setupUI$$$();

        for (String el : filePathDisplay(reverted)) {
            myRevertedFilesModel.addElement(el);
        }
        for (String el : exceptionDisplay(errors)) {
            myErrorsModel.addElement(el);
        }

        if (reverted.isEmpty()) {
            myRevertedFilesPanel.setVisible(false);
        }
        if (errors.isEmpty()) {
            myErrorsListPanel.setVisible(false);
        }
        if (!errors.isEmpty() || !reverted.isEmpty()) {
            myNothingDonePanel.setVisible(false);
        }

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    public static void show(@NotNull Project project, @NotNull Collection<FilePath> reverted,
            @NotNull List<P4StatusMessage> errors) {
        RevertedFilesDialog dialog = new RevertedFilesDialog(project, reverted, errors);
        dialog.pack();
        final Dimension bounds = dialog.getSize();
        final Rectangle parentBounds = dialog.getOwner().getBounds();
        dialog.setLocation(parentBounds.x + (parentBounds.width - bounds.width) / 2,
                parentBounds.y + (parentBounds.height - bounds.height) / 2);
        dialog.setVisible(true);
    }

    private void createUIComponents() {
        // place custom component creation code here

        // Cannot use the actual values, as they haven't been initialized yet (just how the generated GUI code works)
        myRevertedFilesModel = new DefaultListModel/*<String>*/();
        myRevertedFilesList = new JBList(myRevertedFilesModel);

        myErrorsModel = new DefaultListModel/*<String>*/();
        myErrorsList = new JBList(myErrorsModel);
    }

    @NotNull
    private List<String> filePathDisplay(@NotNull final Collection<FilePath> reverted) {
        List<String> ret = new ArrayList<String>(reverted.size());
        for (FilePath file : reverted) {
            ret.add(file.getIOFile().toString());
        }
        return ret;
    }

    @NotNull
    private List<String> exceptionDisplay(@NotNull final Collection<P4StatusMessage> errors) {
        List<String> ret = new ArrayList<String>(errors.size());
        for (P4StatusMessage error : errors) {
            // TODO the errors should be linked to files; add them to the UI output.
            ret.add(error.getMessage().getLocalizedMessage());
        }
        return ret;
    }

    /**
     * @noinspection ALL
     */
    private Font getFont1494608681498(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) {
            return null;
        }
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(),
                size >= 0 ? size : currentFont.getSize());
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1,
                new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null,
                        null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        buttonOK = new JButton();
        this.$$$loadButtonText$$$(buttonOK,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("button.ok"));
        panel2.add(buttonOK,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myRevertedFilesPanel = new JPanel();
        myRevertedFilesPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(myRevertedFilesPanel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("reverted.files.list"));
        myRevertedFilesPanel.add(label1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("reverted.files.reverted.tooltip"));
        myRevertedFilesPanel.add(scrollPane1,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null,
                        0, false));
        scrollPane1.setViewportView(myRevertedFilesList);
        myErrorsListPanel = new JPanel();
        myErrorsListPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(myErrorsListPanel,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("reverted.files.errors"));
        myErrorsListPanel.add(label2,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("reverted.files.errors.tooltip"));
        myErrorsListPanel.add(scrollPane2,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null,
                        0, false));
        scrollPane2.setViewportView(myErrorsList);
        myNothingDonePanel = new JPanel();
        myNothingDonePanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(myNothingDonePanel,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("reverted.files.nothing"));
        myNothingDonePanel.add(label3,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final Spacer spacer2 = new Spacer();
        myNothingDonePanel.add(spacer2,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
