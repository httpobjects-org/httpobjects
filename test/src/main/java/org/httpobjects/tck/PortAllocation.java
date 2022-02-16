package org.httpobjects.tck;

public class PortAllocation {
    public final int port;
    public final int allocationNum;

    public PortAllocation(int port, int allocationNum) {
        this.port = port;
        this.allocationNum = allocationNum;
    }

    @Override
    public String toString() {
        return "PortAllocation{" +
                "port=" + port +
                ", allocationNum=" + allocationNum +
                '}';
    }
}
