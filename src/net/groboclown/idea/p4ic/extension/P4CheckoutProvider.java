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
package net.groboclown.idea.p4ic.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class P4CheckoutProvider implements CheckoutProvider {
    private static final Logger LOG = Logger.getInstance(P4EditFileProvider.class);

    @Override
    public void doCheckout(@NotNull Project project, @Nullable Listener listener) {
        FileDocumentManager.getInstance().saveAllDocuments();

        P4Vcs vcs = P4Vcs.getInstance(project);

        LOG.warn("No implementation for checkout yet");
    }

    @Override
    public String getVcsName() {
        return P4Vcs.VCS_NAME;
    }
}
