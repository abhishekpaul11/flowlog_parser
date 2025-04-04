package org.illumio.data;


public class FlowLog {

    private int version;
    private String accountId;
    private String eniId;
    private String sourceIp;
    private String destinationIp;
    private int sourcePort;
    private int destinationPort;
    private int protocol;
    private long packets;
    private long bytes;
    private long startTime;
    private long endTime;
    private Action action;
    private LogStatus logStatus;

    public FlowLog(int version, String accountId, String eniId, String sourceIp, String destinationIp,
                   int sourcePort, int destinationPort, int protocol, long packets,
                   long bytes, long startTime, long endTime, Action action, LogStatus logStatus) {
        this.version = version;
        this.accountId = accountId;
        this.eniId = eniId;
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.protocol = protocol;
        this.packets = packets;
        this.bytes = bytes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.action = action;
        this.logStatus = logStatus;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEniId() {
        return eniId;
    }

    public void setEniId(String eniId) {
        this.eniId = eniId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public int getProtocol() {
        return protocol;
    }

    public void setProtocol(int protocol) {
        this.protocol = protocol;
    }

    public long getPackets() {
        return packets;
    }

    public void setPackets(int packets) {
        this.packets = packets;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(int bytes) {
        this.bytes = bytes;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public LogStatus getLogStatus() {
        return logStatus;
    }

    public void setLogStatus(LogStatus logStatus) {
        this.logStatus = logStatus;
    }

    @Override
    public String toString() {
        return "FlowLog{" +
                "accountId='" + accountId + '\'' +
                ", eniId='" + eniId + '\'' +
                ", sourceIp='" + sourceIp + '\'' +
                ", destinationIp='" + destinationIp + '\'' +
                ", sourcePort=" + sourcePort +
                ", destinationPort=" + destinationPort +
                ", protocol=" + protocol +
                ", packets=" + packets +
                ", bytes=" + bytes +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", action='" + action + '\'' +
                ", logStatus='" + logStatus + '\'' +
                '}';
    }
}

