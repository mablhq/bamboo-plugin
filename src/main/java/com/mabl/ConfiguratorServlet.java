package com.mabl;

import com.atlassian.extras.common.log.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mabl.domain.GetApiKeyResult;
import com.mabl.domain.GetApplicationsResult;
import com.mabl.domain.GetEnvironmentsResult;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ConfiguratorServlet extends HttpServlet {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger.Log log = Logger.getInstance(this.getClass());

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String action = request.getParameter("ACTION");
        String restApiKey = request.getParameter("restApiKey");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        RestApiClient apiClient = new RestApiClient(MablConstants.MABL_REST_API_BASE_URL, restApiKey);

        switch (action) {
            case "environments":
                doGetEnvironments(apiClient, response);
                break;
            case "applications":
                doGetApplications(apiClient, response);
                break;
        }

	}

	private void doGetEnvironments(RestApiClient apiClient, HttpServletResponse response) {
        try {
            GetApiKeyResult getApiKeyResult = apiClient.getApiKeyResult(apiClient.getRestApiKey());
            GetEnvironmentsResult getEnvironmentsResult = apiClient.getEnvironmentsResult(getApiKeyResult.organization_id);
            writeToWriter(response, objectMapper.writeValueAsString(getEnvironmentsResult.environments));
        } catch (JsonProcessingException | RuntimeException e) {
            log.error(String.format("Unexpected status returned from doGetEnvironments: Reason '%s'", e.getMessage()));
            writeToWriter(response, "[]");
        }
    }

    private void doGetApplications(RestApiClient apiClient, HttpServletResponse response) {
        try {
            GetApiKeyResult getApiKeyResult = apiClient.getApiKeyResult(apiClient.getRestApiKey());
            GetApplicationsResult getApplicationsResult = apiClient.getApplicationsResult(getApiKeyResult.organization_id);
            writeToWriter(response, objectMapper.writeValueAsString(getApplicationsResult.applications));
        } catch (JsonProcessingException | RuntimeException e) {
            log.error(String.format("Unexpected status returned from doGetApplications: Reason '%s'", e.getMessage()));
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
