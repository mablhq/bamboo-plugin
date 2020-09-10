package com.mabl;

import com.atlassian.extras.common.log.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mabl.domain.GetApiKeyResult;
import com.mabl.domain.GetApplicationsResult;
import com.mabl.domain.GetEnvironmentsResult;
import com.mabl.domain.GetLabelsResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class ConfiguratorServlet extends HttpServlet {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger.Log log = Logger.getInstance(this.getClass());

    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("ACTION");
        String restApiKey = request.getParameter("restApiKey");
        if(StringUtils.isBlank(restApiKey)) {
        	writeToWriter(response, "[]");
        	return;
        }
        String proxyAddress = request.getParameter("proxyAddress");
        String proxyUsername = request.getParameter("proxyUsername");
        String proxyPassword = request.getParameter("proxyPassword");
        ProxyConfiguration proxyConfig = new ProxyConfiguration(proxyAddress, proxyUsername, proxyPassword);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        RestApiClient apiClient = new RestApiClient(MablConstants.MABL_REST_API_BASE_URL, restApiKey, proxyConfig);

        switch (action) {
            case "applications":
                doGetApplications(apiClient, response);
                break;
            case "environments":
                doGetEnvironments(apiClient, response);
                break;
            case "labels":
                doGetLabels(apiClient, response);
                break;
            default:
                try {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "request not supported");
                } catch (IOException e) {
                    log.warn("Failed to send response");
                }
        }

	}

	private void doGetApplications(RestApiClient apiClient, HttpServletResponse response) {
        try {
            GetApiKeyResult getApiKeyResult = apiClient.getApiKeySelf();
            GetApplicationsResult getApplicationsResult = apiClient.getApplicationsResult(getApiKeyResult.organization_id);
            writeToWriter(response, objectMapper.writeValueAsString(getApplicationsResult.applications));
        } catch (JsonProcessingException | RuntimeException e) {
            log.error(String.format("Unexpected status returned from doGetApplications: Reason '%s'", e.getMessage()), e);
            writeToWriter(response, "[]");
        }
    }

	private void doGetEnvironments(RestApiClient apiClient, HttpServletResponse response) {
        try {
            GetApiKeyResult getApiKeyResult = apiClient.getApiKeySelf();
            GetEnvironmentsResult getEnvironmentsResult = apiClient.getEnvironmentsResult(getApiKeyResult.organization_id);
            writeToWriter(response, objectMapper.writeValueAsString(getEnvironmentsResult.environments));
        } catch (JsonProcessingException | RuntimeException e) {
            log.error(String.format("Unexpected status returned from doGetEnvironments: Reason '%s'", e.getMessage()), e);
            writeToWriter(response, "[]");
        }
    }

    private void doGetLabels(RestApiClient apiClient, HttpServletResponse response) {
        try {
            GetApiKeyResult getApiKeyResult = apiClient.getApiKeySelf();
            GetLabelsResult getLabelsResult = apiClient.getLabelsResult(getApiKeyResult.organization_id);
            writeToWriter(response, objectMapper.writeValueAsString(getLabelsResult.labels));
        } catch (JsonProcessingException | RuntimeException e) {
            log.error(String.format("Unexpected status returned from doGetLabels: Reason '%s'", e.getMessage()), e);
            writeToWriter(response, "[]");
        }
    }

    private void writeToWriter(HttpServletResponse response, String message) {
        try {
            response.getWriter().write(message);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
