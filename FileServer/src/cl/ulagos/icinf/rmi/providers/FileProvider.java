package cl.ulagos.icinf.rmi.providers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Permite la comunicación entre proveedores de archivos.
 * 
 * @author Nicolás Stuardo
 */
public interface FileProvider {
	/**
	 * Obtiene el nombre del proveedor de archivos.
	 * 
	 * @return El nombre del proveedor
	 */
	public String getProviderName();

	/**
	 * Lee el directorio en la ruta indicada.
	 * 
	 * @param path La ruta a listar.
	 * @return Un listado de entradas de directorio.
	 * @throws NotADirectoryException Si la ruta no corresponde a un directorio.
	 * @throws InvalidAccessException Si el directorio está fuera de los directorios
	 *                                accesibles al usuario.
	 * @throws EntryNotFoundException si no se encuentra la ruta indicada.
	 */
	public List<DirEntry> readDirectory(String path)
			throws InvalidAccessException, EntryNotFoundException, NotADirectoryException;

	/**
	 * Obtiene información del archivo o directorio indicado.
	 * 
	 * @param path Ruta y nombre del archivo o directorio.
	 * @return Información del archivo o directorio.
	 * @throws InvalidAccessException Si el directorio está fuera de los directorios
	 *                                accesibles al usuario.
	 * @throws EntryNotFoundException Si no se encuentra la ruta indicada.
	 * @throws EntryInUseException    Si se está modificando el archivo indicado.
	 */
	public DirEntry getInfo(String path) throws InvalidAccessException, EntryNotFoundException;

	/**
	 * Crea un directorio.
	 * 
	 * @param path Ruta y nombre del directorio nuevo.
	 * @return true o false dependiendo de si se completó la operación.
	 * @throws EntryAlreadyExistsException Si el directorio ya existe.
	 * @throws InvalidAccessException      Si el directorio está fuera de los
	 *                                     directorios accesibles al usuario.
	 */
	public boolean createDirectory(String path) throws InvalidAccessException, EntryAlreadyExistsException;

	/**
	 * Mueve o renombra un directorio en la ruta indicada.
	 * 
	 * @param path Ruta al directorio a renombrar.
	 * @param Ruta al nuevo directorio.
	 * @return true o false dependiendo de si se completó la operación.
	 * @throws NotADirectoryException      Si la ruta no corresponde a un
	 *                                     directorio.
	 * @throws EntryNotFoundException      Si el directorio no existe.
	 * @throws InvalidAccessException      Si el directorio está fuera de los
	 *                                     directorios accesibles al usuario.
	 * @throws EntryAlreadyExistsException Si el directorio ya existe.
	 */
	public boolean moveDirectory(String path, String newName)
			throws InvalidAccessException, EntryNotFoundException, NotADirectoryException, EntryAlreadyExistsException;

	/**
	 * Elimina un directorio.
	 * 
	 * @param path Ruta y nombre del directorio a eliminar.
	 * @return true o false dependiendo de si se completó la operación.
	 * @throws DirectoryNotEmptyException Si el directorio no está vacío.
	 * @throws NotADirectoryException     Si la ruta no corresponde a un directorio.
	 * @throws EntryNotFoundException     Si el directorio no existe.
	 * @throws InvalidAccessException     Si el directorio está fuera de los
	 *                                    directorios accesibles al usuario.
	 * @throws EntryInUseException        Si el directorio está bloqueado por otro
	 *                                    proceso.
	 */
	public boolean deleteDirectory(String path) throws InvalidAccessException, EntryNotFoundException,
			NotADirectoryException, DirectoryNotEmptyException, EntryInUseException;

	/**
	 * Elimina un directorio.
	 * 
	 * @param path    Ruta y nombre del directorio a eliminar.
	 * @param recurse Modo recursivo.
	 * @return true o false dependiendo de si se completó la operación.
	 * @throws DirectoryNotEmptyException Si el directorio no está vacío.
	 * @throws NotADirectoryException     Si la ruta no corresponde a un directorio.
	 * @throws EntryNotFoundException     Si el directorio no existe.
	 * @throws InvalidAccessException     Si el directorio está fuera de los
	 *                                    directorios accesibles al usuario.
	 * @throws EntryInUseException        Si el directorio está bloqueado por otro
	 *                                    proceso.
	 */
	public boolean deleteDirectory(String path, boolean recurse) throws InvalidAccessException, EntryNotFoundException,
			NotADirectoryException, DirectoryNotEmptyException, EntryInUseException;

	/**
	 * Coloca un archivo en la ruta indicada. Si existe lo reemplaza. Recibe el
	 * archivo desde un búfer.
	 * 
	 * @param path   Ruta al archivo a colocar.
	 * @param size   Tamaño en bytes a recibir.
	 * @param buffer Búfer de entrada con bytes del archivo.
	 * @return true o false dependiendo de si se completó la operación.
	 * @throws FileProviderException  Si el proveedor informa un problema interno.
	 * @throws EntryInUseException    Si el archivo está bloqueado por otro proceso.
	 * @throws InvalidAccessException Si el archivo está fuera de los directorios
	 *                                accesibles al usuario.
	 */
	public boolean putFile(String path, long size, InputStream buffer)
			throws InvalidAccessException, EntryInUseException, FileProviderException;

	/**
	 * Obtiene un búfer para la lectura de un archivo.
	 * 
	 * @param path   Ruta al archivo a obtener.
	 * @param buffer Búfer de salida para enviar el archivo.
	 * @return boolean true o false si se obtiene correctamente o no.
	 * @throws FileProviderException  Si el proveedor informa un problema interno.
	 * @throws InvalidAccessException Si el archivo está fuera de los directorios
	 *                                accesibles al usuario.
	 * @throws EntryNotFoundException Si el archivo no existe.
	 * @throws NotAFileException      Si la ruta no corresponde a un archivo.
	 * @throws EntryInUseException    Si el archivo está bloqueado por otro proceso.
	 */
	public boolean retrieveFile(String path, OutputStream buffer) throws EntryInUseException, NotAFileException,
			EntryNotFoundException, InvalidAccessException, FileProviderException;

	/**
	 * Mueve o renombra un archivo en la ruta indicada.
	 * 
	 * @param path   Ruta al archivo a colocar.
	 * @param Nombre del nuevo archivo o ruta donde mover.
	 * @return true o false dependiendo de si se completó la operación.
	 * @throws NotAFileException           Si la ruta no corresponde a un archivo.
	 * @throws EntryNotFoundException      Si el archivo no existe.
	 * @throws InvalidAccessException      Si el archivo está fuera de los
	 *                                     directorios accesibles al usuario.
	 * @throws EntryAlreadyExistsException Si el archivo ya existe.
	 */
	public boolean moveFile(String path, String newName)
			throws InvalidAccessException, EntryNotFoundException, NotAFileException, EntryAlreadyExistsException;

	/**
	 * Elimina un archivo.
	 * 
	 * @param path Ruta y nombre del archivo a eliminar.
	 * @return true o false dependiendo de si se completó la operación.
	 * @throws NotAFileException      Si la ruta no corresponde a un archivo.
	 * @throws InvalidAccessException Si el archivo está fuera de los directorios
	 *                                accesibles al usuario.
	 * @throws EntryNotFoundException Si el archivo no existe.
	 * @throws EntryInUseException    Si el archivo está bloqueado por otro proceso.
	 */
	public boolean deleteFile(String path)
			throws EntryInUseException, NotAFileException, EntryNotFoundException, InvalidAccessException;
}
