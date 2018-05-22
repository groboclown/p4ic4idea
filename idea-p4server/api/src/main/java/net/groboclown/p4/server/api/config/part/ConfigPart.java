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

package net.groboclown.p4.server.api.config.part;

import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ConfigProblem;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;

/**
 * ConfigPart classes can be mutable or immutable.  If they support reloading
 * the values, then it's expected that any value in the class can be changed.
 * With the {@link MultipleConfigPart} sub-class, many different ConfigPart
 * instances can be used together.
 */
public interface ConfigPart {
    @NotNull
    @Nls(capitalization = Nls.Capitalization.Title)
    String getSourceName();

    /**
     *
     * @return true if {@link #getServerName()} returns non-null
     */
    boolean hasServerNameSet();

    /**
     *
     * @return non-null if {@link #hasServerNameSet()} returns true.
     */
    @Nullable
    P4ServerName getServerName();

    /**
     *
     * @return true if {@link #getClientname()} returns non-null
     */
    boolean hasClientnameSet();

    @Nullable
    String getClientname();

    /**
     *
     * @return true if {@link #getUsername()} returns non-null
     */
    boolean hasUsernameSet();

    @Nullable
    String getUsername();

    /**
     *
     * @return true if {@link #getPlaintextPassword()} returns non-null
     */
    boolean hasPasswordSet();

    /**
     * Returns the plaintext password that might have been set.  If the user
     * has a blank password, then this must return an empty string.
     *
     * @return the plaintext password, or null if not set.
     */
    @Nullable
    String getPlaintextPassword();

    /**
     *
     * @return true if, regardless of password storage settings, the user requests
     *      that any connection must use a user-typed password.  The password can
     *      be cached by the IDE through its password storage mechanisms.
     */
    boolean requiresUserEnteredPassword();

    /**
     *
     * @return true if the user specified a location, regardless of whether the file
     *      exists or is readable.
     */
    boolean hasAuthTicketFileSet();

    /**
     * The specification for the authentication ticket file.  This must match what the
     * user specified, even if the file doesn't exist, or isn't readable.
     *
     * @return the authentication ticket file, whether the file exists or not, or is readable or not.
     */
    @Nullable
    File getAuthTicketFile();


    /**
     *
     * @return true if the user specified a location, regardless of whether the file
     *      exists or is readable.
     */
    boolean hasTrustTicketFileSet();

    /**
     * The specification for the trust ticket file.  This must match what the
     * user specified, even if the file doesn't exist, or isn't readable.
     *
     * @return the trust ticket file, whether the file exists or not, or is readable or not.
     */
    @Nullable
    File getTrustTicketFile();

    boolean hasServerFingerprintSet();

    @Nullable
    String getServerFingerprint();

    /**
     *
     * @return true if {@link #getClientHostname()} returns non-null
     */
    boolean hasClientHostnameSet();

    /**
     * Allow for custom setting the client hostname.
     *
     * @return hostname of the client.
     */
    @Nullable
    String getClientHostname();

    /**
     *
     * @return true if {@link #getIgnoreFileName()} returns non-null
     */
    boolean hasIgnoreFileNameSet();

    @Nullable
    String getIgnoreFileName();

    /**
     *
     * @return true if {@link #getDefaultCharset()} returns non-null
     */
    boolean hasDefaultCharsetSet();

    @Nullable
    String getDefaultCharset();

    /**
     *
     * @return true if {@link #getLoginSso()} returns non-null
     */
    boolean hasLoginSsoSet();

    /**
     *
     * @return the login SSO executable command, or null if not set.
     */
    @Nullable
    String getLoginSso();

    /**
     * Attempts to reload the values from the source.  If there were problems loading, then
     * the method should return <tt>false</tt>, and {@link #getConfigProblems()} should contain
     * the encountered issue.
     *
     * @return true if the reload didn't encounter issues, or false if there were problems.
     */
    boolean reload();

    /**
     * FIXME this should take a resource bundle or some custom class that can perform message translation.
     *
     * @return all discovered configuration problems
     */
    @NotNull
    Collection<ConfigProblem> getConfigProblems();

    /**
     *
     * @return true if the {@link #getConfigProblems()} contains a problem marked as
     *      error.
     */
    boolean hasError();
}
