package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Queries the LIMS for fields important to fastq.gz data access permissions.
 */
public class GetRequestPermissionsTask {
    private static Log log = LogFactory.getLog(GetRequestPermissionsTask.class);

    private ConnectionLIMS conn;
    private String requestId;

    public GetRequestPermissionsTask(String requestId, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.conn = conn;
    }

    public GetRequestPermissionsTask.RequestPermissions execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            List<DataRecord> requestList = drm.queryDataRecords("Request", "RequestId = '" + this.requestId + "'", user);
            if (requestList.size() != 1) {  // error: request ID not found or more than one found
                log.error("Request not found:" + requestId);
                return null;
            }

            DataRecord dataRecord = requestList.get(0);
            String dataAccessEmails = dataRecord.getStringVal("DataAccessEmails", user);
            String labHeadEmail = dataRecord.getStringVal("LabHeadEmail", user).toLowerCase();

            Boolean isCmoRequest = Boolean.FALSE;
            Boolean bicAnalysis = Boolean.FALSE;
            try {
                isCmoRequest = dataRecord.getBooleanVal("IsCmoRequest", user);
                bicAnalysis = dataRecord.getBooleanVal("BICAnalysis", user);
            } catch (NullPointerException e) {
                log.warn("Correct invalid null valid in database for request: " + requestId);
            }

            String labName = labHeadEmailToLabName(labHeadEmail);

            return new RequestPermissions(requestId, labName, labHeadEmail, isCmoRequest, bicAnalysis, dataAccessEmails);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    protected static String labHeadEmailToLabName(String labHeadEmail) {
        if (emailToLabMap.containsKey(labHeadEmail))
            return emailToLabMap.get(labHeadEmail);
        // there are 10 old LIMS projects with a @sloankettering.edu labHeadEmail address
        else if (labHeadEmail.endsWith("@mskcc.org") || labHeadEmail.endsWith("@sloankettering.edu"))
            return labHeadEmail.split("@")[0];
        else
            return labHeadEmail.split("@")[0] + "_EXTERNAL";
    }

    protected static Map<String, String> emailToLabMap = new HashMap<>();
    static {
        // this table could move to ngs-stats DB if updates are frequent/necessary
        // this endpoint would then just return the labHeadEmail and not the lab share folder name
        emailToLabMap.put("a-haimovitz-friedman@ski.mskcc.org", "haimovia");
        emailToLabMap.put("a-zelenetz@mskcc.org", "zeleneta");
        emailToLabMap.put("a-zelenetz@ski.mskcc.org", "zeleneta");
        emailToLabMap.put("d-scheinberg@ski.mskcc.org", "scheinbd");
        emailToLabMap.put("f-giancotti@ski.mskcc.org", "giancotf");
        emailToLabMap.put("j-massague@ski.mskcc.org", "massaguej");
        emailToLabMap.put("k-anderson@ski.mskcc.org", "kanderson");
        emailToLabMap.put("m-baylies@ski.mskcc.org", "bayliesm");
        emailToLabMap.put("m-jasin@ski.mskcc.org", "jasinm");
        emailToLabMap.put("m-mcdevitt@ski.mskcc.org", "m-mcdevitt");
        emailToLabMap.put("m-moore@ski.mskcc.org", "moorem");
        emailToLabMap.put("m-ptashne@ski.mskcc.org", "ptashne");
        emailToLabMap.put("m-sadelain@ski.mskcc.org", "sadelaim");
        emailToLabMap.put("m-van-den-brink@ski.mskcc.org", "vandenbm");
        emailToLabMap.put("p-tempst@ski.mskcc.org", "tempstp");
        emailToLabMap.put("r-benezra@ski.mskcc.org", "benezrar");
        emailToLabMap.put("r-kolesnick@ski.mskcc.org", "rkolesnick");
        emailToLabMap.put("s-keeney@ski.mskcc.org", "keeneys");
        emailToLabMap.put("s-shuman@ski.mskcc.org", "sshuman");
        emailToLabMap.put("w-mark@ski.mskcc.org", "markw");
    }

    public static class RequestPermissions {
        public String requestId;
        public String labName;
        public String labHeadEmail;
        public Boolean isCmoRequest;
        public Boolean bicAnalysis;
        public String dataAccessEmails;

        public RequestPermissions(String requestId, String labName, String labHeadEmail,
                                  Boolean isCmoRequest, Boolean bicAnalysis, String dataAccessEmails) {
            this.requestId = requestId;
            this.labName = labName;
            this.labHeadEmail = labHeadEmail;
            this.isCmoRequest = isCmoRequest;
            this.bicAnalysis = bicAnalysis;
            this.dataAccessEmails = dataAccessEmails;
        }
    }
}