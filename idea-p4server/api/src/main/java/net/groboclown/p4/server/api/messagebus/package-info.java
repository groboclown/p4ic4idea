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
 * Basic communication between components.  Requests are run through the message bus, rather than directly
 * through an API.
 * <p>
 * All messages must be one of {@link net.groboclown.p4.server.api.messagebus.ApplicationMessage}
 * or {@link net.groboclown.p4.server.api.messagebus.ProjectMessage}, depending upon the
 * scope of the messages.
 */
package net.groboclown.p4.server.api.messagebus;