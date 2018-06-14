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
import com.mabl.domain.GetDeploymentsResult;
import com.mabl.domain.GetEnvironmentsResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mabl.MablConstants.APPLICATION_FIELD;
import static com.mabl.MablConstants.APPLICATION_LABEL_PROPERTY;
import static com.mabl.MablConstants.CUSTOM_APPLICATION_FIELD;
import static com.mabl.MablConstants.CUSTOM_APPLICATION_LABEL_PROPERTY;
import static com.mabl.MablConstants.CUSTOM_ENVIRONMENT_FIELD;
import static com.mabl.MablConstants.CUSTOM_ENVIRONMENT_LABEL_PROPERTY;
import static com.mabl.MablConstants.CUSTOM_SELECTOR_VALUE;
import static com.mabl.MablConstants.CUSTOM_URI_FIELD;
import static com.mabl.MablConstants.CUSTOM_URI_LABL_PROPERTY;
import static com.mabl.MablConstants.ENVIRONMENT_FIELD;
import static com.mabl.MablConstants.ENVIRONMENT_LABEL_PROPERTY;
import static com.mabl.MablConstants.MABL_REST_API_BASE_URL;
import static com.mabl.MablConstants.PLAN_TAGS_FIELD;
import static com.mabl.MablConstants.PLAN_TAGS_LABEL_PROPERTY;
import static com.mabl.MablConstants.REST_API_KEY_FIELD;
import static com.mabl.MablConstants.REST_API_KEY_LABEL_PROPERTY;
import static com.mabl.MablConstants.URI_FIELD;
import static com.mabl.MablConstants.URI_LABL_PROPERTY;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.endsWith;

