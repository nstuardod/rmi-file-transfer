package cl.ulagos.icinf.rmi.server;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cl.ulagos.icinf.rmi.StringTable;
import cl.ulagos.icinf.rmi.providers.CannotInitProviderException;
import cl.ulagos.icinf.rmi.providers.DirEntry;
import cl.ulagos.icinf.rmi.providers.EntryNotFoundException;
import cl.ulagos.icinf.rmi.providers.FileProvider;
import cl.ulagos.icinf.rmi.providers.FileProviderException;
import cl.ulagos.icinf.rmi.providers.InvalidAccessException;
import cl.ulagos.icinf.rmi.server.FileTransferService.TransferMode;
import cl.ulagos.icinf.rmi.server.providers.LocalFileProvider;
import cl.ulagos.icinf.rmi.server.util.ServerUtils;

public class FileServerImpl extends UnicastRemoteObject implements FileServer {

	private static Logger logger = Logger.getLogger("FileServerImpl"); //$NON-NLS-1$
	private static final int monitorInterval = 60;
	private static final int PUERTO = 8522; // U - L - A - C (Control)
	private static final int LARGO_PASSWORDS = 8;
	private static final Random RANDOM = new SecureRandom();
	private Map<String, SessionStatus> sessions;
	private Map<SessionToken, FileProvider> providers;
	private Runnable sessionMonitor;
	private ScheduledExecutorService scheduler;
	private UsersDatabase userDatabase;

	public FileServerImpl() throws CannotInitServerException, RemoteException {
		super(PUERTO);
		sessions = new HashMap<String, SessionStatus>();
		providers = new HashMap<SessionToken, FileProvider>();
		try {
			userDatabase = new UsersDatabase();
		} catch (UserDBException e) {
			// Libera el objeto exportado
			UnicastRemoteObject.unexportObject(this, true);
			throw new CannotInitServerException(StringTable.getString("FileServerImpl.DB_INITIALIZATION_FAILED"), e); //$NON-NLS-1$
		}
		sessionMonitor = new Runnable() {
			private Logger logger = Logger.getLogger("sessionMonitor"); //$NON-NLS-1$

			@Override
			public void run() {
				logger.info(StringTable.getString("FileServerImpl.SESSION_SWEEP_START")); //$NON-NLS-1$
				int nRemovedSessions = 0;
				Set<Entry<String, SessionStatus>> setS = sessions.entrySet();
				for (Entry<String, SessionStatus> entry : setS) {
					if (!entry.getValue().isSessionValid()) {
						logger.info(String.format(StringTable.getString("FileServerImpl.SESSION_TERMINATED"), entry.getKey())); //$NON-NLS-1$
						SessionToken realToken = entry.getValue().getToken();
						providers.remove(realToken);
						sessions.remove(entry.getKey());
						nRemovedSessions++;
					}
				}
				if (nRemovedSessions > 0)
					logger.info(String.format(StringTable.getString("FileServerImpl.TERMINATED_SESSIONS"), nRemovedSessions)); //$NON-NLS-1$
			}
		};
		// Arranca un monitor de sesiones
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(sessionMonitor, monitorInterval, monitorInterval, TimeUnit.SECONDS);
		logger.info(String.format(StringTable.getString("FileServerImpl.SCHEDULING_SESSIONMONITOR"), monitorInterval)); //$NON-NLS-1$
	}

