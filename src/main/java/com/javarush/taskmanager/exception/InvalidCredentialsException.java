package com.javarush.taskmanager.exception;

import com.javarush.taskmanager.util.ExceptionMessages;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super(ExceptionMessages.INVALID_CREDENTIALS_MSG);
    }
}