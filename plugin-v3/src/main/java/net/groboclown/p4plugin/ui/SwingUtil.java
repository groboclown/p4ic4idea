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

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class SwingUtil {
    public static JLabel createLabelFor(String text, JComponent editComponent) {
        JLabel ret = new JLabel();
        loadLabelText(ret, text);
        ret.setVerticalAlignment(0);
        ret.setLabelFor(editComponent);
        return ret;
    }

    public static void loadLabelText(JLabel component, String text) {
        StringBuilder result = new StringBuilder();
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

    public static void loadButtonText(AbstractButton component, String text) {
        StringBuilder result = new StringBuilder();
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

    public enum ButtonType {
        ACCENT(0),
        MAJOR(6),
        MINOR(2);

        private final int borderSize;

        ButtonType(int borderSize) {
            this.borderSize = borderSize;
        }
    }

    /**
     * Set a button to have only the icon.
     *
     * @param button
     * @param icon
     */
    public static JButton iconOnlyButton(@NotNull JButton button, @NotNull Icon icon, @NotNull ButtonType type) {
        button.setIcon(icon);
        button.setPreferredSize(new Dimension(icon.getIconWidth() + type.borderSize,
                icon.getIconHeight() + type.borderSize));
        return button;
    }
}
