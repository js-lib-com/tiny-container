/**
 * HTTP-RMI and W3C events stream servlets. Net package uses networking services provided by servlet container. 
 * 
 * <p>
 * This package implements a lite version of remote method invocation using HTTP for transport. Also
 * provides support logic for W3C Server-Sent Events. Using HTTP for transport is a natural choice. HTTP is a lingua franca; it passes
 * through firewalls and proxies and is hard to imagine a network that does not allow this protocol to go out.
 * 
 * <h3 id="http-rmi">HTTP-RMI, Hyper Text Remote Invocation Protocol</h3>
 * HTTP-RMI is a remote method invocation protocol using HTTP as transport. This package provides server side incoming requests 
 * handler, see {@link js.tiny.container.net.HttpRmiServlet}; usually a HTTP-RMI request uses <b>rmi</b> extension. HTTP-RMI can be used to connect 
 * JavaScript client logic via AJAX requests. Also using this library one can connect two Java processes via the same HTTP-RMI 
 * protocol. Anyway, because it is a text protocol ending points language is not really relevant.
 * <p> 
 * It uses <code>requestURI</code> to convey class and method names and HTTP message body for method parameters and returned value.
 * Current implementation supports both objects and bytes streams for both parameters and returned value.
 * Objects are always encoded <b>application/json</b> and streams <b>application/octet-stream</b>.
 * <p>
 * From protocol specification perspective HTTP request method is not relevant but current server implementation uses <code>POST</code> if there is content 
 * to send and <code>GET</code> otherwise. This depart from REST concept that promote HTTP verbs to convey meaning about request. To conclude, 
 * here is HTTP-RMI protocol description.
 * 
 * <h3>HTTP-RMI Protocol Description</h3>
 * HTTP-RMI has the same grammar as HTTP but with a syntax constrain and slightly different semantic. The constrain is on request start line
 * that uses HTTP request URI for qualified method name. One semantic difference is that HTTP message without <code>Content-Type</code>
 * header is considered <code>application/octet-stream</code> whereas HTTP-RMI consider it <code>application/json</code>. Another semantic
 * difference is on HTTP-RMI response status code, see <em>HTTP-RMI Response</em> section below. Please note that a message may contain
 * additional headers required by HTTP itself like <code>Host</code> used for Apache virtual host.
 * <pre>
 * http-rmi-message = request | response
 * 
 * request = request-start-line CRLF
 *           *(request-header CRLF)
 *           CRLF
 *           [ request-body ]
 *          
 * request-start-line = "POST" SP [ "/" context ] "/" class-name "/" method-name "." extension SP VER
 * context = &lt; application context name &gt;
 * class-name = &lt; qualified class name using / instead of . &gt;
 * method-name = &lt; method name &gt;
 * extension = "rmi"
 *           
 * request-header = [ content-type ]  
 *                | [ content=length ]
 * content-type = "application/json" | "application/octet-stream"
 * content-length = &lt; content length &gt;
 * 
 * request-body = &lt; actual parameters list encoded accordingly content type header &gt;
 * 
 * response = response-start-line CRLF
 *            *(response-header CRLF)
 *            CRLF
 *            [ response-body ]
 * 
 * response-start-line = VER SP status-code SP reason CRLF
 * status-code = 200 | 400 | 401 | 403 | 404 | 500
 * reason = &lt; explanatory text - inherited from HTTP but not used in HTTP-RMI &gt;
 * 
 * response-header = [ content-type ]
 *                 | [ content-length ]
 * content-type = "application/json" | "application/octet-stream"
 * content-length = &lt; content length &gt;
 *                  
 * response-body = &lt; return value encoded accordingly content type header &gt;
 * 
 * VER = "HTTP/1.1"
 * SP = " "
 * CRLF = "\r\n"
 * </pre>
 * <p>
 * Because of dynamically typed scripting languages HTTP does not transport parameter types and because compiled languages does
 * not preserve parameter names into the binary, protocol is not able to transport parameter name. For these reasons HTTP-RMI
 * transport parameters ordered by position and instance data only. It is caller responsibility to ensure that parameters order
 * and type are correct.
 * 
 * <h3>HTTP-RMI Request</h3>
 * Below is a sample HTTP-RMI request, as serialized on the wire. The target method is <code>js.bbnet.hub.PushNotificationService#push</code>
 * and it has a single argument, a notification instance. Class and method names are carried out by request URI whereas parameter is 
 * serialized JSON into request body. Please note that even if a single parameter it is encoded as an array, the actual parameters list.
 * In this sample, application context is <code>app</code>. 
 * <pre>
 *  POST /app/js/bbnet/hub/PushNotificationService/push.rmi HTTP/1.1CRLF
 *  Content-Type: application/jsonCRLF
 *  Content-Length: 61CRLF
 *  CRLF
 *  [{"id":1,"title":"Intelihouse","text":"Kitchen smoke sensor."}]
 * </pre>
 * Message body contains remote method parameters list, ordered by position. It is caller responsibility to send parameters in the number 
 * and order required by remote method signature.
 * <p>
 * HTTP specifications does not mandate <code>Content-Type</code> header, server logic being required to consider it as 
 * <code>application/octet-stream</code>. HTTP-RMI server implementation departs a little and consider <code>application/json</code>
 * as default, in the case content type is not supplied. This way, a simplified HTTP-RMI request may look like:
 * <pre>
 *  POST /app/js/bbnet/hub/PushNotificationService/push.rmi HTTP/1.1CRLF
 *  Content-Length: 61CRLF
 *  CRLF
 *  [{"id":1,"title":"Intelihouse","text":"Kitchen smoke sensor."}]
 * </pre>
 * 
 * <h4>HTTP-RMI for Remote Method with no Formal Parameters.</h4>
 * HTTP specs does not mandate <code>Content-Length</code> header if there is no message body. Considering this, if the remote method about 
 * to be invoked has no formal parameters HTTP-RMI request is reduced to:
 * <pre>
 *  POST /app/js/bbnet/hub/PushNotificationService/reset.rmi HTTP/1.1CRLF
 *  CRLF
 * </pre> 
 * 
 * <h3>HTTP-RMI Response</h3>
 * HTTP-RMI response convey back to client information about overall method execution state and method return value, if any, encoded into 
 * response body. Current implementation recognizes next response status codes:
 * <ul>
 * <li>200 - method executed successfully, body may contain method returned value encoded accordingly response <code>Content-Type</code>,
 * <li>400 - business constrain broken, body contains {@link js.rmi.BusinessException} instance encoded <code>application/json</code>,
 * <li>401 - not authorized,
 * <li>403 - server understood the request but actively refuses to process it; common cause may be Tomcat filtering by remote address,
 * <li>404 - resource not found may occur if front end HTTP server is not configured correctly or trying to use incorrect protocol, e.g. 
 * attempt to access securely a public method,
 * <li>500 - execution fail, body contains {@link js.rmi.RemoteException} instance encoded <code>application/json</code>.
 * </ul>
 * Response <code>Content-Type</code> header should be set to reflect used encoding. As with request, current implementation uses 
 * <code>application/json</code> and <code>application/octet-stream</code>. 
 * If response content type is <code>application/json</code> HTTP-RMI does not use <code>Transfer-Encoding</code>, specifically does not 
 * support chunked transfer, so always include <code>Content-Length</code>.
 * 
 * <h4>HTTP-RMI Response for Valid Return Value</h4>
 * Method return value, in current implementation, can be sent using JSON or octet-stream encoding. Response status code is 200, successful 
 * execution. For streams <code>Content-Length</code> can miss in which case <code>Transfer-Encoding</code> is <code>chunked</code>. 
 * <pre>
 *  HTTP/1.1 200CRLF
 *  Content-Type: application/jsonCRLF
 *  Content-Length: 44CRLF
 *  CRLF
 *  {"id":1,"firstName":"John","lastName":"Doe"}
 * </pre>
 * <pre>
 *  HTTP/1.1 200CRLF
 *  Content-Type: application/octet-streamCRLF
 *  Content-Length: 78CRLF
 *  CRLF
 *  &lt;stream of bytes&gt;
 * </pre>
 * 
 * <h4>HTTP-RMI Response for Exception</h4>
 * If method execution fails {@link js.tiny.container.net.HttpRmiServlet} creates an exception transport instance, see {@link js.rmi.RemoteException}, and
 * initialize it with target exception class name and message. Then sent it back to client using JSON encoding and response status code 500,
 * server internal error. 
 * <pre>
 *  HTTP/1.1 500CRLF
 *  Content-Type: application/jsonCRLF
 *  Content-Length: 70CRLF
 *  CRLF
 *  {"cause":"js.db.PersistenceException","message":"Connection timeout."}
 * </pre>
 * 
 * <h4>Void HTTP-RMI Response</h4>
 * HTTP-RMI response for void methods has no <code>Content-Type</code> but <code>Content-Length</code> should be 0 and of course response 
 * body should be empty. HTTP specs does not strictly require <code>Content-Length</code> response header; if is missing client should read 
 * till server closes the connection. In order to help client implementation HTTP-RMI requires <code>Content-Length</code> response header
 * even for void return values. Finally, response status code is 200 since it is a successful invocation.
 * <pre>
 *  HTTP/1.1 200CRLF
 *  Content-Length: 0CRLF
 *  CRLF
 * </pre>
 * 
 * <h4>NULL HTTP-RMI Response</h4>
 * NULL return value is always send back using JSON encoding and 200 response status code because it is a valid execution.
 * <pre>
 *  HTTP/1.1 200CRLF
 *  Content-Type: application/jsonCRLF
 *  Content-Length: 4CRLF
 *  CRLF
 *  null
 * </pre>
 * 
 * <h3>HTTP-RMI Client</h3>
 * This section describe HTTP-RMI usage from client point of view. Here are covered only Java and JavaScript languages, but based on principles
 * described here and on <em>HTTP-RMI Protocol Description</em> section porting to other languages should be reasonable easy. 
 * 
 * <h4>HTTP-RMI Java Client</h4>
 * Here is described a HTTP-RMI client running in a web application powered by j(s)-lib Server library. This is the most likely to be used 
 * scenario when client is written in Java language.
 * <p>
 * First we need to acquire a remote service instance. For this we need to know the URL of the context where service is deployed and uses
 * global {@link js.tiny.container.core.Factory}, as in snippet below. The returned instance is in fact a Java Proxy that implements remote service interface
 * and delegates {@link js.net.client.HttpRmiTransactionHandler} as invocation handler.
 * <pre>
 *  URL implementationURL = "http://services.bbnet.ro";
 *  WeatherService service = Factory.getInstance(implementationURL, WeatherService.class);
 * </pre>
 * Once we have service instance we simply invoke the method using language means. The actual remote invocation can occur synchronously, when
 * caller thread is blocked till invocation completion.   
 * <pre>
 *  Weather weather = service.getCurrentWeather(47.1569, 27.5903);
 * </pre>
 * The second option is to execute it asynchronously when we provide a {@link js.lang.Callback} and return immediately. After remote invocation 
 * completion callback <code>handle</code> method is executed.
 * <pre>
 *  service.getCurrentWeather(47.1569, 27.5903, new Callback(Weather) {
 *      void handle(Weather weather) {
 *      }
 *  });
 * </pre>
 * No matter which variant caller may choose, when service method is called, {@link js.net.client.HttpRmiTransactionHandler#invoke(Object, java.lang.reflect.Method, Object[])}
 * is executed. It takes care to create {@link js.net.client.HttpRmiTransaction} instance and delegates it for actual HTTP-RMI transaction execution.
 * Note that {@link js.net.client.HttpRmiTransaction} is always synchronous. If {@link js.lang.Callback} was supplied to service method invocation, 
 * {@link js.net.client.HttpRmiTransactionHandler} uses {@link js.lang.AsyncTask} to wrap synchronous {@link js.net.client.HttpRmiTransaction} execution.   
 * 
 * <h3>Remote Service Client</h3>
 * Lets say on context <em>http://services.bbnet.ro</em> we have deployed <code>js.bbnet.services.WeatherService</code> service, which has 
 * its implementation, of course, on the same context.
 * <pre>
 *  package js.bbnet.service;
 *  
 *  {@literal @}Remote
 *  public interface WeatherService {
 *      Weather getCurrentWeather(double latitude, double longitude);
 *  }
 *  
 *  public class WeatherServiceImpl interface WeatherService {
 *      public Weather getCurrentWeather(double latitude, double longitude) {
 *          // service implementation
 *      }
 *  }
 * </pre>
 * On client side, in order to create the Java Proxy instance of the remote service we need the service interface. There are two means to 
 * invoke the service method, asynchronously or not, and we need to provide user code from client with both methods. Is obvious that the 
 * overload with <code>callback</code> parameter is executed asynchronously. Note that the <code>callback</code> parameter, if present, 
 * should be the last.
 * <pre>
 *  package js.bbnet.service;
 *  
 *  public interface WeatherService {
 *      Weather getCurrentWeather(double latitude, double longitude);
 *      
 *      void getCurrentWeather(double latitude, double longitude, Callback&lt;Weather&gt; callback);
 *  }
 * </pre>
 * Now, the logic from client can create its own service interface, as above - but be aware to respect the original package even if not part
 * of client code base package root. Anyway, it is a good practice for service implementor to provide the interface, transport objects and
 * service specific exceptions, if any declared by remote method signature. In order to avoid conflict with its own local interface
 * service implementation uses a service provider package. By convention service provider package is a sub-package of service interface 
 * package, with the name <code>client</code>. Please note the package name from below sample code.
 * <pre>
 *  package js.bbnet.service.client;
 *  
 *  public interface WeatherService {
 *      Weather getCurrentWeather(double latitude, double longitude);
 *      
 *      void getCurrentWeather(double latitude, double longitude, Callback&lt;Weather&gt; callback);
 *  }
 *  . . .
 *  public class Weather {
 *  } 
 * </pre>
 * Above service interface and transport object class is archived and distributed by service manufacturer. Client code needs only to include
 * the JAR in its class path. This solution is not only convenient but it solves parameters oder limitation - HTTP-RMI protocol transport
 * method parameters ordered by position and is client code responsibility to ensure order and types correctness. 
 * 
 * <h4>Synchronous Mode</h4>
 * Usually, HTTP-RMI transactions are executed asynchronous in a separated execution thread; actually current implementation uses {@link js.lang.AsyncTask}.
 * If remote method does return value caller should provide a callback to get it.  
 * <pre>
 *  service.getCurrentWeather(47.1569, 27.5903, new Callback(Weather) {
 *      void handle(Weather weather) {
 *      	// handle remote method returned value
 *      }
 *  });
 * </pre>
 * This asynchronous usage pattern may not be desirable if caller should invoke a number of remote methods in sequence like in snippet below.
 * <pre>
 *  DirtyFiles dirtyFiles = buildManager.getDirtyFiles(productionDir);
 *  buildManager.synchronize(productionDir, dirtyFiles);
 * </pre>
 * For such cases HTTP-RMI supports synchronous mode, auto-magically enabled when remote method does return value and callback is not supplied
 * on remote invocation. There is no way to enable synchronous mode for a void remote method; workaround is to change remote method signature
 * to return a boolean always true. Caller should be aware that synchronous mode blocks caller execution thread till remote method execution completes.
 * 
 * <h4>Exception Handling</h4>
 * HTTP-RMI is inherently complex and bad things can happen on caller machine, networking or remote machine. Local failing conditions are
 * somewhat under caller control and is free to handle local generated exception as may consider. Networking problems rise {@link java.io.IOException}.
 * Server side exception are propagated to client using {@link js.rmi.RemoteException}. Processing rule is simple: if remote service 
 * interface declare an exception this client implementation will propagate it to the caller. Otherwise throws unchecked {@link js.rmi.RmiException}.
 * <pre>
 *  try {
 *      service.method();
 *  }
 *  catch(ServiceException exception) {
 *      // handle service checked exception
 *  } 
 * </pre>
 * For asynchronous calls things are a little more complicated. Due to asynchronous execution in a separated thread is not possible to simple
 * catch remote invocation exception. Anyway, if one found himself in a desperate need to catch remote exceptions for a particular method only
 * there is an work around: call remote method synchronously and wraps into {@link js.lang.AsyncTask}. 
 * <pre>
 *  AsyncTask&lt;Object&gt; task = new AsyncTask&lt;Object&gt;() {
 *      protected Object execute() throws Throwable {
 *          try {
 *              service.method();
 *          }
 *          catch(ServiceException exception) {
 *              // handle service checked exception
 *          } 
 *      }
 *  };
 *  task.start();
 * </pre>
 * Above solution may be of help only when a specific exception handling for a particular remote method is desired. For application global 
 * asynchronous exception handling one can use {@link js.lang.AsyncExceptionListener}.
 * 
 * <h4>Business Exception Handling</h4>
 * Business exceptions are constrains imposed by business logic that cannot be enforced on client. Under normal conditions data is validated
 * by client logic and reach the server side normalized. Anyway, there may be complex business constrains between data items, data that is 
 * persisted on server. In order to avoid server communication delays and adding complexity to client data validation logic, this library uses
 * {@link js.rmi.BusinessException} to signal a constrain is not fulfilled. Client side logic will display an alert based on business 
 * exception error code.
 * <p>
 * Business exception instance is sent back to client code with HTTP status code 400 - HttpServletResponse.SC_BAD_REQUEST and JSON 
 * serialization. For sample code see above <em>Exception Handling</em> replacing <em>ServiceException</em> with <em>BusinessException</em>.
 * 
 * <h3>Java HttpURLConnection Client</h3>
 * For Java applications that does not benefit from j(s)-lib Server library one can write an HTTP-RMI client using raw HTTP connection class
 * supplied by JRE. A simplified sample code is listed below. Note that implementation should take care of parameters proper types and order
 * and that parameters list are always an array, even if with a single item. Also it should handle returned value, including exceptions.
 * <pre>
 *  URL request = new URL("http://services.bbnet.ro/js/bbnet/services/WeatherService/getCurrentWeather.rmi");
 *  HttpURLConnection connection = (HttpURLConnection)request.openConnection();
 *  connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
 *  
 *  JSON.stringify(connection.getOutputStream(), new Object[] {47.1569, 27.5903});
 *  
 *  switch(connection.getResponseCode()) {
 *      case 200:
 *          Weather weather = JSON.parse(connection.getInputStream(), Weather.class);
 *          break;
 *      case 400:
 *          BusinessException businessException = JSON.parse(connection.getErrorStream(), BusinessException.class);
 *          // alert user based on error code carried by this business exception
 *          break;
 *      case 401:
 *          // handle authorization
 *          break;
 *      case 404:
 *          // handle resource not found
 *          break;
 *      case 500:
 *          RemoteException remoteException = JSON.parse(connection.getErrorStream(), RemoteException.class);
 *          throw Classes.newInstance(remoteException.getCause(), remoteException.getMessage());
 *          break;
 *      default:
 *  }
 * </pre>
 * There is an implementation issue related to HttpURLConnection response handling. First we need to retrieve HTTP response status code using
 * connection.getResponseCode(). At this point HttpURLConnection perform the actual HTTP transaction: send request, wait for response and
 * parse headers and prepare body for access via a stream. Now, if status code is 200 HTTP response body is accessed via connection.getInputStream();
 * for all other codes uses instead connection.getErrorStream(). Trying to use  connection.getInputStream() when status code is not 200 will
 * rise IOException.
 *  
 * <h4>HTTP-RMI JavaScript Client</h4>
 * This HTTP-RMI client uses j(s)-lib Client library.
 * . . .
 * <pre>
 *  js.bbnet.services.WeatherService = {
 *      _REMOTE_CONTEXT_URL: "http://services.bbnet.ro/",
 *      
 *      getCurrentWeather: function(latitude, longitude) {
 *          var __callback__ = arguments[2];
 *          var __scope__ = arguments[3];
 *          
 *          var rmi = new js.net.RMI(this._REMOTE_CONTEXT_URL);
 *          rmi.setMethod("js.bbnet.services.WeatherService", "getCurrentWeather");
 *          rmi.setParameters(latitude, longitude);
 *          rmi.exec(__callback__, __scope__);
 *      }
 *  };
 * </pre>
 * <pre>
 *  js.bbnet.services.WeatherService.getCurrentWeather(47.1569, 27.5903, function(weather) {
 *      weatherView.setObject(weather);
 *  }, this);
 * </pre>
 * 
 * <h4>JavaScript W3C XHLHttpRequest Client</h4>
 * Consuming remote services via HTTP-RMI using W3C XMLHttpRequest is standard: create XHR instance, register event listener, open,
 * set request headers and send. When used as HTTP-RMI client there are couple issues to note:
 * <ul>
 * <li>HTTP method is always POST
 * <li>URL to open is fully qualified, that is, contains remote application context
 * <li>Remote method actual arguments are encoded into request body using specified content type 
 * </ul>  
 * Note that this solution is subject to same origin constrain and is usable with servers configured with CORS or from Smart TVs 
 * application based on JavaScript where cross domain requests are allowed. 
 * <pre>
 *  var xhr = new XMLHttpRequest();
 *  xhr.onreadystatechange = function() {
 *      if(xhr.readyState == 4) {
 *          var weather = JSON.parse(xhr.responseText);
 *          weatherView.setObject(weather);
 *      }
 *  };
 *  xhr.open("POST", "http://services.bbnet.ro/js/bbnet/services/WeatherService/getCurrentWeather.rmi", true);
 *  xhr.setRequestHeader("Content-Type", "application/json; charset=UTF-8");
 *  xhr.send(JSON.stringify([ 47.1569, 27.5903 ]));
 * </pre>
 * 
 * <h3>Server-Sent Events</h3>
 * Event stream format is that described by W3C Server-Sent Events, section 6. For your convenience below is a simplified grammar
 * description. Now is worth to note that <code>event</code> field value should be a valid event type name; accordingly DOM Events 
 * specs event type name has the same syntax as a XML name. This is important because a Java class name qualifies and can be used as
 * <code>event</code> field value.  
 * <pre>
 *  event-stream = *event
 *  event = *field CRLF
 *  field = name ":" value CRLF
 *  name = "event" / "data" / "id" /  "retry"
 * </pre>
 * This library implementation imposes couple constrains as described by below grammar. As a consequence, this library event reader is
 * not able to parse 3pty events streams; this is by design, in order to ensure consistent data on client side. Anyway, a 3pty source 
 * event, that is, event stream client is able to read events generated by this library server side logic, provided is willing to parse
 * JSON formatted <code>data</code> field value. 
 * <pre>
 *  event-stream = *event
 *  event = event-field data-field [retry-field] CRLF
 *  event-field = "event" ":" &lt; event qualified class name &gt; CRLF
 *  data-field = "data" ":" &lt; json encoded event instance &gt; CRLF
 *  retry-field = "retry" ":" &lt; retry timeout in milliseconds &gt; CRLF
 * </pre>
 * Here are listed this library constrains compared with W3C grammar:
 * <table summary="Grammar Constraints">
 * <tr>
 * <td><code>event</code> field cannot be empty, always contains event class
 * <td>if <code>event</code> is empty uses <code>Message</code> class
 * <tr>
 * <td><code>data</code> field value is encoded JSON
 * <td><code>data</code> field value is plain text
 * <tr>
 * <td><code>event</code> is always before <code>data</code> and <code>retry</code> last
 * <td>a particular fields order is not mandatory
 * <tr>
 * <td>if field name is not recognized throw exception
 * <td>ignore field if name is not recognized
 * <tr>
 * <td><code>id</code> field is not processed and throw exception
 * <td>uses <code>id</code> to set <code>Last-Event-ID</code> on reconnect
 * </table>
 * 
 * <h4>JavaScript W3C EventSource Client</h4>
 * In order to listen for a event of a particular class just user that class qualified name as first argument for <code>addEventListener</code>. 
 * <pre>
 *  var eventSource = new EventSource("http://hub.bbnet.ro/test.event");
 *  eventSource.addEventListener("Alert", function(ev) {
 *      message.innerHTML = "hub alert: " + ev.data;
 *  });
 * </pre>
 * @author Iulian Rotaru
 * @version draft
 */
package js.tiny.container.net;

