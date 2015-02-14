
package net.groboclown.idea.p4ic.compat;

import org.jetbrains.annotations.NotNull;

public interface CompatFactory {
    @NotNull
    String getMinCompatibleApiVersion();

    @NotNull
    String getMaxCompatibleApiVersion();

    @NotNull
    CompatManager createCompatManager()
            throws IllegalStateException;
}
