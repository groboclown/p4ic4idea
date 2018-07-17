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

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class DummyProgressIndicator implements ProgressIndicator {
    public static final ProgressIndicator DUMMY = new DummyProgressIndicator();

    @NotNull
    public static ProgressIndicator nullSafe(@Nullable ProgressIndicator pi) {
        if (pi == null) {
            return DUMMY;
        }
        return pi;
    }

    private DummyProgressIndicator() {
        // do nothing - shouldn't be instantiated
    }


    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setText(String s) {

    }

    @Override
    public String getText() {
        return null;
    }

    @Override
    public void setText2(String s) {

    }

    @Override
    public String getText2() {
        return null;
    }

    @Override
    public double getFraction() {
        return 0;
    }

    @Override
    public void setFraction(double v) {

    }

    @Override
    public void pushState() {

    }

    @Override
    public void popState() {

    }

    @Override
    public void startNonCancelableSection() {

    }

    @Override
    public void finishNonCancelableSection() {

    }

    @Override
    public boolean isModal() {
        return false;
    }

    @NotNull
    @Override
    public ModalityState getModalityState() {
        return ModalityState.NON_MODAL;
    }

    @Override
    public void setModalityProgress(ProgressIndicator progressIndicator) {

    }

    @Override
    public boolean isIndeterminate() {
        return false;
    }

    @Override
    public void setIndeterminate(boolean b) {

    }

    @Override
    public void checkCanceled()
            throws ProcessCanceledException {

    }

    @Override
    public boolean isPopupWasShown() {
        return false;
    }

    @Override
    public boolean isShowing() {
        return false;
    }
}
