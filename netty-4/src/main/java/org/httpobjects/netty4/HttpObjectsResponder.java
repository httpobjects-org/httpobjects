package org.httpobjects.netty4;

import java.net.URI;
import java.util.List;

import org.httpobjects.*;
import org.httpobjects.path.PathPattern;
import org.httpobjects.util.HttpObjectUtil;
import org.httpobjects.util.Method;

public class HttpObjectsResponder{
	private final ErrorHandler errorHandler;
	private final List<HttpObject> objects;
    private final Response defaultResponse = DSL.NOT_FOUND();

	public HttpObjectsResponder(List<HttpObject> objects, ErrorHandler errorHandler) {
		this.objects = objects;
		this.errorHandler = errorHandler;
	}

	public Response respond(RequestAccumulator request, ConnectionInfo connectionInfo) {

		final String uri = pathFromUri(request.beforeBody.uri());

		for(HttpObject next : objects){
		    final PathPattern pattern = next.pattern();
			if(pattern.matches(uri)){
				Method m = Method.fromString(request.beforeBody.getMethod().name());
				Response out;
				try{
					HttpObject match = next;
					Request in = Translate.readRequest(pattern, request, connectionInfo);
					out = HttpObjectUtil.invokeMethod(match, m, in).join();
				}catch(Throwable t){
					out = errorHandler.createErrorResponse(
							next,
							m,
							t);
				}
				if(out!=null) return out;
			}
		}

        return defaultResponse;
	}


	private String pathFromUri(String uri){
		try{
			return new URI(uri).getPath();
		}catch (Throwable t){
			t.printStackTrace();
			throw new RuntimeException("Unable to parse uri: " + uri, t);
		}
	}
}
