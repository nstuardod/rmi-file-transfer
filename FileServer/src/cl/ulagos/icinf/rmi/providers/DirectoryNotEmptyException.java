package cl.ulagos.icinf.rmi.providers;

/**
 * Excepción que se arroja si el directorio no está vacío.
 * 
 * @author Nicolás Stuardo
 *
 */
public class DirectoryNotEmptyException extends FileProviderException {

	public DirectoryNotEmptyException(String message) {
		super(message);
	}

}
