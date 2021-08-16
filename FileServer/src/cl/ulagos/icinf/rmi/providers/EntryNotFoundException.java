package cl.ulagos.icinf.rmi.providers;

/**
 * Excepción que se arroja si la entrada de directorio no existe.
 * 
 * @author Nicolás Stuardo
 *
 */
public class EntryNotFoundException extends FileProviderException {

	public EntryNotFoundException(String message) {
		super(message);
	}

}
