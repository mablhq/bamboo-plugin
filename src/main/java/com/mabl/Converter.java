package com.mabl;

import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.mabl.domain.CreateDeploymentProperties;

import java.util.Map;
import java.util.function.Function;

public class Converter {

    public static final Function<CustomVariableContext, CreateDeploymentProperties> customVariableContextToCreateDeploymentProperties
            = new Function<CustomVariableContext, CreateDeploymentProperties>() {
        @Override
        public CreateDeploymentProperties apply(CustomVariableContext customVariableContext) {
            Map<String, VariableDefinitionContext> vars = customVariableContext.getVariableContexts();
            CreateDeploymentProperties props = new CreateDeploymentProperties();

            // Repository specific props (Only exists because of other non-mabl steps)
            props.setRepositoryBranchName(getProperty("planRepository.branchName", vars));
            props.setRepositoryRevisionNumber(getProperty("planRepository.revision", vars));
            props.setRepositoryUrl(getProperty("planRepository.repositoryUrl", vars));
            props.setRepositoryName(getProperty("planRepository.name", vars));
            props.setRepositoryPreviousRevisionNumber(getProperty("planRepository.previousRevision", vars));
            props.setRepositoryCommitUsername(getProperty("planRepository.username", vars));

            // Bamboo info about the mabl step that should be there no matter what
            props.setBuildPlanId(getProperty("planKey", vars));
            props.setBuildPlanName(getProperty("planName", vars));
            props.setBuildPlanJobId(getProperty("shortJobKey", vars));
            props.setBuildPlanJobName(getProperty("shortJobName", vars));
            props.setBuildPlanNumber(getProperty("buildNumber", vars));
            props.setBuildPlanResultUrl(getProperty("buildResultsUrl", vars));

            return props;
        }

        private String getProperty(
                String propertyToGet,
                Map<String, VariableDefinitionContext> context
        ) {
           if(context.containsKey(propertyToGet)) {
               return context.get(propertyToGet).getValue();
           }

           return null;
        }
    };
}
