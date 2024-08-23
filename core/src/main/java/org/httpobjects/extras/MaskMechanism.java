package org.httpobjects.extras;

import org.httpobjects.*;
import org.httpobjects.path.Path;
import org.httpobjects.path.PathParamName;
import org.httpobjects.path.PathPattern;
import org.httpobjects.util.HttpObjectUtil;
import org.httpobjects.util.Method;

import java.util.ArrayList;
import java.util.List;

public class MaskMechanism {

    public static final HttpObject mask(HttpObject thiss, HttpObject that) {
        return maskResources(thiss, that, DSL.NOT_FOUND());
    }

    public static final HttpObject mask(HttpObject thiss, HttpObject that, Representation notFound) {
        return maskResources(thiss, that, DSL.NOT_FOUND(notFound));
    }

    private static PathPattern maskPatterns(final PathPattern left,
                                            final PathPattern right) {
        return new PathPattern() {

            @Override
            public List<PathParamName> varNames() {
                List<PathParamName> result = new ArrayList<PathParamName>();
                result.addAll(left.varNames());
                result.addAll(right.varNames());
                return result;
            }

            @Override
            public boolean matches(String path) {
                return left.matches(path) || right.matches(path);
            }

            @Override
            public Path match(String path) {
                if (left.matches(path)) {
                    return left.match(path);
                } else {
                    return right.match(path);
                }
            }

            @Override
            public String raw() {
                return left.raw() + ":" + right.raw();
            }
        };
    }

    private static HttpObject maskResources(final HttpObject left,
                                            final HttpObject right,
                                            final Response notFound) {
        return new HttpObject(maskPatterns(left.pattern(), right.pattern())) {

            private Response match(Method method, Request req) {
                if (left.pattern().matches(req.path().toString())) {
                    return HttpObjectUtil.invokeMethod(left, method, req);
                } else if (right.pattern().matches(req.path().toString())) {
                    return HttpObjectUtil.invokeMethod(right, method, req);
                } else {
                    return notFound;
                }
            }

            @Override
            public Response delete(Request req) {
                return match(Method.DELETE, req);
            }

            @Override
            public Response get(Request req) {
                return match(Method.GET, req);
            }

            @Override
            public Response head(Request req) {
                return match(Method.HEAD, req);
            }

            @Override
            public Response options(Request req) {
                return match(Method.OPTIONS, req);
            }

            @Override
            public Response post(Request req) {
                return match(Method.POST, req);
            }

            @Override
            public Response put(Request req) {
                return match(Method.PUT, req);
            }

            @Override
            public Response trace(Request req) {
                return match(Method.TRACE, req);
            }

            @Override
            public Response patch(Request req) {
                return match(Method.PATCH, req);
            }
        };
    }

}
