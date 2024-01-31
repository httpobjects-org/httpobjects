package org.httpobjects.util;

import org.httpobjects.HttpObject;

public class PathMapEntry {
    public final String pathPattern;
    public final HttpObject object;

    public PathMapEntry(String pathPattern, HttpObject object) {
        this.pathPattern = pathPattern;
        this.object = object;
    }
}