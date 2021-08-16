package cl.ulagos.icinf.rmi.client;

import java.io.BufferedReader;
import java.io.Console;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cl.ulagos.icinf.rmi.providers.DirEntry;
import cl.ulagos.icinf.rmi.providers.EntryType;
import cl.ulagos.icinf.rmi.server.FileManagementException;
import cl.ulagos.icinf.rmi.server.FileServer;
import cl.ulagos.icinf.rmi.server.FileServerException;
import cl.ulagos.icinf.rmi.server.InvalidLoginException;
import cl.ulagos.icinf.rmi.server.InvalidTokenException;
import cl.ulagos.icinf.rmi.server.SessionToken;

public class TextModeClientLegacy {

	private static final int BUFFER_SIZE = 2048;
	private static final int PUERTO = 8527;
	private static final String RMIServerName = "RMIFileServer"; //$NON-NLS-1$
	private static final String prompt = StringTable.getString("TextModeClient.PROMPT"); //$NON-NLS-1$
	private static SessionToken token;
	private static FileServer serverObj;
	private static String serverAddress;
	private static Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			try {
				if (serverObj != null) {
					System.err.println(StringTable.getString("TextModeClient.LOGGING_OUT")); //$NON-NLS-1$
					serverObj.logout(token);
				}
			} catch (RemoteException e) {
				System.err.println(
						StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
			} catch (InvalidTokenException e) {
				System.err.println(StringTable.getString("TextModeClient.LOG_OUT_ERROR") + e.getMessage()); //$NON-NLS-1$
			}
		}
	};

	private static void help() {
		String[] cmds = { StringTable.getString("TextModeClient.HELP_PASSWD"), //$NON-NLS-1$
				StringTable.getString("TextModeClient.HELP_LS"), //$NON-NLS-1$
				StringTable.getString("TextModeClient.HELP_MKDIR"), //$NON-NLS-1$
				StringTable.getString("TextModeClient.HELP_RMDIR"), //$NON-NLS-1$
				StringTable.getString("TextModeClient.HELP_RM"), //$NON-NLS-1$
				StringTable.getString("TextModeClient.HELP_MV"), //$NON-NLS-1$
				StringTable.getString("TextModeClient.HELP_GET"), //$NON-NLS-1$
				StringTable.getString("TextModeClient.HELP_PUT"), //$NON-NLS-1$
				StringTable.getString("TextModeClient.HELP_HELP") }; //$NON-NLS-1$
		for (String cmd : cmds)
			System.out.println(cmd);
	}

	private static void changePassword() {
		String p1, p2;
		Console c = System.console();
		try {
			System.out.print(StringTable.getString("TextModeClient.PASSWD_ENTER_CURRENT_PASSWORD")); //$NON-NLS-1$
			p1 = new String(c.readPassword());
			System.out.print(StringTable.getString("TextModeClient.PASSWD_ENTER_NEW_PASSWORD")); //$NON-NLS-1$
			p2 = new String(c.readPassword());

			serverObj.changePassword(token, p1, p2);
			System.out.println(StringTable.getString("TextModeClient.PASSWD_CHANGE_SUCCESSFUL")); //$NON-NLS-1$
		} catch (FileServerException e) {
			System.err.println(StringTable.getString("TextModeClient.PASSWD_CHANGE_FAILED") + e.getMessage()); //$NON-NLS-1$
		} catch (RemoteException e) {
			System.err.println(
					StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
		} catch (IOError e) {
			System.err.println(StringTable.getString("TextModeClient.CONSOLE_IO_ERROR")); //$NON-NLS-1$
		}
	}

	private static void list(String path) {
		try {
			List<DirEntry> list = serverObj.readDirectory(token, path);
			if (list.size() == 0) {
				System.out.println(StringTable.getString("TextModeClient.LIST_NO_ELEMENTS")); //$NON-NLS-1$
			}
			System.out
					.println(String.format(StringTable.getString("TextModeClient.LIST_TOTAL"), list.size())); //$NON-NLS-1$
			for (DirEntry de : list) {
				System.out.println(String.format("<%s> %8d %s", de.getType() == EntryType.DIRECTORY ? "DIR" : "FILE", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						de.getSize(), de.getName()));
			}
		} catch (InvalidTokenException e) {
			System.err.println(StringTable.getString("TextModeClient.SESSION_EXPIRED_MESSAGE")); //$NON-NLS-1$
		} catch (FileManagementException e) {
			System.err.println(StringTable.getString("TextModeClient.FILE_MANAGEMENT_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
		} catch (RemoteException e) {
			System.err.println(
					StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
		}
	}

	private static void makeDirectory(String path) {
		try {
			if (serverObj.createDirectory(token, path))
				System.out.println(StringTable.getString("TextModeClient.MKDIR_SUCCESSFUL")); //$NON-NLS-1$
		} catch (InvalidTokenException e) {
			System.err.println(StringTable.getString("TextModeClient.SESSION_EXPIRED_MESSAGE")); //$NON-NLS-1$
		} catch (FileManagementException e) {
			System.err.println(StringTable.getString("TextModeClient.FILE_MANAGEMENT_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
		} catch (RemoteException e) {
			System.err.println(
					StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
		}
	}

	private static void removeDirectory(String path) {
		try {
			if (serverObj.deleteDirectory(token, path))
				System.out.println(StringTable.getString("TextModeClient.RMDIR_SUCCESSFUL")); //$NON-NLS-1$
		} catch (InvalidTokenException e) {
			System.err.println(StringTable.getString("TextModeClient.SESSION_EXPIRED_MESSAGE")); //$NON-NLS-1$
		} catch (FileManagementException e) {
			System.err.println(StringTable.getString("TextModeClient.FILE_MANAGEMENT_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
		} catch (RemoteException e) {
			System.err.println(
					StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
		}
	}

	private static void removeFile(String path) {
		try {
			if (serverObj.deleteFile(token, path))
				System.out.println(StringTable.getString("TextModeClient.RM_SUCCESSFUL")); //$NON-NLS-1$
		} catch (InvalidTokenException e) {
			System.err.println(StringTable.getString("TextModeClient.SESSION_EXPIRED_MESSAGE")); //$NON-NLS-1$
		} catch (FileManagementException e) {
			System.err.println(StringTable.getString("TextModeClient.FILE_MANAGEMENT_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
		} catch (RemoteException e) {
			System.err.println(
					StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
		}
	}

	private static void move(String path, String newName) {
		try {
			DirEntry info = serverObj.getInfo(token, path);
			if (info.getType() == EntryType.DIRECTORY) {
				if (serverObj.moveDirectory(token, path, newName))
					System.out.println(StringTable.getString("TextModeClient.MV_DIR_SUCCESSFUL")); //$NON-NLS-1$
			} else if (info.getType() == EntryType.FILE) {
				if (serverObj.moveFile(token, path, newName))
					System.out.println(StringTable.getString("TextModeClient.MV_FILE_SUCCESSFUL")); //$NON-NLS-1$
			}
		} catch (InvalidTokenException e) {
			System.err.println(StringTable.getString("TextModeClient.SESSION_EXPIRED_MESSAGE")); //$NON-NLS-1$
		} catch (FileManagementException e) {
			System.err.println(StringTable.getString("TextModeClient.FILE_MANAGEMENT_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
		} catch (RemoteException e) {
			System.err.println(
					StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
		}
	}

	private static boolean confirmAction(String message, String message2) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println(message);
		while (true) {
			System.out.print(String.format(StringTable.getString("TextModeClient.YES_NO_PROMPT"), message)); //$NON-NLS-1$
			try {
				String answer = reader.readLine().toLowerCase();
				if (answer.charAt(0) == 's' || answer.charAt(0) == 'y') {
					return true;
				} else if (answer.charAt(0) == 'n') {
					return false;
				} else {
					System.err.println(StringTable.getString("TextModeClient.CONFIRM_INVALID_INPUT_MESSAGE")); //$NON-NLS-1$
				}
			} catch (IOException e) {
				System.err.println(StringTable.getString("TextModeClient.CONFIRM_IO_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
				return false;
			}
		}
	}

	private static boolean getFile(String path) {
		int count, downloadPort;
		long size, startTime, lastTime, endTime, receivedBytes = 0;
		final long printInterval = 125;
		FileOutputStream fos;
		DataInputStream is;
		Socket downloadSocket;
		byte[] fileBytes;
		try {
			// Obtener el tamaño de archivo.
			DirEntry de = serverObj.getInfo(token, path);
			size = de.getSize();
			String filename = de.getName();
			// Solicitar la transferencia.
			downloadPort = serverObj.getFile(token, path);

			downloadSocket = new Socket(serverAddress, downloadPort);
			downloadSocket.setSoTimeout(10000);
			if (new File(filename).exists()) {
				if (!confirmAction(
						String.format(StringTable.getString("TextModeClient.TRANSFER_RECEIVE_FILE_EXISTS"), //$NON-NLS-1$
								filename), StringTable.getString("TextModeClient.TRANSFER_RECEIVE_CONFIRM_OVERWRITE"))) { //$NON-NLS-1$
					if (downloadSocket != null)
						downloadSocket.close();
					return false;
				}
			}
			try {
				fos = new FileOutputStream(filename, false);
				is = new DataInputStream(downloadSocket.getInputStream());
			} catch (IOException e) {
				System.err.println(
						StringTable.getString("TextModeClient.TRANSFER_RECEIVE_SOCKET_OPEN_EXCEPTION")); //$NON-NLS-1$
				if (downloadSocket != null)
					downloadSocket.close();
				return false;
			}
			fileBytes = new byte[BUFFER_SIZE];
			try {
				startTime = System.currentTimeMillis();
				lastTime = startTime;
				while (receivedBytes < size) {
					count = is.read(fileBytes);
					fos.write(fileBytes, 0, count);
					receivedBytes += count;
					// Controla el spam
					if (System.currentTimeMillis() - lastTime >= printInterval) {
						System.out.print(String.format(
								StringTable.getString("TextModeClient.TRANSFER_RECEIVE_PROGRESS"), //$NON-NLS-1$
								receivedBytes, size, receivedBytes * 100 / size));
						lastTime = System.currentTimeMillis();
					}
				}
				endTime = System.currentTimeMillis();
				System.out.println();
				System.out.println(String.format(
						StringTable.getString("TextModeClient.TRANSFER_RECEIVE_RESULT"), receivedBytes, size, //$NON-NLS-1$
						(endTime - startTime) / 1000.0f));
				return receivedBytes == size;
			} catch (SocketTimeoutException e) {
				System.err.println(StringTable.getString("TextModeClient.TRANSFER_SOCKET_TIMEOUT_MESSAGE")); //$NON-NLS-1$
			} catch (IOException e) {
				System.err.println(StringTable.getString("TextModeClient.TRANSFER_RECEIVE_IO_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
			} finally {
				fos.close();
				downloadSocket.close();
			}
		} catch (InvalidTokenException e) {
			System.err.println(StringTable.getString("TextModeClient.SESSION_EXPIRED_MESSAGE")); //$NON-NLS-1$
		} catch (FileServerException e) {
			System.err.println(StringTable.getString("TextModeClient.TRANSFER_FILE_SERVER_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
		} catch (RemoteException e) {
			System.err.println(
					StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
		} catch (IOException e) {
			System.err.println(StringTable.getString("TextModeClient.TRANSFER_RECEIVE_SOCKET_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
		}
		return false;
	}

	private static boolean putFile(String filename, String path) {
		int count, uploadPort;
		long size, startTime, lastTime, endTime = 0, sentBytes = 0;
		final long printInterval = 125;
		FileInputStream fis;
		DataOutputStream os;
		Socket uploadSocket;
		byte[] fileBytes;
		try {
			// Obtener el tamaño de archivo.
			{
				File sourceFile = new File(filename);
				if (!sourceFile.exists()) {
					System.err.println(String.format(StringTable.getString("TextModeClient.TRANSFER_SEND_FILE_NOT_EXISTS"), filename)); //$NON-NLS-1$
					return false;
				} else {
					size = sourceFile.length();
				}
			}
			// Solicitar la transferencia.
			uploadPort = serverObj.putFile(token, path, size);

			try {
				uploadSocket = new Socket(serverAddress, uploadPort);
				uploadSocket.setSoTimeout(10000);
			} catch (IOException e) {
				System.err
						.println(StringTable.getString("TextModeClient.TRANSFER_SEND_SOCKET_OPEN_EXCEPTION")); //$NON-NLS-1$
				return false;
			}
			try {
				fis = new FileInputStream(filename);
				os = new DataOutputStream(uploadSocket.getOutputStream());
			} catch (IOException e) {
				System.err.println(StringTable.getString("TextModeClient.TRANSFER_SEND_FILE_OPEN_EXCEPTION")); //$NON-NLS-1$
				if (uploadSocket != null)
					uploadSocket.close();
				return false;
			}

			fileBytes = new byte[BUFFER_SIZE];
			try {
				startTime = System.currentTimeMillis();
				lastTime = startTime;
				while (sentBytes < size) {
					count = fis.read(fileBytes);
					os.write(fileBytes, 0, count);
					sentBytes += count;
					// Controla el spam
					if (System.currentTimeMillis() - lastTime >= printInterval) {
						System.out.print(
								String.format(StringTable.getString("TextModeClient.TRANSFER_SEND_PROGRESS"), //$NON-NLS-1$
										sentBytes, size, sentBytes * 100 / size));
						lastTime = System.currentTimeMillis();
					}
				}
				endTime = System.currentTimeMillis();
				System.out.println();
				System.out.println(String.format(StringTable.getString("TextModeClient.TRANSFER_SEND_RESULT"), //$NON-NLS-1$
						sentBytes, size, (endTime - startTime) / 1000.0f));
				return sentBytes == size;
			} catch (SocketTimeoutException e) {
				System.err.println(StringTable.getString("TextModeClient.TRANSFER_SOCKET_TIMEOUT_MESSAGE")); //$NON-NLS-1$
			} catch (IOException e) {
				System.err
						.println(StringTable.getString("TextModeClient.TRANSFER_SEND_IO_EXCEPTION") //$NON-NLS-1$
								+ e.getMessage());
			} finally {
				fis.close();
				uploadSocket.close();
			}
		} catch (InvalidTokenException e) {
			System.err.println(StringTable.getString("TextModeClient.SESSION_EXPIRED_MESSAGE")); //$NON-NLS-1$
		} catch (FileServerException e) {
			System.err.println(StringTable.getString("TextModeClient.TRANSFER_FILE_SERVER_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
		} catch (RemoteException e) {
			System.err.println(
					StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
		} catch (IOException e) {
			System.err.println(
					StringTable.getString("TextModeClient.TRANSFER_SOCKET_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
		}
		return false;
	}

	private static boolean parseCommandLine(String cmd) throws Exception {
		if (cmd.equals("logout") || cmd.equals("exit")) //$NON-NLS-1$ //$NON-NLS-2$
			return false;

		StringTokenizer mTokenizer = new StringTokenizer(cmd);
		if (mTokenizer.hasMoreTokens()) {
			// Identificar comando
			String theCommand = mTokenizer.nextToken();
			if (theCommand.equals("passwd")) { //$NON-NLS-1$
				changePassword();
			} else if (theCommand.equals("ls")) { //$NON-NLS-1$
				if (!mTokenizer.hasMoreTokens()) {
					System.err.println(StringTable.getString("TextModeClient.USAGE_LS")); //$NON-NLS-1$
					return true;
				}
				String path = mTokenizer.nextToken();
				list(path);
				return true;
			} else if (theCommand.equals("mkdir")) { //$NON-NLS-1$
				if (!mTokenizer.hasMoreTokens()) {
					System.err.println(StringTable.getString("TextModeClient.USAGE_MKDIR")); //$NON-NLS-1$
				}
				String path = mTokenizer.nextToken();
				makeDirectory(path);
			} else if (theCommand.equals("rmdir")) { //$NON-NLS-1$
				if (!mTokenizer.hasMoreTokens()) {
					System.err.println(StringTable.getString("TextModeClient.USAGE_MKDIR")); //$NON-NLS-1$
					return true;
				}
				String path = mTokenizer.nextToken();
				removeDirectory(path);
			} else if (theCommand.equals("rm")) { //$NON-NLS-1$
				if (!mTokenizer.hasMoreTokens()) {
					System.err.println(StringTable.getString("TextModeClient.USAGE_MKDIR")); //$NON-NLS-1$
					return true;
				}
				String path = mTokenizer.nextToken();
				removeFile(path);
			} else if (theCommand.equals("mv")) { //$NON-NLS-1$
				if (mTokenizer.countTokens() < 2) {
					System.err.println(StringTable.getString("TextModeClient.USAGE_MV")); //$NON-NLS-1$
					return true;
				}
				String path = mTokenizer.nextToken().trim();
				String newName = mTokenizer.nextToken().trim();
				move(path, newName);
			} else if (theCommand.equals("get")) { //$NON-NLS-1$
				if (!mTokenizer.hasMoreTokens()) {
					System.err.println(StringTable.getString("TextModeClient.USAGE_GET")); //$NON-NLS-1$
					return true;
				}
				String path = mTokenizer.nextToken();
				getFile(path);
			} else if (theCommand.equals("put")) { //$NON-NLS-1$
				if (mTokenizer.countTokens() < 2) {
					System.err.println(StringTable.getString("TextModeClient.USAGE_PUT")); //$NON-NLS-1$
					return true;
				}
				String filename = mTokenizer.nextToken().trim();
				String path = mTokenizer.nextToken().trim();
				putFile(filename, path);
			} else if (theCommand.equals("help")) { //$NON-NLS-1$
				help();
			} else {
				System.err.println(StringTable.getString("TextModeClient.COMMAND_NOT_RECOGNIZED")); //$NON-NLS-1$
				help();
			}
		}
		return true;
	}

	public static void start(String hostname) {
		System.out.println(StringTable.getString("TextModeClient.USING_CLIENT")); //$NON-NLS-1$

		try {
			String path = String.format("rmi://%s:%d/%s", hostname, PUERTO, RMIServerName); //$NON-NLS-1$
			System.out.println(
					String.format(StringTable.getString("TextModeClient.CONNECTING_TO_SERVER"), path)); //$NON-NLS-1$
			serverObj = (FileServer) Naming.lookup(path);
		} catch (NotBoundException e) {
			System.err.println(StringTable.getString("TextModeClient.RMI_LOOKUP_NOT_BOUND_MESSAGE")); //$NON-NLS-1$
			close(true);
		} catch (RemoteException e) {
			System.err.println(StringTable.getString("TextModeClient.RMI_LOOKUP_REMOTE_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
			close(true);
		} catch (Exception e) {
			e.printStackTrace();
			close(true);
		}

		serverAddress = new String(hostname);

		// Inicio de sesión
		Console console = System.console();
		String input = new String();

		while (true) {
			try {
				String username = "", password = ""; //$NON-NLS-1$ //$NON-NLS-2$
				while (username.isEmpty()) {
					System.out.print(StringTable.getString("TextModeClient.LOGIN_USERNAME_PROMPT")); //$NON-NLS-1$
					username = console.readLine();
				}
				while (password.isEmpty()) {
					System.out.print(StringTable.getString("TextModeClient.LOGIN_PASSWORD_PROMPT")); //$NON-NLS-1$
					password = new String(console.readPassword());
				}
				// El login
				System.out.println(StringTable.getString("TextModeClient.LOGGING_IN")); //$NON-NLS-1$
				token = serverObj.login(username, password);
				break;
			} catch (InvalidLoginException e) {
				System.err.println(StringTable.getString("TextModeClient.LOGIN_INVALID_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
			} catch (RemoteException e) {
				System.err.println(StringTable.getString("TextModeClient.LOGIN_REMOTE_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
				close(true);
			} catch (IOError e) {
				System.err.println(StringTable.getString("TextModeClient.LOGIN_CONSOLE_IO_ERROR")); //$NON-NLS-1$
				close(true);
			}
		}

		// Mantener la sesión abierta mandando un ping cada 30 segundos.
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		Runnable keepAliveProcess = new Runnable() {
			@Override
			public void run() {
				try {
					serverObj.ping(token);
				} catch (InvalidTokenException e) {
					System.err.println(
							StringTable.getString("TextModeClient.KEEP_ALIVE_SESSION_EXPIRED_MESSAGE")); //$NON-NLS-1$
					scheduler.shutdownNow();
					close(true);
				} catch (RemoteException e) {
					System.err.println(
							StringTable.getString("TextModeClient.KEEP_ALIVE_REMOTE_EXCEPTION")); //$NON-NLS-1$
					scheduler.shutdownNow();
					close(true);
				}
			}
		};
		scheduler.scheduleAtFixedRate(keepAliveProcess, 30, 30, TimeUnit.SECONDS);

		// Un resguardo para cerrar la sesión antes de ser liquidados por un Ctrl+C
		Runtime.getRuntime().addShutdownHook(shutdownHook);

		while (true) {
			try {
				System.out.print(prompt + "> "); //$NON-NLS-1$
				input = console.readLine();
				if (!parseCommandLine(input))
					break;
			} catch (IOError e) {
				System.err.println(StringTable.getString("TextModeClient.COMMAND_READ_ERROR")); //$NON-NLS-1$
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}

		try {
			scheduler.shutdownNow();
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
			System.out.println(StringTable.getString("TextModeClient.LOGGING_OUT")); //$NON-NLS-1$
			serverObj.logout(token);
		} catch (InvalidTokenException e) {
			System.err.println(StringTable.getString("TextModeClient.LOG_OUT_ERROR") + e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
		} catch (RemoteException e) {
			System.err.println(
					StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
		}
	}

	private static void close(boolean forced) {
		Runtime.getRuntime().removeShutdownHook(shutdownHook);
		if (forced)
			System.exit(1);
	}
}
