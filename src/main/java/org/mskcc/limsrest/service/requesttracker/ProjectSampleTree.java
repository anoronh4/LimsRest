package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.isCompletedStatus;
import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.isValidStage;

/**
 * Data Model of the tree structure descending from one ProjectSample
 */
public class ProjectSampleTree {
    private static Log log = LogFactory.getLog(ProjectSampleTree.class);

    public WorkflowSample getRoot() {
        return root;
    }

    public void setRoot(WorkflowSample root) {
        this.root = root;
    }

    private WorkflowSample root;                   // Parent Sample of the tree
    private Map<Long, WorkflowSample> sampleMap;   // Map Record IDs to their enriched sample information
    private Map<String, SampleStageTracker> stageMap;   // Map to all stages by their stage name
    private User user;

    public ProjectSampleTree(WorkflowSample root, User user) {
        this.user = user;
        this.root = root;
        this.sampleMap = new HashMap<>();
        this.stageMap = new TreeMap<>(new StatusTrackerConfig.StageComp());     // Order map by order of stages
    }

    public List<WorkflowSample> getSamples() {
        return new ArrayList(sampleMap.values());
    }

    /**
     * Adds sample to the sample map
     *
     * @param sample
     */
    public void addSample(WorkflowSample sample){
        this.sampleMap.put(sample.getRecordId(), sample);
    }

    public void setSampleMap(Map<Long, WorkflowSample> sampleMap) {
        this.sampleMap = sampleMap;
    }

    /**
     * Returns ordered list of stages in the ProjectSample
     * @return
     */
    public List<SampleStageTracker> getStages() {
        return new ArrayList<>(stageMap.values());
    }

    public void setStageMap(Map<String, SampleStageTracker> stageMap) {
        this.stageMap = stageMap;
    }

    public User getUser() {
        return user;
    }

    /**
     * Adds stage to known Stages of this tree
     * @param node
     */
    public void addStageToTracked(WorkflowSample node){
        String stageName = node.getStage();

        if(isValidStage(stageName)){
            SampleStageTracker stage;
            if(this.stageMap.containsKey(stageName)){
                stage = this.stageMap.get(stageName);
                stage.updateStageTimes(node);
            } else {
                stage = new SampleStageTracker(stageName, 1, 0, node.getStartTime(), node.getUpdateTime());
                this.stageMap.put(stageName, stage);
            }
        } else {
            log.warn(String.format("Unable to determine record '%d' stageName '%s'", node.getRecordId(), stageName));
        }
    }

    /**
     * Updates the tree stages and leaf sample completion status
     *      - Tree stage in the stageMap becomes incomplete IF sample's status is not in a completed state
     *      - Leaf completion status needs to be set because there are no children to determine completion
     * @param leaf
     */
    public void updateTreeOnLeafStatus(WorkflowSample leaf){
        String stageName = leaf.getStage();
        SampleStageTracker stage = this.stageMap.get(stageName);

        // Failed leafs do not modify the completion status
        if(leaf.getFailed()){
            // Fail all nodes in path to failed leaf until reaching root (parent == null) or node w/ non-failed children
            WorkflowSample parent = leaf.getParent();
            while(parent != null && allChildrenFailed(parent)){
                parent.setFailed(Boolean.TRUE);
                parent = parent.getParent();
            }
            // Record the sample has failed if the parent is null as this means the failed path reached the root
            if(parent == null){
                stage.addFailedSample();
            }
        } else {
            String status = leaf.getStatus();
            if(!isCompletedStatus(status)){
                // A leaf w/ an incomplete status makes the stage incomplete, as this leaf is still pending
                // A failed leaf does not make the stage incomplete as other branches may successfully complete this stage
                stage.setComplete(Boolean.FALSE);
            } else {
                leaf.setComplete(Boolean.TRUE);         // Default leaf completion state is FALSE
                if(stage.getComplete() != null){
                    // Update completion status, but leaf is failed if any leaf node is in an incomplete state
                    // TODO - WHAT?
                    stage.setComplete(Boolean.TRUE && stage.getComplete());
                } else {
                    // Initialize stage to true
                    stage.setComplete(Boolean.TRUE);
                }
            }
        }
    }

    /**
     * Returns whether input sample has only failed children
     *
     * @param sample
     * @return
     */
    private boolean allChildrenFailed(WorkflowSample sample){
        List<WorkflowSample> children = sample.getChildren();
        for(WorkflowSample child : children){
            if(!child.getFailed()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts tree representation into a project Sample
     *
     * @return
     */
    public ProjectSample convertToProjectSample() {
        if(this.root == null) return null;

        ProjectSample projectSample = new ProjectSample(this.root.getRecordId());
        List<SampleStageTracker> stages = getStages();
        List<WorkflowSample> workflowSamples = getSamples();
        projectSample.addStage(stages);
        Boolean isFailed = Boolean.TRUE;    // One non-failed sample will set this to True
        Boolean isComplete = Boolean.TRUE;  // All sub-samples need to be complete or the sample to be complete
        for(WorkflowSample sample : workflowSamples){
            isFailed = isFailed && sample.getFailed();
            isComplete = isComplete && sample.getComplete();
        }
        projectSample.setFailed(isFailed);
        projectSample.setComplete(isComplete);
        projectSample.setRoot(getRoot());

        return projectSample;
    }
}
