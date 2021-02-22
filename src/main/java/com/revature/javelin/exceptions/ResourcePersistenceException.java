package com.revature.javelin.exceptions;

/**
 * ResourcePersistenceExceptions are used when data is being
 * created in the database and conflicts with data already
 * in the database. Does not have a default message, and one must
 * be provided upon instantiation.
 */
public class ResourcePersistenceException extends RuntimeException {

    public ResourcePersistenceException(String message) {
        super(message);
    }
}
