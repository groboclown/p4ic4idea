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

package net.groboclown.p4plugin.ui.history;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.commands.server.ListLabelsQuery;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.values.P4Label;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.ui.EdtSinkProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class ChooseLabelDialog
        extends JDialog {
    private static final Logger LOG = Logger.getInstance(ChooseLabelDialog.class);
    private static final int MAX_LABEL_RESULTS = 200;

    private final ChooseListener listener;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField mySearchField;
    private JButton mySearchButton;
    private JTable mySearchResults;
    private AsyncProcessIcon mySearchSpinner;
    private ListTableModel<P4Label> searchResultsModel;
    private final EdtSinkProcessor<P4Label> searchResultProcessor;

    public ChooseLabelDialog(@NotNull Project project,
            @NotNull List<ClientConfig> configs, @NotNull ChooseListener listener) {
        super(WindowManager.getInstance().suggestParentWindow(project),
                P4Bundle.message("search.label.title"));
        this.listener = listener;
        this.searchResultProcessor = new EdtSinkProcessor<>();

        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        searchResultProcessor.addIcon(mySearchSpinner);
        searchResultProcessor.addDisabledWhileRunningComponent(mySearchButton);
        // Note: use a new array list instead of the empty list, because the empty list cannot be appended to.
        searchResultProcessor.addStarter(() -> searchResultsModel.setItems(new ArrayList<>()));
        searchResultProcessor.addBatchConsumer((labels) -> {
            LOG.info("Loading " + labels.size() + " labels into the table.");
            searchResultsModel.addRows(labels);
        });

        mySearchSpinner.setVisible(false);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // TODO replace with FilterComponent
        mySearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                String text = mySearchField.getText();
                mySearchButton.setEnabled(text != null && !text.trim().isEmpty());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                String text = mySearchField.getText();
                mySearchButton.setEnabled(text != null && !text.trim().isEmpty());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                String text = mySearchField.getText();
                mySearchButton.setEnabled(text != null && !text.trim().isEmpty());
            }
        });

        mySearchButton.addActionListener(e -> {
            final String filterText = mySearchField.getText();
            final String filter = filterText == null ? "" : ("*" + filterText + "*");
            LOG.info("Loading labels from " + configs.size() + " configs with filter [" + filter + "]");
            searchResultProcessor.processBatchAnswer(() -> configs.stream()
                    .map((clientConfig) ->
                            P4ServerComponent
                                    .query(project, new OptionalClientServerConfig(clientConfig),
                                            new ListLabelsQuery(filter, MAX_LABEL_RESULTS)))
                    .reduce(Answer.resolve(Collections.emptyList()),
                            (listAnswer, queryResult) -> listAnswer.futureMap((src, sink) -> {
                                LOG.info("Processing next label query");
                                queryResult
                                        .whenCompleted((c) -> {
                                            List<P4Label> ret = new ArrayList<>(src);
                                            ret.addAll(c.getLabels());
                                            LOG.info("Loaded " + c.getLabels().size() + " additional labels from " + c
                                                    .getServerConfig().getServerName());
                                            sink.resolve(ret);
                                        })
                                        .whenServerError(sink::reject);
                            }),
                            (listAnswer, listAnswer2) -> listAnswer.mapAsync((src) ->
                                    listAnswer2.map((add) -> {
                                        List<P4Label> ret = new ArrayList<>(src);
                                        ret.addAll(add);
                                        LOG.info("Joined " + src.size() + " labels with " + add.size() + " labels");
                                        return ret;
                                    }))), true);
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(
                e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
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
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null,
                        null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel3,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FormLayout(
                "fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow",
                "center:d:grow,top:4dlu:noGrow,center:d:grow"));
        panel3.add(panel4, BorderLayout.SOUTH);
        mySearchField = new JTextField();
        CellConstraints cc = new CellConstraints();
        panel4.add(mySearchField, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        mySearchButton = new JButton();
        this.$$$loadButtonText$$$(mySearchButton,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "search.label.search-button"));
        mySearchButton.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "search.label.search-button.tooltip"));
        panel4.add(mySearchButton, cc.xy(3, 3));
        panel4.add(mySearchSpinner, cc.xy(5, 3));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        panel3.add(panel5, BorderLayout.CENTER);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel5.add(scrollPane1, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        mySearchResults.setAutoCreateRowSorter(false);
        scrollPane1.setViewportView(mySearchResults);
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

    public interface ChooseListener {
        void onChoice(@Nullable P4Label labelName);
    }

    public static void show(@NotNull Project project, @NotNull List<ClientConfig> configs,
            @NotNull ChooseListener listener) {
        ChooseLabelDialog dialog = new ChooseLabelDialog(project, configs, listener);
        dialog.pack();
        final Dimension bounds = dialog.getSize();
        final Rectangle parentBounds = dialog.getOwner().getBounds();
        dialog.setLocation(parentBounds.x + (parentBounds.width - bounds.width) / 2,
                parentBounds.y + (parentBounds.height - bounds.height) / 2);
        dialog.setVisible(true);
    }

    private void onOK() {
        // add your code here
        dispose();
        int selectedRow = mySearchResults.getSelectedRow();
        P4Label selectedLabel = null;
        if (selectedRow >= 0 && selectedRow < searchResultsModel.getRowCount()) {
            selectedLabel = searchResultsModel.getItem(selectedRow);
        }
        listener.onChoice(selectedLabel);
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
        listener.onChoice(null);
    }

    private void createUIComponents() {
        searchResultsModel =
                new ListTableModel<>(
                        new ColumnInfo<P4Label, String>(P4Bundle.getString("search.label.label-name")) {
                            @Nullable
                            @Override
                            public String valueOf(P4Label o) {
                                return o == null ? null : o.getName();
                            }
                        },
                        new ColumnInfo<P4Label, String>(P4Bundle.getString("search.label.label-description")) {
                            @Nullable
                            @Override
                            public String valueOf(P4Label o) {
                                return o == null ? null : o.getDescription();
                            }
                        });
        mySearchResults = new JBTable(searchResultsModel);
        mySearchResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mySearchSpinner = new AsyncProcessIcon("Searching for labels");
    }
}
