package com.infomaximum.database.exeption;

/**
 * Created by kris on 06.09.17.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Throwable cause) {
        super(cause);
    }
}

