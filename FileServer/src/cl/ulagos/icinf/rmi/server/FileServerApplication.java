package cl.ulagos.icinf.rmi.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import cl.ulagos.icinf.rmi.StringTable;

public class FileServerApplication {
	private static final int PUERTO = 8527; // U - L - A - R (Registry)
	private static Logger logger = Logger.getLogger("FileServerApplication"); //$NON-NLS-1$

	private static void help() {
		String[] cmds = { StringTable.getString("FileServerApplication.ADDUSER_HELP"), //$NON-NLS-1$
				StringTable.getString("FileServerApplication.RESET_PW_HELP"), //$NON-NLS-1$
				StringTable.getString("FileServerApplication.DELUSER_HELP"), StringTable.getString("FileServerApplication.LS_USERS_HELP"), //$NON-NLS-1$ //$NON-NLS-2$
				StringTable.getString("FileServerApplication.HELP_HELP") }; //$NON-NLS-1$
		for (String cmd : cmds)
			System.out.println(cmd);
	}

	private static void addUser(FileServerImpl server, String username) {
		try {
			String password = server.createUser(username);
			System.out.println(String.format(StringTable.getString("FileServerApplication.NEW_USER_PASSWORD"), password)); //$NON-NLS-1$
		} catch (UserAlreadyExistsException e) {
			System.err.println(StringTable.getString("FileServerApplication.USER_ALREADY_EXISTS")); //$NON-NLS-1$
		} catch (FileServerException e) {
			System.err.println(e.getMessage());
		}
	}

