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

import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;

/** The scrolling outer panel can cause the inner tabs to get sized all wrong,
 * because of the scrollpanes in scrollpane.
 * This helps keep the tabs sized right so we essentially ignore the outer scroll pane.
 */
public class WrapperPanel
        extends JPanel
        implements Scrollable {
    private final JPanel wrapped;
    private final boolean adjustHeight;
    private final boolean allowHorizontalScrolling;
    private Dimension size;

    public WrapperPanel(JPanel wrapped) {
        this(wrapped, false, false);
    }

    public WrapperPanel(JPanel wrapped, boolean adjustHeight, boolean allowHorizontalScrolling) {
        this.wrapped = wrapped;
        this.adjustHeight = adjustHeight;
        this.allowHorizontalScrolling = allowHorizontalScrolling;

        setLayout(new BorderLayout());
        add(wrapped, BorderLayout.CENTER);

        updateSize();

        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                updateSize();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                updateSize();
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                updateSize();
            }
        });

        addHierarchyBoundsListener(new HierarchyBoundsListener() {
            @Override
            public void ancestorMoved(HierarchyEvent e) {
                updateSize();
            }

            @Override
            public void ancestorResized(HierarchyEvent e) {
                updateSize();
            }
        });
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return size;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 0;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 0;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private void updateSize() {
        final Dimension prevSize = this.size;
        Dimension size = wrapped.getPreferredSize();
        Container sizer = getParent();
        // Search for a scroll pane parent.
        while (sizer != null && !(sizer instanceof JScrollPane)) {
            sizer = sizer.getParent();
        }
        if (sizer != null) {
            JScrollPane scroll = (JScrollPane) sizer;
            int margin = JBUI.scale(10); // TODO adjust this to a better size.
            if (scroll.getVerticalScrollBar() != null) {
                margin += scroll.getVerticalScrollBar().getWidth();
            }
            Dimension prefSize = sizer.getPreferredSize();
            if (adjustHeight) {
                size = new Dimension(prefSize.width - margin, prefSize.height);
            } else {
                size = new Dimension(prefSize.width - margin, size.height);
            }
            if (!allowHorizontalScrolling) {
                scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            }
        }
        if (!size.equals(prevSize)) {
            this.size = size;
            setPreferredSize(size);
            wrapped.revalidate();
            wrapped.doLayout();
            wrapped.repaint();
        }
    }
}
