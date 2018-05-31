package com.mabl;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.extras.common.log.Logger;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.mabl.domain.GetApplicationsResult;
import com.mabl.domain.GetEnvironmentsResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.endsWith;

@Scanned
public class CreateDeploymentConfigurator extends AbstractTaskConfigurator {
    private I18nResolver i18nResolver;
    private final Logger.Log log = Logger.getInstance(this.getClass());

    public CreateDeploymentConfigurator(@ComponentImport I18nResolver i18nResolver) {
        this.i18nResolver = i18nResolver;
    }

    @Override
    public Map<String, String> generateTaskConfigMap(
            @NotNull final ActionParametersMap params,
            @Nullable final TaskDefinition previousTaskDefinition
    ) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put("restApiKey", params.getString("restApiKey"));
        config.put("environmentId", params.getString("environmentId"));
        config.put("applicationId", params.getString("applicationId"));
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);

        context.put("restApiKey", "");
        context.put("environmentId", "");
        context.put("applicationId", "");
    }

    @Override
    public void populateContextForEdit(
            @NotNull final Map<String, Object> context,
            @NotNull final TaskDefinition taskDefinition
    ) {
        super.populateContextForEdit(context, taskDefinition);
        String restApiKey = taskDefinition.getConfiguration().get("restApiKey");
        context.put("restApiKey", restApiKey);
        context.put("environmentId", taskDefinition.getConfiguration().get("environmentId"));
        context.put("environmentsList", getEnvironmentsList(restApiKey));
        context.put("applicationId", taskDefinition.getConfiguration().get("applicationId"));
        context.put("applicationsList", getApplicationsList(restApiKey));
    }

    @Override
    public void validate(
            @NotNull final ActionParametersMap params,
            @NotNull final ErrorCollection errorCollection
    ) {
        super.validate(params, errorCollection);


        final String restApiKeyValue = params.getString("restApiKey");
        final String restApiKeyLabel = getLabel("restApiKey");
        if(isEmpty(restApiKeyValue)) {
            errorCollection.addError("restApiKey", String.format("'%s' is required.", restApiKeyLabel));
        } else if(!restApiKeyIsValid(restApiKeyValue)) {
            errorCollection.addError("restApiKey", String.format("The entered '%s' is invalid.", restApiKeyLabel));
        }

        final String environmentIdValue = params.getString("environmentId");
        final String environmentIdLabel = getLabel("environmentId");
        if(!environmentIdIsValid(environmentIdValue)) {
            errorCollection.addError("environmentId", String.format(
                    "The entered '%s' is invalid. %s",
                    environmentIdLabel,
                    i18nResolver.getText("createdeployment.environmentId.hint"))
            );

        }
        final String applicationIdValue = params.getString("applicationId");
        final String applicationIdLabel = getLabel("applicationId");
        if(!applicationIdIsValid(applicationIdValue)) {
            errorCollection.addError("applicationId", String.format(
                    "The entered '%s' is invalid. %s",
                    applicationIdLabel,
                    i18nResolver.getText("createdeployment.applicationId.hint"))
            );
        }

        if(isEmpty(environmentIdValue) && isEmpty(applicationIdValue)) {
            String error = String.format("One of '%s' or '%s' is required.", environmentIdLabel, applicationIdLabel);
            errorCollection.addError("environmentId", error);
            errorCollection.addError("applicationId", error);
        }
    }

    public Map<String, String> getEnvironmentsList(String restApiKey) {
        Map<String, String> envMap = new HashMap<>();
        try(RestApiClient apiClient = new RestApiClient(MablConstants.MABL_REST_API_BASE_URL, restApiKey)) {
            String organizationId = apiClient.getApiKeyResult(restApiKey).organization_id;
            GetEnvironmentsResult results = apiClient.getEnvironmentsResult(organizationId);
            for(GetEnvironmentsResult.Environment environment : results.environments) {
                envMap.put(environment.id, environment.name);
            }
        } catch (RuntimeException e) {
            log.error(String.format("Unexpected results trying to fetch EnvironmentsList: Reason '%s'", e.getMessage()));
        }

        return envMap;
    }

    public Map<String, String> getApplicationsList(String restApiKey) {
        Map<String, String> appMap = new HashMap<>();
        try(RestApiClient apiClient = new RestApiClient(MablConstants.MABL_REST_API_BASE_URL, restApiKey)) {
            String organizationId = apiClient.getApiKeyResult(restApiKey).organization_id;
            GetApplicationsResult results = apiClient.getApplicationsResult(organizationId);
            for(GetApplicationsResult.Application application: results.applications) {
                appMap.put(application.id, application.name);
            }
        } catch (RuntimeException e) {
            log.error(String.format("Unexpected results trying to fetch ApplicationsList: Reason '%s'", e.getMessage()));
        }

        return appMap;
    }

    private boolean restApiKeyIsValid(String restApiKey) {
        try(RestApiClient apiClient = new RestApiClient(MablConstants.MABL_REST_API_BASE_URL, restApiKey)) {
            String organizationId = apiClient.getApiKeyResult(restApiKey).organization_id;
            return !isEmpty(organizationId ) && endsWith(organizationId, "-w");
        } catch (RuntimeException e) {
            log.error(String.format("Unexpected results trying to validate ApiKey: Reason '%s'", e.getMessage()));
            return false;
        }
    }

    private boolean environmentIdIsValid(String environmentId) {
        return isEmpty(environmentId) || endsWith(environmentId, "-e");
    }

    private boolean applicationIdIsValid(String applicationId) {
        return isEmpty(applicationId) || endsWith(applicationId, "-a");
    }

    private String getLabel(String key) {
        return i18nResolver.getText(String.format("createdeployment.%s.label", key));
    }
}
