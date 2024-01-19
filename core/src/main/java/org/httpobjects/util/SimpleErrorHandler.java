package org.httpobjects.util;

import org.httpobjects.DSL;
import org.httpobjects.ErrorHandler;
import org.httpobjects.HttpObject;
import org.httpobjects.Response;
import org.httpobjects.util.Method;

import java.util.UUID;

public class SimpleErrorHandler implements ErrorHandler {

    @Override
    public Response createErrorResponse(HttpObject next, Method m, Throwable t) {
        UUID errorId = UUID.randomUUID();
        System.err.println("Error " + errorId);
        t.printStackTrace();
        return DSL.INTERNAL_SERVER_ERROR(DSL.Text("There was an error.  This is error " + errorId));
    }
}
