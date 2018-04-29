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

package net.groboclown.p4plugin.extension;

import com.intellij.ide.errorTreeView.HotfixData;
import com.intellij.openapi.vcs.ActionType;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsExceptionsHotFixer;

import java.util.List;
import java.util.Map;

public class P4ExceptionsHotFixer implements VcsExceptionsHotFixer {
    @Override
    public Map<HotfixData, List<VcsException>> groupExceptions(ActionType actionType, List<VcsException> list) {
        return null;
    }
}
