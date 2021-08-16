package cl.ulagos.icinf.rmi.server;

/**
 * Representa errores de manejo de archivos
 * 
 * @author Nicolás Stuardo
 *
 */
public class FileManagementException extends FileServerException {

	public FileManagementException() {}

	public FileManagementException(String message) {
		super(message);
	}

	public FileManagementException(Throwable cause) {
		super(cause);
	}

	public FileManagementException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileManagementException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
