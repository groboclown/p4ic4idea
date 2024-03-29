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
 * The IDE api + the plugin design implies that this is stored per project.  That means that
 * the state restore action triggers cache updates.  The big thing to remember with this
 * storage is that there MUST be a single model of objects for the cache objects to pull
 * from.
 *
 * For now, this is highly un-optimized.
 */
package net.groboclown.p4.server.impl.cache.store;