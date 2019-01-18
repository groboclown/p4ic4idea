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

package net.groboclown.p4plugin.ui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;

public class DoubleMinMaxSpinnerModel
        implements SpinnerModel {
    private static final double EPSILON = 0.001;

    private final List<ChangeListener> listeners = new ArrayList<>();
    private final double minValue;
    private final double maxValue;
    private final double step;
    private double value;

    public DoubleMinMaxSpinnerModel(final double minValue, final double maxValue, final double step,
            final double initialValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        this.value = initialValue;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(final Object value) {
        if (!(value instanceof Number)) {
            // This includes a null check.
            return;
        }
        double newValue = Math.min(
                maxValue,
                Math.max(
                        minValue,
                        ((Number) value).doubleValue()));
        // No equals for doubles.
        if (Math.abs(newValue - this.value) > EPSILON) {
            this.value = newValue;
            synchronized (listeners) {
                for (ChangeListener listener : listeners) {
                    listener.stateChanged(new ChangeEvent(this));
                }
            }
        }
    }

    @Override
    public Object getNextValue() {
        return Math.min(maxValue, value + step);
    }

    @Override
    public Object getPreviousValue() {
        return Math.max(minValue, value - step);
    }

    @Override
    public void addChangeListener(final ChangeListener l) {
        if (l != null) {
            synchronized (listeners) {
                listeners.add(l);
            }
        }
    }

    @Override
    public void removeChangeListener(final ChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }
}
