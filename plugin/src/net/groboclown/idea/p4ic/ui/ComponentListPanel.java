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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.VerticalFlowLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A JPanel that has child panels arranged along an axis (either vertical or horizontal)
 * that can be moved and selected.
 */
public class ComponentListPanel<T extends ComponentListPanel.WithRootPanel> extends JPanel {
    private static final Logger LOG = Logger.getInstance(ComponentListPanel.class);

    public interface SelectableComponent {
        void onComponentSelected(boolean selected);
    }
    public interface WithRootPanel {
        JPanel getRootPanel();
    }

    public enum SelectedPositionDescription {
        NOT_SELECTED(false, false, false),
        ONLY_ONE(false, false, true),
        AT_TOP(false, true, true),
        AT_BOTTOM(true, false, true),
        IN_MIDDLE(true, true, true);

        private final boolean canMoveUp;
        private final boolean canMoveDown;
        private final boolean canRemove;

        SelectedPositionDescription(boolean canMoveUp, boolean canMoveDown, boolean canRemove) {
            this.canMoveUp = canMoveUp;
            this.canMoveDown = canMoveDown;
            this.canRemove = canRemove;
        }

        public boolean canMoveUp() {
            return canMoveUp;
        }

        public boolean canMoveDown() {
            return canMoveDown;
        }

        public boolean canRemove() {
            return canRemove;
        }
    }

    private final Object sync = new Object();
    private volatile int selectedIndex = -1;
    private final List<T> children = new ArrayList<T>();
    private final List<ListSelectionListener> selectionListeners =
            Collections.synchronizedList(new ArrayList<ListSelectionListener>());

    public ComponentListPanel() {
        super(new VerticalFlowLayout());
    }

    @NotNull
    public final List<T> getChildren() {
        synchronized (sync) {
            return new ArrayList<T>(children);
        }
    }

    public final void addSelectionListener(@NotNull ListSelectionListener listener) {
        selectionListeners.add(listener);
    }

    @NotNull
    public final SelectedPositionDescription getSelectedPositionDescription() {
        final int size = getChildrenCount();
        if (size <= 0 || selectedIndex < 0 || selectedIndex >= size) {
            return SelectedPositionDescription.NOT_SELECTED;
        }
        if (size == 1 && selectedIndex == 0) {
            return SelectedPositionDescription.ONLY_ONE;
        }
        if (selectedIndex == 0) {
            return SelectedPositionDescription.AT_TOP;
        }
        if (selectedIndex + 1 >= size) {
            return SelectedPositionDescription.AT_BOTTOM;
        }
        return SelectedPositionDescription.IN_MIDDLE;
    }

    @Nullable
    public final T getChildAt(int index) {
        synchronized (sync) {
            if (index < 0 || index >= children.size()) {
                return null;
            }
            return children.get(index);
        }
    }

    @Nullable
    public final T getSelectedChild() {
        return getChildAt(selectedIndex);
    }

    public final int getSelectedIndex() {
        return selectedIndex;
    }

    public final void addChild(@NotNull final T child) {
        addChildListeners(child);
        synchronized (sync) {
            children.add(child);
            add(child.getRootPanel(), getComponentCount() - 1);
        }
        if (getChildrenCount() == 1) {
            setSelectedChild(child);
        }
        onChildrenChanged();
    }

    /**
     * Adds the child relative to where the currently selected child is.
     *
     */
    public final void addChildRelativeToSelected(@NotNull final T child, boolean before, boolean makeSelected) {
        if (getChildrenCount() <= 0) {
            addChild(child);
            return;
        }
        addChildListeners(child);
        synchronized (sync) {
            final int insertPos;
            if (selectedIndex < 0 || selectedIndex >= children.size()) {
                if (before || children.isEmpty()) {
                    insertPos = 0;
                } else {
                    insertPos = children.size() - 1;
                }
            } else if (before) {
                insertPos = selectedIndex;
            } else if (children.isEmpty()) {
                insertPos = 0;
            } else {
                // just in case
                insertPos = selectedIndex + 1;
            }
            children.add(insertPos, child);
            if (makeSelected) {
                setSelectedChild(child);
            }
        }
        reloadChildren();
    }

