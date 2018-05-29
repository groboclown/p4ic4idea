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

package net.groboclown.p4.server.impl.cache.store;

import net.groboclown.p4.server.api.values.P4JobField;
import net.groboclown.p4.server.api.values.P4JobSpec;
import net.groboclown.p4.server.impl.values.P4JobFieldImpl;
import net.groboclown.p4.server.impl.values.P4JobSpecImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class P4JobSpecStore {

    @SuppressWarnings("WeakerAccess")
    public static class State {
        public String comment;
        public List<FieldState> fields;
    }


    @SuppressWarnings("WeakerAccess")
    public static class FieldState {
        public int code;
        public String name;
        public String dataType;
        public int length;
        public String fieldType;
        public String preset;
        public List<String> selectValues;
    }


    @Nullable
    public static State getStateNullable(@Nullable P4JobSpec spec) {
        if (spec == null) {
            return null;
        }
        return getState(spec);
    }

    @NotNull
    public static State getState(@NotNull P4JobSpec spec) {
        State ret = new State();
        ret.comment = spec.getComment();
        ret.fields = new ArrayList<>(spec.getFields().size());
        for (P4JobField field : spec.getFields()) {
            ret.fields.add(getState(field));
        }
        return ret;
    }

    @Nullable
    public static P4JobSpec readNullable(@Nullable State state) {
        if (state == null) {
            return null;
        }
        return read(state);
    }

    @NotNull
    public static P4JobSpec read(@NotNull State state) {
        List<P4JobField> fields = new ArrayList<>(state.fields.size());
        for (FieldState field : state.fields) {
            fields.add(read(field));
        }
        return new P4JobSpecImpl(state.comment, fields);
    }


    @NotNull
    private static FieldState getState(@NotNull P4JobField field) {
        FieldState ret = new FieldState();

        ret.code = field.getCode();
        ret.name = field.getName();
        ret.dataType = field.getRawDataType();
        ret.length = field.getLength();
        ret.fieldType = field.getRawFieldType();
        ret.preset = field.getPreset();
        ret.selectValues = new ArrayList<>(field.getSelectValues());

        return ret;
    }

    @NotNull
    private static P4JobField read(@NotNull FieldState state) {
        return new P4JobFieldImpl(
                state.code,
                state.name,
                state.dataType,
                state.length,
                state.fieldType,
                state.preset,
                state.selectValues
        );
    }
}
