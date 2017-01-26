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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.part.SimpleDataPart;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ResourceBundle;

public class PropertyConfigPanel
        extends ConfigPartPanel<SimpleDataPart> {
    private JPanel rootPanel;
    private JLabel portFieldLabel;
    private JTextField portField;
    private JTextField userField;
    private JLabel userFieldLabel;
    private TextFieldWithBrowseButton authTicketFileField;
    private JLabel authTicketFileFieldLabel;
    private TextFieldWithBrowseButton trustTicketFileField;
    private JLabel trustTicketFileFieldLabel;
    private JTextField hostnameField;
    private JLabel hostnameFieldLabel;
    private JTextField ignoreFileNameField;
    private JLabel ignoreFileNameFieldLabel;
    private JTextField charsetField;
    private JLabel charsetFieldLabel;

    PropertyConfigPanel(@NotNull Project project, @NotNull final SimpleDataPart part) {
        super(project, part);

        authTicketFileFieldLabel.setLabelFor(authTicketFileField);
        trustTicketFileFieldLabel.setLabelFor(trustTicketFileField);

        portField.setText(nullEmptyTrim(part.getRawServerName()));
        portField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                part.setServerName(portField.getText());
            }
        });

        userField.setText(nullEmptyTrim(part.getUsername()));
        userField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                part.setUsername(userField.getText());
            }
        });

        authTicketFileField.setText(nullEmptyFile(project, part.getAuthTicketFile()));
        authTicketFileField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                part.setAuthTicketFile(authTicketFileField.getText());
            }
        });

        trustTicketFileField.setText(nullEmptyFile(project, part.getTrustTicketFile()));
        trustTicketFileField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                part.setTrustTicketFile(trustTicketFileField.getText());
            }
        });

        hostnameField.setText(nullEmptyTrim(part.getClientHostname()));
        hostnameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                part.setClientHostname(hostnameField.getText());
            }
        });

        ignoreFileNameField.setText(nullEmptyTrim(part.getIgnoreFileName()));
        ignoreFileNameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                part.setIgnoreFilename(ignoreFileNameField.getText());
            }
        });

        charsetField.setText(nullEmptyTrim(part.getDefaultCharset()));
        charsetField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                part.setDefaultCharset(charsetField.getText());
            }
        });
    }

    @Nls
    @NotNull
    @Override
    public String getTitle() {
        return P4Bundle.getString("configuration.stack.property.title");
    }

    @NotNull
    @Override
    public JPanel getRootPanel() {
        return rootPanel;
    }

    @NotNull
    @Override
    SimpleDataPart copyPart() {
        return new SimpleDataPart(getProject(), getConfigPart());
    }

    @Override
    public boolean isModified(@NotNull SimpleDataPart originalPart) {
        return !originalPart.equals(getConfigPart());
    }

    @NotNull
    private static String nullEmptyTrim(@Nullable String str) {
        if (str == null) {
            return "";
        }
        return str.trim();
    }

    @NotNull
    private static String nullEmptyFile(@NotNull Project project, @Nullable File f) {
        if (f == null) {
            return project.getBaseDir().getPath();
        }
        return f.getAbsolutePath();
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
        rootPanel.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:d:grow",
                "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        portFieldLabel = new JLabel();
        portFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(portFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.port.label"));
        CellConstraints cc = new CellConstraints();
        rootPanel.add(portFieldLabel, cc.xy(1, 1));
        portField = new JTextField();
        rootPanel.add(portField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        userField = new JTextField();
        rootPanel.add(userField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        userFieldLabel = new JLabel();
        userFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(userFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.username.label"));
        rootPanel.add(userFieldLabel, cc.xy(1, 3));
        authTicketFileField = new TextFieldWithBrowseButton();
        rootPanel.add(authTicketFileField, cc.xy(3, 5));
        authTicketFileFieldLabel = new JLabel();
        authTicketFileFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(authTicketFileFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.authticketfile.label"));
        rootPanel.add(authTicketFileFieldLabel, cc.xy(1, 5));
        trustTicketFileField = new TextFieldWithBrowseButton();
        rootPanel.add(trustTicketFileField, cc.xy(3, 7));
        trustTicketFileFieldLabel = new JLabel();
        trustTicketFileFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(trustTicketFileFieldLabel,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                        .getString("configuration.properties.trustticketfile.label"));
        rootPanel.add(trustTicketFileFieldLabel, cc.xy(1, 7));
        hostnameField = new JTextField();
        rootPanel.add(hostnameField, cc.xy(3, 9, CellConstraints.FILL, CellConstraints.DEFAULT));
        hostnameFieldLabel = new JLabel();
        hostnameFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(hostnameFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.hostname.label"));
        rootPanel.add(hostnameFieldLabel, cc.xy(1, 9));
        ignoreFileNameField = new JTextField();
        rootPanel.add(ignoreFileNameField, cc.xy(3, 11, CellConstraints.FILL, CellConstraints.DEFAULT));
        ignoreFileNameFieldLabel = new JLabel();
        ignoreFileNameFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(ignoreFileNameFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.ignore.label"));
        rootPanel.add(ignoreFileNameFieldLabel, cc.xy(1, 11));
        charsetField = new JTextField();
        rootPanel.add(charsetField, cc.xy(3, 13, CellConstraints.FILL, CellConstraints.DEFAULT));
        charsetFieldLabel = new JLabel();
        charsetFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(charsetFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.charset.label"));
        rootPanel.add(charsetFieldLabel, cc.xy(1, 13));
        portFieldLabel.setLabelFor(portField);
        userFieldLabel.setLabelFor(userField);
        ignoreFileNameFieldLabel.setLabelFor(ignoreFileNameField);
        charsetFieldLabel.setLabelFor(charsetField);
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