	private String getRootForUser(String user) throws FileServerException {
		try {
			String root = userDatabase.getRootFolder(user);
			if (root == null)
				throw new FileServerException(String.format(StringTable.getString("FileServerImpl.COULD_NOT_FIND_ROOT_DIRECTORY"), user)); //$NON-NLS-1$
			else
				return root;
		} catch (UserDBException e) {
			throw new FileServerException(StringTable.getString("FileServerImpl.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}
	}

	@Override
	public synchronized SessionToken login(String user, String password) throws InvalidLoginException, RemoteException {
		user = user.trim();
		password = password.trim();
		try {
			if (!userDatabase.authenticateUser(user, password)) {
				logger.warn(String.format(StringTable.getString("FileServerImpl.LOG_FAILED_LOGIN_ATTEMPT"), user)); //$NON-NLS-1$
				throw new InvalidLoginException(StringTable.getString("FileServerImpl.LOGIN_ATTEMPT_FAILED")); //$NON-NLS-1$
			}
		} catch (UserDBException e) {
			throw new RemoteException(StringTable.getString("FileServerImpl.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}

		byte[] token_bytes = new byte[24];
		RANDOM.nextBytes(token_bytes);
		SessionToken newToken = new SessionToken(ServerUtils.bytesToHex(token_bytes));
		try {
			FileProvider provider = new LocalFileProvider(getRootForUser(user));
			providers.put(newToken, provider);
		} catch (CannotInitProviderException e) {
			throw new RemoteException(StringTable.getString("FileServerImpl.COULD_NOT_ACCESS_USER_DIRECTORY"), e); //$NON-NLS-1$
		} catch (Exception e) {
			throw new RemoteException(StringTable.getString("FileServerImpl.COULD_NOT_GET_USER_DIRECTORY"), e); //$NON-NLS-1$
		}
		sessions.put(newToken.getToken(), new SessionStatus(user, newToken));
		logger.info(String.format(StringTable.getString("FileServerImpl.USER_LOGS_IN"), user)); //$NON-NLS-1$
		return newToken;
	}

	public String createUser(String user) throws UserAlreadyExistsException, FileServerException {
		user = user.trim();
		try {
			if (userDatabase.userExists(user))
				throw new UserAlreadyExistsException(String.format(StringTable.getString("FileServerImpl.USER_ALREADY_EXISTS"), user)); //$NON-NLS-1$

			String password = ServerUtils.randomString(LARGO_PASSWORDS);
			// Que alguien ponga una forma buena de asignar directorios raíz.
			userDatabase.addUser(user, password, user);
			logger.info(String.format(StringTable.getString("FileServerImpl.ADDUSER_SUCCESS"), user)); //$NON-NLS-1$
			return password;
		} catch (UserDBException e) {
			throw new FileServerException(StringTable.getString("FileServerImpl.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}
	}

	public void deleteUser(String user) throws UserNotExistsException, FileServerException {
		user = user.trim();

		try {
			if (!userDatabase.userExists(user))
				throw new UserNotExistsException(String.format(StringTable.getString("FileServerImpl.USER_NOT_EXISTS"), user)); //$NON-NLS-1$
		} catch (UserDBException e) {
			throw new FileServerException(StringTable.getString("FileServerImpl.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}
		/*
		 * XXX: En vez de modificar la interfaz de nuevo otra vez iniciamos un proveedor
		 * para eliminar el directorio del usuario
		 */
		{
			try {
				String root = getRootForUser(user);
				FileProvider provider = new LocalFileProvider();
				provider.getInfo(root);
				provider.deleteDirectory(root, true);
				logger.info(String.format(StringTable.getString("FileServerImpl.USER_DIRECTORY_DELETED"), user)); //$NON-NLS-1$
				userDatabase.deleteUser(user);
			} catch (EntryNotFoundException e) {
				logger.warn(
						StringTable.getString("FileServerImpl.USER_DIRECTORY_NOT_EXISTS")); //$NON-NLS-1$
			} catch (CannotInitProviderException e) {
				throw new FileServerException(StringTable.getString("FileServerImpl.USER_DIRECTORY_NOT_FOUND"), e); //$NON-NLS-1$
			} catch (UserDBException e) {
				throw new FileServerException(StringTable.getString("FileServerImpl.DB_QUERY_FAILED"), e); //$NON-NLS-1$
			} catch (FileProviderException e) {
				throw new FileServerException(StringTable.getString("FileServerImpl.USER_DIRECTORY_DELETION_FAILED"), e); //$NON-NLS-1$
			}
		}
		logger.info(String.format(StringTable.getString("FileServerImpl.DELUSER_SUCCESS"), user)); //$NON-NLS-1$
	}

	public String resetPassword(String user) throws UserNotExistsException, FileServerException {
		user = user.trim();
		try {
			if (!userDatabase.userExists(user))
				throw new UserNotExistsException(String.format(StringTable.getString("FileServerImpl.USER_NOT_EXISTS"), user)); //$NON-NLS-1$
			String password = ServerUtils.randomString(LARGO_PASSWORDS);
			// Que alguien ponga una forma buena de asignar directorios raíz.
			userDatabase.changePassword(user, password);
			logger.info(String.format(StringTable.getString("FileServerImpl.PASSWORD_RESET_SUCCESS"), user)); //$NON-NLS-1$
			return password;
		} catch (UserDBException e) {
			throw new FileServerException(StringTable.getString("FileServerImpl.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}
	}

	@Override
	public void changePassword(SessionToken token, String password, String newPassword)
			throws RemoteException, InvalidLoginException, InvalidTokenException, FileServerException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}

		// Obtiene el nombre de usuario del SessionInfo
		String user = sessions.get(token.getToken()).getUsername();
		password = password.trim();
		newPassword = newPassword.trim();

		if (newPassword.isEmpty()) {
			throw new FileServerException(StringTable.getString("FileServerImpl.PASSWORD_CHANGE_FAILED_EMPTY")); //$NON-NLS-1$
		}

		try {
			if (!userDatabase.authenticateUser(user, password))
				throw new InvalidLoginException(StringTable.getString("FileServerImpl.PASSWORD_CHANGE_INVALID_PASSWORD")); //$NON-NLS-1$
		} catch (UserDBException e) {
			throw new FileServerException(StringTable.getString("FileServerImpl.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}
		try {
			userDatabase.changePassword(user, newPassword);
			logger.info(String.format(StringTable.getString("FileServerImpl.PASSWORD_CHANGE_SUCCESS"), user)); //$NON-NLS-1$
		} catch (UserDBException e) {
			throw new FileServerException(StringTable.getString("FileServerImpl.PASSWORD_CHANGE_FAILED_IN_DB"), e); //$NON-NLS-1$
		}
	}

	public void changePassword(String user, String password, String newPassword)
			throws RemoteException, FileServerException {
		user = user.trim();
		password = password.trim();
		newPassword = newPassword.trim();

		try {
			if (!userDatabase.userExists(user))
				throw new FileServerException(String.format(StringTable.getString("FileServerImpl.USER_NOT_EXISTS"), user)); //$NON-NLS-1$
			if (!userDatabase.authenticateUser(user, password))
				throw new FileServerException(StringTable.getString("FileServerImpl.PASSWORD_CHANGE_INVALID_PASSWORD2")); //$NON-NLS-1$
		} catch (UserDBException e) {
			throw new FileServerException(StringTable.getString("FileServerImpl.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}

		try {
			userDatabase.changePassword(user, newPassword);
			logger.info(String.format(StringTable.getString("FileServerImpl.PASSWORD_CHANGE_SUCCESS"), user)); //$NON-NLS-1$
		} catch (UserDBException e) {
			throw new RemoteException(StringTable.getString("FileServerImpl.PASSWORD_CHANGE_FAILED"), e); //$NON-NLS-1$
		}
	}

	public List<String> getUsers() throws FileServerException {
		try {
			return userDatabase.listUsers();
		} catch (UserDBException e) {
			throw new FileServerException(StringTable.getString("FileServerImpl.DB_QUERY_FAILED"), e); //$NON-NLS-1$
		}
	}

	@Override
	public void ping(SessionToken token) throws InvalidTokenException, RemoteException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		sessions.get(token.getToken()).updateDate();
	}

	private void dumpMaps() {
		logger.debug(StringTable.getString("FileServerImpl.MAPS_DUMP")); //$NON-NLS-1$
		logger.debug(StringTable.getString("FileServerImpl.SESSIONS_DUMP")); //$NON-NLS-1$
		Set<Entry<String, SessionStatus>> setS = sessions.entrySet();
		for (Entry<String, SessionStatus> entry : setS) {
			logger.debug(String.format("%s -> [%s, %s]", entry.getKey(), entry.getValue().getUsername(), //$NON-NLS-1$
					entry.getValue().getLastActionDate()));
		}

		Set<Entry<SessionToken, FileProvider>> setP = providers.entrySet();
		logger.debug(StringTable.getString("FileServerImpl.PROVIDERS_DUMP")); //$NON-NLS-1$
		for (Entry<SessionToken, FileProvider> entry : setP) {
			logger.debug(String.format("%s -> [%s]", entry.getKey(), entry.getValue().getProviderName())); //$NON-NLS-1$
		}
	}

	@Override
	public synchronized void logout(SessionToken token) throws RemoteException, InvalidTokenException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		logger.info(String.format(StringTable.getString("FileServerImpl.USER_LOGS_OUT"), sessions.get(token.getToken()).getUsername())); //$NON-NLS-1$
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		providers.remove(realToken);
		sessions.remove(token.getToken());
	}

	@Override
	public DirEntry getInfo(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		FileProvider provider = providers.get(realToken);
		path = path.trim();

		try {
			return provider.getInfo(path);
		} catch (InvalidAccessException ex) {
			throw new FileManagementException(StringTable.getString("FileServerImpl.PATH_INVALID")); //$NON-NLS-1$
		} catch (FileProviderException ex) {
			throw new FileManagementException(ex.getMessage());
		}
	}

	@Override
	public List<DirEntry> readDirectory(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		FileProvider provider = providers.get(realToken);
		path = path.trim();

		try {
			return provider.readDirectory(path);
		} catch (InvalidAccessException ex) {
			throw new FileManagementException(StringTable.getString("FileServerImpl.PATH_INVALID")); //$NON-NLS-1$
		} catch (FileProviderException ex) {
			throw new FileManagementException(ex.getMessage());
		}
	}

	@Override
	public boolean createDirectory(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException {
		logger.trace("Entrado createDirectory"); //$NON-NLS-1$
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		FileProvider provider = providers.get(realToken);
		path = path.trim();

		try {
			return provider.createDirectory(path);
		} catch (InvalidAccessException ex) {
			throw new FileManagementException(StringTable.getString("FileServerImpl.PATH_INVALID")); //$NON-NLS-1$
		} catch (FileProviderException ex) {
			throw new FileManagementException(ex.getMessage());
		}
	}

	@Override
	public boolean moveDirectory(SessionToken token, String path, String newName)
			throws RemoteException, InvalidTokenException, FileManagementException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		FileProvider provider = providers.get(realToken);
		path = path.trim();

		try {
			return provider.moveDirectory(path, newName);
		} catch (InvalidAccessException ex) {
			throw new FileManagementException(StringTable.getString("FileServerImpl.PATH_INVALID")); //$NON-NLS-1$
		} catch (FileProviderException ex) {
			throw new FileManagementException(ex.getMessage());
		}
	}

	@Override
	public boolean deleteDirectory(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		FileProvider provider = providers.get(realToken);
		path = path.trim();

		try {
			return provider.deleteDirectory(path);
		} catch (InvalidAccessException ex) {
			throw new FileManagementException(StringTable.getString("FileServerImpl.PATH_INVALID")); //$NON-NLS-1$
		} catch (FileProviderException ex) {
			throw new FileManagementException(ex.getMessage());
		}
	}

	@Override
	public boolean deleteDirectory(SessionToken token, String path, boolean recurse)
			throws RemoteException, InvalidTokenException, FileManagementException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		FileProvider provider = providers.get(realToken);
		path = path.trim();

		try {
			return provider.deleteDirectory(path, recurse);
		} catch (InvalidAccessException ex) {
			throw new FileManagementException(ex.getMessage());
		} catch (FileProviderException ex) {
			throw new FileManagementException(ex.getMessage());
		}
	}

	@Override
	public int getFile(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException, FileServerException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		FileProvider provider = providers.get(realToken);
		path = path.trim();

		try {
			DirEntry info = provider.getInfo(path);
			logger.info(String.format(StringTable.getString("FileServerImpl.GET_REQUEST_RECEIVED"), sessions.get(token.getToken()).getUsername(), //$NON-NLS-1$
					path, info.getSize()));
			// Crear el socket para la transferencia.
			logger.debug(StringTable.getString("FileServerImpl.START_FILETRANSFERSERVICE")); //$NON-NLS-1$
			FileTransferService transferService = new FileTransferService(provider);
			transferService.configure(path, info.getSize(), TransferMode.SEND);
			int port = transferService.createSocket();

			// Empieza a escuchar (y a transmitir pero en otro hilo)
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(transferService);
			return port;

		} catch (InvalidAccessException e) {
			throw new FileManagementException(StringTable.getString("FileServerImpl.PATH_INVALID")); //$NON-NLS-1$
		} catch (EntryNotFoundException e) {
			throw new FileManagementException(String.format(StringTable.getString("FileServerImpl.FILE_NOT_EXISTS"), path)); //$NON-NLS-1$
		} catch (IOException e) {
			throw new FileServerException(StringTable.getString("FileServerImpl.TRANSFER_SOCKET_CREATION_FAILED"), e); //$NON-NLS-1$
		}
	}

	@Override
	public int putFile(SessionToken token, String path, long size)
			throws RemoteException, InvalidTokenException, FileManagementException, FileServerException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		FileProvider provider = providers.get(realToken);
		path = path.trim();

		try {
			logger.info(String.format(StringTable.getString("FileServerImpl.PUT_REQUEST_RECEIVED"), sessions.get(token.getToken()).getUsername(), //$NON-NLS-1$
					path, size));
			// Crear el socket para la transferencia.
			FileTransferService transferService = new FileTransferService(provider);
			transferService.configure(path, size, TransferMode.RECEIVE);
			int port = transferService.createSocket();

			// Empieza a escuchar (y a recibir pero en otro hilo)
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(transferService);
			return port;

		} catch (IOException e) {
			throw new FileServerException(StringTable.getString("FileServerImpl.TRANSFER_SOCKET_CREATION_FAILED"), e); //$NON-NLS-1$
		}
	}

	@Override
	public boolean moveFile(SessionToken token, String path, String newName)
			throws RemoteException, InvalidTokenException, FileManagementException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		FileProvider provider = providers.get(realToken);
		path = path.trim();
		try {
			return provider.moveFile(path, newName);
		} catch (InvalidAccessException ex) {
			throw new FileManagementException(StringTable.getString("FileServerImpl.PATH_INVALID")); //$NON-NLS-1$
		} catch (FileProviderException ex) {
			throw new FileManagementException(ex.getMessage());
		}
	}

	@Override
	public boolean deleteFile(SessionToken token, String path)
			throws RemoteException, InvalidTokenException, FileManagementException {
		if (!sessions.containsKey(token.getToken())) {
			throw new InvalidTokenException(StringTable.getString("FileServerImpl.INVALID_TOKEN")); //$NON-NLS-1$
		}
		SessionToken realToken = sessions.get(token.getToken()).getToken();
		FileProvider provider = providers.get(realToken);
		path = path.trim();
		try {
			return provider.deleteFile(path);
		} catch (InvalidAccessException ex) {
			throw new FileManagementException(StringTable.getString("FileServerImpl.PATH_INVALID")); //$NON-NLS-1$
		} catch (FileProviderException ex) {
			throw new FileManagementException(ex.getMessage());
		}
	}
}
