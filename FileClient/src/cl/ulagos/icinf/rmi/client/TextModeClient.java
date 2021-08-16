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
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.thirdparty.ThreadFactoryWithNamePrefix;

import cl.ulagos.icinf.rmi.providers.DirEntry;
import cl.ulagos.icinf.rmi.providers.EntryType;
import cl.ulagos.icinf.rmi.server.FileManagementException;
import cl.ulagos.icinf.rmi.server.FileServer;
import cl.ulagos.icinf.rmi.server.FileServerException;
import cl.ulagos.icinf.rmi.server.InvalidLoginException;
import cl.ulagos.icinf.rmi.server.InvalidTokenException;
import cl.ulagos.icinf.rmi.server.SessionToken;

public class TextModeClient {

	interface Command {
		int run(String[] params) throws InvalidTokenException, RemoteException;
	}

	private class ChangeDirectoryCommand implements Command {
		public int run(String[] params) throws RemoteException, InvalidTokenException {
			if (params.length < 1) {
				System.err.println(StringTable.getString("TextModeClient.USAGE_CD")); //$NON-NLS-1$
				return RETURN_ERROR;
			}

			try {
				String newDirectory = getNewPath(params[0]);
				DirEntry testEntry = serverObj.getInfo(token, newDirectory);
				if (testEntry.getType() != EntryType.DIRECTORY) {
					System.err.println(String.format("%s: no es un directorio.", params[0]));
					return RETURN_ERROR;
				} else {
					currentDirectory = newDirectory;
					return RETURN_OK;
				}
			} catch (FileManagementException e) {
				System.err.println(String.format("%s: No existe el directorio.", params[0]));
				return RETURN_ERROR;
			}
		}
	}

	private class ChangePasswordCommand implements Command {
		public int run(String[] params) throws RemoteException, InvalidTokenException {
			char[] caPassword;
			String oldPassword, newPassword, newPassword2;
			Console c = System.console();

			try {
				while (true) {
					caPassword = c.readPassword(StringTable.getString("TextModeClient.PASSWD_ENTER_CURRENT_PASSWORD")); //$NON-NLS-1$
					if (caPassword == null)
						continue;
					oldPassword = new String(caPassword).trim();
					if (oldPassword.isEmpty())
						continue;
					break;
				}
				while (true) {
					caPassword = c.readPassword(StringTable.getString("TextModeClient.PASSWD_ENTER_NEW_PASSWORD")); //$NON-NLS-1$
					if (caPassword == null)
						continue;
					newPassword = new String(caPassword).trim();
					if (newPassword.isEmpty())
						continue;
					break;
				}
				while (true) {
					caPassword = c.readPassword(StringTable.getString("TextModeClient.PASSWD_CONFIRM_NEW_PASSWORD")); //$NON-NLS-1$
					if (caPassword == null)
						continue;
					newPassword2 = new String(caPassword).trim();
					if (newPassword2.isEmpty())
						continue;
					break;
				}
			} catch (IOError e) {
				System.err.println(StringTable.getString("TextModeClient.CONSOLE_IO_ERROR")); //$NON-NLS-1$
				return RETURN_ERROR;
			}

			if (!newPassword2.equals(newPassword)) {
				System.err.println(StringTable.getString("TextModeClient.PASSWD_CHANGE_PWDS_DO_NOT_MATCH")); //$NON-NLS-1$
				return RETURN_ERROR;
			}

			if (newPassword.equals(oldPassword)) {
				System.err.println(StringTable.getString("TextModeClient.PASSWD_CHANGE_MUST_BE_DIFFERENT")); //$NON-NLS-1$
				return RETURN_ERROR;
			}

			try {
				serverObj.changePassword(token, oldPassword, newPassword);
				System.out.println(StringTable.getString("TextModeClient.PASSWD_CHANGE_SUCCESSFUL")); //$NON-NLS-1$
				return RETURN_OK;
			} catch (FileServerException e) {
				System.err.println(StringTable.getString("TextModeClient.PASSWD_CHANGE_FAILED") + e.getMessage()); //$NON-NLS-1$
				return RETURN_ERROR;
			}
		}

	}

