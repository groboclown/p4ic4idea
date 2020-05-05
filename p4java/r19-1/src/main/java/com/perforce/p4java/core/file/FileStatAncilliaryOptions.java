/**
 *
 */
package com.perforce.p4java.core.file;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple way to encapsulate the complex ancilliary output options available
 * for the IServer getExtendedFiles method.<p>
 *
 * NOTE: no guidance is given here on how to use this class; please refer to the
 * main Perforce fstat documentation for details of each option.
 */

public class FileStatAncilliaryOptions {

        private boolean allRevs = false;				// -Of
        private boolean fileSizeDigest = false;			// -Ol
        private boolean bothPathTypes = false;			// -Op
        private boolean pendingIntegrationRecs = false;	// -Or
        private boolean excludeLocalPath = false;		// -Os
        private boolean showAttributes = false;			// -Oa
        private boolean showHexAttributes = false;		// -Oae

        public FileStatAncilliaryOptions() {
        }

        public FileStatAncilliaryOptions(boolean allRevs,
                        boolean fileSizeDigest, boolean bothPathTypes,
                        boolean pendingIntegrationRecs, boolean excludeLocalPath) {
                this.allRevs = allRevs;
                this.fileSizeDigest = fileSizeDigest;
                this.bothPathTypes = bothPathTypes;
                this.pendingIntegrationRecs = pendingIntegrationRecs;
                this.excludeLocalPath = excludeLocalPath;
        }

        /**
         * Return a list of strings, one element for each enabled option.
         *
         * @return non-null but possibly-empty list of strings.
         */
        public List<String> toStrings() {
                List<String> retVal = new ArrayList<String>();

                if (this.isAllRevs()) {
                        retVal.add("-Of");
                }
                if (this.isFileSizeDigest()) {
                        retVal.add("-Ol");
                }
                if (this.isBothPathTypes()) {
                        retVal.add("-Op");
                }
                if (this.isPendingIntegrationRecs()) {
                        retVal.add("-Or");
                }
                if (this.isExcludeLocalPath()) {
                        retVal.add("-Os");
                }
                if (this.isShowHexAttributes()) {
                        retVal.add("-Oae");
                } else if (this.isShowAttributes()) {
                        retVal.add("-Oa");
                }

                return retVal;
        }

        public boolean isAllRevs() {
                return allRevs;
        }
        public void setAllRevs(boolean allRevs) {
                this.allRevs = allRevs;
        }
        public boolean isFileSizeDigest() {
                return fileSizeDigest;
        }
        public void setFileSizeDigest(boolean fileSizeDigest) {
                this.fileSizeDigest = fileSizeDigest;
        }
        public boolean isBothPathTypes() {
                return bothPathTypes;
        }
        public void setBothPathTypes(boolean bothPathTypes) {
                this.bothPathTypes = bothPathTypes;
        }
        public boolean isPendingIntegrationRecs() {
                return pendingIntegrationRecs;
        }
        public void setPendingIntegrationRecs(boolean pendingIntegrationRecs) {
                this.pendingIntegrationRecs = pendingIntegrationRecs;
        }
        public boolean isExcludeLocalPath() {
                return excludeLocalPath;
        }
        public void setExcludeLocalPath(boolean excludeLocalPath) {
                this.excludeLocalPath = excludeLocalPath;
        }

        /**
         * @since 2011.1
         */
        public boolean isShowAttributes() {
                return showAttributes;
        }
        /**
         * @since 2011.1
         */
        public void setShowAttributes(boolean showAttributes) {
                this.showAttributes = showAttributes;
        }
        /**
         * @since 2011.1
         */
        public boolean isShowHexAttributes() {
                return showHexAttributes;
        }
        /**
         * @since 2011.1
         */
        public void setShowHexAttributes(boolean showHexAttributes) {
                this.showHexAttributes = showHexAttributes;
        }
}
