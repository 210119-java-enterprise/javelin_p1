package com.revature.javelin.exceptions;

public class InvalidColumnsException extends RuntimeException {

    public InvalidColumnsException() {
        super("No such column found");
    }

    public InvalidColumnsException(String message) {
        super(message);
    }
    
}
