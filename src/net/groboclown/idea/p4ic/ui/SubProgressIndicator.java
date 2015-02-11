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
package net.groboclown.idea.p4ic.ui;

import com.intellij.ide.util.DelegatingProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.WrappedProgressIndicator;
import org.jetbrains.annotations.NotNull;

public class SubProgressIndicator extends DelegatingProgressIndicator {
    private final double min;
    private final double range;

    public SubProgressIndicator(@NotNull ProgressIndicator indicator,
            double startFraction, double endFraction) {
        super(indicator);
        assert startFraction >= 0.0;
        assert startFraction < endFraction;
        assert endFraction <= 1.0;
        this.min = startFraction;
        this.range = endFraction - min;
    }


    @Override
    public void setFraction(final double fraction) {
        assert fraction >= 0.0;
        assert fraction <= 1.0;
        super.setFraction((fraction * range) + min);
    }
}
