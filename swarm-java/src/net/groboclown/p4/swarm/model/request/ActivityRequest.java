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

package net.groboclown.p4.swarm.model.request;

import net.groboclown.p4.swarm.model.PageableRequest;
import net.groboclown.p4.swarm.model.anno.NullIf;
import net.groboclown.p4.swarm.model.anno.ToString;
import net.groboclown.p4.swarm.model.request.stringify.ListToString;

import java.util.List;

public class ActivityRequest implements PageableRequest {
    public enum ActivityType {
        change, comment, job, review
    }

    @NullIf
    private int change = -1;

    private String stream = null;

    private ActivityType type;

    @NullIf
    private int after = -1;

    @NullIf
    private int max = -1;

    @ToString(ListToString.class)
    private List<String> fields;


    public int getChange() {
        return change;
    }

    public void setChange(int change) {
        this.change = change;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public int getAfter() {
        return after;
    }

    @Override
    public void setAfter(int after) {
        this.after = after;
    }

    public int getMax() {
        return max;
    }

    @Override
    public void setMax(int max) {
        this.max = max;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }
}
