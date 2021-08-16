package cl.ulagos.icinf.rmi.server;

/**
 * Excepción arrojada si el usuario no existe.
 * 
 * @author Nicolás Stuardo
 */
public class UserNotExistsException extends Exception {

	public UserNotExistsException() {
	}

	public UserNotExistsException(String message) {
		super(message);
	}

	public UserNotExistsException(Throwable cause) {
		super(cause);
	}

	public UserNotExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public UserNotExistsException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
