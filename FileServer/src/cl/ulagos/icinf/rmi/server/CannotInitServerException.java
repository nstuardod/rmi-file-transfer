package cl.ulagos.icinf.rmi.server;

/**
 * Representa un error de inicialización del servidor.
 * 
 * @author Nicolás Stuardo
 *
 */
public class CannotInitServerException extends Exception {

	public CannotInitServerException() {
	}

	public CannotInitServerException(String message) {
		super(message);
	}

	public CannotInitServerException(Throwable cause) {
		super(cause);
	}

	public CannotInitServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public CannotInitServerException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
