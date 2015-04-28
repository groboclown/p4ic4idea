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

package net.groboclown.idea.p4ic.compat;

public class IncompatibleApiVersionException extends IllegalStateException {
    public IncompatibleApiVersionException(String apiVersion) {
        // TODO make internationalized
        // Can't use P4Bundle, because it's in a child project.
        super("IDE version " + apiVersion + " not compatible with the P4 plugin");
    }


    public IncompatibleApiVersionException(String firstVersion, String secondVersion, Exception source) {
        super("Invalid IDEA version number (" + firstVersion + " vs " + secondVersion + ")", source);
    }
}
