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
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.ui.DoubleMinMaxSpinnerModel;
import net.groboclown.p4plugin.ui.IntMinMaxSpinnerModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ResourceBundle;

// eighth
// eighteenth

/**
 * Global properties.
 */
public class UserPreferencesPanel {
    private static final Logger LOG = Logger.getInstance(UserPreferencesPanel.class);

    private enum UserMessageLevel {
        VERBOSE(UserProjectPreferences.USER_MESSAGE_LEVEL_VERBOSE, "verbose"),
        INFO(UserProjectPreferences.USER_MESSAGE_LEVEL_INFO, "info"),
        WARNING(UserProjectPreferences.USER_MESSAGE_LEVEL_WARNING, "warn"),
        ERROR(UserProjectPreferences.USER_MESSAGE_LEVEL_ERROR, "error")

        // "ALWAYS" is not an option.  The least you can set is error.
        ;

        private final String displayName;
        final int value;

        UserMessageLevel(int value, String name) {
            this.value = value;
            this.displayName = P4Bundle.getString("user.prefs.user_message." + name);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private JPanel myRootPanel;
    private JSpinner myMaxTimeoutSecondsSpinner;
    private JRadioButton myPreferRevisionNumber;
    private JRadioButton myPreferChangelist;
    private JCheckBox myConcatenateChangelistNameCommentCheckBox;
    private JSpinner mySocketSoTimeoutSecondsSpinner;
    private JCheckBox myAutoCheckoutCheckBox;
    private JSpinner myMaxConnectionsSpinner;
    private JSpinner myMaxClientRetrieveSpinner;
    private JSpinner myMaxChangelistRetrieveSpinner;
    private JSpinner myMaxFileRetrieveSpinner;
    private JCheckBox myRemoveP4ChangelistCheckBox;
    private JComboBox<UserMessageLevel> myUserMessageLevelComboBox;
    private JSpinner myMaxChangelistNameSpinner;
    private JSpinner myRetryActionCountSpinner;
    private JCheckBox myNotifyOnReverts;
    private ButtonGroup myPreferRevisionGroup;


    UserPreferencesPanel() {
        LOG.debug("UI constructed, now setting up models");
        setupSpinner(myMaxTimeoutSecondsSpinner, new DoubleMinMaxSpinnerModel(
                millisToSeconds(UserProjectPreferences.MIN_LOCK_WAIT_TIMEOUT_MILLIS),
                millisToSeconds(UserProjectPreferences.MAX_LOCK_WAIT_TIMEOUT_MILLIS),
                0.5,
                millisToSeconds(UserProjectPreferences.DEFAULT_LOCK_WAIT_TIMEOUT_MILLIS)));
        setupSpinner(mySocketSoTimeoutSecondsSpinner, new DoubleMinMaxSpinnerModel(
                millisToSeconds(UserProjectPreferences.MIN_SOCKET_SO_TIMEOUT_MILLIS),
                millisToSeconds(UserProjectPreferences.MAX_SOCKET_SO_TIMEOUT_MILLIS),
                .1,
                millisToSeconds(UserProjectPreferences.DEFAULT_SOCKET_SO_TIMEOUT_MILLIS)));
        setupSpinner(myMaxConnectionsSpinner, new IntMinMaxSpinnerModel(
                UserProjectPreferences.MIN_SERVER_CONNECTIONS,
                UserProjectPreferences.MAX_SERVER_CONNECTIONS,
                1,
                UserProjectPreferences.DEFAULT_SERVER_CONNECTIONS));
        setupSpinner(myMaxChangelistNameSpinner, new IntMinMaxSpinnerModel(
                UserProjectPreferences.MIN_CHANGELIST_NAME_LENGTH,
                UserProjectPreferences.MAX_CHANGELIST_NAME_LENGTH,
                1,
                UserProjectPreferences.DEFAULT_MAX_CHANGELIST_NAME_LENGTH));
        setupSpinner(myMaxClientRetrieveSpinner, new IntMinMaxSpinnerModel(
                UserProjectPreferences.MIN_CLIENT_RETRIEVE_COUNT,
                UserProjectPreferences.MAX_CLIENT_RETRIEVE_COUNT,
                5,
                UserProjectPreferences.DEFAULT_MAX_CLIENT_RETRIEVE_COUNT));
        setupSpinner(myMaxChangelistRetrieveSpinner, new IntMinMaxSpinnerModel(
                UserProjectPreferences.MIN_CHANGELIST_RETRIEVE_COUNT,
                UserProjectPreferences.MAX_CHANGELIST_RETRIEVE_COUNT,
                5,
                UserProjectPreferences.DEFAULT_MAX_CHANGELIST_RETRIEVE_COUNT));
        setupSpinner(myMaxFileRetrieveSpinner, new IntMinMaxSpinnerModel(
                UserProjectPreferences.MIN_FILE_RETRIEVE_COUNT,
                UserProjectPreferences.MAX_FILE_RETRIEVE_COUNT,
                50,
                UserProjectPreferences.DEFAULT_MAX_FILE_RETRIEVE_COUNT));
        setupSpinner(myRetryActionCountSpinner, new IntMinMaxSpinnerModel(
                UserProjectPreferences.MIN_RETRY_ACTION_COUNT,
                UserProjectPreferences.MAX_RETRY_ACTION_COUNT,
                1,
                UserProjectPreferences.DEFAULT_RETRY_ACTION_COUNT));
        myMaxFileRetrieveSpinner.getEditor().setEnabled(true);
        for (UserMessageLevel value : UserMessageLevel.values()) {
            myUserMessageLevelComboBox.addItem(value);
        }
        // Future settings here

        myPreferRevisionGroup = new ButtonGroup();
        myPreferRevisionGroup.add(myPreferChangelist);
        myPreferRevisionGroup.add(myPreferRevisionNumber);
        LOG.debug("Completed panel setup");
    }


    void loadSettingsIntoGUI(@NotNull UserProjectPreferences userPrefs) {
        LOG.debug("Loading settings into UI");
        myMaxTimeoutSecondsSpinner.setValue(millisToSeconds(userPrefs.getLockWaitTimeoutMillis()));
        mySocketSoTimeoutSecondsSpinner.setValue(millisToSeconds(userPrefs.getSocketSoTimeoutMillis()));
        myMaxConnectionsSpinner.setValue(userPrefs.getMaxServerConnections());
        myMaxChangelistNameSpinner.setValue(userPrefs.getMaxChangelistNameLength());
        myPreferRevisionGroup.setSelected(
                userPrefs.getPreferRevisionsForFiles()
                        ? myPreferRevisionNumber.getModel()
                        : myPreferChangelist.getModel()
                , true);
        myConcatenateChangelistNameCommentCheckBox.setSelected(userPrefs.getConcatenateChangelistNameComment());
        myAutoCheckoutCheckBox.setSelected(userPrefs.getAutoCheckoutModifiedFiles());
        myMaxClientRetrieveSpinner.setValue(userPrefs.getMaxClientRetrieveCount());
        myMaxChangelistRetrieveSpinner.setValue(userPrefs.getMaxChangelistRetrieveCount());
        myMaxFileRetrieveSpinner.setValue(userPrefs.getMaxFileRetrieveCount());
        myRemoveP4ChangelistCheckBox.setSelected(userPrefs.getRemoveP4Changelist());
        myUserMessageLevelComboBox.setSelectedItem(messageLevelFromInt(userPrefs.getUserMessageLevel()));
        myRetryActionCountSpinner.setValue(userPrefs.getRetryActionCount());
        myNotifyOnReverts.setSelected(userPrefs.getNotifyOnRevert());
        // Future settings here
        LOG.debug("Finished loading settings into the UI");
    }


    void saveSettingsToConfig(@NotNull UserProjectPreferences userPrefs) {
        userPrefs.setLockWaitTimeoutMillis(getMaxTimeoutMillis());
        userPrefs.setSocketSoTimeoutMillis(getSocketSoTimeoutMillis());
        userPrefs.setMaxServerConnections(getMaxServerConnections());
        userPrefs.setMaxChangelistNameLength(getMaxChangelistNameLength());
        userPrefs.setPreferRevisionsForFiles(getPreferRevisionsForFiles());
        userPrefs.setConcatenateChangelistNameComment(getConcatenateChangelistNameComment());
        userPrefs.setAutoCheckoutModifiedFiles(getAutoCheckout());
        userPrefs.setMaxClientRetrieveCount(getMaxClientRetrieveCount());
        userPrefs.setMaxChangelistRetrieveCount(getMaxChangelistRetrieveCount());
        userPrefs.setMaxFileRetrieveCount(getMaxFileRetrieveCount());
        userPrefs.setRemoveP4Changelist(getRemoveP4Changelist());
        userPrefs.setUserMessageLevel(getUserMessageLevel());
        userPrefs.setRetryActionCount(getRetryActionCount());
        userPrefs.setNotifyOnRevert(getNotifyOnRevert());
        // Future settings here
    }


    boolean isModified(@NotNull final UserProjectPreferences preferences) {
        return
                getMaxTimeoutMillis() != preferences.getLockWaitTimeoutMillis() ||
                        getSocketSoTimeoutMillis() != preferences.getSocketSoTimeoutMillis() ||
                        getMaxServerConnections() != preferences.getMaxServerConnections() ||
                        getMaxChangelistNameLength() != preferences.getMaxChangelistNameLength() ||
                        getPreferRevisionsForFiles() != preferences.getPreferRevisionsForFiles() ||
                        getConcatenateChangelistNameComment() != preferences.getConcatenateChangelistNameComment() ||
                        getAutoCheckout() != preferences.getAutoCheckoutModifiedFiles() ||
                        getMaxClientRetrieveCount() != preferences.getMaxClientRetrieveCount() ||
                        getMaxChangelistRetrieveCount() != preferences.getMaxChangelistRetrieveCount() ||
                        getMaxFileRetrieveCount() != preferences.getMaxFileRetrieveCount() ||
                        getRemoveP4Changelist() != preferences.getRemoveP4Changelist() ||
                        getUserMessageLevel() != preferences.getUserMessageLevel() ||
                        getRetryActionCount() != preferences.getRetryActionCount() ||
                        getNotifyOnRevert() != preferences.getNotifyOnRevert();
        // Future settings here
    }


    private int getMaxTimeoutMillis() {
        return secondsToMillis(myMaxTimeoutSecondsSpinner);
    }

    private boolean getPreferRevisionsForFiles() {
        return myPreferRevisionGroup.getSelection() == null ||
                myPreferRevisionGroup.getSelection() == myPreferRevisionNumber.getModel();
    }

    private int getSocketSoTimeoutMillis() {
        return secondsToMillis(mySocketSoTimeoutSecondsSpinner);
    }

    private int getMaxServerConnections() {
        return toInt(myMaxConnectionsSpinner);
    }

    private int getMaxChangelistNameLength() {
        return toInt(myMaxChangelistNameSpinner);
    }

    private int getMaxClientRetrieveCount() {
        return toInt(myMaxClientRetrieveSpinner);
    }

    private boolean getConcatenateChangelistNameComment() {
        return myConcatenateChangelistNameCommentCheckBox.isSelected();
    }

    private boolean getAutoCheckout() {
        return myAutoCheckoutCheckBox.isSelected();
    }

    private int getMaxChangelistRetrieveCount() {
        return toInt(myMaxChangelistRetrieveSpinner);
    }

    private int getMaxFileRetrieveCount() {
        return toInt(myMaxFileRetrieveSpinner);
    }

    private boolean getRemoveP4Changelist() {
        return myRemoveP4ChangelistCheckBox.isSelected();
    }

    private int getUserMessageLevel() {
        Object level = myUserMessageLevelComboBox.getSelectedItem();
        if (level instanceof UserMessageLevel) {
            return ((UserMessageLevel) level).value;
        }
        return UserProjectPreferences.DEFAULT_USER_MESSAGE_LEVEL;
    }

    private int getRetryActionCount() {
        return toInt(myRetryActionCountSpinner);
    }

    private boolean getNotifyOnRevert() {
        return myNotifyOnReverts.isSelected();
    }


    private void createUIComponents() {
        // place custom component creation code here
    }

    JPanel getRootPanel() {
        return myRootPanel;
    }


    private static double millisToSeconds(long millis) {
        return ((double) millis) / 1000.;
    }

    private static int secondsToMillis(JSpinner secondsSpinner) {
        double seconds = ((Number) secondsSpinner.getModel().getValue()).doubleValue();
        return (int) Math.floor(seconds * 1000.);
    }

    private static int toInt(JSpinner spinner) {
        return ((Number) spinner.getModel().getValue()).intValue();
    }

    private static void setupSpinner(JSpinner spinner, SpinnerModel model) {
        spinner.setModel(model);
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.setEnabled(true);
        editor.getTextField().setEditable(true);
    }

    private static UserMessageLevel messageLevelFromInt(int value) {
        for (UserMessageLevel userMessageLevel : UserMessageLevel.values()) {
            if (userMessageLevel.value == value) {
                return userMessageLevel;
            }
        }
        for (UserMessageLevel userMessageLevel : UserMessageLevel.values()) {
            if (userMessageLevel.value == UserProjectPreferences.DEFAULT_USER_MESSAGE_LEVEL) {
                return userMessageLevel;
            }
        }
        LOG.warn("UserProjectPreferences.DEFAULT_USER_MESSAGE_LEVEL is an invalid value");
        return UserMessageLevel.INFO;
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
        myRootPanel.setLayout(new GridLayoutManager(12, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("user.prefs.max_timeout"));
        label1.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.max_timeout.tooltip"));
        myRootPanel.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        myRootPanel.add(panel1,
                new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myConcatenateChangelistNameCommentCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(myConcatenateChangelistNameCommentCheckBox,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                        .getString("user.prefs.concatenate-changelist"));
        myConcatenateChangelistNameCommentCheckBox.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                        .getString("user.prefs.concatenate-changelist.tooltip"));
        panel1.add(myConcatenateChangelistNameCommentCheckBox,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        myAutoCheckoutCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(myAutoCheckoutCheckBox,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("user.prefs.auto-checkout"));
        myAutoCheckoutCheckBox.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.auto-checkout.tooltip"));
        panel1.add(myAutoCheckoutCheckBox,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        myRemoveP4ChangelistCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(myRemoveP4ChangelistCheckBox,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                        .getString("user.prefs.remove_p4_changelist"));
        myRemoveP4ChangelistCheckBox.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.remove_p4_changelist.tooltip"));
        panel1.add(myRemoveP4ChangelistCheckBox,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        myNotifyOnReverts = new JCheckBox();
        this.$$$loadButtonText$$$(myNotifyOnReverts,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("user.prefs.notify_on_revert"));
        myNotifyOnReverts.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.notify_on_revert.tooltip"));
        panel1.add(myNotifyOnReverts,
                new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        myRootPanel.add(panel2,
                new GridConstraints(10, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("user.prefs.rev_display"),
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                this.$$$getFont$$$(null, -1, -1, panel2.getFont())));
        myPreferRevisionNumber = new JRadioButton();
        this.$$$loadButtonText$$$(myPreferRevisionNumber,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("user.prefs.revision"));
        panel2.add(myPreferRevisionNumber,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1,
                new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myPreferChangelist = new JRadioButton();
        this.$$$loadButtonText$$$(myPreferChangelist,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("user.prefs.prefer_changelist"));
        panel2.add(myPreferChangelist,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        myRootPanel.add(spacer2,
                new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("user.prefs.socket-so-timeout"));
        label2.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.socket-so-timeout.tooltip"));
        myRootPanel.add(label2, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("user.prefs.max_connections"));
        myRootPanel.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myMaxConnectionsSpinner = new JSpinner();
        myRootPanel.add(myMaxConnectionsSpinner,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        myRootPanel.add(panel3,
                new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myMaxTimeoutSecondsSpinner = new JSpinner();
        myMaxTimeoutSecondsSpinner.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.max_timeout.tooltip"));
        panel3.add(myMaxTimeoutSecondsSpinner);
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("user.prefs.max_timeout.unit"));
        panel3.add(label4);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        myRootPanel.add(panel4,
                new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        mySocketSoTimeoutSecondsSpinner = new JSpinner();
        mySocketSoTimeoutSecondsSpinner.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.socket-so-timeout.tooltip"));
        panel4.add(mySocketSoTimeoutSecondsSpinner);
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.socket-so-timeout.unit"));
        panel4.add(label5);
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.max_client_retrieve"));
        label6.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.max_client_retrieve.tooltip"));
        myRootPanel.add(label6, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myMaxClientRetrieveSpinner = new JSpinner();
        myRootPanel.add(myMaxClientRetrieveSpinner,
                new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        myMaxChangelistRetrieveSpinner = new JSpinner();
        myRootPanel.add(myMaxChangelistRetrieveSpinner,
                new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.max_changelist_retrieve"));
        label7.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.max_changelist_retrieve.tooltip"));
        myRootPanel.add(label7, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myMaxFileRetrieveSpinner = new JSpinner();
        myRootPanel.add(myMaxFileRetrieveSpinner,
                new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label8 = new JLabel();
        this.$$$loadLabelText$$$(label8, ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.max_file_retrieve_count"));
        label8.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.max_file_retrieve_count.tooltip"));
        myRootPanel.add(label8, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myUserMessageLevelComboBox = new JComboBox();
        myRootPanel.add(myUserMessageLevelComboBox,
                new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label9 = new JLabel();
        this.$$$loadLabelText$$$(label9, ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.user_message_level"));
        label9.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.user_message_level.tooltip"));
        myRootPanel.add(label9, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myMaxChangelistNameSpinner = new JSpinner();
        myRootPanel.add(myMaxChangelistNameSpinner,
                new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label10 = new JLabel();
        this.$$$loadLabelText$$$(label10, ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.max_changelist_name"));
        label10.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.max_changelist_name.tooltip"));
        myRootPanel.add(label10, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myRetryActionCountSpinner = new JSpinner();
        myRootPanel.add(myRetryActionCountSpinner,
                new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label11 = new JLabel();
        this.$$$loadLabelText$$$(label11, ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.retry-action-count"));
        label11.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("user.prefs.retry-action-count.tooltip"));
        myRootPanel.add(label11, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label1.setLabelFor(myMaxTimeoutSecondsSpinner);
        label3.setLabelFor(myMaxConnectionsSpinner);
        label6.setLabelFor(myMaxClientRetrieveSpinner);
        label7.setLabelFor(myMaxClientRetrieveSpinner);
        label8.setLabelFor(myMaxFileRetrieveSpinner);
        label9.setLabelFor(myUserMessageLevelComboBox);
        label10.setLabelFor(myMaxChangelistNameSpinner);
        label11.setLabelFor(myRetryActionCountSpinner);
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

}
