package cl.ulagos.icinf.rmi.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import cl.ulagos.icinf.rmi.providers.DirEntry;

public interface FileServer extends Remote {
	/**
	 * Hace login al servidor
	 * 
	 * @param user     Nombre de usuario
	 * @param password Contraseña
	 * @return Token de sesión.
	 * @throws InvalidLoginException Si el nombre de usuario o contraseña no son
	 *                               válidos.
	 * @throws RemoteException       Si existe un problema con la comunicación.
	 */
	public SessionToken login(String user, String password) throws InvalidLoginException, RemoteException;

	/**
	 * Cierra la sesión en el servidor
	 * 
	 * @param token Token de sesión
	 * @throws InvalidTokenException Si el token es inválido o ha expirado.
	 */
	public void logout(SessionToken token) throws RemoteException, InvalidTokenException;

	/**
	 * Cambia la contraseña del usuario.
	 * 
	 * @param token       Token de sesión
	 * @param password    Contraseña actual
	 * @param newPassword Nueva Contraseña
	 * @throws InvalidLoginException Si la contraseña no coincide con el usuario.
	 * @throws InvalidTokenException Si el token es inválido o ha expirado.
	 * @throws FileServerException   Si ocurre un problema interno.
	 * @throws RemoteException       Si existe un problema con la comunicación.
	 */
	public void changePassword(SessionToken token, String password, String newPassword)
			throws RemoteException, InvalidLoginException, InvalidTokenException, FileServerException;

	/**
	 * Mantiene la sesión activa.
	 * 
	 * @param token Token de sesión
	 * @throws InvalidTokenException Si el token es inválido o ha expirado.
	 * @throws RemoteException       Si existe un problema con la comunicación.
	 */
	public void ping(SessionToken token) throws InvalidTokenException, RemoteException;

	/**
	 * Obtiene información de un archivo o directorio.
	 * 
	 * @param token Token de sesión
	 * @param path  Ruta al archivo o directorio.
	 * @return Información del archivo o directorio.
	 * @throws InvalidTokenException   Si el token es inválido o ha expirado.
	 * @throws FileManagementException Si ocurre un error al obtener la información
	 *                                 solicitada.
	 * @throws RemoteException         Si existe un problema con la comunicación.
	 */
	public DirEntry getInfo(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException;

	/**
	 * Obtiene un listado del directorio solicitado.
	 * 
	 * @param token Token de sesión
	 * @param path  Ruta al archivo o directorio.
	 * @return Listado de contenidos del directorio. Si el directorio está vacío
	 *         devuelve una lista vacía.
	 * @throws InvalidTokenException   Si el token es inválido o ha expirado.
	 * @throws FileManagementException Si ocurre un error al obtener la información
	 *                                 solicitada.
	 * @throws RemoteException         Si existe un problema con la comunicación.
	 */
	public List<DirEntry> readDirectory(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException;

	/**
	 * Crea un directorio en la ruta solicitada.
	 * 
	 * @param token Token de sesión
	 * @param path  Ruta al directorio.
	 * @return true o false dependiendo Si la acción se realizó correctamente o no.
	 * @throws InvalidTokenException   Si el token es inválido o ha expirado.
	 * @throws FileManagementException Si ocurre un error al realizar la información
	 *                                 solicitada.
	 * @throws RemoteException         Si existe un problema con la comunicación.
	 */
	public boolean createDirectory(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException;

	/**
	 * Mueve o renombra un directorio.
	 * 
	 * @param token Token de sesión
	 * @param path  Ruta al directorio.
	 * @return true o false dependiendo Si la acción se realizó correctamente o no.
	 * @throws InvalidTokenException   Si el token es inválido o ha expirado.
	 * @throws FileManagementException Si ocurre un error al realizar la información
	 *                                 solicitada.
	 * @throws RemoteException         Si existe un problema con la comunicación.
	 */
	public boolean moveDirectory(SessionToken token, String path, String newName)
			throws RemoteException, InvalidTokenException, FileManagementException;

	/**
	 * Elimina un directorio.
	 * 
	 * @param token Token de sesión
	 * @param path  Ruta al directorio.
	 * @return true o false dependiendo Si la acción se realizó correctamente o no.
	 * @throws InvalidTokenException   Si el token es inválido o ha expirado.
	 * @throws FileManagementException Si ocurre un error al realizar la información
	 *                                 solicitada.
	 * @throws RemoteException         Si existe un problema con la comunicación.
	 */
	public boolean deleteDirectory(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException;

	/**
	 * Mueve o renombra un directorio.
	 * 
	 * @param token   Token de sesión
	 * @param path    Ruta al directorio.
	 * @param recurse Si es true, se elimina el directorio junto con todos sus
	 *                elementos.
	 * @return true o false dependiendo Si la acción se realizó correctamente o no.
	 * @throws InvalidTokenException   Si el token es inválido o ha expirado.
	 * @throws FileManagementException Si ocurre un error al realizar la información
	 *                                 solicitada.
	 * @throws RemoteException         Si existe un problema con la comunicación.
	 */
	public boolean deleteDirectory(SessionToken token, String path, boolean recurse)
			throws RemoteException, InvalidTokenException, FileManagementException;

	/**
	 * Solicita un archivo al servidor.
	 * 
	 * @param token Token de sesión
	 * @param path  Ruta del archivo a enviar.
	 * @return Un número de puerto al que conectarse para la transferencia.
	 * @throws InvalidTokenException   Si el token utilizado es inválido.
	 * @throws FileManagementException
	 * @throws RemoteException         Si existe un problema con la comunicación.
	 */
	public int getFile(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException, FileServerException;

	/**
	 * Envía un archivo al servidor.
	 * 
	 * @param token Token de sesión
	 * @param path  Ruta del archivo a recibir
	 * @return Un número de puerto al que conectarse para la transferencia.
	 * @throws InvalidTokenException   Si el token utilizado es inválido.
	 * @throws FileManagementException
	 * @throws RemoteException         Si existe un problema con la comunicación.
	 */
	public int putFile(SessionToken token, String path, long size)
			throws RemoteException, InvalidTokenException, FileManagementException, FileServerException;

	/**
	 * Renombra un archivo.
	 * 
	 * @param token Token de sesión
	 * @param path  Ruta al archivo.
	 * @return true o false dependiendo Si la acción se realizó correctamente o no.
	 * @throws InvalidTokenException   Si el token es inválido o ha expirado.
	 * @throws FileManagementException Si ocurre un error al realizar el renombrado.
	 * @throws RemoteException         Si existe un problema con la comunicación.
	 */
	public boolean moveFile(SessionToken token, String path, String newName)
			throws RemoteException, InvalidTokenException, FileManagementException;

	/**
	 * Elimina un archivo.
	 * 
	 * @param token Token de sesión
	 * @param path  Ruta al archivo.
	 * @return true o false dependiendo Si la acción se realizó correctamente o no.
	 * @throws InvalidTokenException   Si el token es inválido o ha expirado.
	 * @throws FileManagementException Si ocurre un error al intentar realizar la
	 *                                 eliminación.
	 * @throws RemoteException         Si existe un problema con la comunicación.
	 */
	public boolean deleteFile(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException;
}
