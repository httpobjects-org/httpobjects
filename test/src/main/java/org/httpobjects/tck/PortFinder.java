package org.httpobjects.tck;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PortFinder {
    private static Set<Integer> usedPorts = new HashSet<Integer>();
    public static Set<Integer> usedPorts(){
        return Collections.unmodifiableSet(PortFinder.usedPorts);
    }
    public static int findFreePort(){
        return allocateFreePort(null).port;
    }

    private static void log(String m){
        System.err.println("[" + PortFinder.class.getSimpleName()  +"] " + m);
    }

    /**
     * WARNING: there is no guarantee that the port is still free after this has returned ... so, if your code makes that assumption, there is an inherent race condition.
     * Finds a port that was free at the time of allocation.  Tries to find ports that the underlying system wants us to use.
     * The availablility of the allocated ports is verified by starting and stopping a server on that port.
     */
    public synchronized static PortAllocation allocateFreePort(Object context){
        if(usedPorts.isEmpty()){
            log("WARNING: no existing ports found.  Is this really the first one?");
        }
        Integer allocatedPort = null;
        int attemptNum = 0;
        while(allocatedPort==null){
            if(attemptNum>10){
                throw new RuntimeException("Couldn't find a port after " + attemptNum + " attempts");
            }
            attemptNum++;
            try {
                final int port = useSystemToFindCandidatePort();
                if(usedPorts.contains(port)){
                    log("WARNING: this port was already allocated: " + port);
                }else{
                    startAndStopServerOnPort(port);

                    if(waitForPortToBeUnreachable(port)){
                        allocatedPort = port;
                    }

                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        usedPorts.add(allocatedPort);
        final PortAllocation allocation = new PortAllocation(allocatedPort, usedPorts.size());
        log("Allocation # " + allocation.allocationNum + " for " + (context == null ? "anonymous" : context.getClass().getSimpleName()) + ":" + allocation);
        return allocation;
    }

    private static void startAndStopServerOnPort(Integer port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        port = serverSocket.getLocalPort();
        serverSocket.setSoTimeout(0);
        serverSocket.close();

        acceptAllConnections(serverSocket);

        if(!serverSocket.isClosed()){
            throw new RuntimeException("Somehow, I'm still bound to port " + port);
        }
    }

    /**
     * Experience shows that, while this should in-theory result in a port that was free, it doesn't always work
     */
    private static Integer useSystemToFindCandidatePort() throws IOException {
        Integer port;
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.setSoTimeout(0);
        serverSocket.close();

        acceptAllConnections(serverSocket);

        if(!serverSocket.isClosed()){
            throw new RuntimeException("Somehow, I'm still bound to port " + port);
        }
        return port;
    }

    private static void acceptAllConnections(ServerSocket serverSocket) {
        new Thread(){
            @Override
            public void run() {
                while(!serverSocket.isClosed()){
                    try {
                        serverSocket.accept();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private static boolean waitForPortToBeUnreachable(Integer port) throws InterruptedException {
        boolean isReachable = true;

        int attemptNum = 0;
        while(isReachable && attemptNum < 3){
            attemptNum ++;
            try {
                Socket socket = new Socket("127.0.0.1", port);
                socket.close();
                isReachable = true;
                log("WARNING: this port not yet free (attempt #" + attemptNum + "): " + port);
                Thread.sleep(100);
            } catch (IOException e) {
//                log("INFO: couldn't connect to port " + port + "; assuming this means it's free.  Exception was " + e.getClass().getSimpleName() + ": " + e.getMessage());
                isReachable = false;
            }
        }
        if(isReachable){
            log("WARNING: gave up on port " + port + " after " + attemptNum + " attempts");
        }
        return !isReachable;
    }
}
