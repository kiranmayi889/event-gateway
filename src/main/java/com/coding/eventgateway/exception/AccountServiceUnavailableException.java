package com.coding.eventgateway.exception;

public class AccountServiceUnavailableException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AccountServiceUnavailableException(String message) {
        super(message);
    }

}