package com.jslib.container.servlet;

class CT {
	/** Name used for root context path. */
	public static final String ROOT_CONTEXT = "root";

	public static final String PARAMETER_PREVIEW_CONTEXT = "com.jslib.container.preview.context";

	/** Application name is loaded from web descriptor, <display-name> element. */
	public static final String LOG_APP_NAME = "app_name";
	/** Context name is deployed WAR archive name and is part of HTTP request URI. */
	public static final String LOG_CONTEXT_NAME = "context_name";
	public static final String LOG_SERVLET_NAME = "sevlet_name";
	public static final String LOG_REMOTE_HOST = "remote_host";
	public static final String LOG_SESSION_ID = "session_id";
	public static final String LOG_TRACE_ID = "trace_id";
	public static final String LOG_TRACE_TIMESTAMP = "trace_timestamp";
}
