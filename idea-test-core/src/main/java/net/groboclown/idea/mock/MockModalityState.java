// Copyright (C) Zilliant, Inc.
package net.groboclown.idea.mock;

import com.intellij.openapi.application.ModalityState;
import org.jetbrains.annotations.NotNull;

public class MockModalityState extends ModalityState {
    @Override
    public boolean dominates(@NotNull ModalityState modalityState) {
        return false;
    }

    @Override
    public String toString() {
        return "modal";
    }
}
