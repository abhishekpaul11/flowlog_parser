package org.illumio.data;

public enum Action {
    ACCEPT("ACCEPT"),
    REJECT("REJECT");

    private final String action;

    Action(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return action;
    }
}
