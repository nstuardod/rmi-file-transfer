package cl.ulagos.icinf.rmi.client;

public class ClientUtils {
	public static boolean checkNewName(String newName) {
		final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"',
				':' };
		final String[] ILLEGAL_BASENAMES = { "aux", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
				"com9", "con", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "nul", "prn" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$

		final String[] ILLEGAL_FILENAMES = { ".", ".." }; //$NON-NLS-1$ //$NON-NLS-2$

		for (char c : ILLEGAL_CHARACTERS)
			if (newName.indexOf(c) != -1)
				return false;
		for (String s : ILLEGAL_BASENAMES)
			if (newName.startsWith(s))
				return false;
		for (String s : ILLEGAL_FILENAMES)
			if (newName.equals(s))
				return false;
		return true;
	}

}
