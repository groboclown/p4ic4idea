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

package net.groboclown.p4.server.impl.config.part;

/**
 * From the "p4 set" Perforce documentation:
 * <blockquote>
 *     On OS X, without this option, p4 set sets the variables in your <tt>com.perforce.environment</tt> property
 *     list in the <tt>~/Library/Preferences</tt> folder; when you use the -s option (and have administrative
 *     privileges), the variables are set in the system <tt>/Library/Preferences</tt> folder.
 * </blockquote>
 * The file will be named <tt>~/Library/Preferences/com.perforce.environment.plist</tt>.
 * <p>
 *
 * </p>
 */
public class OsXPreferencesPart {
}
