package com.infomaximum.database.exception;

public class SequenceNotFoundException extends DatabaseException {

    public SequenceNotFoundException(String name) {
        super("Sequence " + name + " not found.");
    }
}
