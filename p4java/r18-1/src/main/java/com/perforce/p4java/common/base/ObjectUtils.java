package com.perforce.p4java.common.base;

public class ObjectUtils {
    /**
     * Returns {@code true} if the provided reference is {@code null} otherwise
     * returns {@code false}.
     *
     * @param obj a reference to be checked against {@code null}
     * @return {@code true} if the provided reference is {@code null} otherwise
     * {@code false}
     * @apiNote It will be replaced by Objects.nonNull after jdk1.8
     */
    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    /**
     * Returns {@code true} if the provided reference is {@code null} otherwise
     * returns {@code false}.
     *
     * @param obj a reference to be checked against {@code null}
     * @return {@code true} if the provided reference is {@code null} otherwise
     * {@code false}
     * @apiNote It will be replaced by Objects.nonNull after jdk1.8
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
}
