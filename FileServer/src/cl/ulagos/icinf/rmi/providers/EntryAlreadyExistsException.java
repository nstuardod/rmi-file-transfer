package cl.ulagos.icinf.rmi.providers;

/**
 * Excepción que se arroja si la entrada de directorio ya existe.
 * 
 * @author Nicolás Stuardo
 *
 */
public class EntryAlreadyExistsException extends FileProviderException {

	public EntryAlreadyExistsException(String message) {
		super(message);
	}

}
