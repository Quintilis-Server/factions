package org.quintilis.factions.exceptions;

public class ConfigFileNullValueException extends Exception {
    public ConfigFileNullValueException(String value) {
        super("The value " + value + " is null.");
    }
}
