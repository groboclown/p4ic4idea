// Copyright (C) Zilliant, Inc.
package net.groboclown.p4plugin;

import com.intellij.openapi.vcs.changes.LocalChangeList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** A place to store IDE constants. */
public final class IdeConsts {
    public static final String DEFAULT_LOCAL_CHANGELIST_NAME;

    static {
        // In IDE version 201, the LocalChangeList.DEFAULT_NAME field was removed.
        String localName = "default-changelist-name";
        try {
            Field field = LocalChangeList.class.getField("DEFAULT_NAME");
            if (Modifier.isStatic(field.getModifiers())) {
                final Object value = field.get(null);
                if (value instanceof String) {
                    localName = (String) value;
                }
            }
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Error | Exception e) {
            // Ignore; use the default value.
        }

        DEFAULT_LOCAL_CHANGELIST_NAME = localName;
    }
}
