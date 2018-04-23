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
 * All the exceptions used by the plugin.
 * <p>
 * These should be precise enough to indicate the source of the error.
 * <p>
 * They should not be directly printed to the user; instead, they should be passed through the
 * UserExceptionMessageHandler.
 * <p>
 * In general exception throwing should be avoided wherever possible.  Instead, error handling
 * should be dealt with in actions.  They should be limited to reporting up issues to the
 * Idea calling interface, or should be dealt with as early as possible when encountered from
 * the Perforce server.
 */
package net.groboclown.p4.server.api.exceptions;