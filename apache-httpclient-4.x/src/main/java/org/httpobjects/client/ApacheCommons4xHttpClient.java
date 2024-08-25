package org.httpobjects.client;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.httpobjects.Representation;
import org.httpobjects.Response;
import org.httpobjects.ResponseCode;
import org.httpobjects.header.GenericHeaderField;
import org.httpobjects.header.HeaderField;
import org.httpobjects.impl.fn.Fn;
import org.httpobjects.impl.fn.FunctionalJava;
import org.httpobjects.impl.fn.Seq;
import org.httpobjects.representation.ImmutableRep;
import org.httpobjects.util.HttpObjectUtil;

public final class ApacheCommons4xHttpClient implements HttpClient {
	private final org.apache.http.client.HttpClient client;

	public ApacheCommons4xHttpClient() {
		this(HttpClientBuilder.create().disableRedirectHandling().build());
	}

	public ApacheCommons4xHttpClient(org.apache.http.client.HttpClient client) {
		super();
		this.client = client;
	}

	private final HttpResponse execute(HttpUriRequest request){
		try {
			return client.execute(request);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RemoteObject resource(final String uri) {
		return new RemoteObject() {

			@Override
			public Response trace(Representation r, String query, HeaderField ... fields) {
				return doit(query, r, fields, "trace", uri);
			}

			@Override
			public Response put(Representation r, String query, HeaderField ... fields) {
				return doit(query, r, fields, "put", uri);
			}

			@Override
			public Response post(Representation r, String query, HeaderField ... fields) {
				return doit(query, r, fields, "post", uri);
			}
			@Override
			public Response patch(Representation r, String query, HeaderField ... fields) {
				return doit(query, r, fields, "patch", uri);
			}

			@Override
			public Response options(Representation r, String query, HeaderField ... fields) {
				return doit(query, r, fields, "options", uri);
			}

			@Override
			public Response head(Representation r, String query, HeaderField ... fields) {
				return doit(query, r, fields, "head", uri);
			}

			@Override
			public Response get(Representation r, String query, HeaderField ... fields) {
				return doit(query, r, fields, "get", uri);
			}

			@Override
			public Response delete(Representation r, String query, HeaderField ... fields) {
				return doit(query, r, fields, "delete", uri);
			}
		};
	}

	private Response doit(String query, Representation r, HeaderField[] fields, final String method, final String uri) {
		return translate(execute(translate(method, uri, query, r, fields)));
	}

	/**
	 * This is missing in apache client 4.0
	 */
	private final class GenericHttpRequest extends HttpEntityEnclosingRequestBase {
		private final String method;
		private GenericHttpRequest(final String method, final String uri) {
			this.method = method.toUpperCase();
			setURI(URI.create(uri));
		}

		@Override
		public String getMethod() {
			return method;
		}
	}

	private Response translate(org.apache.http.HttpResponse apache) {
		final ResponseCode code = ResponseCode.forCode(apache.getStatusLine().getStatusCode());
		final Seq<HeaderField> headerFields = FunctionalJava.map(asList(apache.getAllHeaders()), new Fn<org.apache.http.Header, HeaderField>() {
			@Override
			public HeaderField exec(org.apache.http.Header in) {
				return new GenericHeaderField(in.getName(), in.getValue());
			}
		});

		final HttpEntity apacheBody = apache.getEntity();
		final Representation representation;
		if(apacheBody==null){
			representation = null;
		}else{
			representation = translate(apacheBody, 0);
		}

		return new Response(code, representation, headerFields.toList().toArray(new HeaderField[]{}));
	}

	private Representation translate(final HttpEntity apache, int tries) {
		try {
			String contentType = apache.getContentType() == null ? null :
					apache.getContentType().getValue();
			InputStream content = apache.getContent();
			return new ImmutableRep(contentType, content);
		} catch (IOException err) {
			if (tries > 10) throw new RuntimeException(err);
			else return translate(apache, tries + 1);
		}
	}

	private org.apache.http.client.methods.HttpUriRequest translate(String method, String uri, String query, Representation r, HeaderField[] fields) {
		final GenericHttpRequest in = new GenericHttpRequest(method, uri + query);

		for(HeaderField field: fields){
			in.addHeader(field.name(), field.value());
		}

		if(r!=null && in instanceof HttpEntityEnclosingRequest){
			((HttpEntityEnclosingRequest)in).setEntity(translate(r));
		}

		return in;
	}

	private AbstractHttpEntity translate(final Representation representation) {
		final AbstractHttpEntity entity = new ByteArrayEntity(representation.data().readToMemoryUnbounded());

		entity.setContentType(representation.contentType());

		return entity;
	}
}