@Scanned
public class CreateDeploymentConfigurator extends AbstractTaskConfigurator {
    private I18nResolver i18nResolver;
    private final Logger.Log log = Logger.getInstance(this.getClass());
    private final List<String> fields = new ArrayList<>(Arrays.asList(
            REST_API_KEY_FIELD,
            ENVIRONMENT_FIELD,
            CUSTOM_ENVIRONMENT_FIELD,
            APPLICATION_FIELD,
            CUSTOM_APPLICATION_FIELD,
            PLAN_TAGS_FIELD,
            URI_FIELD,
            CUSTOM_URI_FIELD
    ));

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
        for(String field : fields) {
            config.put(field, params.getString(field));
        }
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);
        for(String field : fields) {
            context.put(field, "");
        }
    }

    @Override
    public void populateContextForEdit(
            @NotNull final Map<String, Object> context,
            @NotNull final TaskDefinition taskDefinition
    ) {
        super.populateContextForEdit(context, taskDefinition);
        for(String field : fields) {
            context.put(field, taskDefinition.getConfiguration().get(field));
        }

        String restApiKeyValue = taskDefinition.getConfiguration().get(REST_API_KEY_FIELD);
        context.put("urisList", getUrisList(restApiKeyValue));
        context.put("environmentsList", getEnvironmentsList(restApiKeyValue));
        context.put("applicationsList", getApplicationsList(restApiKeyValue));
    }

    @Override
    public void validate(
            @NotNull final ActionParametersMap params,
            @NotNull final ErrorCollection errorCollection
    ) {
        super.validate(params, errorCollection);

        validateRestApiKey(params, errorCollection);
        validateUrl(params, errorCollection);
        validateEnvironment(params, errorCollection);
        validateApplication(params, errorCollection);
        validatePlanTags(params, errorCollection);
    }

    private void validateRestApiKey(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        final String restApiKeyValue = params.getString(REST_API_KEY_FIELD);
        final String restApiKeyLabel = getLabel(REST_API_KEY_LABEL_PROPERTY);
        if(isEmpty(restApiKeyValue)) {
            errorCollection.addError(REST_API_KEY_FIELD, String.format("'%s' is required.", restApiKeyLabel));
        } else if(!restApiKeyIsValid(restApiKeyValue)) {
            errorCollection.addError(REST_API_KEY_FIELD, String.format("The entered '%s' is invalid.", restApiKeyLabel));
        }
    }

    private void validateUrl(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        final String uriValue = params.getString(URI_FIELD);
        final String customUrlValue = params.getString(CUSTOM_URI_FIELD);

        if(isEmpty(uriValue)) {
            errorCollection.addError(URI_FIELD, String.format("Please select a '%s'.", getLabel(URI_LABL_PROPERTY)));
        } else if(uriValue.equals(CUSTOM_SELECTOR_VALUE) && isEmpty(customUrlValue)) {
            errorCollection.addError(CUSTOM_URI_FIELD, String.format("Please '%s'.", getLabel(CUSTOM_URI_LABL_PROPERTY)));
        }
    }

    private void validateEnvironment(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        final String environmentIdValue = params.getString(ENVIRONMENT_FIELD);
        final String customEnvironmentValue = params.getString(CUSTOM_ENVIRONMENT_FIELD);

        if(isEmpty(environmentIdValue)) {
            errorCollection.addError(ENVIRONMENT_FIELD, String.format("Please select an '%s'.", getLabel(ENVIRONMENT_LABEL_PROPERTY)));
        } else if(environmentIdValue.equals(CUSTOM_SELECTOR_VALUE) && isEmpty(customEnvironmentValue)) {
            errorCollection.addError(CUSTOM_ENVIRONMENT_FIELD, String.format("Please '%s'.", getLabel(CUSTOM_ENVIRONMENT_LABEL_PROPERTY)));
        }
    }

    private void validateApplication(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        final String applicationIdValue = params.getString(APPLICATION_FIELD);
        final String customApplicationValue = params.getString(CUSTOM_APPLICATION_FIELD);

        if(isEmpty(applicationIdValue)) {
            errorCollection.addError(APPLICATION_FIELD, String.format("Please select an '%s'.", getLabel(APPLICATION_LABEL_PROPERTY)));
        } else if(applicationIdValue.equals(CUSTOM_SELECTOR_VALUE) && isEmpty(customApplicationValue)) {
            errorCollection.addError(CUSTOM_APPLICATION_FIELD, String.format("Please '%s'.", getLabel(CUSTOM_APPLICATION_LABEL_PROPERTY)));
        }
    }

    private void validatePlanTags(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        final String planTags = params.getString(PLAN_TAGS_FIELD);
        if(!isEmpty(planTags)) {
            try {
                new ObjectMapper().readValue(planTags, new TypeReference<List<List<String>>>() {});
            } catch (IOException e) {
                errorCollection.addError(PLAN_TAGS_FIELD, String.format("'%s' is malformed and should have the structure '%s'",
                        getLabel(PLAN_TAGS_LABEL_PROPERTY),
                        "[[\"tag1.1\",\"tag1.2\"],[\"tag2.1\",\"tag2.2\",\"tag2.3\"]]"
                ));
            }
        }
    }

    private Map<String, String> getUrisList(String restApiKey) {
        Map<String, String> envMap = new HashMap<>();
        envMap.put(CUSTOM_SELECTOR_VALUE, getLabel(CUSTOM_URI_LABL_PROPERTY));
        try(RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, restApiKey)) {
            String organizationId = apiClient.getApiKeyResult(restApiKey).organization_id;
            GetDeploymentsResult results = apiClient.getDeploymentsResult(organizationId);
            for(GetDeploymentsResult.Deployment deployment : results.deployments) {
                envMap.put(deployment.uri, deployment.uri);
            }
        } catch (RuntimeException e) {
            log.error(String.format("Unexpected results trying to fetch UrisList: Reason '%s'", e.getMessage()));
        }

        return envMap;
    }

    private Map<String, String> getEnvironmentsList(String restApiKey) {
        Map<String, String> envMap = new HashMap<>();
        envMap.put(CUSTOM_SELECTOR_VALUE, getLabel(CUSTOM_ENVIRONMENT_LABEL_PROPERTY));
        try(RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, restApiKey)) {
            String organizationId = apiClient.getApiKeyResult(restApiKey).organization_id;
            GetEnvironmentsResult results = apiClient.getEnvironmentsResult(organizationId);
            for(GetEnvironmentsResult.Environment environment : results.environments) {
                envMap.put(environment.name, environment.name);
            }
        } catch (RuntimeException e) {
            log.error(String.format("Unexpected results trying to fetch EnvironmentsList: Reason '%s'", e.getMessage()));
        }

        return envMap;
    }

    private Map<String, String> getApplicationsList(String restApiKey) {
        Map<String, String> appMap = new HashMap<>();
        appMap.put(CUSTOM_SELECTOR_VALUE, getLabel(CUSTOM_APPLICATION_LABEL_PROPERTY));
        try(RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, restApiKey)) {
            String organizationId = apiClient.getApiKeyResult(restApiKey).organization_id;
            GetApplicationsResult results = apiClient.getApplicationsResult(organizationId);
            for(GetApplicationsResult.Application application: results.applications) {
                appMap.put(application.name, application.name);
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
