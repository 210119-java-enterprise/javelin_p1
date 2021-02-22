package com.revature.javelin.exceptions;

public class InvalidQueryException extends RuntimeException {

    public InvalidQueryException() {
        super("No query found");
    }

    public InvalidQueryException(String message) {
        super(message);
    }
    
}
