package cl.ulagos.icinf.rmi.server;

public class InvalidTokenException extends FileServerException {
	public InvalidTokenException(String message) {
		super(message);
	}
}
