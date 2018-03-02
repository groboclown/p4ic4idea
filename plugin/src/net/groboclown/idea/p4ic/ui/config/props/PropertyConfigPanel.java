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
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.part.SimpleDataPart;
import net.groboclown.idea.p4ic.util.EqualUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.ResourceBundle;

public class PropertyConfigPanel
        extends ConfigPartPanel<SimpleDataPart> {
    private static final Logger LOG = Logger.getInstance(PropertyConfigPanel.class);


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
    private TextFieldWithBrowseButton loginSsoField;
    private JLabel loginSsoFieldLabel;

    PropertyConfigPanel(@NotNull Project project, @NotNull final SimpleDataPart part) {
        super(project, part);

        portField.setText(nullEmptyTrim(part.getRawServerName()));

        userField.setText(nullEmptyTrim(part.getUsername()));

        authTicketFileFieldLabel.setLabelFor(authTicketFileField);
        authTicketFileField.setText(nullEmptyFile(project, part.getAuthTicketFile()));
        authTicketFileField.setButtonEnabled(true);
        authTicketFileField.addBrowseFolderListener(
                P4Bundle.getString("configuration.properties.authticket.chooser.title"),
                P4Bundle.getString("configuration.properties.authticket.chooser.desc"),
                project,
                FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
        );

        trustTicketFileFieldLabel.setLabelFor(trustTicketFileField);
        trustTicketFileField.setText(nullEmptyFile(project, part.getTrustTicketFile()));
        trustTicketFileField.setButtonEnabled(true);
        trustTicketFileField.addBrowseFolderListener(
                P4Bundle.getString("configuration.properties.trustticket.chooser.title"),
                P4Bundle.getString("configuration.properties.trustticket.chooser.desc"),
                project,
                FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
        );

        hostnameField.setText(nullEmptyTrim(part.getClientHostname()));

        ignoreFileNameField.setText(nullEmptyTrim(part.getIgnoreFileName()));

        charsetField.setText(nullEmptyTrim(part.getDefaultCharset()));

        loginSsoFieldLabel.setLabelFor(loginSsoField);
        loginSsoField.setText(nullEmptyTrim(part.getLoginSso()));
        loginSsoField.setButtonEnabled(true);
        loginSsoField.addBrowseFolderListener(
                P4Bundle.getString("configuration.properties.loginsso.chooser.title"),
                P4Bundle.getString("configuration.properties.loginsso.chooser.desc"),
                project,
                FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
        );
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

    @Override
    public void updateConfigPartFromUI() {
        updateConfigPartFromUI(getConfigPart());
    }

    private void updateConfigPartFromUI(@NotNull SimpleDataPart part) {
        part.setClientname(null);
        part.setDefaultCharset(charsetField.getText());
        part.setIgnoreFilename(ignoreFileNameField.getText());
        part.setClientHostname(hostnameField.getText());
        part.setTrustTicketFile(trustTicketFileField.getText());
        part.setAuthTicketFile(authTicketFileField.getText());
        part.setUsername(userField.getText());
        part.setServerName(portField.getText());
        part.setLoginSsoFile(loginSsoField.getText());
    }

    @NotNull
    @Override
    SimpleDataPart copyPart() {
        return new SimpleDataPart(getProject(), getConfigPart());
    }

    @Override
    public boolean isModified(@NotNull SimpleDataPart originalPart) {
        // Accurate is-modified requires creating a new part, so that the real values
        // can be compared.
        SimpleDataPart newPart = new SimpleDataPart(getProject(), Collections.<String, String>emptyMap());
        updateConfigPartFromUI(newPart);
        return !newPart.equals(originalPart);
    }

    private static boolean isNotEqual(@NotNull JTextField field, @Nullable String value) {
        return !EqualUtil.isEqual(field.getText(), value);
    }

    private static boolean isNotEqual(@NotNull TextFieldWithBrowseButton field, @Nullable File file) {
        final File f;
        if (field.getText().isEmpty()) {
            f = null;
        } else {
            f = new File(field.getText());
        }
        return !FileUtil.filesEqual(f, file);
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
            // return project.getBaseDir().getPath();
            return "";
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
                "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        portFieldLabel = new JLabel();
        portFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(portFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.port.label"));
        portFieldLabel.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4port.tooltip"));
        CellConstraints cc = new CellConstraints();
        rootPanel.add(portFieldLabel, cc.xy(1, 1));
        portField = new JTextField();
        portField.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4port.tooltip"));
        rootPanel.add(portField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        userField = new JTextField();
        userField.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4user.tooltip"));
        rootPanel.add(userField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        userFieldLabel = new JLabel();
        userFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(userFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.username.label"));
        userFieldLabel.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4user.tooltip"));
        rootPanel.add(userFieldLabel, cc.xy(1, 3));
        authTicketFileField = new TextFieldWithBrowseButton();
        authTicketFileField.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4tickets.tooltip"));
        rootPanel.add(authTicketFileField, cc.xy(3, 5));
        authTicketFileFieldLabel = new JLabel();
        authTicketFileFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(authTicketFileFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.authticketfile.label"));
        authTicketFileFieldLabel.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4tickets.tooltip"));
        rootPanel.add(authTicketFileFieldLabel, cc.xy(1, 5));
        trustTicketFileField = new TextFieldWithBrowseButton();
        trustTicketFileField.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4trust.tooltip"));
        rootPanel.add(trustTicketFileField, cc.xy(3, 7));
        trustTicketFileFieldLabel = new JLabel();
        trustTicketFileFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(trustTicketFileFieldLabel,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                        .getString("configuration.properties.trustticketfile.label"));
        trustTicketFileFieldLabel.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4trust.tooltip"));
        rootPanel.add(trustTicketFileFieldLabel, cc.xy(1, 7));
        hostnameField = new JTextField();
        hostnameField.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4host.tooltip"));
        rootPanel.add(hostnameField, cc.xy(3, 9, CellConstraints.FILL, CellConstraints.DEFAULT));
        hostnameFieldLabel = new JLabel();
        hostnameFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(hostnameFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.hostname.label"));
        hostnameFieldLabel.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4host.tooltip"));
        rootPanel.add(hostnameFieldLabel, cc.xy(1, 9));
        ignoreFileNameField = new JTextField();
        ignoreFileNameField.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4ignore.tooltip"));
        rootPanel.add(ignoreFileNameField, cc.xy(3, 11, CellConstraints.FILL, CellConstraints.DEFAULT));
        ignoreFileNameFieldLabel = new JLabel();
        ignoreFileNameFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(ignoreFileNameFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.ignore.label"));
        ignoreFileNameFieldLabel.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4ignore.tooltip"));
        rootPanel.add(ignoreFileNameFieldLabel, cc.xy(1, 11));
        charsetField = new JTextField();
        charsetField.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4charset.tooltip"));
        rootPanel.add(charsetField, cc.xy(3, 13, CellConstraints.FILL, CellConstraints.DEFAULT));
        charsetFieldLabel = new JLabel();
        charsetFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(charsetFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.charset.label"));
        charsetFieldLabel.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4charset.tooltip"));
        rootPanel.add(charsetFieldLabel, cc.xy(1, 13));
        loginSsoFieldLabel = new JLabel();
        loginSsoFieldLabel.setHorizontalAlignment(11);
        this.$$$loadLabelText$$$(loginSsoFieldLabel, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.loginsso.label"));
        loginSsoFieldLabel.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4loginsso.tooltip"));
        rootPanel.add(loginSsoFieldLabel, cc.xy(1, 15));
        loginSsoField = new TextFieldWithBrowseButton();
        loginSsoField.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.properties.p4loginsso.tooltip"));
        rootPanel.add(loginSsoField, cc.xy(3, 15));
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
