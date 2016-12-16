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

package net.groboclown.idea.p4ic.compat.idea150;

import net.groboclown.idea.p4ic.compat.*;
import org.jetbrains.annotations.NotNull;

public class CompatManager150 extends CompatManager {
    private final UICompat150 uiCompat = new UICompat150();
    private final VcsCompat150 vcsCompat = new VcsCompat150();
    private final HistoryCompat150 historyCompat = new HistoryCompat150();
    private final AuthenticationCompat150 authCompat = new AuthenticationCompat150();

    @NotNull
    @Override
    public UICompat getUICompat() {
        return uiCompat;
    }

    @NotNull
    @Override
    public VcsCompat getVcsCompat() {
        return vcsCompat;
    }

    @NotNull
    @Override
    public HistoryCompat getHistoryCompat() {
        return historyCompat;
    }

    @NotNull
    @Override
    public AuthenticationCompat getAuthenticationCompat() {
        return authCompat;
    }
}
