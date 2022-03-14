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
import com.intellij.ui.FilterComponent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchSelectPanel<T> extends JPanel {
    private final Object itemSync = new Object();
    private final List<ColumnInfo<T, String>> searchableColumns;
    private final List<SelectedItem<T>> items = new ArrayList<>();
    private final ListTableModel<SelectedItem<T>> tableModel;

    public interface SelectionChangedListener {
        /**
         *
         * @param count number of items selected.  If < 0, then an error was encountered loading the data.
         */
        void selectionCount(int count);
    }

    public SearchSelectPanel(@NotNull P4CommandRunner.QueryAnswer<? extends Collection<T>> query,
            @Nullable final SelectionChangedListener listener, @NotNull List<ColumnInfo<T, ?>> columns) {
        super(new BorderLayout());

        Runnable onSelectionChange = () -> {
            if (listener == null) {
                return;
            }
            int count = 0;
            synchronized (itemSync) {
                for (SelectedItem<T> item : items) {
                    if (item.selected) {
                        count++;
                    }
                }
            }
            listener.selectionCount(count);
        };

        ColumnInfo<?, ?>[] columnArray = new ColumnInfo[columns.size() + 1];
        columnArray[0] = new BooleanColumnInfo<SelectedItem<T>>(P4Bundle.getString("search-select.column.selected"), true) {
            @Override
            protected boolean booleanValue(SelectedItem<T> o) {
                return o.selected;
            }

            @Override
            protected void setBooleanValue(SelectedItem<T> o, boolean value) {
                o.selected = value;
                onSelectionChange.run();
            }
        };

        this.searchableColumns = new ArrayList<>(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo<T, ?> column = columns.get(i);
            columnArray[i + 1] = proxyColumnInfo(column, onSelectionChange);
            if (String.class.equals(column.getColumnClass())) {
                //noinspection unchecked
                searchableColumns.add((ColumnInfo<T, String>) column);
            }
        }
        tableModel = new ListTableModel<>(columnArray);

        final JBTable table = new JBTable(tableModel);
        table.getColumnModel().getColumn(0).setCellEditor(new BooleanTableCellEditor());
        table.getColumnModel().getColumn(0).setCellRenderer(new BooleanTableCellRenderer(SwingConstants.CENTER));
        this.add(new JBScrollPane(table), BorderLayout.CENTER);
        this.add(table.getTableHeader(), BorderLayout.NORTH);
        table.getEmptyText().setText(P4Bundle.getString("search-select.status.loading"));


        final FilterComponent filterComponent = new FilterComponent("search-select", 10, true) {
            @Override
            public void filter() {
                String text = getFilter();
                filterTable(text);
            }
        };

        this.add(filterComponent, BorderLayout.SOUTH);

        query
                .whenCompleted((tValues) -> {
                    table.getEmptyText().setText(P4Bundle.getString("search-select.status.empty"));
                    synchronized (itemSync) {
                        items.clear();
                        tValues.forEach(t -> items.add(new SelectedItem<>(t)));
                    }
                    filterTable(filterComponent.getFilter());
                })
                .whenServerError(e -> {
                    table.getEmptyText().setText(P4Bundle.message("search-select.status.error",
                            e.getLocalizedMessage()));
                    synchronized (itemSync) {
                        items.clear();
                    }
                    if (listener != null) {
                        listener.selectionCount(-1);
                    }
                });


    }

    public Stream<T> getSelectedItems() {
        return tableModel.getItems().stream()
                .filter(t -> t.selected)
                .map(t -> t.value);
    }

    public void setItems(Collection<T> items) {
        tableModel.setItems(
                items.stream()
                    .map(SelectedItem::new)
                    .collect(Collectors.toList())
        );
    }


    private void filterTable(String text) {
        final List<SelectedItem<T>> visibleItems;
        if (text == null || text.isEmpty()) {
            synchronized (itemSync) {
                visibleItems = new ArrayList<>(items);
            }
        } else {
            List<SelectedItem<T>> rootItems;
            synchronized (itemSync) {
                rootItems = new ArrayList<>(items);
            }
            visibleItems = new ArrayList<>(rootItems.size());
            for (SelectedItem<T> item : rootItems) {
                if (item.selected) {
                    visibleItems.add(item);
                    continue;
                }
                for (ColumnInfo<T, String> searchableColumn : searchableColumns) {
                    String value = searchableColumn.valueOf(item.value);
                    if (value == null || value.isEmpty()) {
                        // Skip blank values when a filter is on.
                        continue;
                    }
                    String lc = value.toLowerCase();
                    if (lc.contains(text)) {
                        visibleItems.add(item);
                        break;
                    }
                }
            }
        }
        SwingUtilities.invokeLater(() -> tableModel.setItems(visibleItems));
    }

    private <Aspect> ColumnInfo<SelectedItem<T>, Aspect> proxyColumnInfo(ColumnInfo<T, Aspect> src,
            Runnable listener) {
        return new ProxyColumnInfo<>(src, listener);
    }


    private class ProxyColumnInfo<Aspect> extends ColumnInfo<SelectedItem<T>, Aspect> {
        private final ColumnInfo<T, Aspect> proxy;
        private final Runnable listener;

        private ProxyColumnInfo(ColumnInfo<T, Aspect> proxy,
                Runnable listener) {
            super(proxy.getName());
            this.proxy = proxy;
            this.listener = listener;
        }

        @Override
        @Nullable
        public Icon getIcon() {
            return proxy.getIcon();
        }

        @Override
        @Nullable
        public Aspect valueOf(SelectedItem<T> o) {
            return proxy.valueOf(o.value);
        }

        @Override
        @Nullable
        public Comparator<SelectedItem<T>> getComparator() {
            return new SelectedOrderComparator<>(proxy.getComparator());
        }

        @Override
        public Class<?> getColumnClass() {
            return proxy.getColumnClass();
        }

        @Override
        public boolean isCellEditable(SelectedItem<T> item) {
            return proxy.isCellEditable(item.value);
        }

        @Override
        public void setValue(SelectedItem<T> item, Aspect value) {
            proxy.setValue(item.value, value);
            listener.run();
        }

        @Override
        @Nullable
        public TableCellRenderer getRenderer(SelectedItem<T> item) {
            return proxy.getRenderer(item.value);
        }

        @Override
        public TableCellRenderer getCustomizedRenderer(SelectedItem<T> o, TableCellRenderer renderer) {
            return proxy.getCustomizedRenderer(o.value, renderer);
        }

        @Override
        @Nullable
        public TableCellEditor getEditor(SelectedItem<T> item) {
            return proxy.getEditor(item.value);
        }

        @Override
        @Nullable
        public String getMaxStringValue() {
            return proxy.getMaxStringValue();
        }

        @Override
        @Nullable
        public String getPreferredStringValue() {
            return proxy.getPreferredStringValue();
        }

        @Override
        public int getAdditionalWidth() {
            return proxy.getAdditionalWidth();
        }

        @Override
        public int getWidth(JTable table) {
            return proxy.getWidth(table);
        }

        @Override
        public void setName(String s) {
            proxy.setName(s);
        }

        @Override
        @Nullable
        public String getTooltipText() {
            return proxy.getTooltipText();
        }

        @Override
        public boolean hasError() {
            return proxy.hasError();
        }

    }


    private static class SelectedOrderComparator<T> implements Comparator<SelectedItem<T>> {
        private final Comparator<T> proxy;

        private SelectedOrderComparator(Comparator<T> proxy) {
            this.proxy = proxy;
        }

        @Override
        public int compare(SelectedItem<T> o1, SelectedItem<T> o2) {
            return (
                    (o1.selected && !o2.selected)
                        ? -1
                        : (!o1.selected && o2.selected)
                            ? 1
                            : (proxy == null ? 0 : proxy.compare(o1.value, o2.value))
            );
        }
    }


    private static class SelectedItem<T> {
        final T value;
        boolean selected;

        private SelectedItem(T value) {
            this.value = value;
        }
    }
}
