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

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TextFieldUtil {
    public static void addTo(@NotNull final JTextField field, @NotNull final TextFieldListener listener) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.textUpdated(e, field.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.textUpdated(e, field.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.textUpdated(e, field.getText());
            }
        });

        field.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                listener.enabledStateChanged(evt);
            }
        });
    }
    public static void addTo(@NotNull final JTextArea field, @NotNull final TextFieldListener listener) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.textUpdated(e, field.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.textUpdated(e, field.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.textUpdated(e, field.getText());
            }
        });

        field.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                listener.enabledStateChanged(evt);
            }
        });
    }
}
