package cl.ulagos.icinf.rmi.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import cl.ulagos.icinf.rmi.StringTable;

/**
 * Representa la base de datos de usuarios de la aplicación. La implementación
 * se realiza con una BD SQLite.
 */
public class UsersDatabase {
	private Logger logger = Logger.getLogger("UsersDatabase"); //$NON-NLS-1$
	private Connection conn;

	public UsersDatabase() throws UserDBException {
		String url = "jdbc:sqlite:users.db"; //$NON-NLS-1$
		try {
			conn = DriverManager.getConnection(url);
			initDatabase();
		} catch (SQLException e) {
			throw new UserDBException(StringTable.getString("UsersDatabase.DB_CONNECTION_FAILED"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Inicializa la tabla en la base de datos.
	 * 
	 * @throws SQLException Si no existe conexión a la BD o no se puede crear la
	 *                      tabla.
	 */
	private void initDatabase() throws SQLException {
		String testSentence = "SELECT username FROM usuarios LIMIT 1"; //$NON-NLS-1$
		String createSentence = "CREATE TABLE usuarios (username VARCHAR(32) PRIMARY KEY, password CHAR(64), rootFolder TEXT);"; //$NON-NLS-1$
		Statement stmt = null;
		// No operar en conexiones no existentes.
		if (conn == null)
			return;

		try {
			stmt = conn.createStatement();
			stmt.executeQuery(testSentence);
			return;
		} catch (SQLException e) { // Seguramente no existe la tabla
			try {
				stmt.executeUpdate(createSentence);
			} catch (SQLException ex) {
				throw new SQLException(StringTable.getString("UsersDatabase.DB_INITIALIZATION_FAILED"), ex); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Agrega un usuario nuevo a la base de datos.
	 * 
	 * @param user       Nombre de usuario
	 * @param password   Contraseña en texto plano
	 * @param rootFolder Ruta al directorio raíz del usuario.
	 * @throws UserDBException Si no existe conexión o la BD no está inicializada
	 *                         correctamente.
	 */
	public void addUser(String user, String password, String rootFolder) throws UserDBException {
		String sentence = "INSERT INTO usuarios (username, password, rootFolder) VALUES (?,?,?);"; //$NON-NLS-1$

		// No quiero espacios al inicio o final de nombres ni contraseñas.
		user = user.trim();
		password = password.trim();
		rootFolder = rootFolder.trim();

		// MD5, SHA1, SHA2 por si solos son inseguros.
		// bcrypt y PBKDF2 son mejores opciones.
		String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

		try {
			PreparedStatement stmt = conn.prepareStatement(sentence);
			stmt.setString(1, user);
			stmt.setString(2, hashedPassword);
			stmt.setString(3, rootFolder);
			stmt.executeUpdate();
			logger.info(String.format(StringTable.getString("UsersDatabase.USER_CREATION_SUCCESS"), user, rootFolder)); //$NON-NLS-1$
		} catch (SQLException e) {
			throw new UserDBException(StringTable.getString("UsersDatabase.USER_CREATION_FAILED"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Comprueba si el usuario existe en la base de datos.
	 * 
	 * @param user nombre de usuario
	 * @return Devuelve true si existe una coincidencia en la base de datos. false
	 *         de lo contrario.
	 * @throws UserDBException Si no existe conexión o la BD no está inicializada
	 *                         correctamente.
	 */
	public boolean userExists(String user) throws UserDBException {
		String sentence = "SELECT COUNT(*) FROM usuarios WHERE username = ?;"; //$NON-NLS-1$

		// No quiero espacios al inicio o final de nombres ni contraseñas.
		user = user.trim();

		try {
			PreparedStatement stmt = conn.prepareStatement(sentence);
			stmt.setString(1, user);
			ResultSet set = stmt.executeQuery();
			set.next();
			return set.getInt(1) > 0;
		} catch (SQLException e) {
			throw new UserDBException(StringTable.getString("UsersDatabase.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Obtiene la lista de usuarios en la base de datos.
	 * 
	 * @return Un listado de usuarios en la base de datos.
	 * @throws UserDBException Si no existe conexión o la BD no está inicializada
	 *                         correctamente.
	 */
	public List<String> listUsers() throws UserDBException {
		String sentence = "SELECT username FROM usuarios;"; //$NON-NLS-1$

		try {
			Statement stmt = conn.createStatement();
			ResultSet set = stmt.executeQuery(sentence);

			List<String> results = new ArrayList<String>();
			while (set.next()) {
				results.add(set.getString(1));
			}
			return results;
		} catch (SQLException e) {
			throw new UserDBException(StringTable.getString("UsersDatabase.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Elimina al usuario de la base de datos.
	 * 
	 * @param user Nombre de usuario
	 * @throws UserDBException Si no existe conexión o la BD no está inicializada
	 *                         correctamente.
	 */
	public void deleteUser(String user) throws UserDBException {
		String sentence = "DELETE FROM usuarios WHERE username = ?;"; //$NON-NLS-1$

		user = user.trim();

		try {
			PreparedStatement stmt = conn.prepareStatement(sentence);
			stmt.setString(1, user);
			stmt.executeUpdate();
			logger.info(String.format(StringTable.getString("UsersDatabase.USER_DELETE_SUCCESS"), user)); //$NON-NLS-1$
		} catch (SQLException e) {
			throw new UserDBException(StringTable.getString("UsersDatabase.USER_DELETE_FAILED"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Cambia la contraseña de un usuario
	 * 
	 * @param user        Nombre de usuario
	 * @param newPassword Contraseña nueva
	 * @throws UserDBException Si no existe conexión o la BD no está inicializada
	 *                         correctamente.
	 */
	public void changePassword(String user, String newPassword) throws UserDBException {
		String sentence = "UPDATE usuarios SET password = ? WHERE username = ?;"; //$NON-NLS-1$

		// No quiero espacios al inicio o final de nombres ni contraseñas.
		user = user.trim();
		newPassword = newPassword.trim();
		String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

		try {
			PreparedStatement stmt = conn.prepareStatement(sentence);
			stmt.setString(1, hashedPassword);
			stmt.setString(2, user);
			stmt.executeUpdate();
			logger.info(String.format(StringTable.getString("UsersDatabase.PASSWORD_UPDATE_SUCCESS"), user)); //$NON-NLS-1$
		} catch (SQLException e) {
			throw new UserDBException(StringTable.getString("UsersDatabase.PASSWORD_UPDATE_FAILED"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Comprueba si las credenciales de acceso entregadas son válidas.
	 * 
	 * @param user     Nombre de usuario
	 * @param password Contraseña en texto plano
	 * @return true si existe un usuario con dicho nombre y la verificación de
	 *         contraseña tiene éxito. false si una de las dos fracasa.
	 * @throws UserDBException Si no existe conexión o la BD no está inicializada
	 *                         correctamente.
	 */
	public boolean authenticateUser(String user, String password) throws UserDBException {
		String sentence = "SELECT password FROM usuarios WHERE username = ?;"; //$NON-NLS-1$
		user = user.trim();
		password = password.trim();
		try {
			PreparedStatement stmt = conn.prepareStatement(sentence);
			stmt.setString(1, user);
			ResultSet set = stmt.executeQuery();
			if (!set.next()) {
				logger.info(String.format(StringTable.getString("UsersDatabase.USER_NOT_FOUND"), user)); //$NON-NLS-1$
				return false;
			}
			String hashed = set.getString(1);
			return BCrypt.checkpw(password, hashed);
		} catch (SQLException e) {
			throw new UserDBException(StringTable.getString("UsersDatabase.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Obtiene la carpeta raíz del usuario en el sistema de archivos.
	 * 
	 * @param user Nombre de usuario
	 * @return La ruta al directorio raíz del usuario (relativa). null si este no
	 *         existe.
	 * @throws UserDBException Si no existe conexión o la BD no está inicializada
	 *                         correctamente.
	 */
	public String getRootFolder(String user) throws UserDBException {
		String sentence = "SELECT rootFolder FROM usuarios WHERE username = ?;"; //$NON-NLS-1$

		// No quiero espacios al inicio o final de nombres ni contraseñas.
		user = user.trim();

		try {
			PreparedStatement stmt = conn.prepareStatement(sentence);
			stmt.setString(1, user);
			ResultSet set = stmt.executeQuery();
			if (!set.next())
				return null;
			return set.getString(1);
		} catch (SQLException e) {
			throw new UserDBException(StringTable.getString("UsersDatabase.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}
	}
}
