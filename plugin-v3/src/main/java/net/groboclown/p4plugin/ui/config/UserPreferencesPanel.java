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

package net.groboclown.p4plugin.ui.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import net.groboclown.p4plugin.preferences.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class UserPreferencesPanel {
    private static final Logger LOG = Logger.getInstance(UserPreferencesPanel.class);

    private JSpinner myMaxTimeout;
    private JPanel myRootPanel;
    private JCheckBox myOpenForEditInCheckBox;
    private JRadioButton myPreferRevisionNumber;
    private JRadioButton myPreferChangelist;
    private JCheckBox myEditedWithoutCheckoutCheckBox;
    private JSpinner myMaxRetryAuthenticationSpinner;
    private JCheckBox myReconnectWithEachRequest;
    private JCheckBox myConcatenateChangelistNameComment;
    private JSpinner mySocketSoTimeoutSpinner;
    private JCheckBox myShowMessageDialog;
    private ButtonGroup myPreferRevisionGroup;


    UserPreferencesPanel() {
        LOG.debug("UI constructed, now setting up models");
        myMaxTimeout.setModel(new MinMaxSpinnerModel(
                UserProjectPreferences.MIN_LOCK_WAIT_TIMEOUT_MILLIS,
                UserProjectPreferences.MAX_LOCK_WAIT_TIMEOUT_MILLIS,
                500,
                UserProjectPreferences.DEFAULT_LOCK_WAIT_TIMEOUT_MILLIS));
        myMaxTimeout.getEditor().setEnabled(true);
        myMaxRetryAuthenticationSpinner.setModel(new MinMaxSpinnerModel(
                UserProjectPreferences.MIN_MAX_AUTHENTICATION_RETRIES,
                UserProjectPreferences.MAX_MAX_AUTHENTICATION_RETRIES,
                1,
                UserProjectPreferences.DEFAULT_MAX_AUTHENTICATION_RETRIES));
        myMaxRetryAuthenticationSpinner.getEditor().setEnabled(true);
        mySocketSoTimeoutSpinner.setModel((new MinMaxSpinnerModel(
                UserProjectPreferences.MIN_SOCKET_SO_TIMEOUT_MILLIS,
                UserProjectPreferences.MAX_SOCKET_SO_TIMEOUT_MILLIS,
                100,
                UserProjectPreferences.DEFAULT_MAX_AUTHENTICATION_RETRIES
        )));
        mySocketSoTimeoutSpinner.getEditor().setEnabled(true);
        myPreferRevisionGroup = new ButtonGroup();
        myPreferRevisionGroup.add(myPreferChangelist);
        myPreferRevisionGroup.add(myPreferRevisionNumber);
        LOG.debug("Completed panel setup");
    }


    void loadSettingsIntoGUI(@NotNull UserProjectPreferences userPrefs) {
        LOG.debug("Loading settings into UI");
        myOpenForEditInCheckBox.setSelected(userPrefs.getEditInSeparateThread());
        myMaxTimeout.setValue(userPrefs.getLockWaitTimeoutMillis());
        myMaxRetryAuthenticationSpinner.setValue(userPrefs.getMaxAuthenticationRetries());
        mySocketSoTimeoutSpinner.setValue(userPrefs.getSocketSoTimeoutMillis());
        myPreferRevisionGroup.setSelected(
                userPrefs.getPreferRevisionsForFiles()
                        ? myPreferRevisionNumber.getModel()
                        : myPreferChangelist.getModel()
                , true);
        myEditedWithoutCheckoutCheckBox.setSelected(userPrefs.getEditedWithoutCheckoutVerify());
        myReconnectWithEachRequest.setSelected(userPrefs.getReconnectWithEachRequest());
        myConcatenateChangelistNameComment.setSelected(userPrefs.getConcatenateChangelistNameComment());
        myShowMessageDialog.setSelected(userPrefs.getShowDialogConnectionMessages());
        LOG.debug("Finished loading settings into the UI");
    }


    void saveSettingsToConfig(@NotNull UserProjectPreferences userPrefs) {
        userPrefs.setEditInSeparateThread(getOpenForEditInSeparateThread());
        userPrefs.setLockWaitTimeoutMillis(getMaxTimeout());
        userPrefs.setSocketSoTimeoutMillis(getSocketSoTimeout());
        userPrefs.setPreferRevisionsForFiles(getPreferRevisionsForFiles());
        userPrefs.setEditedWithoutCheckoutVerify(getEditedWithoutCheckoutVerify());
        userPrefs.setMaxAuthenticationRetries(getMaxAuthenticationRetries());
        userPrefs.setReconnectWithEachRequest(getReconnectWithEachRequest());
        userPrefs.setConcatenateChangelistNameComment(getConcatenateChangelistNameComment());
        userPrefs.setShowDialogConnectionMessages(getShowMessageDialog());
    }


    boolean isModified(@NotNull final UserProjectPreferences preferences) {
        return
                getOpenForEditInSeparateThread() != preferences.getEditInSeparateThread() ||
                        getMaxTimeout() != preferences.getLockWaitTimeoutMillis() ||
                        getSocketSoTimeout() != preferences.getSocketSoTimeoutMillis() ||
                        getPreferRevisionsForFiles() != preferences.getPreferRevisionsForFiles() ||
                        getEditedWithoutCheckoutVerify() != preferences.getEditedWithoutCheckoutVerify() ||
                        getMaxAuthenticationRetries() != preferences.getMaxAuthenticationRetries() ||
                        getReconnectWithEachRequest() != preferences.getReconnectWithEachRequest() ||
                        getConcatenateChangelistNameComment() != preferences.getConcatenateChangelistNameComment() ||
                        getShowMessageDialog() != preferences.getShowDialogConnectionMessages();
    }


    private boolean getOpenForEditInSeparateThread() {
        return myOpenForEditInCheckBox.isSelected();
    }


    private int getMaxTimeout() {
        return (Integer) myMaxTimeout.getModel().getValue();
    }

    private boolean getPreferRevisionsForFiles() {
        return myPreferRevisionGroup.getSelection() == null ||
                myPreferRevisionGroup.getSelection() == myPreferRevisionNumber.getModel();
    }

    private boolean getEditedWithoutCheckoutVerify() {
        return myEditedWithoutCheckoutCheckBox.isSelected();
    }

    private int getMaxAuthenticationRetries() {
        return (Integer) myMaxRetryAuthenticationSpinner.getModel().getValue();
    }

    private int getSocketSoTimeout() {
        return (Integer) mySocketSoTimeoutSpinner.getModel().getValue();
    }

    private boolean getReconnectWithEachRequest() {
        return myReconnectWithEachRequest.isSelected();
    }

    private boolean getConcatenateChangelistNameComment() {
        return myConcatenateChangelistNameComment.isSelected();
    }

    private boolean getShowMessageDialog() {
        return myShowMessageDialog.isSelected();
    }

    private void createUIComponents() {
        // place custom component creation code here
    }

    JPanel getRootPanel() {
        return myRootPanel;
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
     * @noinspection ALL
     */
    private Font getFont1495815520855(String fontName, int style, int size, Font currentFont) {
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
     * @noinspection ALL
     */
    private Font getFont1495815749256(String fontName, int style, int size, Font currentFont) {
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        myRootPanel = new JPanel();
        myRootPanel.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("user.prefs.max_timeout"));
        label1.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.max_timeout.tooltip"));
        myRootPanel.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myMaxTimeout = new JSpinner();
        myMaxTimeout.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.max_timeout.tooltip"));
        myRootPanel.add(myMaxTimeout,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        myRootPanel.add(panel1,
                new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myOpenForEditInCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(myOpenForEditInCheckBox, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.edit_in_separate_thread"));
        myOpenForEditInCheckBox.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.edit_in_separate_thread.tooltip"));
        panel1.add(myOpenForEditInCheckBox,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        myEditedWithoutCheckoutCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(myEditedWithoutCheckoutCheckBox,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                        .getString("configuration.user.compare-contents"));
        myEditedWithoutCheckoutCheckBox.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.user.compare-contents.tooltip"));
        panel1.add(myEditedWithoutCheckoutCheckBox,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        myReconnectWithEachRequest = new JCheckBox();
        this.$$$loadButtonText$$$(myReconnectWithEachRequest,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("user.prefs.always_reconnect"));
        myReconnectWithEachRequest.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.always_reconnect.tooltip"));
        panel1.add(myReconnectWithEachRequest,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        myConcatenateChangelistNameComment = new JCheckBox();
        this.$$$loadButtonText$$$(myConcatenateChangelistNameComment,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                        .getString("user.prefs.concatenate-changelist"));
        myConcatenateChangelistNameComment.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.concatenate-changelist.tooltip"));
        panel1.add(myConcatenateChangelistNameComment,
                new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        myShowMessageDialog = new JCheckBox();
        this.$$$loadButtonText$$$(myShowMessageDialog,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("user.prefs.message-dialog"));
        myShowMessageDialog.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.message-dialog.tooltip"));
        panel1.add(myShowMessageDialog,
                new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        myRootPanel.add(panel2,
                new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("user.prefs.rev_display"),
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                this.$$$getFont$$$(null, -1, -1, panel2.getFont())));
        myPreferRevisionNumber = new JRadioButton();
        this.$$$loadButtonText$$$(myPreferRevisionNumber,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("user.prefs.revision"));
        panel2.add(myPreferRevisionNumber,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1,
                new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myPreferChangelist = new JRadioButton();
        this.$$$loadButtonText$$$(myPreferChangelist, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.prefer_changelist"));
        panel2.add(myPreferChangelist,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        myRootPanel.add(spacer2,
                new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("user.prefs.max_auth_retry"));
        label2.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.max_auth_retry.tooltip"));
        myRootPanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myMaxRetryAuthenticationSpinner = new JSpinner();
        myMaxRetryAuthenticationSpinner.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.max_auth_retry.tooltip"));
        myRootPanel.add(myMaxRetryAuthenticationSpinner,
                new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.socket-so-timeout"));
        label3.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.socket-so-timeout.tooltip"));
        myRootPanel.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mySocketSoTimeoutSpinner = new JSpinner();
        mySocketSoTimeoutSpinner.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.prefs.socket-so-timeout.tooltip"));
        myRootPanel.add(mySocketSoTimeoutSpinner,
                new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        label1.setLabelFor(myMaxTimeout);
        label2.setLabelFor(myMaxRetryAuthenticationSpinner);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
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
        return myRootPanel;
    }


    static class MinMaxSpinnerModel
            implements SpinnerModel {
        private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();
        private final int minValue;
        private final int maxValue;
        private final int step;
        private int value;

        MinMaxSpinnerModel(final int minValue, final int maxValue, final int step, final int initialValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.step = step;
            this.value = initialValue;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(final Object value) {
            if (value == null || !(value instanceof Number)) {
                return;
            }
            int newValue = Math.min(
                    maxValue,
                    Math.max(
                            minValue,
                            ((Number) value).intValue()));
            if (newValue != this.value) {
                this.value = newValue;
                synchronized (listeners) {
                    for (ChangeListener listener : listeners) {
                        listener.stateChanged(new ChangeEvent(this));
                    }
                }
            }
        }

        @Override
        public Object getNextValue() {
            return Math.min(maxValue, value + step);
        }

        @Override
        public Object getPreviousValue() {
            return Math.max(minValue, value - step);
        }

        @Override
        public void addChangeListener(final ChangeListener l) {
            if (l != null) {
                synchronized (listeners) {
                    listeners.add(l);
                }
            }
        }

        @Override
        public void removeChangeListener(final ChangeListener l) {
            synchronized (listeners) {
                listeners.remove(l);
            }
        }
    }
}
