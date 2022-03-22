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

package net.groboclown.p4plugin.messages;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.extension.P4Vcs;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TODO see if this should use VcsNotifier instead.
 */
public class UserMessage {
    public static final int VERBOSE = UserProjectPreferences.USER_MESSAGE_LEVEL_VERBOSE;
    public static final int INFO = UserProjectPreferences.USER_MESSAGE_LEVEL_INFO;
    public static final int WARNING = UserProjectPreferences.USER_MESSAGE_LEVEL_WARNING;
    public static final int ERROR = UserProjectPreferences.USER_MESSAGE_LEVEL_ERROR;
    public static final int ALWAYS = UserProjectPreferences.USER_MESSAGE_LEVEL_ALWAYS;

    /**
     * Shows a notification.  Can be invoked from any thread.
     *
     * @param project source project
     * @param message message to display to the user
     * @param title notification title
     * @param icon notification icon
     */
    public static void showNotification(@Nullable Project project, int level,
            @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String message,
            @NotNull @Nls(capitalization = Nls.Capitalization.Title) String title,
            @NotNull NotificationType icon) {
        if (UserProjectPreferences.isUserMessageLevel(project, level)) {
            Notification notification = createNotification(P4Vcs.VCS_NAME, title, message, icon, null, null);
            Notifications.Bus.notify(notification, project);
        }
    }

    public static void showNotification(@Nullable Project project, int level,
            @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String message,
            @NotNull @Nls(capitalization = Nls.Capitalization.Title) String title,
            @NotNull NotificationType icon, @Nullable NotificationListener question,
            @Nullable Runnable onMessageExpired) {
        if (UserProjectPreferences.isUserMessageLevel(project, level)) {
            Notification notification = createNotification(P4Vcs.VCS_NAME, title, message, icon, question, onMessageExpired);
            Notifications.Bus.notify(notification, project);
        }
    }

    @NotNull
    private static Notification createNotification(@NotNull final String dialogKey, @NotNull String title,
            @NotNull String message, @NotNull NotificationType icon, @Nullable NotificationListener question,
            @Nullable Runnable onMessageExpired) {
        final String groupId = P4Bundle.getString("notification.groupid");
        Notification ret = new Notification(groupId, title, message, icon);
        if (question != null) {
            ret.setListener(question);
        }
        if (onMessageExpired != null) {
            ret.whenExpired(onMessageExpired);
        }

        return ret;
    }
}
