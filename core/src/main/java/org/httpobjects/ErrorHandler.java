package org.httpobjects;

import org.httpobjects.util.Method;

public interface ErrorHandler {
    Response createErrorResponse(HttpObject next, Method m, Throwable t);
}
