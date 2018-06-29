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

package net.groboclown.p4plugin.ui.vcsroot;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.SwingUtil;
import net.groboclown.p4plugin.ui.vcsroot.part.ClientNamePartUI;
import net.groboclown.p4plugin.ui.vcsroot.part.EnvPartUI;
import net.groboclown.p4plugin.ui.vcsroot.part.FilePartUI;
import net.groboclown.p4plugin.ui.vcsroot.part.PropertiesPartUI;
import net.groboclown.p4plugin.ui.vcsroot.part.RequirePasswordPartUI;
import net.groboclown.p4plugin.ui.vcsroot.part.ServerFingerprintPartUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class ConfigPartStack {
    private static final Icon ADD_ITEM = AllIcons.General.Add;
    private JPanel rootPane;
    private JButton myAddItemButton;
    private JPanel partStackPanel;
    private final VirtualFile vcsRoot;
    private final ConfigConnectionController connectionController;
    private final List<ConfigPartWrapper> parts = new ArrayList<>();
    private final List<ConfigPartUIFactory> partFactories = Collections.unmodifiableList(Arrays.asList(
            FilePartUI.FACTORY,
            EnvPartUI.FACTORY,
            ClientNamePartUI.FACTORY,
            RequirePasswordPartUI.FACTORY,
            ServerFingerprintPartUI.FACTORY,
            PropertiesPartUI.FACTORY
    ));

    ConfigPartStack(VirtualFile vcsRoot, ConfigConnectionController connectionController) {
        $$$setupUI$$$();
        this.vcsRoot = vcsRoot;
        this.connectionController = connectionController;
        SwingUtil.iconOnlyButton(myAddItemButton, ADD_ITEM, SwingUtil.ButtonType.MINOR);
        myAddItemButton.addActionListener(e -> showAddItemPopup());
    }

    void setParts(List<ConfigPart> configParts) {
        synchronized (parts) {
            parts.clear();
            for (ConfigPart part: configParts) {
                boolean matched = false;
                for (ConfigPartUIFactory partFactory: partFactories) {
                    ConfigPartUI partUI = partFactory.createForPart(part, connectionController);
                    if (partUI != null) {
                        WrapListener listener = new WrapListener();
                        ConfigPartWrapper wrapper = new ConfigPartWrapper(partUI, listener);
                        // index of the wrapper set during fireListOrderChanged.
                        listener.wrapper = wrapper;
                        parts.add(wrapper);
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    throw new IllegalArgumentException("No UI config for part " + part);
                }
            }
        }
        fireListOrderChanged();
    }

    List<ConfigPart> getParts() {
        List<ConfigPart> ret;
        synchronized (parts) {
            ret = new ArrayList<>(parts.size());
            for (ConfigPartWrapper part: parts) {
                ret.add(part.updateConfigPart());
            }
        }
        return ret;
    }

    boolean isModified(List<ConfigPart> configParts) {
        List<ConfigPart> pendingParts = getParts();
        if (pendingParts.size() != configParts.size()) {
            return true;
        }
        for (int i = 0; i < pendingParts.size(); i++) {
            if (!pendingParts.get(i).equals(configParts.get(i))) {
                return true;
            }
        }
        return false;
    }

    private void showAddItemPopup() {
        final ListPopup popup = createConfigPartPopup(factory -> {
            if (factory != null) {
                addChildFirst(factory.createEmpty(vcsRoot, connectionController));
            }
        });
        popup.showUnderneathOf(myAddItemButton);
    }

    private ListPopup createConfigPartPopup(final Consumer<ConfigPartUIFactory> onChosen) {
        return JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<ConfigPartUIFactory>(
                P4Bundle.getString("configuration.stack.choose.title"),
                partFactories) {
            @Nullable
            @Override
            public PopupStep onChosen(ConfigPartUIFactory configPartType, boolean finalChoice) {
                onChosen.consume(configPartType);
                return super.onChosen(configPartType, finalChoice);
            }

            @Override
            public void canceled() {
                onChosen.consume(null);
            }

            @Nullable
            @Override
            public Icon getIconFor(ConfigPartUIFactory factory) {
                return factory.getIcon();
            }

            @NotNull
            @Override
            public String getTextFor(ConfigPartUIFactory factory) {
                return factory.getName();
            }
        });
    }


    private void fireListOrderChanged() {
        synchronized (parts) {
            partStackPanel.removeAll();
            final int size = parts.size();
            for (int i = 0; i < size; i++) {
                ConfigPartWrapper wrapper = parts.get(i);
                wrapper.setListPosition(i, i == 0, i >= parts.size() - 1);
                partStackPanel.add(wrapper.getRootPane());
            }
        }
        partStackPanel.getParent().revalidate();
        partStackPanel.getParent().doLayout();
        partStackPanel.getParent().repaint();
    }

    private void addChildFirst(ConfigPartUI partUI) {
        WrapListener listener = new WrapListener();
        ConfigPartWrapper wrapper = new ConfigPartWrapper(partUI, listener);
        listener.wrapper = wrapper;
        synchronized (parts) {
            parts.add(0, wrapper);
            // setting the position will be done during order change call.
            // wrapper.setListPosition(0, true, parts.size() == 1);
        }
        fireListOrderChanged();
    }

    private void move(ConfigPartWrapper wrapper, int positionChange) {
        final int origPos = wrapper.getPosition();
        final boolean swapped;
        synchronized (parts) {
            final int size = parts.size();
            if (origPos < 0 || origPos >= size || wrapper != parts.get(origPos)) {
                throw new IllegalStateException("Child at " + origPos + " is invalid");
            }
            final int newPos = origPos + positionChange;
            if (newPos >= 0 && newPos < size) {
                // we can swap
                final ConfigPartWrapper swap = parts.get(newPos);
                parts.set(newPos, wrapper);
                parts.set(origPos, swap);
                // "reload children" will set the position for us
                swapped = true;
            } else {
                swapped = false;
            }
        }

        if (swapped) {
            fireListOrderChanged();
        }
    }

    private void remove(ConfigPartWrapper wrapper) {
        synchronized (parts) {
            final int size = parts.size();
            int pos = wrapper.getPosition();
            if (pos < 0 || pos >= size || wrapper != parts.get(pos)) {
                throw new IllegalStateException("Child at " + pos + " is invalid");
            }
            parts.remove(pos);
        }
        fireListOrderChanged();
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
        rootPane = new JPanel();
        rootPane.setLayout(new BorderLayout(0, 0));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        rootPane.add(panel1, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("configuration.stack.title"));
        panel1.add(label1, BorderLayout.CENTER);
        myAddItemButton = new JButton();
        myAddItemButton.setHideActionText(true);
        myAddItemButton.setText("");
        myAddItemButton.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("configuration.connection-choice.picker.tooltip"));
        panel1.add(myAddItemButton, BorderLayout.EAST);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        rootPane.add(panel2, BorderLayout.CENTER);
        final JScrollPane scrollPane1 = new JScrollPane();
        CellConstraints cc = new CellConstraints();
        panel2.add(scrollPane1, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        scrollPane1.setViewportView(partStackPanel);
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
        return rootPane;
    }

    private class WrapListener
            implements ListPositionChangeController {
        ConfigPartWrapper wrapper;

        @Override
        public void moveUpPosition() {
            move(wrapper, -1);
        }

        @Override
        public void moveDownPosition() {
            move(wrapper, 1);
        }

        @Override
        public void removePart() {
            remove(wrapper);
        }
    }

    private void createUIComponents() {
        // custom component creation code
        partStackPanel = new JPanel(new VerticalFlowLayout());
    }


}
