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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.util.ui.UIUtil;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A JPanel that has value panels layered vertically
 * that can be moved and removed in their order.
 */
public class ComponentListPanel<T extends ComponentListPanel.WithRootPanel> extends JPanel {

    public interface WithRootPanel {
        @NotNull
        @Nls
        String getTitle();

        @NotNull
        JPanel getRootPanel();
    }
    public interface ChildListChanged<C extends WithRootPanel> {
        void onChildListChanged(Collection<C> childList);
    }

    private final Object sync = new Object();
    private final List<ChildWrapper> children = new ArrayList<ChildWrapper>();
    private final List<ChildListChanged<T>> childChangeListeners = new ArrayList<ChildListChanged<T>>();

    public ComponentListPanel() {
        super(new VerticalFlowLayout());
    }

    public void addChildListChangeListener(@NotNull ChildListChanged<T> listener) {
        childChangeListeners.add(listener);
    }

    @NotNull
    public final List<T> getChildren() {
        final List<T> ret;
        synchronized (sync) {
            ret = new ArrayList<T>(children.size());
            for (ChildWrapper child : children) {
                ret.add(child.value);
            }
        }
        return ret;
    }

    public final void addChildAt(int index, @NotNull final T child) {
        if (index < 0) {
            index = 0;
        }
        synchronized (sync) {
            int size = children.size();
            if (index > size) {
                index = size;
            }
            final ChildWrapper wrapper = createChildWrapper(child);
            children.add(index, wrapper);
        }
        fireChildListChanged();
        reloadChildren();
    }

    public void removeAllChildren() {
        synchronized (sync) {
            children.clear();
        }
        fireChildListChanged();
        reloadChildren();
    }

    private void fireChildListChanged() {
        final Collection<T> childList = Collections.unmodifiableCollection(getChildren());
        for (ChildListChanged<T> childChangeListener : childChangeListeners) {
            childChangeListener.onChildListChanged(childList);
        }
    }

    private void reloadChildren() {
        removeAll();
        synchronized (sync) {
            final int size = children.size();
            for (int i = 0; i < size; i ++) {
                final ChildWrapper child = children.get(i);
                child.setPosition(i, size);
                add(child);
            }
        }
        onChildrenChanged();
    }

    private void onChildrenChanged() {
        revalidate();
        doLayout();
        repaint();
    }


    private void moveChild(@NotNull final ChildWrapper child, final int change) {
        final int origPos = child.position;
        final boolean swapped;
        synchronized (sync) {
            final int size = children.size();
            if (child.position < 0 || child.position >= size || child != children.get(child.position)) {
                throw new IllegalStateException("Child at " + child.position + " is invalid");
            }
            final int newPos = child.position + change;
            if (newPos >= 0 && newPos < size) {
                // we can swap
                final ChildWrapper swap = children.get(newPos);
                children.set(newPos, child);
                children.set(origPos, swap);
                // "reload children" will set the position for us
                swapped = true;
            } else {
                swapped = false;
            }
        }

        if (swapped) {
            fireChildListChanged();
            reloadChildren();
        }
    }

    private void removeChild(@NotNull final ChildWrapper child) {
        synchronized (sync) {
            final int size = children.size();
            if (child.position < 0 || child.position >= size || child != children.get(child.position)) {
                throw new IllegalStateException("Child at " + child.position + " is invalid");
            }
            children.remove(child.position);
        }
        fireChildListChanged();
        reloadChildren();
    }

    private ChildWrapper createChildWrapper(@NotNull final T child) {
        ChildWrapper panel = new ChildWrapper(child);
        return panel;
    }

    private class ChildWrapper extends JPanel {
        int position = -1;
        final T value;
        private final JButton removeButton;
        private final JButton moveUpButton;
        private final JButton moveDownButton;

        private ChildWrapper(@NotNull final T value) {
            super(new BorderLayout());
            this.value = value;

            JPanel titlePanel = new JPanel(new BorderLayout());

            JLabel title = new JLabel(value.getTitle());
            title.setFont(UIUtil.getTitledBorderFont());
            titlePanel.add(title, BorderLayout.WEST);

            JPanel buttons = new JPanel(new FlowLayout());
            removeButton = new JButton(AllIcons.General.Remove);
            removeButton.setToolTipText(P4Bundle.getString("configuration.stack.remove"));
            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removeChild(ChildWrapper.this);
                }
            });
            buttons.add(removeButton);
            moveUpButton = new JButton(AllIcons.Actions.MoveUp);
            moveUpButton.setToolTipText(P4Bundle.getString("configuration.stack.move-up"));
            moveUpButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveChild(ChildWrapper.this, -1);
                }
            });
            buttons.add(moveUpButton);
            moveDownButton = new JButton(AllIcons.Actions.MoveDown);
            moveDownButton.setToolTipText(P4Bundle.getString("configuration.stack.move-down"));
            moveDownButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveChild(ChildWrapper.this, 1);
                }
            });
            buttons.add(moveDownButton);
            titlePanel.add(buttons, BorderLayout.EAST);

            add(titlePanel, BorderLayout.NORTH);
            add(value.getRootPanel(), BorderLayout.CENTER);
            setBorder(BorderFactory.createLineBorder(UIUtil.getTreeSelectionBorderColor()));
        }

        void setPosition(int pos, int count) {
            this.position = pos;
            moveUpButton.setEnabled(pos > 0);
            moveDownButton.setEnabled(pos + 1 < count);
        }
    }
}
