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

package net.groboclown.p4.simpleswarm.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.groboclown.p4.simpleswarm.exceptions.ResponseFormatException;
import net.groboclown.p4.simpleswarm.impl.JsonUtil;

import java.util.Date;
import java.util.Map;

public class Review {
    private int id;
    private String author;
    private int[] changelists;
    private int[] committedChangelists;
    private String[] commitStatus;
    private Date created;
    private JsonArray deployDetails; // don't know the real type
    private String deployStatus;
    private String description;
    private Map<String, JsonElement> participants;
    private boolean pending;
    private JsonArray projects;
    private String state;
    private String stateLabel;
    private JsonArray testDetails;
    private String testStatus;
    private String type;
    private Date updated;


    Review() {

    }

    /*
      Review:
    type: object
    properties:
      id:
        type: integer
        format: int32
        minimum: 0
        description: unique object identifier
      author:
        type: string
        description: Username that created the review
      changes:
        type: array
        description: Changelists in the review
        items:
          type: integer
          format: int64
      commits:
        type: array
        description: Changelists committed
        items:
          type: integer
          format: int64
      commitStatus:
        $ref: '#/definitions/stringArray'
      created:
        description: timestamp of the review creation
        type: integer
        format: int64
      deployDetails:
        type: array
        items:
          # TODO Need more details about the contents.  This is a guess
          type: string
      depoyStatus:
        type: string
        items:
          # TODO get the enum of valid values
          type: string
      description:
        type: string
        description: Details about the review.
      participants:
        $ref: '#/definitions/ParticipantMap'
      pending:
        type: boolean
      projects:
        # TODO find out what the array really contains
        $ref: '#/definitions/stringArray'
      state:
        # TODO replace with valid enum
        # known entries: needsReview
        type: string
      stateLabel:
        type: string
        description: human readable description of the state
      testDetails:
        # TODO find out what the array really contains
        $ref: '#/definitions/stringArray'
      testStatus:
        # TODO find out what valid values exist
        type: string
      type:
        # TODO find out valid enum values.
        # Discovered so far: 'default'
        type: string
      updated:
        description: time stamp for when the review was last updated
        type: integer
        format: int64
     */
    public Review(JsonObject json)
            throws ResponseFormatException {
        // The review object can be embedded in the object.
        if (json.has("review")) {
            json = json.getAsJsonObject("review");
        }
        this.id = JsonUtil.getIntKey(json, "id");
        this.author = JsonUtil.getNullableStringKey(json, "author");
        this.changelists = JsonUtil.getNullableIntArrayKey(json, "changes");
        this.committedChangelists = JsonUtil.getNullableIntArrayKey(json, "commits");
        this.commitStatus = JsonUtil.getNullableStringArrayKey(json, "commitStatus");
        this.created = JsonUtil.getNullableTimestampKey(json, "created");
        this.deployDetails = JsonUtil.getNullableArrayKey(json, "deployDetails");
        this.deployStatus = JsonUtil.getNullableStringKey(json, "deployStatus");
        this.description = JsonUtil.getNullableStringKey(json, "description");
        this.participants = JsonUtil.getNullableMapKey(json, "participants");
        this.pending = JsonUtil.getNullableBooleanKey(json, "pending", true);
        this.projects = JsonUtil.getNullableArrayKey(json, "projects");
        this.state = JsonUtil.getNullableStringKey(json, "state");
        this.stateLabel = JsonUtil.getNullableStringKey(json, "stateLabel");
        this.testDetails = JsonUtil.getNullableArrayKey(json, "testDetails");
        this.testStatus = JsonUtil.getNullableStringKey(json, "testStatus");
        this.type = JsonUtil.getNullableStringKey(json, "type");
        this.updated = JsonUtil.getNullableTimestampKey(json, "updated");
    }

    public int getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public int[] getChangelists() {
        return changelists;
    }

    public int[] getCommittedChangelists() {
        return committedChangelists;
    }

    public void setCommittedChangelists(int[] committedChangelists) {
        this.committedChangelists = committedChangelists;
    }

    public String[] getCommitStatus() {
        return commitStatus;
    }

    public Date getCreated() {
        return created;
    }

    public JsonArray getDeployDetails() {
        return deployDetails;
    }

    public String getDeployStatus() {
        return deployStatus;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, JsonElement> getParticipants() {
        return participants;
    }

    public boolean isPending() {
        return pending;
    }

    public JsonArray getProjects() {
        return projects;
    }

    public String getState() {
        return state;
    }

    public String getStateLabel() {
        return stateLabel;
    }

    public JsonArray getTestDetails() {
        return testDetails;
    }

    public String getTestStatus() {
        return testStatus;
    }

    public String getType() {
        return type;
    }

    public Date getUpdated() {
        return updated;
    }
}
