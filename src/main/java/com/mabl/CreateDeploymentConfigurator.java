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
import com.mabl.domain.GetLabelsResult;
import com.spotify.docker.client.shaded.org.apache.http.HttpHost;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.mabl.MablConstants.APPLICATION_ID_FIELD;
import static com.mabl.MablConstants.APPLICATION_ID_LABEL_PROPERTY;
import static com.mabl.MablConstants.ENVIRONMENT_ID_FIELD;
import static com.mabl.MablConstants.ENVIRONMENT_ID_LABEL_PROPERTY;
import static com.mabl.MablConstants.MABL_REST_API_BASE_URL;
import static com.mabl.MablConstants.PLAN_LABELS_FIELD;
import static com.mabl.MablConstants.PROXY_ADDRESS_FIELD;
import static com.mabl.MablConstants.PROXY_PASSWORD_FIELD;
import static com.mabl.MablConstants.PROXY_USERNAME_FIELD;
import static com.mabl.MablConstants.REST_API_KEY_FIELD;
import static com.mabl.MablConstants.REST_API_KEY_LABEL_PROPERTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.split;

@Scanned
public class CreateDeploymentConfigurator extends AbstractTaskConfigurator {
    private I18nResolver i18nResolver;
    private final Logger.Log log = Logger.getInstance(this.getClass());

