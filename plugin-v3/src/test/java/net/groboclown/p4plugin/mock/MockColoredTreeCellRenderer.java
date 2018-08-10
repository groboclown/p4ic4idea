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

package net.groboclown.p4plugin.mock;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class MockColoredTreeCellRenderer extends ColoredTreeCellRenderer {
    public static class Appended {
        public final String fragment;
        public final SimpleTextAttributes attributes;
        public final boolean isMainText;

        public Appended(String fragment, SimpleTextAttributes attributes, boolean isMainText) {
            this.fragment = fragment;
            this.attributes = attributes;
            this.isMainText = isMainText;
        }
    }

    public final List<Appended> appendedText = new ArrayList<>();

    public void append(@NotNull @Nls String fragment, @NotNull SimpleTextAttributes attributes, boolean isMainText) {
        appendedText.add(new Appended(fragment, attributes, isMainText));
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree jTree, Object o, boolean b, boolean b1, boolean b2, int i,
            boolean b3) {
        throw new IllegalStateException("Should not be called by decorator");
    }

    @Override
    public void updateUI() {
        // do nothing
    }
}
