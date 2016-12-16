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

package net.groboclown.idea.p4ic.v2.ui.alerts;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles the display of a dialog, for a single message at a time.
 */
class DistinctDialog {
    private static final Logger LOG = Logger.getInstance(LoginFailedHandler.class);

    static final int DIALOG_ALREADY_ACTIVE = -1;
    static final int YES = Messages.YES;
    static final int NO = Messages.NO;
    static final int CANCEL = Messages.CANCEL;
    static final String CANCEL_BUTTON = Messages.CANCEL_BUTTON;

    private static final char JOIN = (char) 127;
    private static final Set<String> ACTIVE_DIALOGS = Collections.synchronizedSet(new HashSet<String>());
    private static final String[] YES_NO = { Messages.YES_BUTTON, Messages.NO_BUTTON };

    interface OnEndHandler {
        void handleInOtherThread();
        void end();
    }


    interface ChoiceActor {
        /**
         * Always run when the user makes a choice.  It is never run if the user is not
         * shown the dialog.
         *
         * @param choice >= 0
         * @param onEndHandler use if the ending happens in another thread.
         */
        void onChoice(int choice, @NotNull OnEndHandler onEndHandler);
    }


    static int showYesNoDialog(@NotNull String dialogKey, @Nullable Project project, String message,
            // Note: IntelliJ > 160 annotation
            // @Nls(capitalization = Nls.Capitalization.Title)
            @NotNull
            String title, @Nullable Icon icon) {
        return showDialog(dialogKey, project, message, title, YES_NO, icon);
    }


    /**
     *
     * @param dialogKey defines the dialog in such a way that multiple versions of the dialog are not shown.
     * @param project project
     * @param message user message
     * @param title dialog title
     * @param options user choice
     * @param icon accompanying icon
     * @return >= 0 means the choice that the user selected, or -1 if the dialog key is already being
     *      processed.
     */
    static int showDialog(@NotNull String dialogKey, @Nullable Project project, String message,
            // Note: IntelliJ > 160 annotation
            // @Nls(capitalization = Nls.Capitalization.Title)
            @NotNull
            String title,
            @NotNull String[] options, @Nullable Icon icon) {
        if (beginAction(dialogKey)) {
            try {
                return Messages.showDialog(project, message, title, options, 0, icon);
            } finally {
                endAction(dialogKey);
            }
        } else {
            LOG.info("Already handled dialog for [" + message + "] " + dialogKey);
            return DIALOG_ALREADY_ACTIVE;
        }
    }


    static void performOnDialog(@NotNull String dialogKey, @Nullable Project project, String message,
            // Note: IntelliJ > 160 annotation
            // @Nls(capitalization = Nls.Capitalization.Title)
            @NotNull
            String title,
            @NotNull String[] options, @Nullable Icon icon,
            @NotNull ChoiceActor actor)
    {
        if (beginAction(dialogKey)) {
            OnEndHandler handler = new OnEndHandlerImpl(dialogKey);
            try {
                int choice = Messages.showDialog(project, message, title, options, 0, icon);
                actor.onChoice(choice, handler);
            } finally {
                handler.end();
            }
        }
    }


    static String key(@Nullable Object type, String... args) {
        StringBuilder sb = new StringBuilder();
        if (type != null) {
            sb.append(type.getClass().getName()).append(JOIN);
        }
        for (String arg: args) {
            sb.append(arg).append(JOIN);
        }
        return sb.toString();
    }


    private static boolean beginAction(@NotNull String key) {
        return ACTIVE_DIALOGS.add(key);
    }

    private static void endAction(@NotNull String key) {
        ACTIVE_DIALOGS.remove(key);
    }


    private static class OnEndHandlerImpl implements OnEndHandler {
        private final String key;
        private final Thread source;
        boolean handledInThread = false;

        private OnEndHandlerImpl(String key) {
            this.key = key;
            this.source = Thread.currentThread();
        }

        @Override
        public void handleInOtherThread() {
            handledInThread = true;
        }

        @Override
        public void end() {
            if (handledInThread) {
                if (source == Thread.currentThread()) {
                    endAction(key);
                }
            } else if (source != Thread.currentThread()) {
                endAction(key);
            }
        }
    }

}