    private static final String PROXY_FORMAT_ERROR_TEMPLATE = "%s: Use format <protocol>://<hostname>:<port>";
    
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
        config.put(PLAN_LABELS_FIELD, join(params.getStringArray(PLAN_LABELS_FIELD), ","));
        config.put(PROXY_ADDRESS_FIELD, params.getString(PROXY_ADDRESS_FIELD));
        config.put(PROXY_USERNAME_FIELD, params.getString(PROXY_USERNAME_FIELD));
        config.put(PROXY_PASSWORD_FIELD, params.getString(PROXY_PASSWORD_FIELD));
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);

        context.put(REST_API_KEY_FIELD, "");
        context.put(ENVIRONMENT_ID_FIELD, "");
        context.put(APPLICATION_ID_FIELD, "");
        context.put(PLAN_LABELS_FIELD, "");
        context.put(PROXY_ADDRESS_FIELD, "");
        context.put(PROXY_USERNAME_FIELD, "");
        context.put(PROXY_PASSWORD_FIELD, "");
    }

    @Override
    public void populateContextForEdit(
            @NotNull final Map<String, Object> context,
            @NotNull final TaskDefinition taskDefinition
    ) {
        super.populateContextForEdit(context, taskDefinition);
        String restApiKeyValue = taskDefinition.getConfiguration().get(REST_API_KEY_FIELD);
        String proxyAddress = taskDefinition.getConfiguration().get(PROXY_ADDRESS_FIELD);
        String proxyUsername = taskDefinition.getConfiguration().get(PROXY_USERNAME_FIELD);
        String proxyPassword = taskDefinition.getConfiguration().get(PROXY_PASSWORD_FIELD);
        ProxyConfiguration proxyConfig = new ProxyConfiguration(proxyAddress, proxyUsername, proxyPassword);
        context.put(PROXY_ADDRESS_FIELD, proxyAddress);
        context.put(PROXY_USERNAME_FIELD, proxyUsername);
        context.put(PROXY_PASSWORD_FIELD, proxyPassword);
        context.put(REST_API_KEY_FIELD, restApiKeyValue);
        context.put(ENVIRONMENT_ID_FIELD, taskDefinition.getConfiguration().get(ENVIRONMENT_ID_FIELD));
        context.put("environmentsList", getEnvironmentsList(restApiKeyValue, proxyConfig));
        context.put(APPLICATION_ID_FIELD, taskDefinition.getConfiguration().get(APPLICATION_ID_FIELD));
        context.put("applicationsList", getApplicationsList(restApiKeyValue, proxyConfig));
        context.put(PLAN_LABELS_FIELD, split(taskDefinition.getConfiguration().get(PLAN_LABELS_FIELD), ","));
        context.put("planLabelsList", getPlanLabelsList(restApiKeyValue, proxyConfig));
    }

    @Override
    public void validate(
            @NotNull final ActionParametersMap params,
            @NotNull final ErrorCollection errorCollection
    ) {
        super.validate(params, errorCollection);

        final ProxyConfiguration proxyConfig = validateProxyConfigs(params, errorCollection);
        final String restApiKeyValue = params.getString(REST_API_KEY_FIELD);
        final String restApiKeyLabel = getLabel(REST_API_KEY_LABEL_PROPERTY);
        if(isBlank(restApiKeyValue)) {
            errorCollection.addError(REST_API_KEY_FIELD, String.format("'%s' is required.", restApiKeyLabel));
        } else if(!restApiKeyIsValid(restApiKeyValue, proxyConfig)) {
            errorCollection.addError(REST_API_KEY_FIELD, String.format("The entered '%s' is invalid.", restApiKeyLabel));
        }

        final String environmentIdValue = params.getString(ENVIRONMENT_ID_FIELD);
        final String applicationIdValue = params.getString(APPLICATION_ID_FIELD);

        if(isBlank(environmentIdValue) && isBlank(applicationIdValue)) {
            String error = String.format("One of '%s' or '%s' is required.",
                    getLabel(ENVIRONMENT_ID_LABEL_PROPERTY),
                    getLabel(APPLICATION_ID_LABEL_PROPERTY)
            );
            errorCollection.addError(ENVIRONMENT_ID_FIELD, error);
            errorCollection.addError(APPLICATION_ID_FIELD, error);
        }
        
        final String proxyAddress = params.getString(PROXY_ADDRESS_FIELD);
        if(!isBlank(proxyAddress)) {
        	try {
        		HttpHost proxy = HttpHost.create(proxyAddress);
        		if(isBlank(proxy.getHostName())) {
        			errorCollection.addError(PROXY_ADDRESS_FIELD,
        					String.format(PROXY_FORMAT_ERROR_TEMPLATE, "No hostname specified"));
        		}
        		int port = proxy.getPort();
        		if(port < 1 || port > 65535) {
        			errorCollection.addError(PROXY_ADDRESS_FIELD,
        					String.format(PROXY_FORMAT_ERROR_TEMPLATE, "Invalid port number provided"));
        		}
        	} catch (Exception exception) {
        		errorCollection.addError(PROXY_ADDRESS_FIELD, 
        				String.format(PROXY_FORMAT_ERROR_TEMPLATE, "Invalid proxy address provided"));
        	}
        }
    }
    
    private ProxyConfiguration validateProxyConfigs(final ActionParametersMap params,
            										final ErrorCollection errorCollection) {
    	final String proxyAddress = params.getString(PROXY_ADDRESS_FIELD);
        if(!isBlank(proxyAddress)) {
        	try {
        		HttpHost proxy = HttpHost.create(proxyAddress);
        		if(isBlank(proxy.getHostName())) {
        			errorCollection.addError(PROXY_ADDRESS_FIELD,
        					String.format(PROXY_FORMAT_ERROR_TEMPLATE, "No hostname specified"));
        		}
        		int port = proxy.getPort();
        		if(port < 1 || port > 65535) {
        			errorCollection.addError(PROXY_ADDRESS_FIELD,
        					String.format(PROXY_FORMAT_ERROR_TEMPLATE, "Invalid port number provided"));
        		}
        	} catch (Exception exception) {
        		errorCollection.addError(PROXY_ADDRESS_FIELD, 
        				String.format(PROXY_FORMAT_ERROR_TEMPLATE, "Invalid proxy address provided"));
        	}
        }
        final String proxyUsername = params.getString(PROXY_USERNAME_FIELD);
        final String proxyPassword = params.getString(PROXY_PASSWORD_FIELD);
        return new ProxyConfiguration(proxyAddress, proxyUsername, proxyPassword);
    }

    private Map<String, String> getApplicationsList(String restApiKey, ProxyConfiguration proxyConfig) {
        Map<String, String> appMap = new HashMap<>();
        try(RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, restApiKey, proxyConfig)) {
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

    private Map<String, String> getEnvironmentsList(String restApiKey, ProxyConfiguration proxyConfig) {
        Map<String, String> envMap = new HashMap<>();
        try(RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, restApiKey, proxyConfig)) {
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

    private Map<String, String> getPlanLabelsList(String restApiKey, ProxyConfiguration proxyConfig) {
        Map<String, String> envMap = new HashMap<>();
        try(RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, restApiKey, proxyConfig)) {
            String organizationId = apiClient.getApiKeyResult(restApiKey).organization_id;
            GetLabelsResult results = apiClient.getLabelsResult(organizationId);
            for(GetLabelsResult.Label label : results.labels) {
                envMap.put(label.name, label.name);
            }
        } catch (RuntimeException e) {
            log.error(String.format("Unexpected results trying to fetch LabelsList: Reason '%s'", e.getMessage()));
        }

        return envMap;
    }

    private boolean restApiKeyIsValid(String restApiKey, ProxyConfiguration proxyConfig) {
        try(RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, restApiKey, proxyConfig)) {
            String organizationId = apiClient.getApiKeyResult(restApiKey).organization_id;
            return !isBlank(organizationId);
        } catch (RuntimeException e) {
            log.error(String.format("Unexpected results trying to validate ApiKey: Reason '%s'", e.getMessage()));
            return false;
        }
    }

    private String getLabel(String key) {
        return i18nResolver.getText(key);
    }
}
