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

package net.groboclown.p4plugin.ui.vcsroot.part;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.impl.config.part.SimpleDataPart;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.vcsroot.ConfigConnectionController;
import net.groboclown.p4plugin.ui.vcsroot.ConfigPartUI;
import net.groboclown.p4plugin.ui.vcsroot.ConfigPartUIFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

public class PropertiesPartUI
        extends ConfigPartUI<SimpleDataPart> {
    public static final ConfigPartUIFactory FACTORY = new Factory();
    private JPanel root;
    private JTextField myPortField;
    private JTextField myUserField;
    private JTextField myHostField;
    private JTextField myLoginSsoField;
    private JTextField myIgnoreField;
    private JTextField myCharsetField;
    private TextFieldWithBrowseButton myTicketField;
    private TextFieldWithBrowseButton myTrustField;
    private JLabel myTicketLabel;
    private JLabel myTrustLabel;

    private PropertiesPartUI(SimpleDataPart part) {
        super(part);

        myPortField.setText(nullEmptyTrim(part.getRawServerName()));

        myUserField.setText(nullEmptyTrim(part.getUsername()));

        myTicketLabel.setLabelFor(myTicketField);
        myTicketField.setText(nullEmptyFile(part.getAuthTicketFile()));
        myTicketField.setButtonEnabled(true);
        myTicketField.addBrowseFolderListener(
                P4Bundle.getString("configuration.properties.authticket.chooser.title"),
                P4Bundle.getString("configuration.properties.authticket.chooser.desc"),
                null,
                FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
        );

        myTrustLabel.setLabelFor(myTrustField);
        myTrustField.setText(nullEmptyFile(part.getTrustTicketFile()));
        myTrustField.setButtonEnabled(true);
        myTrustField.addBrowseFolderListener(
                P4Bundle.getString("configuration.properties.trustticket.chooser.title"),
                P4Bundle.getString("configuration.properties.trustticket.chooser.desc"),
                null,
                FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
        );

        myHostField.setText(nullEmptyTrim(part.getClientHostname()));

        myIgnoreField.setText(nullEmptyTrim(part.getIgnoreFileName()));

        myCharsetField.setText(nullEmptyTrim(part.getDefaultCharset()));

        myLoginSsoField.setText(nullEmptyTrim(part.getLoginSso()));
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @NotNull
    @Override
    public String getPartTitle() {
        return P4Bundle.getString("configuration.stack.type.property");
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getPartDescription() {
        return P4Bundle.getString("configuration.stack.type.property.description");
    }

    @NotNull
    @Override
    protected SimpleDataPart loadUIValuesIntoPart(@NotNull SimpleDataPart part) {
        part.setServerName(nullEmptyGet(myPortField));
        part.setUsername(nullEmptyGet(myUserField));
        part.setAuthTicketFile(nullEmptyGet(myTicketField));
        part.setTrustTicketFile(nullEmptyGet(myTrustField));
        part.setClientHostname(nullEmptyGet(myHostField));
        part.setIgnoreFilename(nullEmptyGet(myIgnoreField));
        part.setDefaultCharset(nullEmptyGet(myCharsetField));
        part.setLoginSsoFile(nullEmptyGet(myLoginSsoField));
        return part;
    }

    @Override
    public JComponent getPanel() {
        return root;
    }

    private static String nullEmptyGet(JTextField field) {
        String t = field.getText();
        if (t == null) {
            return null;
        }
        t = t.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }

    private static File nullEmptyGet(TextFieldWithBrowseButton field) {
        String t = field.getText().trim();
        if (t.isEmpty()) {
            return null;
        }
        return new File(t);
    }

    @NotNull
    private static String nullEmptyTrim(@Nullable String str) {
        if (str == null) {
            return "";
        }
        return str.trim();
    }

    @NotNull
    private static String nullEmptyFile(@Nullable File f) {
        if (f == null) {
            // return vcsRoot.getPath();
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
        root = new JPanel();
        root.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:d:grow",
                "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.port.label"));
        CellConstraints cc = new CellConstraints();
        root.add(label1, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        myPortField = new JTextField();
        myPortField.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.p4port.tooltip"));
        root.add(myPortField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.username.label"));
        root.add(label2, cc.xy(1, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.hostname.label"));
        root.add(label3, cc.xy(1, 5, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        myTicketLabel = new JLabel();
        this.$$$loadLabelText$$$(myTicketLabel, this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.authticketfile.label"));
        root.add(myTicketLabel, cc.xy(1, 7, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        myTrustLabel = new JLabel();
        this.$$$loadLabelText$$$(myTrustLabel, this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.trustticketfile.label"));
        root.add(myTrustLabel, cc.xy(1, 9, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.ignore.label"));
        root.add(label4, cc.xy(1, 13, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.loginsso.label"));
        root.add(label5, cc.xy(1, 11, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.charset.label"));
        root.add(label6, cc.xy(1, 15, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        myUserField = new JTextField();
        myUserField.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.p4user.tooltip"));
        root.add(myUserField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        myHostField = new JTextField();
        myHostField.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.p4host.tooltip"));
        root.add(myHostField, cc.xy(3, 5, CellConstraints.FILL, CellConstraints.DEFAULT));
        myTicketField = new TextFieldWithBrowseButton();
        myTicketField.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.p4tickets.tooltip"));
        root.add(myTicketField, cc.xy(3, 7));
        myTrustField = new TextFieldWithBrowseButton();
        myTrustField.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.p4trust.tooltip"));
        root.add(myTrustField, cc.xy(3, 9));
        myLoginSsoField = new JTextField();
        myLoginSsoField.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.p4loginsso.tooltip"));
        root.add(myLoginSsoField, cc.xy(3, 11, CellConstraints.FILL, CellConstraints.DEFAULT));
        myIgnoreField = new JTextField();
        myIgnoreField.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.p4ignore.tooltip"));
        root.add(myIgnoreField, cc.xy(3, 13, CellConstraints.FILL, CellConstraints.DEFAULT));
        myCharsetField = new JTextField();
        myCharsetField.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "configuration.properties.p4charset.tooltip"));
        root.add(myCharsetField, cc.xy(3, 15, CellConstraints.FILL, CellConstraints.DEFAULT));
        label1.setLabelFor(myPortField);
        label2.setLabelFor(myUserField);
        label3.setLabelFor(myHostField);
        label4.setLabelFor(myIgnoreField);
        label5.setLabelFor(myLoginSsoField);
        label6.setLabelFor(myCharsetField);
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

    private static class Factory
            implements ConfigPartUIFactory {
        @Nls(capitalization = Nls.Capitalization.Title)
        @NotNull
        @Override
        public String getName() {
            return P4Bundle.getString("configuration.stack.type.property");
        }

        @Nullable
        @Override
        public Icon getIcon() {
            return null;
        }

        @Nullable
        @Override
        public ConfigPartUI createForPart(ConfigPart part, ConfigConnectionController controller) {
            if (part instanceof SimpleDataPart) {
                return new PropertiesPartUI((SimpleDataPart) part);
            }
            return null;
        }

        @NotNull
        @Override
        public ConfigPartUI createEmpty(@NotNull VirtualFile vcsRoot, ConfigConnectionController controller) {
            return new PropertiesPartUI(new SimpleDataPart(vcsRoot, getName(), null));
        }
    }

}
