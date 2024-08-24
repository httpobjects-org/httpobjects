package org.httpobjects.netty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.httpobjects.ConnectionInfo;
import org.httpobjects.DSL;
import org.httpobjects.HttpObject;
import org.httpobjects.Query;
import org.httpobjects.Representation;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.eventual.Eventual;
import org.httpobjects.header.HeaderField;
import org.httpobjects.header.request.AuthorizationField;
import org.httpobjects.header.request.RequestHeader;
import org.httpobjects.netty.http.ByteAccumulator;
import org.httpobjects.netty.http.HttpChannelHandler;
import org.httpobjects.path.Path;
import org.httpobjects.path.PathPattern;
import org.httpobjects.representation.ImmutableRep;
import org.httpobjects.util.HttpObjectUtil;
import org.httpobjects.util.Method;
import org.jboss.netty.handler.codec.http.HttpChunkTrailer;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;

public class NettyHttpobjectsRequestHandler implements HttpChannelHandler.RequestHandler {
	private final List<HttpObject> objects;
    private final Response defaultResponse = DSL.NOT_FOUND();

	public NettyHttpobjectsRequestHandler(List<HttpObject> objects) {
		super();
		this.objects = objects;
	}

	@Override
	public Response respond(HttpRequest request, HttpChunkTrailer lastChunk, ByteAccumulator body, ConnectionInfo connectionInfo) {

		final String uri = pathFromUri(request.getUri());

		for(HttpObject next : objects){
		    final PathPattern pattern = next.pattern();
			if(pattern.matches(uri)){
				HttpObject match = null;
				match = next;
				Request in = readRequest(pattern, request, lastChunk, body, connectionInfo);
				Method m = Method.fromString(request.getMethod().getName());
				Eventual<Response> out = HttpObjectUtil.invokeMethod(match, m, in);
				if(out!=null) return out.join();
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

	private Request readRequest(final PathPattern pathPattern, final HttpRequest request, final HttpChunkTrailer lastChunk, final ByteAccumulator body, final ConnectionInfo connectionInfo) {
		return new Request(){

			@Override
			public Method method() {
				return Method.fromString(request.getMethod().toString().toUpperCase());
			}

			@Override
			public boolean hasRepresentation() {
			    return body!=null;
			}

			@Override
			public ConnectionInfo connectionInfo() {
			    return connectionInfo;
			}

			@Override
			public RequestHeader header() {
				List<HeaderField> results = new ArrayList<HeaderField>();
				final HttpHeaders headers = request.headers();
				for(String name: headers.names()){
					for(String value: headers.getAll(name)){
						results.add(HeaderField.parse(name, value));
					}
				}
				return new RequestHeader(results){
					@Override
					public AuthorizationField authorization() {
						final String value = headers.get("Authorization");
						if (value == null) return null;
						return new AuthorizationField(value);
					}
				};
			}

			@Override
			public Request immutableCopy() {
				return this;
			}

			@Override
			public Path path() {
			    return pathPattern.match(jdkURL().getPath());
			}

			private URL jdkURL(){
                try {
                    return new URL("http://foo" + request.getUri());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }

			}

			@Override
			public Query query() {
			    return new Query(jdkURL().getQuery());
			}

			@Override
			public Representation representation() {
				String contentType = request.headers().get("ContentType");
				InputStream data = body != null ? body.toStream() :
						new ByteArrayInputStream("".getBytes());
				return new ImmutableRep(contentType, data);
			}
		};
	}
}
