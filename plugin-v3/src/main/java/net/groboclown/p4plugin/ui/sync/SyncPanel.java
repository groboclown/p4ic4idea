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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.TextFieldListener;
import net.groboclown.p4plugin.ui.TextFieldUtil;
import net.groboclown.p4plugin.ui.history.ChooseLabelDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.List;
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
    private JButton myFindLabelButton;
    private final ButtonGroup syncTypeGroup;


    SyncPanel(@NotNull Project project, @NotNull final SyncOptions parent,
            @NotNull List<ClientConfig> configs) {
        syncTypeGroup = new ButtonGroup();
        $$$setupUI$$$();
        syncTypeGroup.add(mySyncHead);
        mySyncHead.addActionListener(e -> {
            setPairState(false, myRevLabel, myRevision);
            setPairState(false, myOtherLabel, myOther);
            myFindLabelButton.setEnabled(false);
            updateValues(parent);
        });
        syncTypeGroup.add(mySyncRev);
        mySyncRev.addActionListener(e -> {
            setPairState(true, myRevLabel, myRevision);
            setPairState(false, myOtherLabel, myOther);
            myFindLabelButton.setEnabled(false);
            updateValues(parent);
        });
        syncTypeGroup.add(mySyncChangelist);
        mySyncChangelist.addActionListener(e -> {
            setPairState(false, myRevLabel, myRevision);
            setPairState(true, myOtherLabel, myOther);
            myFindLabelButton.setEnabled(!configs.isEmpty());
            updateValues(parent);
        });
        myFindLabelButton.addActionListener(e -> {
            ChooseLabelDialog.show(project, configs, (label) -> {
                if (label == null) {
                    myOther.setText("");
                } else {
                    myOther.setText(label.getName());
                }
                // These should trigger the other actions, but just in case...
                updateValues(parent);
            });
        });
        TextFieldUtil.addTo(myRevision, new TextFieldListener() {
            @Override
            public void textUpdated(@NotNull DocumentEvent e, @Nullable String text) {
                if (getSelectedSyncType() == SyncOptions.SyncType.REV && getRevValue() < 0) {
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
        switch (parent.getSyncType()) {
            case HEAD:
                syncTypeGroup.setSelected(mySyncHead.getModel(), true);
                setPairState(false, myRevLabel, myRevision);
                setPairState(false, myOtherLabel, myOther);
                myFindLabelButton.setEnabled(false);
                break;
            case REV:
                syncTypeGroup.setSelected(mySyncRev.getModel(), true);
                myRevision.setText(Integer.toString(parent.getRev()));
                setPairState(true, myRevLabel, myRevision);
                setPairState(false, myOtherLabel, myOther);
                myFindLabelButton.setEnabled(false);
                break;
            case OTHER:
                syncTypeGroup.setSelected(mySyncChangelist.getModel(), true);
                myOther.setText(parent.getOther());
                setPairState(false, myRevLabel, myRevision);
                setPairState(true, myOtherLabel, myOther);
                myFindLabelButton.setEnabled(!configs.isEmpty());
                break;
            default:
                throw new IllegalStateException("unknown sync type " + parent.getSyncType());
        }
    }


    JPanel getPanel() {
        return myRootPane;
    }


    @NotNull
    SyncOptions getState() {
        return new SyncOptions(
                getSelectedSyncType(),
                getRevValue(),
                getOtherValue(),
                myForce.isSelected()
        );
    }

    private void updateValues(@NotNull final SyncOptions options) {
        int rev = getRevValue();
        String other = getOtherValue();
        switch (getSelectedSyncType()) {
            case HEAD:
                options.setHead();
                break;
            case REV:
                if (rev >= 0) {
                    options.setRevision(getRevValue());
                } else {
                    options.setHead();
                }
                break;
            case OTHER:
                if (other != null) {
                    options.setOther(other);
                } else {
                    options.setHead();
                }
                break;
        }
    }


    @NotNull
    private SyncOptions.SyncType getSelectedSyncType() {
        final ButtonModel selected = syncTypeGroup.getSelection();
        if (selected == null) {
            LOG.warn("No synchronization option button selected");
            return SyncOptions.SyncType.HEAD;
        }
        if (selected.equals(mySyncHead.getModel())) {
            return SyncOptions.SyncType.HEAD;
        }
        if (selected.equals(mySyncRev.getModel())) {
            return SyncOptions.SyncType.REV;
        }
        if (selected.equals(mySyncChangelist.getModel())) {
            return SyncOptions.SyncType.OTHER;
        }
        LOG.warn("Unknown synch option button selection");
        return SyncOptions.SyncType.HEAD;
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

    private int getRevValue() {
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
                    return IFileSpec.NO_FILE_REVISION;
                }
            }
            // allow an empty value; it's the same as "head"
            return IFileSpec.HEAD_REVISION;
        }
        return IFileSpec.NO_FILE_REVISION;
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
        panel1.setLayout(new GridLayoutManager(4, 2, new Insets(4, 4, 4, 4), -1, -1));
        myRootPane.add(panel1, BorderLayout.NORTH);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.to.title"),
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        mySyncHead = new JRadioButton();
        mySyncHead.setSelected(true);
        this.$$$loadButtonText$$$(mySyncHead,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.head"));
        mySyncHead.setToolTipText(
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.head.tooltip"));
        panel1.add(mySyncHead, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mySyncRev = new JRadioButton();
        this.$$$loadButtonText$$$(mySyncRev,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.rev"));
        mySyncRev.setToolTipText(
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.rev.tooltip"));
        panel1.add(mySyncRev, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mySyncChangelist = new JRadioButton();
        this.$$$loadButtonText$$$(mySyncChangelist,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.change"));
        mySyncChangelist.setToolTipText(
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.other.tooltip"));
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
        myRevision.setToolTipText(
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.rev.value.tooltip"));
        panel2.add(myRevision, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        myRevLabel = new JLabel();
        myRevLabel.setEnabled(false);
        this.$$$loadLabelText$$$(myRevLabel,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.rev.value"));
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
        myOther.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "sync.options.other.value.tooltip"));
        panel3.add(myOther, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        myOtherLabel = new JLabel();
        myOtherLabel.setEnabled(false);
        this.$$$loadLabelText$$$(myOtherLabel,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.other.field"));
        panel3.add(myOtherLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:d:grow", "center:d:grow"));
        panel1.add(panel4, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        myFindLabelButton = new JButton();
        myFindLabelButton.setEnabled(false);
        this.$$$loadButtonText$$$(myFindLabelButton,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.find-label"));
        CellConstraints cc = new CellConstraints();
        panel4.add(myFindLabelButton, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        myRootPane.add(panel5, BorderLayout.CENTER);
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4), null,
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        myForce = new JCheckBox();
        this.$$$loadButtonText$$$(myForce,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.force"));
        myForce.setToolTipText(
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "sync.options.force.tooltip"));
        panel5.add(myForce, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel5.add(spacer3,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        myRevLabel.setLabelFor(myRevision);
        myOtherLabel.setLabelFor(myOther);
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
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
