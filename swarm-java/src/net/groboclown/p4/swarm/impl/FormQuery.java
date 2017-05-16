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

package net.groboclown.p4.swarm.impl;

import com.google.gson.annotations.Since;
import net.groboclown.p4.swarm.SwarmVersion;
import net.groboclown.p4.swarm.exceptions.ObjectSerializationException;
import net.groboclown.p4.swarm.model.Pageable;
import net.groboclown.p4.swarm.model.PageableRequest;
import net.groboclown.p4.swarm.model.request.stringify.RequestFieldToString;
import net.groboclown.p4.swarm.model.anno.NullIf;
import net.groboclown.p4.swarm.model.anno.ToString;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormQuery {
    private static final int MAX_PAGE_REQUESTS = 100;
    private static final Map<Class<?>, ClassItem> CLASS_SERIALIZATION = new HashMap<Class<?>, ClassItem>();

    public static <T extends PageableRequest> T nextPage(T lastRequest, Pageable response) {
        lastRequest.setAfter(response.getLastSeen());
        return lastRequest;
    }

    public static Map<String, String> asQuery(SwarmVersion version,
            Object obj, Map<String, String> query) {
        if (query == null) {
            query = new HashMap<String, String>();
        }
        if (obj != null) {
            ClassItem item;
            synchronized (CLASS_SERIALIZATION) {
                item = CLASS_SERIALIZATION.get(obj.getClass());
                if (item == null) {
                    item = new ClassItem(obj.getClass());
                    CLASS_SERIALIZATION.put(obj.getClass(), item);
                }
            }
            item.put(version, obj, query);
        }
        return query;
    }


    private static class ClassItem {
        private final List<FieldItem> fields;

        private ClassItem(Class<?> c) {
            List<FieldItem> ff = new ArrayList<FieldItem>();
            Method[] methods = c.getMethods();
            for (Field field : c.getDeclaredFields()) {
                ff.add(new FieldItem(field, matchMethodToField(methods, field)));
            }
            this.fields = Collections.unmodifiableList(ff);
        }

        void put(SwarmVersion version, Object src, Map<String, String> query) {
            for (FieldItem field : fields) {
                try {
                    field.put(version, src, query);
                } catch (InvocationTargetException e) {
                    throw new ObjectSerializationException(src.getClass(), field.name, e);
                } catch (IllegalAccessException e) {
                    throw new ObjectSerializationException(src.getClass(), field.name, e);
                } catch (InstantiationException e) {
                    throw new ObjectSerializationException(src.getClass(), field.name, e);
                }
            }
        }
    }


    private static Method matchMethodToField(Method[] methods, Field field) {
        String name = field.getName();
        String getterName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (Method method : methods) {
            if (method.getName().equals(getterName)) {
                return method;
            }
        }
        return null;
    }


    private static class FieldItem {
        private final String name;
        private final Field field;
        private final Method getter;

        private FieldItem(Field field, Method getter) {
            this.field = field;
            this.getter = getter;
            this.name = field.getName();
        }

        void put(SwarmVersion version, Object src, Map<String, String> query)
                throws InvocationTargetException, IllegalAccessException, InstantiationException {
            Object value;
            if (getter != null) {
                value = getter.invoke(src);
            } else {
                value = field.get(src);
            }
            if (value != null && Number.class.isInstance(value) && field.isAnnotationPresent(NullIf.class)) {
                Number v = (Number) value;
                NullIf val = field.getAnnotation(NullIf.class);
                if (val != null && v.doubleValue() <= val.value()) {
                    value = null;
                }
            }
            if (version != null && value != null && field.isAnnotationPresent(Since.class)) {
                Since since = field.getAnnotation(Since.class);
                if (since != null && since.value() > version.asFloat()) {
                    value = null;
                }
            }
            if (value != null) {
                String stringValue;
                if (field.isAnnotationPresent(ToString.class)) {
                    ToString ts = field.getAnnotation(ToString.class);
                    if (ts != null) {
                        stringValue = rftsToString(ts.value().newInstance(), value);
                    } else {
                        stringValue = null;
                    }
                } else {
                    stringValue = value.toString();
                }
                if (stringValue != null) {
                    query.put(name, stringValue);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> String rftsToString(RequestFieldToString<T> ts, Object v) {
        return ts.toString((T) v);
    }
}
