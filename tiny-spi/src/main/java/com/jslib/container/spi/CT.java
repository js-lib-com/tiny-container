package com.jslib.container.spi;

public class CT {
	/** Name used for root context path. */
	public static final String ROOT_CONTEXT = "root";

	public static final String PARAMETER_PREVIEW_CONTEXT = "com.jslib.container.preview.context";

	/** Application name is loaded from web descriptor, <display-name> element. */
	public static final String LOG_APP_NAME = "app_name";
	/** Application context name is deployed WAR archive name and is part of HTTP request URI. */
	public static final String LOG_CONTEXT_NAME = "context_name";
	/** Name of a generic service, like servlet or timer task; should be unique on application scope. */
	public static final String LOG_SERVICE_NAME = "service_name";
	public static final String LOG_REMOTE_HOST = "remote_host";
	public static final String LOG_SESSION_ID = "session_id";
	public static final String LOG_TRACE_ID = "trace_id";
	public static final String LOG_TRACE_TIMESTAMP = "trace_timestamp";
}
