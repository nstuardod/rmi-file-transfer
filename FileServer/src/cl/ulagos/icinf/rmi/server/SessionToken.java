package cl.ulagos.icinf.rmi.server;

import java.io.Serializable;

/**
 * Representa un token de sesión
 * 
 * @author Nicolás Stuardo
 *
 */
public class SessionToken implements Serializable {
	private final String mToken;

	public SessionToken(String token) {
		mToken = token;
	}

	public String getToken() {
		return new String(mToken);
	}

	public boolean equals(SessionToken token) {
		return token.mToken.equals(mToken);
	}
}
