package cl.ulagos.icinf.rmi.providers;

/**
 * Representa excepciones cuando la ruta no corresponde a un archivo.
 * 
 * @author Nicolás Stuardo
 *
 */
public class NotAFileException extends FileProviderException {
	public NotAFileException(String message) {
		super(message);
	}
}
