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

package net.groboclown.p4.server.impl.values;

import com.perforce.p4java.core.IJobSpec;
import net.groboclown.p4.server.api.values.P4JobField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class P4JobFieldImpl implements P4JobField {
    private final int code;
    private final String name;
    private final DataType dataType;
    private final String rawDataType;
    private final int length;
    private final FieldType fieldType;
    private final String rawFieldType;
    private final String preset;
    private final List<String> selectValues;

    P4JobFieldImpl(IJobSpec.IJobSpecField field, IJobSpec spec) {
        code = field.getCode();
        name = field.getName();
        rawDataType = field.getDataType();
        length = field.getLength();
        rawFieldType = field.getFieldType();
        preset = spec.getFieldPreset(name);
        List<String> selects = spec.getValues().get(name);
        if (selects == null) {
            selectValues = Collections.emptyList();
        } else {
            selectValues = Collections.unmodifiableList(new ArrayList<>(selects));
        }

        DataType dt;
        try {
            dt = DataType.valueOf(rawDataType.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            dt = DataType.UNKNOWN;
        }
        dataType = dt;

        FieldType ft;
        try {
            ft = FieldType.valueOf(rawFieldType.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            ft = FieldType.UNKNOWN;
        }
        fieldType = ft;
    }

    public P4JobFieldImpl(int code, String name, String rawDataType, int length, String rawFieldType, String preset,
            @NotNull List<String> selectValues) {
        this.code = code;
        this.name = name;
        this.rawDataType = rawDataType;
        this.length = length;
        this.rawFieldType = rawFieldType;
        this.preset = preset;
        this.selectValues = Collections.unmodifiableList(new ArrayList<>(selectValues));

        DataType dt;
        try {
            dt = DataType.valueOf(rawDataType.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            dt = DataType.UNKNOWN;
        }
        dataType = dt;

        FieldType ft;
        try {
            ft = FieldType.valueOf(rawFieldType.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            ft = FieldType.UNKNOWN;
        }
        fieldType = ft;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public String getRawDataType() {
        return rawDataType;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public FieldType getFieldType() {
        return fieldType;
    }

    @Override
    public String getRawFieldType() {
        return rawFieldType;
    }

    @Override
    public String getPreset() {
        return preset;
    }

    @Override
    public List<String> getSelectValues() {
        return selectValues;
    }
}
