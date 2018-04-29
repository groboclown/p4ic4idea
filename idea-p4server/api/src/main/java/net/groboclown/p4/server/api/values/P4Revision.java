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

package net.groboclown.p4.server.api.values;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;

public class P4Revision extends VcsRevisionNumber.Int {
    public static final P4Revision NOT_ON_SERVER = new P4Revision(-1);

    public P4Revision(int value) {
        super(value);
    }
}
