package cl.ulagos.icinf.rmi.client;

import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.thirdparty.ThreadFactoryWithNamePrefix;

import cl.ulagos.icinf.rmi.providers.DirEntry;
import cl.ulagos.icinf.rmi.server.FileManagementException;
import cl.ulagos.icinf.rmi.server.FileServer;
import cl.ulagos.icinf.rmi.server.FileServerException;
import cl.ulagos.icinf.rmi.server.InvalidLoginException;
import cl.ulagos.icinf.rmi.server.InvalidTokenException;
import cl.ulagos.icinf.rmi.server.SessionToken;

/**
 * Clase para manejar el cliente RMI desde la GUI
 * 
 * @author Nicolás Stuardo
 *
 */
public class GuiClientRemote {
	private static final int PUERTO = 8527;
	private static final String RMIServerName = "RMIFileServer"; //$NON-NLS-1$
	private static final String prompt = "Cliente"; //$NON-NLS-1$
	private SessionToken token;
	private FileServer serverObj;

	private String username;
	private String hostname;

	private ScheduledExecutorService scheduler;
	private Runnable keepAliveProcess = new Runnable() {
		@Override
		public void run() {
			try {
				serverObj.ping(token);
			} catch (InvalidTokenException | RemoteException e) {
				scheduler.shutdownNow();
				// TODO: Avisar al usuario y botarlo al login.
			}
		}
	};

	private boolean connected = false;

	private Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			try {
				if (serverObj != null) {
					System.err.println(StringTable.getString("GuiClientRemote.LOGGING_OUT")); //$NON-NLS-1$
					serverObj.logout(token);
				}
			} catch (RemoteException e) {
				System.err.println(StringTable.getString("GuiClientRemote.REMOTE_EXCEPTION_MESSAGE") + e.getMessage()); //$NON-NLS-1$
			} catch (InvalidTokenException e) {
				System.err.println(StringTable.getString("GuiClientRemote.INVALID_TOKEN_EXCEPTION_MESSAGE") + e.getMessage()); //$NON-NLS-1$
			}
		}
	};

	private void close(boolean forced) {
		Runtime.getRuntime().removeShutdownHook(shutdownHook);
		if (forced)
			System.exit(1);
	}

	public void connect(String hostname, String username, String password) throws InvalidLoginException,
			UnknownHostException, NotBoundException, MalformedURLException, ConnectException, RemoteException {
		String path = String.format("rmi://%s:%d/%s", hostname, PUERTO, RMIServerName); //$NON-NLS-1$
		serverObj = (FileServer) Naming.lookup(path);
		token = serverObj.login(username, password);

		this.hostname = hostname;
		this.username = username;
		connected = true;

		// Mantener la sesión abierta mandando un ping cada 30 segundos.
		if (scheduler == null)
			scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryWithNamePrefix("KeepAlivePool")); //$NON-NLS-1$
		if (scheduler.isShutdown())
			scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryWithNamePrefix("KeepAlivePool")); //$NON-NLS-1$

		try {
			scheduler.scheduleAtFixedRate(keepAliveProcess, 30, 30, TimeUnit.SECONDS);
		} catch (RejectedExecutionException e) {
			System.err.println(StringTable.getString("GuiClientRemote.SCHEDULER_REJECTED_EXCEPTION_MESSAGE")); //$NON-NLS-1$
			System.err.println(e.getMessage());
		}
		// Un resguardo para cerrar la sesión antes de ser liquidados por un Ctrl+C
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	public void logout() throws InvalidTokenException, RemoteException {
		if (token == null)
			return;

		if (!scheduler.isShutdown())
			scheduler.shutdown();
		Runtime.getRuntime().removeShutdownHook(shutdownHook);
		connected = false; // porque aunque lo siguiente falle ya estamos fuera.
		serverObj.logout(token);
	}

	public void changePassword(String password, String newPassword)
			throws RemoteException, InvalidLoginException, InvalidTokenException, FileServerException {
		serverObj.changePassword(token, password, newPassword);
	}

	public String getUsername() {
		return new String(username);
	}

	public String getServerHostname() {
		return new String(hostname);
	}

	public boolean isConnected() {
		return connected;
	}

	public List<DirEntry> getFileListing(String path)
			throws RemoteException, InvalidTokenException, FileManagementException {
		return serverObj.readDirectory(token, path);
	}

	public boolean newDirectory(String path) throws RemoteException, InvalidTokenException, FileManagementException {
		return serverObj.createDirectory(token, path);
	}

	public boolean moveDirectory(String path, String newName)
			throws RemoteException, InvalidTokenException, FileManagementException {
		return serverObj.moveDirectory(token, path, newName);
	}

	public boolean deleteDirectory(String path, boolean recurse)
			throws RemoteException, InvalidTokenException, FileManagementException {
		return serverObj.deleteDirectory(token, path, recurse);
	}

	public boolean moveFile(String path, String newName)
			throws RemoteException, InvalidTokenException, FileManagementException {
		return serverObj.moveFile(token, path, newName);
	}

	public boolean deleteFile(String path) throws RemoteException, InvalidTokenException, FileManagementException {
		return serverObj.deleteFile(token, path);
	}

	public int getFile(String path)
			throws RemoteException, InvalidTokenException, FileManagementException, FileServerException {
		return serverObj.getFile(token, path);
	}

	public int putFile(String path, long size)
			throws RemoteException, InvalidTokenException, FileManagementException, FileServerException {
		return serverObj.putFile(token, path, size);
	}

}
