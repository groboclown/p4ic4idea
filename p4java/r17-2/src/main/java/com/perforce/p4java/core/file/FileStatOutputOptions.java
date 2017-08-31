/**
 *
 */
package com.perforce.p4java.core.file;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple way to encapsulate the complex output options available for the
 * IServer getExtendedFiles method. If you're not using that method, this class
 * should probably be ignored, especially given the rather cavalier approach to
 * implementation.
 * <p>
 *
 * NOTE: this class will probably be refactored or replaced in the near-term
 * future.
 * <p>
 *
 * NOTE: no guidance is given here on how to use this class; please refer to the
 * main Perforce fstat documentation for details of each option.
 *
 *
 */

public class FileStatOutputOptions {

        private boolean mappedFiles = false; // -Rc
        private boolean syncedFiles = false; // -Rh
        private boolean openedNotHeadRevFiles = false; // -Rn
        private boolean openedFiles = false; // -Ro
        private boolean openedResolvedFiles = false; // -Rr
        private boolean openedNeedsResolvingFiles = false; // -Ru
        private boolean shelvedFiles = false; // -Rs

        public FileStatOutputOptions() {
        }

        public FileStatOutputOptions(boolean mappedFiles, boolean syncedFiles,
                        boolean openedNotHeadRevFiles, boolean openedFiles,
                        boolean openedResolvedFiles, boolean openedNeedsResolvingFiles) {
                this(mappedFiles, syncedFiles, openedNotHeadRevFiles, openedFiles,
                                openedResolvedFiles, openedNeedsResolvingFiles, false);
        }

        public FileStatOutputOptions(boolean mappedFiles, boolean syncedFiles,
                        boolean openedNotHeadRevFiles, boolean openedFiles,
                        boolean openedResolvedFiles, boolean openedNeedsResolvingFiles,
                        boolean shelvedFiles) {
                this.mappedFiles = mappedFiles;
                this.syncedFiles = syncedFiles;
                this.openedNotHeadRevFiles = openedNotHeadRevFiles;
                this.openedFiles = openedFiles;
                this.openedResolvedFiles = openedResolvedFiles;
                this.openedNeedsResolvingFiles = openedNeedsResolvingFiles;
                this.shelvedFiles = shelvedFiles;
        }

        /**
         * Return a list of strings, one element for each enabled option.
         *
         * @return non-null but possibly-empty list of strings.
         */
        public List<String> toStrings() {
                List<String> retVal = new ArrayList<String>();

                if (this.isMappedFiles()) {
                        retVal.add("-Rc");
                }
                if (this.isSyncedFiles()) {
                        retVal.add("-Rh");
                }
                if (this.isOpenedNotHeadRevFiles()) {
                        retVal.add("-Rn");
                }
                if (this.isOpenedFiles()) {
                        retVal.add("-Ro");
                }
                if (this.isOpenedResolvedFiles()) {
                        retVal.add("-Rr");
                }
                if (this.isOpenedNeedsResolvingFiles()) {
                        retVal.add("-Ru");
                }
                if (this.isShelvedFiles()) {
                        retVal.add("-Rs");
                }

                return retVal;
        }

        public boolean isMappedFiles() {
                return mappedFiles;
        }

        public void setMappedFiles(boolean mappedFiles) {
                this.mappedFiles = mappedFiles;
        }

        public boolean isSyncedFiles() {
                return syncedFiles;
        }

        public void setSyncedFiles(boolean syncedFiles) {
                this.syncedFiles = syncedFiles;
        }

        public boolean isOpenedNotHeadRevFiles() {
                return openedNotHeadRevFiles;
        }

        public void setOpenedNotHeadRevFiles(boolean openedNotHeadRevFiles) {
                this.openedNotHeadRevFiles = openedNotHeadRevFiles;
        }

        public boolean isOpenedFiles() {
                return openedFiles;
        }

        public void setOpenedFiles(boolean openedFiles) {
                this.openedFiles = openedFiles;
        }

        public boolean isOpenedResolvedFiles() {
                return openedResolvedFiles;
        }

        public void setOpenedResolvedFiles(boolean openedResolvedFiles) {
                this.openedResolvedFiles = openedResolvedFiles;
        }

        public boolean isOpenedNeedsResolvingFiles() {
                return openedNeedsResolvingFiles;
        }

        public void setOpenedNeedsResolvingFiles(boolean openedNeedsResolvingFiles) {
                this.openedNeedsResolvingFiles = openedNeedsResolvingFiles;
        }

        /**
         * @return the shelvedFiles
         */
        public boolean isShelvedFiles() {
                return this.shelvedFiles;
        }

        /**
         * @param shelvedFiles
         *            the shelvedFiles to set
         */
        public void setShelvedFiles(boolean shelvedFiles) {
                this.shelvedFiles = shelvedFiles;
        }
}
