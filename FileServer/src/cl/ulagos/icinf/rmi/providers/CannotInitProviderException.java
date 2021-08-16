package cl.ulagos.icinf.rmi.providers;

/**
 * Excepción arrojada cuando no se puede iniciar el proveedor de archivos
 * seleccionado.
 * 
 * @author Nicolás Stuardo
 *
 */
public class CannotInitProviderException extends FileProviderException {

	public CannotInitProviderException(String message) {
		super(message);
	}
}
