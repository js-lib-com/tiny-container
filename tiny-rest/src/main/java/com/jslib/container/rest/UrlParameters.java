package com.jslib.container.rest;

import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import jakarta.servlet.http.HttpServletRequest;

class UrlParameters {
	private static final Log log = LogFactory.getLog(UrlParameters.class);

	private final Map<String, String> parameters = new HashMap<>();

	public UrlParameters(HttpServletRequest httpRequest) {
		Enumeration<String> queryParameterNames = httpRequest.getParameterNames();
		while (queryParameterNames.hasMoreElements()) {
			String name = queryParameterNames.nextElement();
			parameters.put(name, httpRequest.getParameter(name));
		}

		String requestURI = httpRequest.getRequestURI();
		// /tiny-rest/rest/hello/matrix;name=Iulian%20Rotaru;age=58
		String[] requestParts = requestURI.split(";");
		for (int i = 1; i < requestParts.length; ++i) {
			int equalIndex = requestParts[i].indexOf('=');
			try {
				String name = URLDecoder.decode(requestParts[i].substring(0, equalIndex).trim(), "UTF-8");
				String value = URLDecoder.decode(requestParts[i].substring(equalIndex + 1).trim(), "UTF-8");
				parameters.put(name, value);
			} catch (Exception e) {
				log.error("Invalid matrix parameters |{http_request}|: {exception_class}: {exception_message}", requestURI, e.getClass().getSimpleName(), e.getMessage());
			}
		}
	}

	public String getParameter(String name) {
		return parameters.get(name);
	}
}
