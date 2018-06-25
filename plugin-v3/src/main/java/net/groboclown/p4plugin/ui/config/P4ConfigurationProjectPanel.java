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
package net.groboclown.p4plugin.ui.config;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;

public class P4ConfigurationProjectPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(P4ConfigurationProjectPanel.class);

    private final Project project;
    private UserPreferencesPanel myMainPanel;
    private WrapperPanel wrappedPanel;
    private volatile boolean isInitialized = false;

    P4ConfigurationProjectPanel(@NotNull Project project) {
        this.project = project;
    }

    synchronized boolean isModified(@NotNull UserProjectPreferences preferences) {
        return isInitialized && myMainPanel.isModified(preferences);
    }

    synchronized void saveSettings(@NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            // nothing to do
            return;
        }
        myMainPanel.saveSettingsToConfig(preferences);
    }

    synchronized void loadSettings(@NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            getPanel(preferences);
            return;
        }

        LOG.debug("Loading settings into the main panel");
        myMainPanel.loadSettingsIntoGUI(preferences);
    }

    synchronized JPanel getPanel(@NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            LOG.debug("Creating settings panel");
            myMainPanel = new UserPreferencesPanel();
            isInitialized = true;
            wrappedPanel = new WrapperPanel(myMainPanel.getRootPanel());
        }
        loadSettings(preferences);
        return wrappedPanel;
    }

    public synchronized void dispose() {
        // TODO is there a dispose to call on this panel?
        //if (myMainPanel != null) {
        //    Disposer.dispose(myMainPanel);
        //}
        myMainPanel = null;
        wrappedPanel = null;
        isInitialized = false;
    }


    // The scrolling outer panel can cause the inner tabs to get sized all wrong,
    // because of the scrollpanes in scrollpane.
    // This helps keep the tabs sized right so we essentially ignore the outer scroll pane.
    private class WrapperPanel extends JPanel implements Scrollable {
        private final JPanel wrapped;
        private Dimension size;

        private WrapperPanel(JPanel wrapped) {
            this.wrapped = wrapped;

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
            final Container parent = getParent();
            if (parent != null) {
                size = new Dimension(parent.getPreferredSize());
            } else {
                size = new Dimension(wrapped.getPreferredSize());
            }
            if (! size.equals(prevSize)) {
                setPreferredSize(size);
            }
        }
    }
}
