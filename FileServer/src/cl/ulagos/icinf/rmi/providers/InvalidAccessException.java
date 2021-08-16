package cl.ulagos.icinf.rmi.providers;

/**
 * Excepción que se arroja si la entrada de directorio solicitada es inválida o
 * si es ilegal.
 * 
 * @author Nicolás Stuardo
 *
 */
public class InvalidAccessException extends FileProviderException {

	public InvalidAccessException(String message) {
		super(message);
	}

}
