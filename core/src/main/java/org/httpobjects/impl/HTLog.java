package org.httpobjects.impl;

public class HTLog {
    private final String context;

    public HTLog(String context) {
        this.context = context;
    }

    public HTLog(Object context) {
        this(context.getClass().getSimpleName());
    }

    public void debug(String m){
        logAtLevel("debug", m);
    }

    public void error(String m){
        logAtLevel("error", m);
    }

    public void error(String m, Throwable t){
        t.printStackTrace();
        logAtLevel("error", m);
    }

    public void info(String m){
        logAtLevel("info", m);
    }
    public void log(String m){
        info(m);
    }
    public void logAtLevel(String level, String m){
        System.out.println("[" + context + "/" + level + "] " + m);
    }
    public void logThrowable(Throwable t, String m){
        t.printStackTrace();
        log(t.getClass().getSimpleName() + " " + m);
    }
}