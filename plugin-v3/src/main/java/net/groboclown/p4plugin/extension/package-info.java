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
 * What's missing:
 *
 * VcsOutgoingChangesProvider - only needed for DVCS tools, not Perforce.
 * TreeDiffProvider - not needed for Perforce.
 *
 * Interesting tools to use:
 *  AbstractVcsHelperImpl
 *    - Provides lots of useful calls for VCS.
 *    - TransactionRunnable - should we use this?  Probably, for things like
 *    moving files.
 *    - ((AbstractVcsHelperImpl)getInstance(project))
 *    - setCustomExceptionHandler
 *
 * Task.Backgroundable + CoreProgressManager.runProcessWithProgressAsynchronously
 *      + BackgroundableProcessIndicator, and variations therein.
 */
package net.groboclown.p4plugin.extension;