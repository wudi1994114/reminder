package com.core.reminder.service;

public class FileTooLargeException extends IllegalArgumentException {

    public FileTooLargeException(String message) {
        super(message);
    }
}
