package cl.ulagos.icinf.rmi.server;

import java.util.Date;

/**
 * Representa datos de una sesión
 * 
 * @author Nicolás Stuardo
 *
 */
public class SessionStatus {
	public static final long MAX_TIMEOUT_MILLIS = 60 * 10 * 1000;
	private String username;
	private SessionToken token;
	private Date lastAction;

	/**
	 * Crea un nuevo token de sesión.
	 * 
	 * @param username Nombre de usuario.
	 * @param token    Token de sesión.
	 */
	public SessionStatus(String username, SessionToken token) {
		this.username = username;
		this.token = token;
		this.lastAction = new Date();
	}

	/**
	 * Obtiene el nombre de usuario
	 * 
	 * @return Una copia del nombre de usuario.
	 */
	public String getUsername() {
		return new String(this.username);
	}

	/**
	 * Obtiene el token de sesión.
	 * 
	 * @return Token de sesión.
	 */
	public SessionToken getToken() {
		return token;
	}

	/**
	 * Obtiene la fecha de la última acción.
	 * 
	 * @return Fecha y hora de la última acción.
	 */
	public Date getLastActionDate() {
		return (Date) this.lastAction.clone();
	}

	/**
	 * Verifica que la sesión sea válida.
	 * 
	 * @return true si está dentro del timeout tolerable, false si no.
	 */
	public boolean isSessionValid() {
		Date now = new Date();
		long diff = now.getTime() - lastAction.getTime();
		return diff < MAX_TIMEOUT_MILLIS;
	}

	/**
	 * Renueva el timeout de la sesión.
	 * 
	 * @return true si la renovación es válida, false de lo contrario.
	 */
	public boolean updateDate() {
		if (this.isSessionValid()) {
			lastAction = new Date();
			return true;
		} else {
			return false;
		}
	}
}
