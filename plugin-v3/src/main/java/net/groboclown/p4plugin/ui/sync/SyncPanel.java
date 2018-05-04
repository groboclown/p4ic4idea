/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.groboclown.p4plugin.ui.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.TextFieldListener;
import net.groboclown.p4plugin.ui.TextFieldUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.text.NumberFormat;
import java.util.ResourceBundle;


/**
 * Synchronize revision panel.
 * <p/>
 * TODO allow for browsing changelists and labels.
 */
public class SyncPanel {
    private static final Logger LOG = Logger.getInstance(SyncPanel.class);


    private JRadioButton mySyncHead;
    private JRadioButton mySyncRev;
    private JTextField myRevision;
    private JRadioButton mySyncChangelist;
    private JLabel myRevLabel;
    private JLabel myOtherLabel;
    private JTextField myOther;
    private JPanel myRootPane;
    private JCheckBox myForce;
    private ButtonGroup syncTypeGroup;


    SyncPanel(@NotNull final SyncOptionConfigurable parent) {
        syncTypeGroup = new ButtonGroup();
        $$$setupUI$$$();
        syncTypeGroup.add(mySyncHead);
        mySyncHead.addActionListener(e -> {
            setPairState(false, myRevLabel, myRevision);
            setPairState(false, myOtherLabel, myOther);
            updateValues(parent);
        });
        syncTypeGroup.add(mySyncRev);
        mySyncRev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                setPairState(true, myRevLabel, myRevision);
                setPairState(false, myOtherLabel, myOther);
                updateValues(parent);
            }
        });
        syncTypeGroup.add(mySyncChangelist);
        mySyncChangelist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                setPairState(false, myRevLabel, myRevision);
                setPairState(true, myOtherLabel, myOther);
                updateValues(parent);
            }
        });
        TextFieldUtil.addTo(myRevision, new TextFieldListener() {
            @Override
            public void textUpdated(@NotNull DocumentEvent e, @Nullable String text) {
                if (getSelectedSyncType() == SyncOptionConfigurable.SyncType.REV && getRevValue() == null) {
                    Messages.showMessageDialog(
                            P4Bundle.message("sync.options.rev.error"),
                            P4Bundle.message("sync.options.rev.error.title"),
                            Messages.getErrorIcon());
                } else {
                    updateValues(parent);
                }
            }

            @Override
            public void enabledStateChanged(@NotNull PropertyChangeEvent evt) {
                updateValues(parent);
            }
        });
        TextFieldUtil.addTo(myOther, new TextFieldListener() {
            @Override
            public void textUpdated(@NotNull DocumentEvent e, @Nullable String text) {
                updateValues(parent);
            }

            @Override
            public void enabledStateChanged(@NotNull PropertyChangeEvent evt) {
                updateValues(parent);
            }
        });

        // initialize the parent values to the current settings.
        updateValues(parent);
    }


    JPanel getPanel() {
        return myRootPane;
    }


    @NotNull
    SyncOptionConfigurable.SyncOptions getState() {
        return new SyncOptionConfigurable.SyncOptions(
                getSelectedSyncType(),
                getRevValue(),
                getOtherValue(),
                myForce.isSelected()
        );
    }

    private void updateValues(@NotNull final SyncOptionConfigurable parent) {
        final SyncOptionConfigurable.SyncOptions options = getState();
        if (options.hasError()) {
            parent.onOptionChange(null);
        } else {
            parent.onOptionChange(options);
        }
    }


    @NotNull
    private SyncOptionConfigurable.SyncType getSelectedSyncType() {
        final ButtonModel selected = syncTypeGroup.getSelection();
        if (selected == null) {
            LOG.warn("No synchronization option button selected");
            return SyncOptionConfigurable.SyncType.HEAD;
        }
        if (selected.equals(mySyncHead.getModel())) {
            return SyncOptionConfigurable.SyncType.HEAD;
        }
        if (selected.equals(mySyncRev.getModel())) {
            return SyncOptionConfigurable.SyncType.REV;
        }
        if (selected.equals(mySyncChangelist.getModel())) {
            return SyncOptionConfigurable.SyncType.OTHER;
        }
        LOG.warn("Unknown synch option button selection");
        return SyncOptionConfigurable.SyncType.HEAD;
    }

    @Nullable
    private String getOtherValue() {
        if (myOther.isEnabled()) {
            LOG.info("Read 'other' value as [" + myOther.getText() + "]");
            return myOther.getText();
        }
        LOG.info("Read 'other' value as null");
        return null;
    }

    @Nullable
    private Integer getRevValue() {
        if (myRevision.isEnabled()) {
            final String text = myRevision.getText();
            if (text != null && text.length() > 0) {
                try {
                    final int value = Integer.parseInt(text);
                    if (value < 0) {
                        return -1;
                    }
                    return value;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            // allow an empty value; it's the same as "head"
            return -1;
        }
        return null;
    }


    private static void setPairState(boolean state, @NotNull final JLabel label, @NotNull final JTextField field) {
        label.setEnabled(state);
        field.setEnabled(state);
        field.setEditable(state);
    }

    private void createUIComponents() {
        // place custom component creation code here

        NumberFormat intFormat = NumberFormat.getNumberInstance();
        intFormat.setMinimumFractionDigits(0);
        intFormat.setMaximumFractionDigits(0);
        intFormat.setGroupingUsed(false);

        // TODO by using a formatted text field, we lose the nice UI skinning.
        myRevision = new JFormattedTextField(intFormat);
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
        myRootPane = new JPanel();
        myRootPane.setLayout(new BorderLayout(0, 0));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 2, new Insets(4, 4, 4, 4), -1, -1));
        myRootPane.add(panel1, BorderLayout.NORTH);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.to.title")));
        mySyncHead = new JRadioButton();
        mySyncHead.setSelected(true);
        this.$$$loadButtonText$$$(mySyncHead,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.head"));
        mySyncHead.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.head.tooltip"));
        panel1.add(mySyncHead, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mySyncRev = new JRadioButton();
        this.$$$loadButtonText$$$(mySyncRev,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.rev"));
        mySyncRev.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.rev.tooltip"));
        panel1.add(mySyncRev, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mySyncChangelist = new JRadioButton();
        this.$$$loadButtonText$$$(mySyncChangelist,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.change"));
        mySyncChangelist.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.other.tooltip"));
        panel1.add(mySyncChangelist,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        myRevision.setColumns(4);
        myRevision.setEnabled(false);
        myRevision.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("sync.options.rev.value.tooltip"));
        panel2.add(myRevision, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        myRevLabel = new JLabel();
        myRevLabel.setEnabled(false);
        this.$$$loadLabelText$$$(myRevLabel,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.rev.value"));
        panel2.add(myRevLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        myOther = new JTextField();
        myOther.setColumns(10);
        myOther.setEditable(true);
        myOther.setEnabled(false);
        myOther.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("sync.options.other.value.tooltip"));
        panel3.add(myOther, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        myOtherLabel = new JLabel();
        myOtherLabel.setEnabled(false);
        this.$$$loadLabelText$$$(myOtherLabel,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.other.field"));
        panel3.add(myOtherLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        myRootPane.add(panel4, BorderLayout.CENTER);
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4), null));
        myForce = new JCheckBox();
        this.$$$loadButtonText$$$(myForce,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.force"));
        myForce.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("sync.options.force.tooltip"));
        panel4.add(myForce, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        myRevLabel.setLabelFor(myRevision);
        myOtherLabel.setLabelFor(myOther);
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
        return myRootPane;
    }
}