	private static void resetPassword(FileServerImpl server, String username) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String answer = new String();
		try {
			System.out.print(StringTable.getString("FileServerApplication.RESET_PASSWORD_CONFIRM")); //$NON-NLS-1$
			answer = reader.readLine().toLowerCase();
			if (answer.charAt(0) == 'n') {
				System.out.println(StringTable.getString("FileServerApplication.RESET_PASSWORD_FAILURE")); //$NON-NLS-1$
				return;
			} else if (answer.charAt(0) == 's' || answer.charAt(0) == 'y') {
				try {
					String password = server.resetPassword(username);
					System.out.println(String.format(StringTable.getString("FileServerApplication.RESET_PASSWORD_SUCCESS"), password)); //$NON-NLS-1$
				} catch (UserNotExistsException e) {
					System.err.println(StringTable.getString("FileServerApplication.USER_NOT_EXISTS")); //$NON-NLS-1$
				} catch (FileServerException e) {
					System.err.println(e.getMessage());
				}
			} else {
				System.out.println(StringTable.getString("FileServerApplication.PASSWORD_RESET_CANCELLED")); //$NON-NLS-1$
			}
		} catch (IOException e) {
			System.out.println(StringTable.getString("FileServerApplication.PASSWORD_RESET_CANCELLED")); //$NON-NLS-1$
			return;
		}
	}

	private static void deleteUser(FileServerImpl server, String username) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String answer = new String();
		try {
			System.out.print(StringTable.getString("FileServerApplication.USER_DELETION_PROMPT")); //$NON-NLS-1$
			answer = reader.readLine().toLowerCase();
			if (answer.charAt(0) == 'n') {
				System.out.println(StringTable.getString("FileServerApplication.USER_DELETION_CANCELLED")); //$NON-NLS-1$
				return;
			} else if (answer.charAt(0) == 's' || answer.charAt(0) == 'y') {
				try {
					server.deleteUser(username);
				} catch (UserNotExistsException e) {
					System.err.println(StringTable.getString("FileServerApplication.USER_NOT_EXISTS")); //$NON-NLS-1$
				} catch (FileServerException e) {
					System.err.println(e.getMessage());
				}
			} else {
				System.out.println(StringTable.getString("FileServerApplication.USER_DELETION_CANCELLED")); //$NON-NLS-1$
				return;
			}
		} catch (IOException e) {
			System.out.println(StringTable.getString("FileServerApplication.USER_DELETION_CANCELLED")); //$NON-NLS-1$
			return;
		}
	}

	private static void listUsers(FileServerImpl server) {
		List<String> list;
		try {
			list = server.getUsers();
		if (list.size() == 0) {
			System.out.println(StringTable.getString("FileServerApplication.NO_USERS")); //$NON-NLS-1$
			return;
		}
		System.out.println(StringTable.getString("FileServerApplication.USERS_LIST")); //$NON-NLS-1$
		for (String user : list)
			System.out.println(user);
		} catch (FileServerException e) {
			System.err.println(e.getMessage());
		}
	}

	private static void parseCommandLine(FileServerImpl server, String cmd) throws Exception {
		StringTokenizer tokenizer = new StringTokenizer(cmd);
		if (tokenizer.hasMoreTokens()) {
			// Identificar comando
			String theCommand = tokenizer.nextToken();
			if (theCommand.equals("adduser")) { //$NON-NLS-1$
				if (!tokenizer.hasMoreTokens()) {
					System.err.println(StringTable.getString("FileServerApplication.ADDUSER_USAGE")); //$NON-NLS-1$
					return;
				}
				String username = tokenizer.nextToken().trim();
				addUser(server, username);
			} else if (theCommand.equals("deluser")) { //$NON-NLS-1$
				if (!tokenizer.hasMoreTokens()) {
					System.err.println(StringTable.getString("FileServerApplication.DELUSER_USAGE")); //$NON-NLS-1$
					return;
				}
				String username = tokenizer.nextToken().trim();
				deleteUser(server, username);
			} else if (theCommand.equals("reset_pw")) { //$NON-NLS-1$
				if (!tokenizer.hasMoreTokens()) {
					System.err.println(StringTable.getString("FileServerApplication.RESET_PW_USAGE")); //$NON-NLS-1$
					return;
				}
				String username = tokenizer.nextToken().trim();
				resetPassword(server, username);
			} else if (theCommand.equals("ls_users")) { //$NON-NLS-1$
				listUsers(server);
			} else if (theCommand.equals("help")) { //$NON-NLS-1$
				help();
			} else {
				System.err.println(StringTable.getString("FileServerApplication.COMMAND_NOT_RECOGNIZED")); //$NON-NLS-1$
				help();
			}
		}
	}

	public static void main(String args[]) {
		BasicConfigurator.configure();

		// HACK: forzar IPv4
		System.setProperty("java.net.preferIPv4Stack", "true");

		String policyPath = "res/server.policy"; //$NON-NLS-1$
		URL policyURL = FileServerApplication.class.getClassLoader().getResource(policyPath);
		String codebase = FileServerApplication.class.getProtectionDomain().getCodeSource().getLocation().toString();
		if (policyURL == null) {
			logger.fatal(String.format(StringTable.getString("FileServerApplication.SERVER_POLICY_NOT_FOUND"), policyPath)); //$NON-NLS-1$
			return;
		} else {
			String url = policyURL.toString();
			logger.debug(String.format(StringTable.getString("FileServerApplication.SERVER_POLICY_PATH"), url)); //$NON-NLS-1$
			System.setProperty("java.security.policy", url); //$NON-NLS-1$
			logger.debug(String.format(StringTable.getString("FileServerApplication.CODEBASE_PATH"), codebase)); //$NON-NLS-1$
			System.setProperty("java.rmi.server.codebase", codebase); //$NON-NLS-1$
		}

		if (System.getSecurityManager() == null) {
			logger.info(StringTable.getString("FileServerApplication.NO_SECURITY_MANAGER")); //$NON-NLS-1$
			System.setSecurityManager(new SecurityManager());
		}
		FileServer server = null;
		try {
			server = new FileServerImpl();
			logger.info(StringTable.getString("FileServerApplication.SERVER_INITIATED")); //$NON-NLS-1$
			Registry registry = LocateRegistry.createRegistry(PUERTO);
			registry.rebind("RMIFileServer", server); //$NON-NLS-1$
			logger.info(StringTable.getString("FileServerApplication.SERVER_BOUND")); //$NON-NLS-1$

			// Lanza la interfaz de línea de comandos.
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String command = new String();
			while (true) {
				try {
					System.out.print(StringTable.getString("FileServerApplication.PROMPT")); //$NON-NLS-1$
					command = reader.readLine();
					try {
						parseCommandLine((FileServerImpl) server, command);
					} catch (Exception e) {
						logger.error(StringTable.getString("FileServerApplication.COMMAND_EXECUTION_ERROR"), e); //$NON-NLS-1$
					}
				} catch (IOException e) {
					System.err.println(StringTable.getString("FileServerApplication.CONSOLE_READ_IO_ERROR")); //$NON-NLS-1$
				}
			}
		} catch (RemoteException e) {
			logger.error(StringTable.getString("FileServerApplication.SERVER_REMOTE_EXCEPTION"), e); //$NON-NLS-1$
		} catch (CannotInitServerException e) {
			logger.error(StringTable.getString("FileServerApplication.SERVER_INIT_FAILURE"), e); //$NON-NLS-1$
		}
	}
}
