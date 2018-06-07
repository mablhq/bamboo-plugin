package com.mabl;

import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.mabl.domain.CreateDeploymentProperties;

import java.util.Map;
import java.util.function.Function;

public class Converter {

    public static Function<CustomVariableContext, CreateDeploymentProperties> customVariableContextToCreateDeploymentProperties
            = new Function<CustomVariableContext, CreateDeploymentProperties>() {
        @Override
        public CreateDeploymentProperties apply(CustomVariableContext customVariableContext) {
            Map<String, VariableDefinitionContext> vars = customVariableContext.getVariableContexts();
            CreateDeploymentProperties props = new CreateDeploymentProperties();
            props.setBranchName(vars.get("planRepository.branchName").getValue());
            props.setRevisionNumber(vars.get("planRepository.revision").getValue());
            props.setRepositoryUrl(vars.get("planRepository.repositoryUrl").getValue());
            props.setRepositoryName(vars.get("planRepository.name").getValue());
            props.setPreviousRevisionNumber(vars.get("planRepository.previousRevision").getValue());
            props.setCommitUsername(vars.get("planRepository.username").getValue());
            props.setBuildPlanNumber(vars.get("buildNumber").getValue());
            props.setBuildPlanKey(vars.get("buildKey").getValue());
            props.setBuildResultUrl(vars.get("buildResultsUrl").getValue());
            return props;
        }
    };
}
