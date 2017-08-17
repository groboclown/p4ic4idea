package net.groboclown.idea.p4ic.ui.swarm;

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.changes.P4ChangesViewRefresher;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.ui.TextFieldListener;
import net.groboclown.idea.p4ic.ui.TextFieldUtil;
import net.groboclown.idea.p4ic.ui.UserSelectionPanel;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.P4ServerManager;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.cache.state.UserSummaryState;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.p4.simpleswarm.SwarmClient;
import net.groboclown.p4.simpleswarm.exceptions.SwarmServerResponseException;
import net.groboclown.p4.simpleswarm.model.Review;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class CreateSwarmReviewDialog
        extends JDialog {
    private static final Logger LOG = Logger.getInstance(CreateSwarmReviewDialog.class);


    private final Project project;
    private final List<Pair<SwarmClient, P4ChangeListId>> changeLists;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea descTextArea;
    private JPanel userSelectionPanel;
    private JPanel fileListPanel;
    private UserSelectionPanel userSelection;

    private CreateSwarmReviewDialog(@NotNull Project project,
            String description, @NotNull List<Pair<SwarmClient, P4ChangeListId>> changeLists) {
        super(WindowManager.getInstance().suggestParentWindow(project),
                P4Bundle.message("swarm.review.create.title"));
        this.project = project;
        this.changeLists = changeLists;
        userSelection = new UserSelectionPanel(P4Vcs.getInstance(project), getServerIdsFor(changeLists));
        userSelectionPanel.add(userSelection, BorderLayout.CENTER);
        userSelection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkOkButtonState();
            }
        });

        descTextArea.setText(description);
        TextFieldUtil.addTo(descTextArea, new TextFieldListener() {
            @Override
            public void textUpdated(@NotNull DocumentEvent e, @Nullable String text) {
                checkOkButtonState();
            }

            @Override
            public void enabledStateChanged(@NotNull PropertyChangeEvent evt) {
                // ignore
            }
        });

        // TODO add file list to fileListPanel.

        setContentPane(contentPane);
        setModal(false);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonOK.setEnabled(false);

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private boolean checkOkButtonState() {
        String text = descTextArea.getText();
        boolean enabled =
                !userSelection.getSelectedUsers().isEmpty()
                        && text != null && !text.isEmpty();
        buttonOK.setEnabled(enabled);
        return enabled;
    }

    public static void show(@NotNull Project project, @NotNull String description,
            @NotNull List<Pair<SwarmClient, P4ChangeListId>> changeList) {
        CreateSwarmReviewDialog dialog = new CreateSwarmReviewDialog(project, description, changeList);
        dialog.pack();
        final Dimension bounds = dialog.getSize();
        final Rectangle parentBounds = dialog.getOwner().getBounds();
        dialog.setLocation(parentBounds.x + (parentBounds.width - bounds.width) / 2,
                parentBounds.y + (parentBounds.height - bounds.height) / 2);
        dialog.setVisible(true);
    }

    private void onOK() {
        boolean isOk = checkOkButtonState();
        final String text = descTextArea.getText();
        final List<UserSummaryState> users = userSelection.getSelectedUsers();
        dispose();
        if (!isOk) {
            return;
        }

        // TODO Add to project progress bar.

        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Pair<SwarmClient, P4ChangeListId>> shelvedChangelists = shelveChanges(text);
                    if (shelvedChangelists.isEmpty()) {
                        return;
                    }
                    final List<Pair<SwarmClient, Review>> reviews = createReview(text, users, shelvedChangelists);
                    if (reviews.isEmpty()) {
                        return;
                    }
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showSuccess(reviews);
                        }
                    });
                } catch (InterruptedException e) {
                    LOG.info(e);
                }
            }
        });
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }


    // Run in background
    @NotNull
    private List<Pair<SwarmClient, P4ChangeListId>> shelveChanges(String text)
            throws InterruptedException {
        List<P4Server> servers = P4ServerManager.getInstance(project).getOnlineServers();
        List<Pair<SwarmClient, P4ChangeListId>> shelvedChanges = new ArrayList<Pair<SwarmClient, P4ChangeListId>>();
        for (Pair<SwarmClient, P4ChangeListId> pair : changeLists) {
            P4Server server = getServerForChangelist(pair.second, servers);
            if (server != null) {
                try {
                    P4Server.ShelveFileResult res = server.shelveFilesInChangelistForOnline(pair.second, text);
                    if (res.hasShelvedFiles()) {
                        shelvedChanges.add(Pair.create(pair.first, res.getChangelistId()));
                    }
                } catch (P4DisconnectedException e) {
                    // TODO ensure that it's already handled by looking at the calls that were made.
                    LOG.warn(e);
                }
            }
        }
        if (!shelvedChanges.isEmpty()) {
            // Shelved changes were updated, so the change list view needs a refresh.
            P4ChangesViewRefresher.refreshLater(project);
        }
        return shelvedChanges;
    }


    // Run in background
    private List<Pair<SwarmClient, Review>> createReview(String text, @NotNull List<UserSummaryState> users,
            @NotNull List<Pair<SwarmClient, P4ChangeListId>> shelvedChangelists) {
        LOG.info("Creating review for " + shelvedChangelists);
        List<String> loginIds = new ArrayList<String>();
        for (UserSummaryState user : users) {
            loginIds.add(user.getLoginId());
        }
        List<Pair<SwarmClient, Review>> ret = new ArrayList<Pair<SwarmClient, Review>>(shelvedChangelists.size());
        for (Pair<SwarmClient, P4ChangeListId> shelvedChangelist : shelvedChangelists) {
            try {
                Review res = shelvedChangelist.first.createReview(
                        text, shelvedChangelist.second.getChangeListId(),
                        loginIds.toArray(new String[loginIds.size()]),

                        // TODO need to add a UI capability to specify required users.
                        new String[0]);
                ret.add(Pair.create(shelvedChangelist.first, res));
            } catch (IOException e) {
                AlertManager.getInstance().addWarning(project,
                        P4Bundle.message("swarm-client.connection.failed.title"),
                        P4Bundle.message("swarm-client.connection.failed",
                                shelvedChangelist.first.getConfig().getUri()),
                        e, new FilePath[0]);
            } catch (SwarmServerResponseException e) {
                AlertManager.getInstance().addWarning(project,
                        P4Bundle.message("create-swarm-review.review.failed.title"),
                        P4Bundle.message("create-swarm-review.review.failed",
                                shelvedChangelist.first.getConfig().getUri()),
                        e, new FilePath[0]);
            }
        }

        return ret;
    }

    // run in EDT
    private void showSuccess(List<Pair<SwarmClient, Review>> reviews) {
        StringBuilder content = new StringBuilder();
        String next = "";
        for (Pair<SwarmClient, Review> review : reviews) {
            content.append(next)
                    .append("<a href='")
                    .append(review.first.getConfig().getUri())
                    .append("/reviews/")
                    .append(review.second.getId())
                    .append("/'>")
                    .append(review.second.getId())
                    .append("</a>");
            next = "<br>";
        }
        Notification notification = new Notification(P4Vcs.VCS_NAME,
                P4Bundle.getString("create.swarm-review.success.title"),
                content.toString(), NotificationType.INFORMATION,
                new NotificationListener() {
                    @Override
                    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                        final URL url = event.getURL();
                        try {
                            BrowserLauncher.getInstance().browse(url.toURI());
                        } catch (URISyntaxException e) {
                            LOG.info(e);
                        }
                    }
                });
        Notifications.Bus.notify(notification, project);
    }


    @Nullable
    private P4Server getServerForChangelist(@NotNull P4ChangeListId cl, @NotNull List<P4Server> servers) {
        for (P4Server server : servers) {
            if (server.getClientServerId().equals(cl.getClientServerRef())) {
                return server;
            }
        }
        return null;
    }


    @NotNull
    private Collection<ClientServerRef> getServerIdsFor(List<Pair<SwarmClient, P4ChangeListId>> changes) {
        Set<ClientServerRef> ret = new HashSet<ClientServerRef>();
        for (Pair<SwarmClient, P4ChangeListId> change : changes) {
            ret.add(change.second.getClientServerRef());
        }
        return ret;
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("user.selection.description"));
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                        new Dimension(408, 69), null, 0, false));
        descTextArea = new JTextArea();
        descTextArea.setLineWrap(true);
        descTextArea.setRows(4);
        scrollPane1.setViewportView(descTextArea);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(scrollPane2,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null,
                        0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        scrollPane2.setViewportView(panel2);
        userSelectionPanel = new JPanel();
        userSelectionPanel.setLayout(new BorderLayout(0, 0));
        panel2.add(userSelectionPanel, BorderLayout.CENTER);
        userSelectionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("create.swarm-review.users")));
        fileListPanel = new JPanel();
        fileListPanel.setLayout(new FormLayout("fill:d:noGrow", "center:d:noGrow"));
        panel2.add(fileListPanel, BorderLayout.SOUTH);
        final JLabel label2 = new JLabel();
        label2.setText("TODO show list of files");
        CellConstraints cc = new CellConstraints();
        fileListPanel.add(label2, cc.xy(1, 1));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null,
                        null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel3.add(panel4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel4.add(buttonOK,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel4.add(buttonCancel,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        return contentPane;
    }
}
