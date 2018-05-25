package com.mabl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mabl.domain.GetApiKeyResult;
import com.mabl.domain.GetApplicationsResult;
import com.mabl.domain.GetEnvironmentsResult;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ConfiguratorServlet extends HttpServlet {

    ObjectMapper objectMapper = new ObjectMapper();

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
            response.getWriter().write(objectMapper.writeValueAsString(getEnvironmentsResult.environments));
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void doGetApplications(RestApiClient apiClient, HttpServletResponse response) {
        try {
            GetApiKeyResult getApiKeyResult = apiClient.getApiKeyResult(apiClient.getRestApiKey());
            GetApplicationsResult getApplicationsResult = apiClient.getApplicationsResult(getApiKeyResult.organization_id);
            response.getWriter().write(objectMapper.writeValueAsString(getApplicationsResult.applications));
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
