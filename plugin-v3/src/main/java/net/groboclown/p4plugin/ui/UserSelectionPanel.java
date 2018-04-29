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

package net.groboclown.p4plugin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.SpeedSearchBase;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.cache.state.UserSummaryState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class UserSelectionPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(UserSelectionPanel.class);


    private final List<ActionListener> actionListeners = new ArrayList<ActionListener>();
    private final AsyncLoader<UserSummaryState> stateLoader = new AsyncLoader<UserSummaryState>();
    private final ListTableModel<UserSummaryProxy> userTableModel;
    private final JBTable userTable;
    private final LoadingComboboxSpeedSearch userText;

    public UserSelectionPanel(@NotNull final P4Vcs vcs, @Nullable final Collection<ClientServerRef> serverRefs) {
        super(new BorderLayout());
        final AsyncProcessIcon loadingIcon = new AsyncProcessIcon(P4Bundle.getString("user.selection.loading"));
        final AnActionButton loadingIconButton = new AnActionButton() {
            @Override
            public void actionPerformed(AnActionEvent e) {
                // Does nothing
            }

            @Override
            public JComponent getContextComponent() {
                return loadingIcon;
            }
        };
        stateLoader.addListener(new Runnable() {
            @Override
            public void run() {
                loadingIcon.setVisible(false);
                loadingIconButton.setVisible(false);
            }
        });

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
        userTable = new JBTable(userTableModel);
        userTable.setShowColumns(false);
        userTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        userTable.setStriped(true);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(userTable)
                .addExtraAction(loadingIconButton)
                .disableUpDownActions()
                .setAddAction(null)
                .setRemoveAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        doRemove();
                    }
                });
        final JPanel panel = decorator.createPanel();
        add(panel, BorderLayout.CENTER);

        final JPanel addPanel = new JPanel(new BorderLayout());
        add(addPanel, BorderLayout.SOUTH);
        userText = new LoadingComboboxSpeedSearch();
        addPanel.add(userText.getComponent(), BorderLayout.CENTER);
        final JButton addButton = new JButton(AllIcons.General.Add);
        addPanel.add(addButton, BorderLayout.EAST);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doAdd();
            }
        });

        stateLoader.addListener(new AsyncLoader.AsyncLoadedListener<UserSummaryState>() {
            @Override
            public void onLoaded(@NotNull Collection<UserSummaryState> items) {
                verifyUsers(items);
            }
        });
        stateLoader.load(new Callable<Collection<UserSummaryState>>() {
            @Override
            public Collection<UserSummaryState> call()
                    throws Exception {
                return findUsers(vcs, serverRefs);
            }
        });
    }


    @NotNull
    public List<UserSummaryState> getSelectedUsers() {
        List<UserSummaryState> ret = new ArrayList<UserSummaryState>();
        for (UserSummaryProxy userSummaryProxy : userTableModel.getItems()) {
            UserSummaryState state = userSummaryProxy.getState();
            if (state != null) {
                ret.add(state);
            }
        }
        return ret;
    }


    public void addActionListener(@NotNull ActionListener listener) {
        actionListeners.add(listener);
    }


    private void verifyUsers(@NotNull Collection<UserSummaryState> items) {
        Map<String, UserSummaryState> userMap = new HashMap<String, UserSummaryState>();
        for (UserSummaryState user : items) {
            if (user != null) {
                userMap.put(user.getLoginId(), user);
            }
        }
        boolean updated = false;
        for (UserSummaryProxy proxy : userTableModel.getItems()) {
            updated |= proxy.update(userMap.get(proxy.getLoginId()));
        }
        if (updated) {
            userTableModel.fireTableDataChanged();
        }
    }

    private void doAdd() {
        Object val = userText.getComponent().getModel().getSelectedItem();
        if (val != null) {
            String text = val.toString();
            Collection<UserSummaryState> users = stateLoader.getItems();
            if (users != null) {
                for (UserSummaryState user : users) {
                    if (text.equals(user.getLoginId())) {
                        userTableModel.addRow(new UserSummaryProxy(user));
                        fireUsersUpdated();
                        return;
                    }
                }
                UserSummaryProxy proxy = new UserSummaryProxy(text);
                proxy.notFound = true;
                userTableModel.addRow(proxy);
                fireUsersUpdated();
                return;
            }
            userTableModel.addRow(new UserSummaryProxy(text));

            fireUsersUpdated();
        }
    }

    private void doRemove() {
        if (userTable.isEditing()) {
            userTable.getCellEditor().stopCellEditing();
        }

        final int[] selected = userTable.getSelectedRows();
        if (selected.length <= 0) {
            return;
        }
        if (selected.length == 1) {
            userTableModel.removeRow(selected[0]);
            fireUsersUpdated();
            return;
        }
        Arrays.sort(selected);

        List<UserSummaryProxy> oldData = userTableModel.getItems();
        List<UserSummaryProxy> newData = new ArrayList<UserSummaryProxy>(oldData.size() - selected.length);
        int lastRowIndex = 0;
        for (int i = 0; i < oldData.size(); i++) {
            if (lastRowIndex < selected.length && selected[lastRowIndex] == i) {
                lastRowIndex++;
            } else {
                newData.add(oldData.get(i));
            }
        }
        userTableModel.setItems(newData);
        userTableModel.fireTableDataChanged();

        int selection = selected[0];
        if (selection >= newData.size()) {
            selection = newData.size() - 1;
        }
        if (selection >= 0) {
            userTable.setRowSelectionInterval(selection, selection);
        }

        fireUsersUpdated();
    }

    private void fireUsersUpdated() {
        LOG.debug("User list updated");
        for (ActionListener actionListener : actionListeners) {
            actionListener.actionPerformed(new ActionEvent(this, 1, null));
        }
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

        String getLoginId() {
            return loginId;
        }

        String getFullName() {
            return (state == null) ? null : state.getFullName();
        }

        boolean update(UserSummaryState state) {
            if (state != null && loginId.equals(state.getLoginId())) {
                this.state = state;
                this.notFound = false;
                return true;
            } else if (this.state == null) {
                this.notFound = true;
            }
            return false;
        }

        UserSummaryState getState() {
            return state;
        }

        Icon getIcon() {
            if (notFound) {
                return AllIcons.General.Error;
            }
            if (state == null) {
                return AllIcons.Actions.Refresh;
            }
            return null;
        }
    }

    private Collection<UserSummaryState> findUsers(P4Vcs vcs, Collection<ClientServerRef> serverRef) {
        LOG.debug("Loading users for user selection panel");
        List<UserSummaryState> ret = new ArrayList<UserSummaryState>();
        for (P4Server p4Server : getServers(vcs, serverRef)) {
            try {
                Collection<UserSummaryState> serverUsers = p4Server.getUsers();
                ret.addAll(serverUsers);
            } catch (InterruptedException e) {
                return Collections.emptyList();
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Found users " + ret);
        }
        Collections.sort(ret, USER_COMPARATOR);
        return ret;
    }

    @NotNull
    private Collection<P4Server> getServers(P4Vcs vcs, Collection<ClientServerRef> serverRefs) {
        List<P4Server> servers = vcs.getP4Servers();
        if (serverRefs == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using all servers in search: " + servers);
            }
            return servers;
        }
        List<P4Server> ret = new ArrayList<P4Server>();
        for (P4Server server : servers) {
            if (server != null && serverRefs.contains(server.getClientServerId())) {
                ret.add(server);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching for users in " + ret);
        }
        return ret;
    }


    class LoadingCombobox extends ComboBox/*<String>*/
            implements AsyncLoader.AsyncLoadedListener<UserSummaryState> {
        private final List<JBList> lists = new ArrayList<JBList>();

        public LoadingCombobox() {
            super(new CollectionComboBoxModel());
        }

        @Override
        public void onLoaded(@NotNull Collection<UserSummaryState> items) {
            for (JBList list : lists) {
                list.setPaintBusy(false);
            }
        }

        @Override
        protected JBList/*<String>*/ createJBList(ComboBoxModel model) {
            JBList/*<String>*/ ret = super.createJBList(model);
            Collection<UserSummaryState> items = stateLoader.getItems();
            if (items == null) {
                ret.setPaintBusy(true);
                lists.add(ret);
            }
            return ret;
        }
    }


    class LoadingComboboxSpeedSearch extends SpeedSearchBase<LoadingCombobox>
            implements AsyncLoader.AsyncLoadedListener<UserSummaryState> {
        LoadingComboboxSpeedSearch() {
            super(new LoadingCombobox());
            InputMap map = getComponent().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            KeyStroke ks = KeyStroke.getKeyStroke(' ', 0);
            while (map != null) {
                map.remove(ks);
                map = map.getParent();
            }
        }

        protected void selectElement(Object element, String selectedText) {
            myComponent.setSelectedItem(element);
            myComponent.repaint();
        }

        protected int getSelectedIndex() {
            return myComponent.getSelectedIndex();
        }

        protected Object[] getAllElements() {
            ListModel model = myComponent.getModel();
            Object[] elements = new Object[model.getSize()];
            for (int i = 0; i < elements.length; i++) {
                elements[i] = model.getElementAt(i);
            }
            return elements;
        }

        protected String getElementText(Object element) {
            return element == null ? null : element.toString();
        }

        @Override
        public void onLoaded(@NotNull Collection<UserSummaryState> items) {
            // ensure model is loaded.
            getComponent().onLoaded(items);
            repaint();
        }
    }

    class CollectionComboBoxModel extends CollectionListModel/*<String>*/
            implements ComboBoxModel/*<String>*/,
                AsyncLoader.AsyncLoadedListener<UserSummaryState> {
        private Object selected;

        private CollectionComboBoxModel() {
            stateLoader.addListener(this);
        }

        @Override
        public void setSelectedItem(Object anItem) {
            if (anItem != selected) {
                this.selected = anItem;
                update();
            }
        }

        @Override
        public Object getSelectedItem() {
            return selected;
        }

        public void update() {
            fireContentsChanged(this, -1, -1);
        }

        @Override
        public void onLoaded(@NotNull Collection<UserSummaryState> items) {
            List<String> ids = new ArrayList<String>(items.size());
            for (UserSummaryState item : items) {
                ids.add(item.getLoginId());
            }
            replaceAll(ids);
        }
    }

    private static class UserSummaryStateComparator implements Comparator<UserSummaryState> {
        @Override
        public int compare(UserSummaryState o1, UserSummaryState o2) {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return o1.getLoginId().compareTo(o2.getLoginId());
        }
    }
    private static UserSummaryStateComparator USER_COMPARATOR = new UserSummaryStateComparator();
}
