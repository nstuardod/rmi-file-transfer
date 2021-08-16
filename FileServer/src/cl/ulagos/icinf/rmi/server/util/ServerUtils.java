package cl.ulagos.icinf.rmi.server.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class ServerUtils {
	private static Random rnd = new Random();

	public static byte[] getSHA256HashBytes(String message) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256"); //$NON-NLS-1$
		return digest.digest(message.getBytes(StandardCharsets.UTF_8));
	}

	public static String getSHA256Hash(String message) throws NoSuchAlgorithmException {
		return bytesToHex(getSHA256HashBytes(message));
	}

	public static String bytesToHex(byte[] hash) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

	/**
	 * Genera una cadena con caracteres aleatorios.
	 * 
	 * @param len Largo de la cadena en caracteres.
	 * @return Cadena solicitada.
	 */
	public static String randomString(int len) {
		final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"; //$NON-NLS-1$
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));
		return sb.toString();
	}
}
