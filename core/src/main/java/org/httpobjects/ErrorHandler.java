package org.httpobjects;

import org.httpobjects.eventual.Eventual;
import org.httpobjects.util.Method;

public interface ErrorHandler {
    Eventual<Response> createErrorResponse(HttpObject next, Method m, Throwable t);
}
