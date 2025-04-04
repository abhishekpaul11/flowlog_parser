package org.illumio.data;

import java.util.Objects;

public class PortProtocolPair {

    private int destinationPort;
    private String protocol;

    public PortProtocolPair(int destinationPort, String protocol) {
        this.destinationPort = destinationPort;
        this.protocol = protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortProtocolPair that = (PortProtocolPair) o;
        return this.destinationPort == that.destinationPort && this.protocol.equalsIgnoreCase(that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationPort, protocol);
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
