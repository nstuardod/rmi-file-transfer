package cl.ulagos.icinf.rmi.server;

/**
 * Representa un intento de inicio de sesión fallido.
 * 
 * @author Nicolás Stuardo
 */
public class InvalidLoginException extends FileServerException {

	public InvalidLoginException() {
		super();
	}

	public InvalidLoginException(String s) {
		super(s);
	}

	public InvalidLoginException(String s, Throwable cause) {
		super(s, cause);
	}

}
