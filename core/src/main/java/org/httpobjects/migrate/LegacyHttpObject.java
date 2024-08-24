package org.httpobjects.migrate;

import org.httpobjects.DSL;
import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.path.PathPattern;
import org.httpobjects.path.SimplePathPattern;

/**
 * This class is one of 2 mechanisms to make it quicker for pre-1.0 code to migrate to the new (1.0) async API.
 *   - In this case, one can extend `LegacyHttpObject` instead of `HttpObject`, and then wrap it with the `LegacyHttpObjectAdapter` (or `LegacyHttpObject.async()`)
 *   - The other (preferred) mechanism is `SyncHttpObject`
 */
@Deprecated()
public class LegacyHttpObject extends DSL {

    private final PathPattern pathPattern;
    private final Response defaultResponse;

    public LegacyHttpObject(PathPattern pathPattern, Response defaultResponse) {
        super();
        this.pathPattern = pathPattern;
        this.defaultResponse = defaultResponse;
    }

    public LegacyHttpObject(String pathPattern, Response defaultResponse) {
        this(new SimplePathPattern(pathPattern), defaultResponse);
    }

    public LegacyHttpObject(PathPattern pathPattern) {
        this(pathPattern, METHOD_NOT_ALLOWED());
    }

    public LegacyHttpObject(String pathPattern) {
        this(new SimplePathPattern(pathPattern));
    }

    public PathPattern pattern() {
        return pathPattern;
    }


    public Response delete(Request req){return defaultResponse;}
    public Response get(Request req){return defaultResponse;}
    public Response head(Request req){return defaultResponse;}
    public Response options(Request req){return defaultResponse;}
    public Response post(Request req){return defaultResponse;}
    public Response put(Request req){return defaultResponse;}
    public Response trace(Request req){return defaultResponse;}
    public Response patch(Request req){return defaultResponse;}


    public HttpObject async(){
        return new LegacyHttpObjectAdapter(this);
    }
}
