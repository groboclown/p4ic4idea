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

package net.groboclown.p4.swarm;

public class SwarmVersion {
    private static final double EPSILON = 0.0001;
    private final double floatVersion;
    private final String pathVersion;

    SwarmVersion(float floatVersion) {
        if (floatVersion < 1.1) {
            this.floatVersion = 1.0 + EPSILON;
            this.pathVersion = "v1/";
        } else if (floatVersion < 1.9) {
            this.floatVersion = 1.1 + EPSILON;
            this.pathVersion = "v1.1/";
        } else {
            // Take the nearest integer value.
            int nearest = Math.round(floatVersion);
            this.floatVersion = nearest + EPSILON;
            this.pathVersion = 'v' + Integer.toString(nearest) + '/';
        }
    }

    SwarmVersion(float floatVersion, String pathVersion) {
        this.floatVersion = floatVersion;
        this.pathVersion = pathVersion;
    }

    public double asFloat() {
        return floatVersion;
    }

    public String asPath() {
        return pathVersion;
    }

    public boolean isAtLeast(double version) {
        return (version + EPSILON) <= (floatVersion - EPSILON);
    }
}
