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

import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.util.ui.ColumnInfo;

import javax.swing.*;
import java.awt.*;

public abstract class BooleanColumnInfo<Item> extends ColumnInfo<Item, Boolean> {
    private static int EDITABLE_CELL_WIDTH = -2;
    private static int READONLY_CELL_WIDTH = -2;
    private final boolean editable;

    public BooleanColumnInfo(String name, boolean editable) {
        super(name);
        this.editable = editable;
    }

    @Override
    public final Boolean valueOf(Item o) {
        return booleanValue(o);
    }

    protected abstract boolean booleanValue(Item item);

    @Override
    public final boolean isCellEditable(Item item) {
        return editable;
    }

    @Override
    public final void setValue(Item item, Boolean value) {
        setBooleanValue(item, value);
    }

    protected abstract void setBooleanValue(Item item, boolean value);

    @Override
    public final Class<?> getColumnClass() {
        return Boolean.class;
    }

    @Override
    public final int getWidth(JTable table) {
        final int width;
        if (editable) {
            synchronized (BooleanColumnInfo.class) {
                if (EDITABLE_CELL_WIDTH == -2) {
                    BooleanTableCellEditor cell = new BooleanTableCellEditor(false);
                    Component renderer = cell.getTableCellEditorComponent(table, Boolean.TRUE, true, 0, 0);
                    EDITABLE_CELL_WIDTH = renderer.getWidth();
                    if (EDITABLE_CELL_WIDTH <= 0) {
                        EDITABLE_CELL_WIDTH = -1;
                    }
                }
                width = EDITABLE_CELL_WIDTH;
            }
        } else {
            synchronized (BooleanColumnInfo.class) {
                if (READONLY_CELL_WIDTH == -2) {
                    BooleanTableCellRenderer cell = new BooleanTableCellRenderer();
                    Component renderer = cell.getTableCellRendererComponent(table, Boolean.TRUE, true, true, 0, 0);
                    READONLY_CELL_WIDTH = renderer.getWidth();
                    if (READONLY_CELL_WIDTH <= 0) {
                        READONLY_CELL_WIDTH = -1;
                    }
                }
                width = READONLY_CELL_WIDTH;
            }
        }
        return width;
    }
}
