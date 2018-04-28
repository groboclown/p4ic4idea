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

package net.groboclown.idea.p4ic.ui.config.props;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.part.RelativeConfigCompositePart;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class RelativeConfigPartPanel
        extends ConfigPartPanel<RelativeConfigCompositePart> {
    private static final Logger LOG = Logger.getInstance(RelativeConfigPartPanel.class);

    private static final String DEFAULT_FILE_NAME = ".p4config";

    private JPanel rootPanel;
    private JTextField nameField;
    private JLabel nameFieldLabel;

    RelativeConfigPartPanel(@NotNull Project project, @NotNull final RelativeConfigCompositePart part) {
        super(project, part);
        if (part.getName() != null) {
            part.setName(DEFAULT_FILE_NAME);
        }
        nameField.setText(part.getName());
    }

    @Nls
    @NotNull
    @Override
    public String getTitle() {
        return P4Bundle.getString("configuration.stack.relative.title");
    }

    @NotNull
    @Override
    public JPanel getRootPanel() {
        return rootPanel;
    }

    @Override
    public void updateConfigPartFromUI() {
        getConfigPart().setName(nameField.getText());
        LOG.debug("Set relative config file name to " + getConfigPart().getName());
    }

    @NotNull
    @Override
    RelativeConfigCompositePart copyPart() {
        final RelativeConfigCompositePart ret = new RelativeConfigCompositePart(getProject());
        ret.setName(getConfigPart().getName());
        return ret;
    }

    @Override
    public boolean isModified(@NotNull RelativeConfigCompositePart originalPart) {
        return !StringUtil.equals(originalPart.getName(), nameField.getText());
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
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(4, 0));
        nameFieldLabel = new JLabel();
        this.$$$loadLabelText$$$(nameFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.relp4config.file.label"));
        rootPanel.add(nameFieldLabel, BorderLayout.WEST);
        nameField = new JTextField();
        nameField.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("config.rel.path.tooltip"));
        rootPanel.add(nameField, BorderLayout.CENTER);
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
        return rootPanel;
    }
}
