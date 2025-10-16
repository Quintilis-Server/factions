package org.quintilis.factions.exceptions;

public class ConfigFileNullValueException extends RuntimeException {
    public ConfigFileNullValueException(String value) {
        super("The value " + value + " is null.");
    }
}
