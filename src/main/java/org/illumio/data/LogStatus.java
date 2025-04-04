package org.illumio.data;

public enum LogStatus {
    OK("OK"),
    NODATA("NODATA"),
    SKIPDATA("SKIPDATA");

    private final String status;

    LogStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status;
    }
}
