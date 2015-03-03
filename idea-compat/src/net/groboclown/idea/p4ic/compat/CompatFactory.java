
package net.groboclown.idea.p4ic.compat;

import org.jetbrains.annotations.NotNull;

public interface CompatFactory {
    @NotNull
    String getMinCompatibleApiVersion();

    /**
     * The API is compatible with versions up to, but not including,
     * this returned version.
     *
     * @return API version beyond what this factory supports.
     */
    @NotNull
    String getMaxCompatibleApiVersion();

    @NotNull
    CompatManager createCompatManager()
            throws IllegalStateException;
}
