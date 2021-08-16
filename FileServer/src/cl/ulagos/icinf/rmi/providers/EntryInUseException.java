package cl.ulagos.icinf.rmi.providers;

/**
 * Excepción que se arroja si la entrada de directorio está siendo utilizada.
 * 
 * @author Nicolás Stuardo
 *
 */
public class EntryInUseException extends FileProviderException {

	public EntryInUseException(String message) {
		super(message);
	}

}
