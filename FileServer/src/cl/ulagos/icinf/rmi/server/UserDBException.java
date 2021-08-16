package cl.ulagos.icinf.rmi.server;

public class UserDBException extends Exception {

	public UserDBException() {
		super();
	}

	public UserDBException(String message) {
		super(message);
	}

	public UserDBException(String message, Throwable cause) {
		super(message, cause);
	}

}
