
package net.groboclown.idea.p4ic.compat;

public interface CompatFactory {
    String getMinCompatibleApiVersion();

    String getMaxCompatibleApiVersion();

    CompatManager createCompatManager()
            throws IllegalStateException;
}