    public void moveSelectedChildUp() {
        boolean moved = false;
        synchronized (sync) {
            // Note > 0
            if (selectedIndex > 0 && selectedIndex < children.size()) {
                final T child = children.get(selectedIndex);
                final int swappedIndex = selectedIndex - 1;
                final T swapWith = children.get(swappedIndex);
                children.set(selectedIndex, swapWith);
                children.set(swappedIndex, child);
                selectedIndex = swappedIndex;
                moved = true;
            }
        }
        if (moved) {
            reloadChildren();
        }
    }

    public void moveSelectedChildDown() {
        boolean moved = false;
        synchronized (sync) {
            // Note + 1 <
            if (selectedIndex >= 0 && selectedIndex + 1 < children.size()) {
                final T child = children.get(selectedIndex);
                final int swappedIndex = selectedIndex + 1;
                final T swapWith = children.get(swappedIndex);
                children.set(selectedIndex, swapWith);
                children.set(swappedIndex, child);
                selectedIndex = swappedIndex;
                moved = true;
            }
        }
        if (moved) {
            reloadChildren();
        }
    }

    public void removeSelectedChild() {
        T removedChild = null;
        T newSelectedChild = getSelectedChild();
        synchronized (sync) {
            if (selectedIndex >= 0 && selectedIndex < children.size()) {
                removedChild = children.remove(selectedIndex);
                if (selectedIndex < children.size()) {
                    newSelectedChild = children.get(selectedIndex);
                } else {
                    selectedIndex = -1;
                }
            }
        }
        setSelectedChild(newSelectedChild);
        if (removedChild != null) {
            if (removedChild instanceof SelectableComponent) {
                ((SelectableComponent) removedChild).onComponentSelected(false);
            }
            remove(removedChild.getRootPanel());
            onChildrenChanged();
        }
    }

    public void removeAllChildren() {
        setSelectedChild(null);
        synchronized (sync) {
            children.clear();
        }
        reloadChildren();
    }

    private void reloadChildren() {
        while (getComponentCount() > 0) {
            remove(0);
        }
        for (T child : children) {
            add(child.getRootPanel());
        }
        onChildrenChanged();
    }

    private void onChildrenChanged() {
        revalidate();
        doLayout();
        repaint();
    }


    private void setSelectedChild(@Nullable T child) {
        final int newIndex;
        T deselected = null;
        T selected = null;
        if (child != null) {
            synchronized (sync) {
                newIndex = getChildIndex(child);
                if (newIndex != selectedIndex && newIndex >= 0 && newIndex < children.size()) {
                    selected = child;
                    if (selectedIndex >= 0 && selectedIndex < children.size()) {
                        deselected = children.get(selectedIndex);
                    }
                }
            }
        } else {
            newIndex = -1;
        }
        if (newIndex == selectedIndex) {
            return;
        }
        selectedIndex = newIndex;
        if (selected != null) {
            if (selected instanceof SelectableComponent) {
                ((SelectableComponent) selected).onComponentSelected(true);
            }
        }
        if (deselected != null && deselected != selected && deselected instanceof SelectableComponent) {
            ((SelectableComponent) deselected).onComponentSelected(false);
        }
        ListSelectionEvent event = new ListSelectionEvent(this, newIndex, newIndex, false);
        for (ListSelectionListener selectionListener : selectionListeners) {
            selectionListener.valueChanged(event);
        }
        repaint();
    }

    private int getChildrenCount() {
        synchronized (sync) {
            return children.size();
        }
    }

    private int getChildIndex(@NotNull T child) {
        synchronized (sync) {
            return children.indexOf(child);
        }
    }

    private void addChildListeners(@NotNull final T child) {
        child.getRootPanel().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                setSelectedChild(child);
            }
        });
        List<Container> stack = new ArrayList<Container>();
        final MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setSelectedChild(child);
            }
        };
        stack.add(child.getRootPanel());
        while (! stack.isEmpty()) {
            Container next = stack.remove(0);
            next.addMouseListener(mouseListener);
            for (Component component : next.getComponents()) {
                if (component instanceof Container) {
                    stack.add((Container) component);
                } else {
                    component.addMouseListener(mouseListener);
                }
            }
        }
    }
}
