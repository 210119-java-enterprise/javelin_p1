package com.revature.exceptions;

public class TypeMismatchException extends RuntimeException {

    public TypeMismatchException() {
        super("Types are not convertable");
    }

    public TypeMismatchException(String message) {
        super(message);
    }
    
}