	private class ExitCommand implements Command {
		public int run(String[] params) {
			return RETURN_QUIT;
		}
	}

	private boolean confirmAction(String message, String message2) {
		String answer;
		Console c = System.console();
		System.out.println(message);
		while (true) {
			System.out.print(String.format(StringTable.getString("TextModeClient.YES_NO_PROMPT"), message)); //$NON-NLS-1$
			try {
				while (true) {
					answer = c.readLine(); // $NON-NLS-1$
					if (answer == null)
						continue;
					if (answer.isEmpty())
						continue;
					break;
				}
				answer = answer.toLowerCase();
				if (answer.charAt(0) == 's' || answer.charAt(0) == 'y') {
					return true;
				} else if (answer.charAt(0) == 'n') {
					return false;
				} else {
					System.err.println(StringTable.getString("TextModeClient.CONFIRM_INVALID_INPUT_MESSAGE")); //$NON-NLS-1$
				}
			} catch (IOError e) {
				System.err.println(StringTable.getString("TextModeClient.CONSOLE_IO_ERROR")); //$NON-NLS-1$
				return false;
			}
		}
	}

	private class GetCommand implements Command {
		public int run(String[] params) throws RemoteException, InvalidTokenException {
			if (params.length < 1) {
				System.err.println(StringTable.getString("TextModeClient.USAGE_GET")); //$NON-NLS-1$
				return RETURN_ERROR;
			}

			String path = getNewPath(params[0]);

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
									filename),
							StringTable.getString("TextModeClient.TRANSFER_RECEIVE_CONFIRM_OVERWRITE"))) { //$NON-NLS-1$
						if (downloadSocket != null)
							downloadSocket.close();
						return RETURN_ERROR;
					}
				}
				try {
					fos = new FileOutputStream(filename, false);
					is = new DataInputStream(downloadSocket.getInputStream());
				} catch (IOException e) {
					System.err.println(StringTable.getString("TextModeClient.TRANSFER_RECEIVE_SOCKET_OPEN_EXCEPTION")); //$NON-NLS-1$
					if (downloadSocket != null)
						downloadSocket.close();
					return RETURN_ERROR;
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
							System.out.print(
									String.format(StringTable.getString("TextModeClient.TRANSFER_RECEIVE_PROGRESS"), //$NON-NLS-1$
											receivedBytes, size, receivedBytes * 100 / size));
							lastTime = System.currentTimeMillis();
						}
					}
					endTime = System.currentTimeMillis();
					System.out.println();
					System.out.println(String.format(StringTable.getString("TextModeClient.TRANSFER_RECEIVE_RESULT"), //$NON-NLS-1$
							receivedBytes, size, (endTime - startTime) / 1000.0f));
				} catch (SocketTimeoutException e) {
					System.err.println(StringTable.getString("TextModeClient.TRANSFER_SOCKET_TIMEOUT_MESSAGE")); //$NON-NLS-1$
				} catch (IOException e) {
					System.err.println(
							StringTable.getString("TextModeClient.TRANSFER_RECEIVE_IO_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
				} finally {
					fos.close();
					downloadSocket.close();
				}
				return receivedBytes == size ? RETURN_OK : RETURN_ERROR;
			} catch (FileServerException e) {
				System.err.println(StringTable.getString("TextModeClient.TRANSFER_FILE_SERVER_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
			} catch (IOException e) {
				System.err.println(StringTable.getString("TextModeClient.TRANSFER_RECEIVE_SOCKET_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
			}
			return RETURN_ERROR;
		}

	}

	private class HelpCommand implements Command {
		public int run(String[] params) {
			help();
			return RETURN_OK;
		}
	}

	private class ListCommand implements Command {
		public int run(String[] params) throws RemoteException, InvalidTokenException {
			try {
				List<DirEntry> list = serverObj.readDirectory(token, currentDirectory);
				if (list.size() == 0) {
					System.out.println(StringTable.getString("TextModeClient.LIST_NO_ELEMENTS")); //$NON-NLS-1$
				} else {
					System.out.println(String.format(StringTable.getString("TextModeClient.LIST_TOTAL"), list.size())); //$NON-NLS-1$
					for (DirEntry de : list)
						System.out.println(
								String.format("<%s> %8d %s", de.getType() == EntryType.DIRECTORY ? "DIR" : "FILE", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										de.getSize(), de.getName()));
				}
				return RETURN_OK;
			} catch (FileManagementException e) {
				System.err.println(StringTable.getString("TextModeClient.FILE_MANAGEMENT_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
				return RETURN_ERROR;
			}
		}

	}

	private class MakeDirCommand implements Command {
		public int run(String[] params) throws RemoteException, InvalidTokenException {
			if (params.length < 1) {
				System.err.println(StringTable.getString("TextModeClient.USAGE_MKDIR")); //$NON-NLS-1$
				return RETURN_ERROR;
			}
			try {
				String newDirectory = getNewPath(params[0]);
				if (serverObj.createDirectory(token, newDirectory))
					System.out.println(StringTable.getString("TextModeClient.MKDIR_SUCCESSFUL")); //$NON-NLS-1$
				return RETURN_OK;
			} catch (FileManagementException e) {
				System.err.println(StringTable.getString("TextModeClient.FILE_MANAGEMENT_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
				return RETURN_ERROR;
			}

		}

	}

	private class MoveCommand implements Command {
		public int run(String[] params) throws RemoteException, InvalidTokenException {
			if (params.length < 2) {
				System.err.println(StringTable.getString("TextModeClient.USAGE_MV")); //$NON-NLS-1$
				return RETURN_ERROR;
			}
			String oldPath = getNewPath(params[0]);
			String newPath = params[1];

			try {
				DirEntry info = serverObj.getInfo(token, oldPath);
				if (info.getType() == EntryType.DIRECTORY) {
					if (serverObj.moveDirectory(token, oldPath, newPath))
						System.out.println(StringTable.getString("TextModeClient.MV_DIR_SUCCESSFUL")); //$NON-NLS-1$
				} else if (info.getType() == EntryType.FILE) {
					if (serverObj.moveFile(token, oldPath, newPath))
						System.out.println(StringTable.getString("TextModeClient.MV_FILE_SUCCESSFUL")); //$NON-NLS-1$
				}
				return RETURN_OK;
			} catch (FileManagementException e) {
				System.err.println(StringTable.getString("TextModeClient.FILE_MANAGEMENT_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
				return RETURN_ERROR;
			}
		}
	}

	private class PutCommand implements Command {
		public int run(String[] params) {
			if (params.length < 1) {
				System.err.println(StringTable.getString("TextModeClient.USAGE_PUT")); //$NON-NLS-1$
				return RETURN_ERROR;
			}

			int count, uploadPort;
			long size, startTime, lastTime, endTime = 0, sentBytes = 0;
			final long printInterval = 125;
			FileInputStream fis;
			DataOutputStream os;
			Socket uploadSocket;
			byte[] fileBytes;

			String filename = params[0];
			String path = getNewPath(currentDirectory + "/" + params[0]);
			try {
				// Obtener el tamaño de archivo.
				{
					File sourceFile = new File(filename);
					if (!sourceFile.exists()) {
						System.err.println(String.format(
								StringTable.getString("TextModeClient.TRANSFER_SEND_FILE_NOT_EXISTS"), filename)); //$NON-NLS-1$
						return RETURN_ERROR;
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
					System.err.println(StringTable.getString("TextModeClient.TRANSFER_SEND_SOCKET_OPEN_EXCEPTION")); //$NON-NLS-1$
					return RETURN_ERROR;
				}
				try {
					fis = new FileInputStream(filename);
					os = new DataOutputStream(uploadSocket.getOutputStream());
				} catch (IOException e) {
					System.err.println(StringTable.getString("TextModeClient.TRANSFER_SEND_FILE_OPEN_EXCEPTION")); //$NON-NLS-1$
					if (uploadSocket != null)
						uploadSocket.close();
					return RETURN_ERROR;
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
							System.out
									.print(String.format(StringTable.getString("TextModeClient.TRANSFER_SEND_PROGRESS"), //$NON-NLS-1$
											sentBytes, size, sentBytes * 100 / size));
							lastTime = System.currentTimeMillis();
						}
					}
					endTime = System.currentTimeMillis();
					System.out.println();
					System.out.println(String.format(StringTable.getString("TextModeClient.TRANSFER_SEND_RESULT"), //$NON-NLS-1$
							sentBytes, size, (endTime - startTime) / 1000.0f));
				} catch (SocketTimeoutException e) {
					System.err.println(StringTable.getString("TextModeClient.TRANSFER_SOCKET_TIMEOUT_MESSAGE")); //$NON-NLS-1$
				} catch (IOException e) {
					System.err.println(StringTable.getString("TextModeClient.TRANSFER_SEND_IO_EXCEPTION") //$NON-NLS-1$
							+ e.getMessage());
				} finally {
					fis.close();
					uploadSocket.close();
				}
				return sentBytes == size ? RETURN_OK : RETURN_ERROR;
			} catch (FileServerException e) {
				System.err.println(StringTable.getString("TextModeClient.TRANSFER_FILE_SERVER_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
				return RETURN_ERROR;
			} catch (IOException e) {
				System.err.println(StringTable.getString("TextModeClient.TRANSFER_SOCKET_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
				return RETURN_ERROR;
			}
		}

	}

	private class RemoveCommand implements Command {
		public int run(String[] params) throws RemoteException, InvalidTokenException {
			if (params.length < 1) {
				System.err.println(StringTable.getString("TextModeClient.USAGE_RM")); //$NON-NLS-1$
				return RETURN_ERROR;
			}
			String path = getNewPath(params[0]);

			try {
				if (serverObj.deleteFile(token, path))
					System.out.println(StringTable.getString("TextModeClient.RM_SUCCESSFUL")); //$NON-NLS-1$
				return RETURN_OK;
			} catch (FileManagementException e) {
				System.err.println(StringTable.getString("TextModeClient.FILE_MANAGEMENT_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
				return RETURN_ERROR;
			}
		}

	}

	private class RmDirCommand implements Command {
		public int run(String[] params) throws RemoteException, InvalidTokenException {
			if (params.length < 1) {
				System.err.println(StringTable.getString("TextModeClient.USAGE_RMDIR")); //$NON-NLS-1$
				return RETURN_ERROR;
			}
			try {
				String newDirectory = getNewPath(params[0]);
				if (serverObj.deleteDirectory(token, newDirectory))
					System.out.println(StringTable.getString("TextModeClient.RMDIR_SUCCESSFUL")); //$NON-NLS-1$
				return RETURN_OK;
			} catch (FileManagementException e) {
				System.err.println(StringTable.getString("TextModeClient.FILE_MANAGEMENT_EXCEPTION") //$NON-NLS-1$
						+ e.getMessage());
				return RETURN_ERROR;
			}
		}

	}

	private static final int BUFFER_SIZE = 2048;
	private static final String prompt = StringTable.getString("TextModeClient.PROMPT"); //$NON-NLS-1$
	private static final int PUERTO = 8527;
	private static final int RETURN_ERROR = 1;

	private static final int RETURN_OK = 0;

	private static final int RETURN_QUIT = -1;

	private static final String RMIServerName = "RMIFileServer"; //$NON-NLS-1$

	public static void start(String hostname) {
		FileServer serverObj = null;
		String path = String.format("rmi://%s:%d/%s", hostname, PUERTO, RMIServerName); //$NON-NLS-1$

		System.out.println(StringTable.getString("TextModeClient.USING_CLIENT_V2")); //$NON-NLS-1$

		if (System.console() == null) {
			System.err.println("No se puede operar sin una terminal. La E/S estándar no basta.");
			System.exit(1);
		}

		try {
			System.out.println(String.format(StringTable.getString("TextModeClient.CONNECTING_TO_SERVER"), path)); //$NON-NLS-1$
			serverObj = (FileServer) Naming.lookup(path);
		} catch (ConnectException e) {
			System.err.println(StringTable.getString("TextModeClient.RMI_REFUSED_CONNECTION_EXCEPTION")); //$NON-NLS-1$
		} catch (NotBoundException e) {
			System.err.println(StringTable.getString("TextModeClient.RMI_NOT_BOUND_EXCEPTION")); //$NON-NLS-1$
		} catch (RemoteException e) {
			System.err.println(StringTable.getString("TextModeClient.RMI_REMOTE_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (serverObj == null)
				System.exit(1);
		}

		TextModeClient client = new TextModeClient(serverObj, hostname);
		try {
			while (!client.login())
				;
		} catch (RemoteException e) {
			System.err.println(StringTable.getString("TextModeClient.LOGIN_REMOTE_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(client.shutdownHook);
		client.enterCommandInterpreter();
		System.exit(0);
	}

	private HashMap<String, Command> commands;
	private String currentDirectory = "/"; //$NON-NLS-1$
	private boolean logged;
	private ScheduledExecutorService pingScheduler;
	private String serverAddress;
	private FileServer serverObj;
	private Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			if (serverObj != null) {
				if (logged) {
					System.err.println(StringTable.getString("TextModeClient.LOGGING_OUT")); //$NON-NLS-1$
					logout();
				}
			}
		}
	};

	private SessionToken token;

	public TextModeClient(FileServer serverObject, String hostname) {
		serverObj = serverObject;
		serverAddress = new String(hostname);
		logged = false;
	}

	private Deque<String> tokenizePath(String path) {
		StringTokenizer tokenizer = new StringTokenizer(path, "/");
		Deque<String> pathComponents = new LinkedList<String>();
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (".".equals(token)) {
				continue;
			} else {
				pathComponents.add(token);
			}
		}
		return pathComponents;
	}

	private Deque<String> canonicalizePath(Deque<String> path) {
		Deque<String> pathCopy = new LinkedList<String>(path);
		Deque<String> result = new LinkedList<String>();
		while (!pathCopy.isEmpty()) {
			String element = pathCopy.removeFirst();
			if (".".equals(element)) {
				continue;
			} else if ("..".equals(element)) {
				if (!result.isEmpty())
					result.removeLast();
			} else {
				result.addLast(element);
			}
		}
		return result;
	}

	private String pathToString(Deque<String> path) {
		Deque<String> pathCopy = new LinkedList<String>(path);
		StringBuilder resultingPath = new StringBuilder();
		resultingPath.append('/');
		while (!pathCopy.isEmpty()) {
			resultingPath.append(pathCopy.removeFirst());
			if (!pathCopy.isEmpty())
				resultingPath.append('/');
		}
		return resultingPath.toString();
	}

	private String getNewPath(String input) {
		String result;
		if (input.charAt(0) == '/') {
			result = pathToString(canonicalizePath(tokenizePath(input)));
		} else {
			Deque<String> resultingPath = tokenizePath(currentDirectory);
			resultingPath.addAll(tokenizePath(input));
			result = pathToString(canonicalizePath(resultingPath));
		}
		return result;
	}

	private void enterCommandInterpreter() {
		String input;
		Console c = System.console();

		commands = new HashMap<>();
		commands.put("ls", new ListCommand());
		commands.put("cd", new ChangeDirectoryCommand());
		commands.put("mv", new MoveCommand());
		commands.put("rm", new RemoveCommand());
		commands.put("rmdir", new RmDirCommand());
		commands.put("mkdir", new MakeDirCommand());
		commands.put("get", new GetCommand());
		commands.put("put", new PutCommand());
		commands.put("passwd", new ChangePasswordCommand());
		commands.put("help", new HelpCommand());
		commands.put("quit", new ExitCommand());
		commands.put("exit", new ExitCommand());

		System.out.println(StringTable.getString("TextModeClient.COMMAND_PROMPT_INTRO")); //$NON-NLS-1$
		while (true) {
			try {
				input = c.readLine(String.format("%s> ", currentDirectory)); //$NON-NLS-1$
				if (input == null)
					continue;
				if (input.isEmpty())
					continue;
				if (dispatchCommand(input) == RETURN_QUIT)
					break;
			} catch (IOError e) {
				System.err.println(StringTable.getString("TextModeClient.COMMAND_READ_ERROR")); //$NON-NLS-1$
			}
		}
		System.err.println(StringTable.getString("TextModeClient.QUITTING"));
	}

	private int dispatchCommand(String input) {
		Queue<String> tokenizedInput = new LinkedList<String>();
		Pattern regexPattern = Pattern.compile("\"([^\"]*)\"|(\\S+)");
		Matcher matcher = regexPattern.matcher(input);

		while (matcher.find()) {
			if (matcher.group(1) != null)
				tokenizedInput.add(matcher.group(1));
			else
				tokenizedInput.add(matcher.group(2));
		}

		String command = tokenizedInput.remove();

		String[] parameters = new String[tokenizedInput.size()];
		for (int i = 0; tokenizedInput.size() > 0; i++)
			parameters[i] = tokenizedInput.remove();

		if (!commands.containsKey(command)) {
			System.err.println(StringTable.getString("TextModeClient.COMMAND_NOT_RECOGNIZED")); //$NON-NLS-1$
			return help();
		} else {
			try {
				return commands.get(command).run(parameters);
			} catch (InvalidTokenException e) {
				System.err.println(StringTable.getString("TextModeClient.SESSION_EXPIRED_MESSAGE")); //$NON-NLS-1$
				logout();
				return RETURN_QUIT;
			} catch (RemoteException e) {
				System.err.println(StringTable.getString("TextModeClient.REMOTE_EXCEPTION") + e.getMessage()); //$NON-NLS-1$
				logout();
				return RETURN_QUIT;
			} // ¿Que tal?
		}
	}

	private int help() {
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

		return 0;
	}

	private boolean login() throws RemoteException {
		char[] caPassword;
		String username, password;
		Console c = System.console();

		if (logged)
			throw new RuntimeException("Ya se ha iniciado sesión.");

		try {
			while (true) {
				username = c.readLine(StringTable.getString("TextModeClient.LOGIN_USERNAME_PROMPT")); //$NON-NLS-1$
				if (username == null)
					continue;
				if (username.isEmpty())
					continue;
				break;
			}
			while (true) {
				caPassword = c.readPassword(StringTable.getString("TextModeClient.LOGIN_PASSWORD_PROMPT")); //$NON-NLS-1$
				if (caPassword == null)
					continue;
				password = new String(caPassword).trim();
				if (password.isEmpty())
					continue;
				break;
			}
		} catch (IOError e) {
			System.err.println(StringTable.getString("TextModeClient.LOGIN_INVALID_INPUT")); //$NON-NLS-1$
			return false;
		}

		try {
			token = serverObj.login(username, password);
			logged = true;
			startSessionWatchdog();
			return true;
		} catch (InvalidLoginException e) {
			System.err.println(StringTable.getString("TextModeClient.LOGIN_INVALID_EXCEPTION") //$NON-NLS-1$
					+ e.getMessage());
		}
		return false;
	}

	private void logout() {
		if (pingScheduler != null)
			if (!pingScheduler.isShutdown())
				pingScheduler.shutdownNow();

		try {
			serverObj.logout(token);
		} catch (Exception e) {
			System.err.println(StringTable.getString("TextModeClient.LOG_OUT_ERROR") + e.getMessage()); //$NON-NLS-1$
		} finally {
			logged = false;
		}
	}

	private void startSessionWatchdog() {
		pingScheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryWithNamePrefix("session-watchdog-thread")); //$NON-NLS-1$
		Runnable watchdog = new Runnable() {
			public void run() {
				try {
					serverObj.ping(token);
				} catch (InvalidTokenException e) {
					System.err.println(StringTable.getString("TextModeClient.KEEP_ALIVE_SESSION_EXPIRED_MESSAGE")); //$NON-NLS-1$
					logout();
				} catch (RemoteException e) {
					System.err.println(StringTable.getString("TextModeClient.KEEP_ALIVE_REMOTE_EXCEPTION")); //$NON-NLS-1$
					logout();
				}
			}
		};
		pingScheduler.scheduleAtFixedRate(watchdog, 30, 30, TimeUnit.SECONDS);
	}
}
