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

/**
 * Classifications of changes that can be made to changelists.  These do not directly relate to any
 * type in the p4java api, but are useful for storing state change information.
 */
public enum P4ChangelistAction {
    CREATE,
    DELETE,
    EDIT_DESCRIPTION,
    ADD_JOB,
    REMOVE_JOB,
    ADD_FILE,
    REMOVE_FILE,
    SHELVE_FILE,
    DELETE_SHELVED_FILE,
    SUBMIT
}
