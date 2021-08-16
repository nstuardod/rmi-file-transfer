package cl.ulagos.icinf.rmi.client;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Callable;

import cl.ulagos.icinf.rmi.providers.DirEntry;
import cl.ulagos.icinf.rmi.server.FileManagementException;
import cl.ulagos.icinf.rmi.server.InvalidTokenException;

/**
 * Clase que solicita al servidor listados de ficheros.
 * 
 * @author nico
 *
 */
public class ServerListingFetcher implements Callable<List<DirEntry>> {
	String path;
	GuiClientRemote client;

	private ServerListingFetcher() {}

	public ServerListingFetcher(GuiClientRemote client, String path) {
		this.path = path;
		this.client = client;
	}

	@Override
	public List<DirEntry> call()
			throws RemoteException, InvalidTokenException, FileManagementException, InterruptedException {
		List<DirEntry> r = client.getFileListing(path);
		return r;
	}
};