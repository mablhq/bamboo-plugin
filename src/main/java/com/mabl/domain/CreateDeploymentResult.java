package com.mabl.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * mabl result from deployment event creation
 */

public class CreateDeploymentResult implements ApiResult {
    public String id;
    public String applicationId;
    public String environmentId;
    public long receivedTime;
    public Set<PlanRunSummary> triggeredPlanRunSummaries;
    public Set<String> planLabels;

    @JsonCreator
    public CreateDeploymentResult(
            @JsonProperty("id") String id,
            @JsonProperty("application_id") String applicationId,
            @JsonProperty("environment_id") String environmentId,
            @JsonProperty("received_time") long receivedTime,
            @JsonProperty("triggered_plan_run_summaries")Set<PlanRunSummary> triggeredPlanRunSummaries,
            @JsonProperty("plan_labels")Set<String> planLabels
            ) {
        this.id = id;
        this.applicationId = applicationId;
        this.environmentId = environmentId;
        this.receivedTime = receivedTime;
        this.triggeredPlanRunSummaries = triggeredPlanRunSummaries;
        this.planLabels = planLabels;
    }


    public static class PlanRunSummary {
        public final String planId;
        public final String planRunId;

        @JsonCreator
        public PlanRunSummary(
                @JsonProperty("plan_id") final String planId,
                @JsonProperty("plan_run_id") final String planRunId
        ) {
            this.planId = planId;
            this.planRunId = planRunId;
        }
    }
}