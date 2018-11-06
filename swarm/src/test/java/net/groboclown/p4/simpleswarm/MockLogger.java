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

package net.groboclown.p4.simpleswarm;

import java.util.ArrayList;
import java.util.List;

public class MockLogger
        implements SwarmLogger {
    public enum V {
        DEBUG, INFO, WARN, ERROR
    }
    public static class Entry {
        public final V level;
        public final String message;
        public final Throwable error;

        public Entry(V level, String message, Throwable error) {
            this.level = level;
            this.message = message;
            this.error = error;
        }
    }
    private final List<Entry> logs = new ArrayList<>();

    public List<Entry> getLogs() {
        return logs;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
        logs.add(new Entry(V.DEBUG, msg, null));
    }

    @Override
    public void debug(Throwable e) {
        logs.add(new Entry(V.DEBUG, null, e));
    }

    @Override
    public void debug(String msg, Throwable e) {
        logs.add(new Entry(V.DEBUG, msg, e));
    }

    @Override
    public void info(String msg) {
        logs.add(new Entry(V.INFO, msg, null));
    }

    @Override
    public void info(Throwable e) {
        logs.add(new Entry(V.INFO, null, e));
    }

    @Override
    public void info(String msg, Throwable e) {
        logs.add(new Entry(V.INFO, msg, e));
    }

    @Override
    public void warn(String msg) {
        logs.add(new Entry(V.WARN, msg, null));
    }

    @Override
    public void warn(Throwable e) {
        logs.add(new Entry(V.WARN, null, e));
    }

    @Override
    public void warn(String msg, Throwable e) {
        logs.add(new Entry(V.WARN, msg, e));
    }

    @Override
    public void error(String msg) {
        logs.add(new Entry(V.ERROR, msg, null));
    }

    @Override
    public void error(Throwable e) {
        logs.add(new Entry(V.ERROR, null, e));
    }

    @Override
    public void error(String msg, Throwable e) {
        logs.add(new Entry(V.ERROR, msg, e));
    }
}
