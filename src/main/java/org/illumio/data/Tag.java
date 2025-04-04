package org.illumio.data;

public record Tag(String original) {

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Tag other) {
            return this.original.equalsIgnoreCase(other.original);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return original.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return original;
    }
}

