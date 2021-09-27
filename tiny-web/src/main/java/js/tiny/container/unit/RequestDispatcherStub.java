package js.tiny.container.unit;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class RequestDispatcherStub implements RequestDispatcher {
	@Override
	public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		throw new UnsupportedOperationException("forward(ServletRequest, ServletResponse)");
	}

	@Override
	public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		throw new UnsupportedOperationException("include(ServletRequest, ServletResponse)");
	}
}
