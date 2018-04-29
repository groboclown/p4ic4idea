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

/**
 * Interfaces and classes that reflect a Perforce stored item, but in terms
 * of the IDE.  These should be translated into concepts that the IDE knows
 * how to deal with.  The implementation of the vcs API should rely on these
 * values being directly used.  This may mean making these abstract when
 * necessary.
 */
package net.groboclown.p4.server.api.values;