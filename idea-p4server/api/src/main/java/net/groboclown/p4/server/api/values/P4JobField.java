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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public interface P4JobField {
    int JOB_ID_CODE = 101;
    int JOB_STATUS_CODE = 102;
    int USER_CODE = 103;
    int DATE_CREATED_CODE = 104;
    int DESCRIPTION_CODE = 105;

    String PRESET_USER = "$user";
    String PRESET_NOW = "$now";
    String PRESET_BLANK = "$blank";

    enum DataType {
        /** a single word (any value) */
        WORD {
            @Override
            public Object convert(Object fieldValue) {
                if (fieldValue == null) {
                    return null;
                }
                return fieldValue.toString();
            }
        },

        /** a date/time field */
        DATE {
            @Override
            public Object convert(Object fieldValue) {
                if (fieldValue == null) {
                    return null;
                }
                if (fieldValue instanceof Date) {
                    return fieldValue;
                }
                String sv = fieldValue.toString();
                if (PRESET_NOW.equals(sv)) {
                    return new Date();
                }
                // SimpleDateFormat is not thread safe, so we can't use a constant here.
                return new SimpleDateFormat("yyyy/MM/dd kk:mm:ss").format(fieldValue.toString());
            }
        },

        /** one of a set of words */
        SELECT {
            @Override
            public Object convert(Object fieldValue) {
                if (fieldValue == null) {
                    return null;
                }
                return fieldValue.toString();
            }
        },

        /** a one-liner */
        LINE {
            @Override
            public Object convert(Object fieldValue) {
                if (fieldValue == null) {
                    return null;
                }
                return fieldValue.toString();
            }
        },
        TEXT {
            @Override
            public Object convert(Object fieldValue) {
                if (fieldValue == null) {
                    return null;
                }
                return fieldValue.toString();
            }
        }, // a block of text
        BULK {
            @Override
            public Object convert(Object fieldValue) {
                if (fieldValue == null) {
                    return null;
                }
                return fieldValue.toString();
            }
        }, //text not indexed for 'p4 jobs -e'

        /**
         * another type; usually indicates that the server
         * is running a future, unsupported version.
         */
        UNKNOWN {
            @Override
            public Object convert(Object fieldValue) {
                return fieldValue;
            }
        };

        public abstract Object convert(Object fieldValue);
    }

    enum FieldType {
        /** no default, and not required to be present */
        OPTIONAL,

        /** default provided, still not required */
        DEFAULT,

        /** default provided, value must be present */
        REQUIRED,

        /** set once to the default and never changed */
        ONCE,

        /**
         * always set to the default when saving the form, adding or deleting fixes with 'p4 fix'
         * or 'p4 changelist', or submitting a change associated with the job with 'p4 submit'.
         */
        ALWAYS,

        /**
         * another type; usually indicates that the server
         * is running a future, unsupported version.
         */
        UNKNOWN
    }

    /**
     *
     * @return a value between 101 and 199.  101 to 105 are reserved.
     */
    int getCode();

    /**
     *
     * @return the name of the field
     */
    String getName();

    /**
     *
     * @return the data type as an enum.
     */
    DataType getDataType();

    /**
     *
     * @return the raw text of the data type; useful for the "UNKNOWN" type
     */
    String getRawDataType();

    /**
     *
     * @return recommended character length of a display box for the field.  If 0, a text box is assumed.
     */
    int getLength();

    /**
     * indicates how to handle the setting of the field.
     *
     * @return how the field should be handled.
     */
    FieldType getFieldType();

    String getRawFieldType();

    /**
     * Default value for the field if its 'setting' flag is other than 'optional'.
     * <p>
     * The following special defaults are recognized:
     * <ul>
     *     <li><tt>$user</tt>: the user entering the job</li>
     *     <li><tt>$now</tt>: the current date</li>
     *     <li><tt>$blank</tt>: the words '<enter description here>'</li>
     * </ul>
     * The Preset for the job status field (code 102) has
     * a special syntax for providing a default fix status
     * for 'p4 fix' and 'p4 change' along with the default
     * status for new jobs: <tt>jobStatus,fix/fixStatus</tt>
     * Otherwise the fixStatus is hardwired to 'closed'.
     *
     * @return preset for the field
     */
    String getPreset();


    List<String> getSelectValues();
}
