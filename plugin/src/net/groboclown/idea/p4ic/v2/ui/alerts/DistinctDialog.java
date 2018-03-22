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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles the display of a dialog, for a single message at a time.
 */
public class DistinctDialog {
    private static final Logger LOG = Logger.getInstance(DistinctDialog.class);

    static final int DIALOG_ALREADY_ACTIVE = -1;
    static final int YES = Messages.YES;
    static final int NO = Messages.NO;

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
         */
        void onChoice(int choice);
    }


    interface AsyncChoiceActor {
        /**
         * Always run when the user makes a choice.  It is never run if the user is not
         * shown the dialog.
         *
         * @param choice >= 0
         * @param onEndHandler use if the ending happens in another thread.
         */
        void onChoice(int choice, @NotNull OnEndHandler onEndHandler);
    }


    public static void showMessageDialog(@Nullable Project project, String message,
            @NotNull
            // @Nls(capitalization = Nls.Capitalization.Title)
            String title, @NotNull NotificationType icon) {
        if (UserProjectPreferences.getShowDialogConnectionMessages(project)) {
            Messages.showMessageDialog(project, message, title, getIcon(icon));
        } else {
            Notification notification = createNotification(P4Vcs.VCS_NAME, title, message, icon, null, null);
            Notifications.Bus.notify(notification, project);
        }
    }


    static void showMessageDialog(@NotNull String dialogKey, @Nullable Project project, String message,
            @NotNull
                    // @Nls(capitalization = Nls.Capitalization.Title)
                    String title, @NotNull NotificationType icon) {
        if (beginAction(dialogKey)) {
            if (UserProjectPreferences.getShowDialogConnectionMessages(project)) {
                try {
                    Messages.showMessageDialog(project, message, title, getIcon(icon));
                } finally {
                    endAction(dialogKey);
                }
            } else {
                OnEndHandlerImpl handler = new OnEndHandlerImpl(dialogKey);
                Notification notification = createNotification(P4Vcs.VCS_NAME, title, message, icon, null, handler);
                Notifications.Bus.notify(notification, project);
            }
        }
    }


    static void performOnYesNoDialog(@NotNull String dialogKey, @Nullable Project project, String message,
            // Note: IntelliJ > 160 annotation
            // @Nls(capitalization = Nls.Capitalization.Title)
            @NotNull
                    String title, @NotNull NotificationType icon, @NotNull ChoiceActor actor) {
        performOnYesNoDialog(dialogKey, project, message, title, icon, new AsyncChoiceActorProxy(actor));
    }


    static void performOnYesNoDialog(@NotNull String dialogKey, @Nullable Project project, String message,
            // Note: IntelliJ > 160 annotation
            // @Nls(capitalization = Nls.Capitalization.Title)
            @NotNull
            String title, @NotNull NotificationType icon, @NotNull AsyncChoiceActor actor) {
        performOnDialog(dialogKey, project, message, title, YES_NO, icon, actor);
    }


    static void performOnDialog(@NotNull String dialogKey, @Nullable Project project, String message,
            // Note: IntelliJ > 160 annotation
            // @Nls(capitalization = Nls.Capitalization.Title)
            @NotNull
            String title,
            @NotNull String[] options, @NotNull NotificationType icon,
            @NotNull ChoiceActor actor) {
        performOnDialog(dialogKey, project, message, title, options, icon, new AsyncChoiceActorProxy(actor));
    }

    static void performOnDialog(@NotNull String dialogKey, @Nullable Project project, String message,
            // Note: IntelliJ > 160 annotation
            // @Nls(capitalization = Nls.Capitalization.Title)
            @NotNull
            String title,
            @NotNull String[] options, @NotNull NotificationType icon,
            @NotNull AsyncChoiceActor actor) {
        if (beginAction(dialogKey)) {
            OnEndHandler handler = new OnEndHandlerImpl(dialogKey);
            if (UserProjectPreferences.getShowDialogConnectionMessages(project)) {
                try {
                    int choice = Messages.showDialog(project, message, title, options, 0, getIcon(icon));
                    actor.onChoice(choice, handler);
                } finally {
                    handler.end();
                }
            } else {
                Question question = new Question(message, options, 0, actor, handler);
                Notification notification = createNotification(dialogKey, title, question.displayMessage, icon,
                        question, handler);
                Notifications.Bus.notify(notification, project);
            }
        }
    }

    @NotNull
    private static Notification createNotification(@NotNull final String dialogKey, @NotNull String title,
            @NotNull String message, @NotNull NotificationType icon, @Nullable NotificationListener question,
            @Nullable final OnEndHandler handler) {
        final String groupId = P4Bundle.getString("notification.groupid");
        Notification ret;
        if (question != null) {
            ret = new Notification(groupId, title, message, icon, question);
        } else {
            ret = new Notification(groupId, title, message, icon);
        }
        ret.whenExpired(new Runnable() {
            @Override
            public void run() {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Notification for " + dialogKey + " expired");
                }
                if (handler != null) {
                    handler.end();
                }
            }
        });
        if (LOG.isDebugEnabled()) {
            LOG.debug("Showing notification for " + dialogKey + ": " + title);
        }

        return ret;
    }


    static String key(@Nullable Object type, Object... args) {
        StringBuilder sb = new StringBuilder();
        if (type != null) {
            sb.append(type.getClass().getName()).append(JOIN);
        }
        for (Object arg: args) {
            sb.append(arg.toString()).append(JOIN);
        }
        return sb.toString();
    }


    @NotNull
    private static Icon getIcon(@NotNull NotificationType notificationType) {
        switch (notificationType) {
            case INFORMATION:
                return Messages.getInformationIcon();
            case WARNING:
                return Messages.getWarningIcon();
            case ERROR:
                return Messages.getErrorIcon();
            default:
                return Messages.getQuestionIcon();
        }
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
        private volatile boolean isActive = true;

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
            if (isActive) {
                if (handledInThread) {
                    if (source == Thread.currentThread()) {
                        isActive = false;
                        endAction(key);
                    }
                } else if (source != Thread.currentThread()) {
                    isActive = false;
                    endAction(key);
                }
            }
        }
    }

    private static class AsyncChoiceActorProxy implements AsyncChoiceActor {
        private final ChoiceActor actor;

        private AsyncChoiceActorProxy(ChoiceActor actor) {
            this.actor = actor;
        }

        @Override
        public void onChoice(int choice, @NotNull OnEndHandler onEndHandler) {
            try {
                actor.onChoice(choice);
            } finally {
                onEndHandler.end();
            }
        }
    }

    // TODO could change this to be like the official IDEA notifications:
    // Have a top summary, and a "Details..." link that shows the real
    // dialog with the question.
    private static class Question implements NotificationListener {
        private final String[] options;
        private final int defaultOptionIndex;
        private final String displayMessage;
        private final AsyncChoiceActor actor;
        private final OnEndHandler endHandler;

        private Question(@NotNull String message, @NotNull String[] options,
                int defaultOptionIndex, @NotNull AsyncChoiceActor actor, @NotNull OnEndHandler endHandler) {
            this.options = options;
            this.defaultOptionIndex = defaultOptionIndex;
            this.actor = actor;
            this.endHandler = endHandler;
            StringBuilder optionMessage = new StringBuilder(message);
            String sep = "<br>";
            for (String option : options) {
                // Strip off the '&' from the option - this is used in the message
                // dialog to make a hotkey for the action.
                String stripped = option.replace("&", "");
                optionMessage
                        .append(sep)
                        .append("<a href=\"http://localhost:99999/")
                        .append(stripped)
                        .append("\">")
                        .append(stripped)
                        .append("</a>");
                sep = "&nbsp;&nbsp;";
            }
            this.displayMessage = optionMessage.toString();
        }

        @Override
        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Notification had hyperlink clicked: " +
                    hyperlinkEvent.getURL() + " -> " + hyperlinkEvent.getDescription());
            }
            URL option = hyperlinkEvent.getURL();
            int choice = defaultOptionIndex;
            if (option != null) {
                String optionChoice = option.getPath();
                while (optionChoice != null && optionChoice.startsWith("/")) {
                    optionChoice = optionChoice.substring(1);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("User clicked option choice `" + optionChoice + "`");
                }
                for (int i = 0; i < options.length; i++) {
                    if (options[i].equals(optionChoice)) {
                        choice = i;
                        break;
                    }
                }
            }
            actor.onChoice(choice, endHandler);
        }
    }
}
