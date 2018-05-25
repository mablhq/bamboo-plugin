package com.mabl;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FieldValueServlet extends HttpServlet {

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");

		String restApiKey = request.getParameter("restApiKey");
		try {
			response.getWriter().write("SUCCESS! restApiKey="+restApiKey);
		} catch (IOException e) {
		    throw new RuntimeException(e.getMessage(), e);
		}
	}
}
