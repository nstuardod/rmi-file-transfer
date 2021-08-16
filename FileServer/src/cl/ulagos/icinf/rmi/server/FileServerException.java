package cl.ulagos.icinf.rmi.server;

/**
 * Representa errores del servidor de archivos.
 * 
 * @author Nicolás Stuardo
 *
 */
public class FileServerException extends Exception {

	public FileServerException() {
		super();
	}

	public FileServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public FileServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileServerException(String message) {
		super(message);
	}

	public FileServerException(Throwable cause) {
		super(cause);
	}

}
