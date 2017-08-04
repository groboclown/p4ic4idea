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

package net.groboclown.idea.p4ic.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.SpeedSearchBase;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.cache.state.UserSummaryState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Note: this has the potential to show incorrect user name information, if
 * the user is connected to multiple servers and the same name is in both
 * servers.
 */
public class UserSelectionFormOld {
    private final P4Vcs vcs;
    private final ClientServerRef serverRef;
    private final Object checkUsersSync = new Object();

    private JPanel root;
    private ListTableModel<UserSummaryProxy> userTableModel;
    private JTable userTable;
    private JTextField userNameTextField;
    private JButton addUserButton;
    private JButton searchButton;
    private JButton removeButton;

    public UserSelectionFormOld(@NotNull P4Vcs vcs, @Nullable ClientServerRef serverRef) {
        this.vcs = vcs;
        this.serverRef = serverRef;

        userTableModel = new ListTableModel<UserSummaryProxy>(
                new ColumnInfo<UserSummaryProxy, String>(
                        P4Bundle.getString("user.selection.column.login")
                ) {
                    @Nullable
                    @Override
                    public String valueOf(UserSummaryProxy o) {
                        return o == null ? null : o.getLoginId();
                    }
                },
                new ColumnInfo<UserSummaryProxy, String>(
                        P4Bundle.getString("user.selection.column.name")
                ) {
                    @Nullable
                    @Override
                    public String valueOf(UserSummaryProxy o) {
                        return o == null ? null : o.getFullName();
                    }
                },
                new ColumnInfo<UserSummaryProxy, Icon>("") {
                    @Nullable
                    @Override
                    public Icon valueOf(UserSummaryProxy o) {
                        return (o == null)
                                ? null
                                : o.getIcon();
                    }
                }
        );

        userTable.setModel(userTableModel);
        userTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                onSelectionChanged();
            }
        });

        userNameTextField.setText("");
        userNameTextField.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                onUserNameTextFieldChanged();
            }
        });

        addUserButton.setIcon(AllIcons.General.Add);
        addUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAddUser();
            }
        });

        removeButton.setIcon(AllIcons.General.Remove);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRemoveUsers();
            }
        });

        searchButton.setIcon(AllIcons.Actions.Search);
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        showSearchDialog();
                    }
                });
            }
        });
    }

    private void showSearchDialog() {
    }

    public JPanel getRootPanel() {
        return root;
    }

    public void setUsers(List<UserSummaryState> users) {
        List<UserSummaryProxy> proxies = new ArrayList<UserSummaryProxy>(users.size());
        for (UserSummaryState user : users) {
            proxies.add(new UserSummaryProxy(user));
        }
        userTableModel.setItems(proxies);
        fireUserListCheck();
    }

    public List<UserSummaryState> getUsers() {
        final List<UserSummaryProxy> proxies = userTableModel.getItems();
        final List<UserSummaryState> ret = new ArrayList<UserSummaryState>(proxies.size());
        for (UserSummaryProxy proxy : proxies) {
            UserSummaryState state = proxy.getState();
            if (state != null) {
                ret.add(state);
            }
        }
        return ret;
    }

    private void onUserNameTextFieldChanged() {
        final String text = userNameTextField.getText();
        addUserButton.setEnabled(text != null && !text.isEmpty());
    }

    private void onSelectionChanged() {
        removeButton.setEnabled(!userTable.getSelectionModel().isSelectionEmpty());
    }

    private void onAddUser() {
        String username = userNameTextField.getText();
        if (username != null && !username.isEmpty()) {
            userTableModel.addRow(new UserSummaryProxy(username));
            fireUserListCheck();
        }
    }

    private void onRemoveUsers() {
        final int[] rows = userTable.getSelectedRows();
        if (rows.length <= 0) {
            return;
        }
        if (rows.length == 1) {
            userTableModel.removeRow(rows[0]);
            return;
        }
        Arrays.sort(rows);

        List<UserSummaryProxy> oldData = userTableModel.getItems();
        List<UserSummaryProxy> newData = new ArrayList<UserSummaryProxy>(oldData.size() - rows.length);
        int lastRowIndex = 0;
        for (int i = 0; i < oldData.size(); i++) {
            if (lastRowIndex < rows.length && rows[lastRowIndex] == i) {
                lastRowIndex++;
            } else {
                newData.add(oldData.get(i));
            }
        }
        userTableModel.setItems(newData);
    }

    private void fireUserListCheck() {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                synchronized (checkUsersSync) {
                    Map<String, UserSummaryState> users = new HashMap<String, UserSummaryState>();
                    for (P4Server p4Server : getServers()) {
                        try {
                            Collection<UserSummaryState> serverUsers = p4Server.getUsers();
                            for (UserSummaryState user : serverUsers) {
                                users.put(user.getLoginId(), user);
                            }
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    boolean updated = false;
                    for (UserSummaryProxy proxy : userTableModel.getItems()) {
                        updated |= proxy.update(users.get(proxy.loginId));
                    }
                    if (updated) {
                        userTableModel.fireTableDataChanged();
                    }
                }
            }
        });
    }

    @NotNull
    private Collection<P4Server> getServers() {
        List<P4Server> servers = vcs.getP4Servers();
        if (serverRef == null) {
            return servers;
        }
        for (P4Server server : servers) {
            if (server != null && server.getClientServerId().equals(serverRef)) {
                return Collections.singleton(server);
            }
        }
        return Collections.emptyList();
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
        root = new JPanel();
        root.setLayout(new BorderLayout(0, 0));
        final JScrollPane scrollPane1 = new JScrollPane();
        root.add(scrollPane1, BorderLayout.CENTER);
        userTable = new JTable();
        scrollPane1.setViewportView(userTable);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout(
                "fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow",
                "center:max(d;4px):noGrow"));
        root.add(panel1, BorderLayout.SOUTH);
        userNameTextField = new JTextField();
        CellConstraints cc = new CellConstraints();
        panel1.add(userNameTextField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("user.selection.name"));
        panel1.add(label1, cc.xy(1, 1));
        addUserButton = new JButton();
        addUserButton.setEnabled(false);
        addUserButton.setText("");
        addUserButton.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("user.selection.add.tooltip"));
        panel1.add(addUserButton, cc.xy(5, 1));
        searchButton = new JButton();
        searchButton.setText("");
        searchButton.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.selection.search.tooltip"));
        panel1.add(searchButton, cc.xy(9, 1));
        removeButton = new JButton();
        removeButton.setHorizontalAlignment(2);
        removeButton.setText("");
        removeButton.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.selection.remove.tooltip"));
        removeButton.setVerticalAlignment(1);
        panel1.add(removeButton, cc.xy(7, 1));
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
    public JComponent $$$getRootComponent$$$() {
        return root;
    }


    private static class UserSummaryProxy {
        final String loginId;
        UserSummaryState state;
        boolean notFound = false;

        UserSummaryProxy(String loginId) {
            this.loginId = loginId;
            this.state = null;
            this.notFound = false;
        }

        UserSummaryProxy(UserSummaryState state) {
            this.loginId = state.getLoginId();
            this.state = state;
            this.notFound = false;
        }

        synchronized String getLoginId() {
            return loginId;
        }

        synchronized String getFullName() {
            return (state == null) ? null : state.getFullName();
        }

        synchronized boolean update(UserSummaryState state) {
            if (state != null && loginId.equals(state.getLoginId())) {
                this.state = state;
                this.notFound = false;
                return true;
            } else if (this.state == null) {
                this.notFound = true;
            }
            return false;
        }

        synchronized UserSummaryState getState() {
            return state;
        }

        synchronized Icon getIcon() {
            if (notFound) {
                return AllIcons.General.Error;
            }
            if (state == null) {
                return AllIcons.Actions.Refresh;
            }
            return null;
        }
    }

    private static class LoadableComboSpeedSearch
            extends SpeedSearchBase<JComboBox/*<String>*/> {
        public LoadableComboSpeedSearch(JComboBox/*<String>*/ component) {
            super(component);
        }

        @Override
        protected int getSelectedIndex() {
            return 0;
        }

        @Override
        protected Object[] getAllElements() {
            return new Object[0];
        }

        @Nullable
        @Override
        protected String getElementText(Object element) {
            return null;
        }

        @Override
        protected void selectElement(Object element, String selectedText) {

        }
    }


}
