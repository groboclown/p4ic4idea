/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.core;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.HashMap;
import java.util.Map;

import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.IJobSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServer;
import org.apache.commons.lang3.Validate;

/**
 * Simple generic default implementation class for the IJob interface.
 */

public class Job extends ServerResource implements IJob {
    /**
     * The max description length for "short" or summary descriptions
     */
    public static int SHORT_DESCR_LENGTH = 128;

    /* it's read only */
    private String jobName = null;
    private Map<String, Object> rawFields = new HashMap<>(6);
    private String description = null;
    private IJobSpec jobSpec = null;
    private static final String[] DESCRIPTION_FIELD_IDS = {"Description", "description", "Desc"};
    private static final String[] JOB_ID_FIELD_IDS = {"Job", "job", "JobId"};

    /**
     * Simple factory method for creating a new Job class.
     *
     * @param server non-null IServer to be associated with this job.
     * @param map    non-null fields map for the job to be created.
     * @return new Job object
     */
    public static Job newJob(IServer server, Map<String, Object> map) {
        Validate.notNull(server, "null server passed to Job.newJob()");
        Validate.notNull(map, "null map passed to Job.newJob()");
        return new Job(server, map);
    }

    public Job(IServer server, Map<String, Object> map) {
        this(server, map, false);
    }

    public Job(IServer server, Map<String, Object> map, boolean longDescriptions) {
        super(true, true);
        this.server = server;

        // Now try to retrieve a handful of "standard" fields
        // if we can...
        if (map != null) {
            jobName = getJobIdString(map);
            description = getDescriptionString(map, longDescriptions);

            // Remove the 'specFormatted' field.
            // See job072366 for more detail.
            map.remove("specFormatted");
            // Assign the raw fields
            rawFields.putAll(map);
        }
    }

    /**
     * This method will refresh by getting the complete job model. If this
     * refresh is successful then this job will be marked as complete.
     */
    @Override
    public void refresh() throws ConnectionException, RequestException, AccessException {
        IServer refreshServer = server;
        String refreshId = jobName;
        if (refreshServer != null && refreshId != null) {
            IJob refreshedJob = refreshServer.getJob(refreshId);
            if (refreshedJob != null) {
                description = refreshedJob.getDescription();
                if (refreshedJob.getRawFields() != null) {
                    rawFields.putAll(refreshedJob.getRawFields());
                } else {
                    rawFields = new HashMap<>(6);
                }
            }
        }
    }

    /**
     * <p>
     * NOTE: do not use this method if the server field has not been set.
     */
    @Override
    public String updateOnServer()
            throws ConnectionException, RequestException, AccessException {

        Validate.notNull(server, "Null server field in Job.updateOnServer");
        return server.updateJob(this);
    }

    @Override
    public void update() throws ConnectionException, RequestException, AccessException {
        server.updateJob(this);
    }

    public String getDescription() {
        return description;
    }

    /**
     * get job name
     *
     * @return
     */
    public String getId() {
        return jobName;
    }

    public IJobSpec getJobSpec() {
        return jobSpec;
    }

    public Map<String, Object> getRawFields() {
        return rawFields;
    }

    /**
     * Set job name
     * @param id job name
     */
    public void setId(String id) {
        this.jobName = id;
        //addOrUpdateRawFieldValue(JOB_ID_FIELD_IDS, id);
    }


    public void setRawFields(Map<String, Object> rawFields) {
        this.rawFields.putAll(rawFields);
    }

    public void setDescription(String description) {
        this.description = description;
        addOrUpdateRawFieldValue(DESCRIPTION_FIELD_IDS, description);
    }

    private void addOrUpdateRawFieldValue(String[] possibleKeys, Object value) {
        boolean isPresent = false;
        for (String possibleKey : possibleKeys) {
            if (rawFields.containsKey(possibleKey)) {
                rawFields.put(possibleKey, value);
                isPresent = true;
                break;
            }
        }

        if (!isPresent) {
            String firstKey = possibleKeys[0];
            rawFields.put(firstKey, value);
        }
    }

    public void setJobSpec(IJobSpec jobSpec) {
        this.jobSpec = jobSpec;
    }

    protected String getJobIdString(Map<String, Object> map) {
        String candidate = (String) map.get(JOB_ID_FIELD_IDS[0]);

        if (isBlank(candidate)) {
            candidate = (String) map.get(JOB_ID_FIELD_IDS[1]);
            if (isBlank(candidate)) {
                candidate = (String) map.get(JOB_ID_FIELD_IDS[2]);
            }
        }

        return candidate;
    }

    protected String getDescriptionString(Map<String, Object> map, boolean longDescriptions) {
        String candidate = (String) map.get(DESCRIPTION_FIELD_IDS[0]);

        if (isBlank(candidate)) {
            candidate = (String) map.get(DESCRIPTION_FIELD_IDS[1]);
            if (isBlank(candidate)) {
                candidate = (String) map.get(DESCRIPTION_FIELD_IDS[2]);
            }
        }

        if (isNotBlank(candidate)
                && !longDescriptions
                && (candidate.length() > SHORT_DESCR_LENGTH)) {
            return candidate.substring(0, SHORT_DESCR_LENGTH - 1);
        }
        return candidate;
    }
}
