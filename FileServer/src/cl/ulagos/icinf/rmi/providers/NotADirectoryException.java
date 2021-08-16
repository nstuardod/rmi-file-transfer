package cl.ulagos.icinf.rmi.providers;

/**
 * Representa excepciones cuando la ruta no corresponde a un directorio.
 * 
 * @author Nicolás Stuardo
 *
 */
public class NotADirectoryException extends FileProviderException {
	public NotADirectoryException(String message) {
		super(message);
	}
}
