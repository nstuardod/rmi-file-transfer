package cl.ulagos.icinf.rmi.server;

/**
 * Excepción arrojada si el usuario ya existe.
 * 
 * @author Nicolás Stuardo
 */
public class UserAlreadyExistsException extends Exception {

	public UserAlreadyExistsException() {
		super();
	}

	public UserAlreadyExistsException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UserAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public UserAlreadyExistsException(String message) {
		super(message);
	}

	public UserAlreadyExistsException(Throwable cause) {
		super(cause);
	}

}
