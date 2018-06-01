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

import static com.mabl.MablConstants.APPLICATION_ID_FIELD;
import static com.mabl.MablConstants.APPLICATION_ID_LABEL_PROPERTY;
import static com.mabl.MablConstants.ENVIRONMENT_ID_FIELD;
import static com.mabl.MablConstants.ENVIRONMENT_ID_LABEL_PROPERTY;
import static com.mabl.MablConstants.MABL_REST_API_BASE_URL;
import static com.mabl.MablConstants.REST_API_KEY_FIELD;
import static com.mabl.MablConstants.REST_API_KEY_LABEL_PROPERTY;
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
    @NotNull
    public Map<String, String> generateTaskConfigMap(
            @NotNull final ActionParametersMap params,
            @Nullable final TaskDefinition previousTaskDefinition
    ) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put(REST_API_KEY_FIELD, params.getString(REST_API_KEY_FIELD));
        config.put(ENVIRONMENT_ID_FIELD, params.getString(ENVIRONMENT_ID_FIELD));
        config.put(APPLICATION_ID_FIELD, params.getString(APPLICATION_ID_FIELD));
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);

        context.put(REST_API_KEY_FIELD, "");
        context.put(ENVIRONMENT_ID_FIELD, "");
        context.put(APPLICATION_ID_FIELD, "");
    }

    @Override
    public void populateContextForEdit(
            @NotNull final Map<String, Object> context,
            @NotNull final TaskDefinition taskDefinition
    ) {
        super.populateContextForEdit(context, taskDefinition);
        String restApiKeyValue = taskDefinition.getConfiguration().get(REST_API_KEY_FIELD);
        context.put(REST_API_KEY_FIELD, restApiKeyValue);
        context.put(ENVIRONMENT_ID_FIELD, taskDefinition.getConfiguration().get(ENVIRONMENT_ID_FIELD));
        context.put("environmentsList", getEnvironmentsList(restApiKeyValue));
        context.put(APPLICATION_ID_FIELD, taskDefinition.getConfiguration().get(APPLICATION_ID_FIELD));
        context.put("applicationsList", getApplicationsList(restApiKeyValue));
    }

    @Override
    public void validate(
            @NotNull final ActionParametersMap params,
            @NotNull final ErrorCollection errorCollection
    ) {
        super.validate(params, errorCollection);


        final String restApiKeyValue = params.getString(REST_API_KEY_FIELD);
        final String restApiKeyLabel = getLabel(REST_API_KEY_LABEL_PROPERTY);
        if(isEmpty(restApiKeyValue)) {
            errorCollection.addError(REST_API_KEY_FIELD, String.format("'%s' is required.", restApiKeyLabel));
        } else if(!restApiKeyIsValid(restApiKeyValue)) {
            errorCollection.addError(REST_API_KEY_FIELD, String.format("The entered '%s' is invalid.", restApiKeyLabel));
        }

        final String environmentIdValue = params.getString(ENVIRONMENT_ID_FIELD);
        final String applicationIdValue = params.getString(APPLICATION_ID_FIELD);

        if(isEmpty(environmentIdValue) && isEmpty(applicationIdValue)) {
            String error = String.format("One of '%s' or '%s' is required.",
                    getLabel(ENVIRONMENT_ID_LABEL_PROPERTY),
                    getLabel(APPLICATION_ID_LABEL_PROPERTY)
            );
            errorCollection.addError(ENVIRONMENT_ID_FIELD, error);
            errorCollection.addError(APPLICATION_ID_FIELD, error);
        }
    }

    private Map<String, String> getEnvironmentsList(String restApiKey) {
        Map<String, String> envMap = new HashMap<>();
        try(RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, restApiKey)) {
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

    private Map<String, String> getApplicationsList(String restApiKey) {
        Map<String, String> appMap = new HashMap<>();
        try(RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, restApiKey)) {
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
        try(RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, restApiKey)) {
            String organizationId = apiClient.getApiKeyResult(restApiKey).organization_id;
            return !isEmpty(organizationId ) && endsWith(organizationId, "-w");
        } catch (RuntimeException e) {
            log.error(String.format("Unexpected results trying to validate ApiKey: Reason '%s'", e.getMessage()));
            return false;
        }
    }

    private String getLabel(String key) {
        return i18nResolver.getText(key);
    }
}
