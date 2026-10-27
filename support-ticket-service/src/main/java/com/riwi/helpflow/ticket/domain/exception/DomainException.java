package com.riwi.helpflow.ticket.domain.exception;

/** Base class for all business/domain exceptions. No HTTP semantics here. */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) { super(message); }
    protected DomainException(String message, Throwable cause) { super(message, cause); }
}
