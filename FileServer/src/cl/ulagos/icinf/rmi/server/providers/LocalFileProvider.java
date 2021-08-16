package cl.ulagos.icinf.rmi.server.providers;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cl.ulagos.icinf.rmi.StringTable;
import cl.ulagos.icinf.rmi.providers.CannotInitProviderException;
import cl.ulagos.icinf.rmi.providers.DirEntry;
import cl.ulagos.icinf.rmi.providers.DirectoryNotEmptyException;
import cl.ulagos.icinf.rmi.providers.EntryAlreadyExistsException;
import cl.ulagos.icinf.rmi.providers.EntryInUseException;
import cl.ulagos.icinf.rmi.providers.EntryNotFoundException;
import cl.ulagos.icinf.rmi.providers.EntryType;
import cl.ulagos.icinf.rmi.providers.FileProvider;
import cl.ulagos.icinf.rmi.providers.FileProviderException;
import cl.ulagos.icinf.rmi.providers.FileProviderInternalException;
import cl.ulagos.icinf.rmi.providers.InvalidAccessException;
import cl.ulagos.icinf.rmi.providers.NotADirectoryException;
import cl.ulagos.icinf.rmi.providers.NotAFileException;

public class LocalFileProvider implements FileProvider {

	private static final int BUFFER_SIZE = 1400;

	private static Logger logger = Logger.getLogger("LocalFileProvider"); //$NON-NLS-1$

	/**
	 * Directorio raíz del proveedor.
	 */
	private final String rootPath;
	/**
	 * Prefijo de las rutas del proveedor;
	 */
	static private final String pathPrefix = "files"; //$NON-NLS-1$

	private String getRelativePath(File file) {
		try {
			String canonicalPath = file.getCanonicalPath();
			return canonicalPath.substring(rootPath.length() + 1);
		} catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Inicializa el proveedor local de archivos en el directorio ./files/ Crea el
	 * directorio si es necesario.
	 */
	public LocalFileProvider() throws CannotInitProviderException {
		this(""); //$NON-NLS-1$
	}

	public LocalFileProvider(String rootPath) throws CannotInitProviderException {
		rootPath = pathPrefix + File.separator + rootPath;
		logger.debug(String.format(StringTable.getString("LocalFileProvider.INIT_LOCALFILEPROVIDER"), rootPath)); //$NON-NLS-1$
		try {
			File path = new File(rootPath);
			if (path.exists()) {
				this.rootPath = path.getCanonicalPath();
			} else {
				logger.info(String.format(StringTable.getString("LocalFileProvider.INIT_CREATE_DIRECTORY"), rootPath)); //$NON-NLS-1$
				if (path.mkdirs())
					this.rootPath = path.getCanonicalPath();
				else
					throw new CannotInitProviderException(
							String.format(StringTable.getString("LocalFileProvider.INIT_CREATE_DIRECTORY_FAILED"), rootPath)); //$NON-NLS-1$
			}
		} catch (IOException ex) {
			throw new CannotInitProviderException(String
					.format(StringTable.getString("LocalFileProvider.CANNOT_INIT_PROVIDER_IO_EXCEPTION"), rootPath)); //$NON-NLS-1$
		}
	}

	@Override
	public String getProviderName() {
		return "LocalFileProvider"; //$NON-NLS-1$
	}

	/**
	 * Comprueba que el directorio solicitado se encuentre dentro del directorio del
	 * proveedor.
	 * 
	 * @param path Directorio a evaluar.
	 * @return true si el directorio existe dentro del árbol gestionado por el
	 *         proveedor. false si está fuera.
	 */
	private boolean isFileOrDirInsidePath(File path) {
		try {
			if (path == null)
				return false;
			path = new File(path.getCanonicalPath());
			if (path.getCanonicalPath().equals(rootPath))
				return true;
			return isFileOrDirInsidePath(path.getParentFile());
		} catch (IOException ex) {
			return false;
		} catch (SecurityException ex) {
			return false;
		}
	}

	@Override
	public DirEntry getInfo(String path) throws InvalidAccessException, EntryNotFoundException {
		File file = new File(rootPath + File.separator + path);

		if (!isFileOrDirInsidePath(file))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$

		if (!file.exists())
			throw new EntryNotFoundException(String.format(StringTable.getString("LocalFileProvider.PATH_NOT_EXISTS"), path)); //$NON-NLS-1$

		return new DirEntry(file.getName(), file.isFile() ? EntryType.FILE : EntryType.DIRECTORY, file.length());
	}

	@Override
	public List<DirEntry> readDirectory(String path)
			throws InvalidAccessException, EntryNotFoundException, NotADirectoryException {
		File file = new File(rootPath + File.separator + path);

		if (!isFileOrDirInsidePath(file))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$

		if (!file.exists())
			throw new EntryNotFoundException(String.format(StringTable.getString("LocalFileProvider.DIRECTORY_NOT_EXISTS"), path)); //$NON-NLS-1$

		if (!file.isDirectory())
			throw new NotADirectoryException(String.format(StringTable.getString("LocalFileProvider.PATH_NOT_A_DIRECTORY"), path)); //$NON-NLS-1$

		File[] list = file.listFiles();
		ArrayList<DirEntry> entries = new ArrayList<DirEntry>();
		for (File f : list) {
			DirEntry de;
			if (f.isFile())
				de = new DirEntry(f.getName(), EntryType.FILE, f.length());
			else
				de = new DirEntry(f.getName(), EntryType.DIRECTORY, f.length());
			entries.add(de);
		}

		return entries;
	}

	@Override
	public boolean createDirectory(String path) throws InvalidAccessException, EntryAlreadyExistsException {
		File file = new File(rootPath + File.separator + path);

		if (!isFileOrDirInsidePath(file))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$

		if (file.exists())
			throw new EntryAlreadyExistsException(String.format(StringTable.getString("LocalFileProvider.FILE_DIRECTORY_ALREADY_EXISTS"), path)); //$NON-NLS-1$

		return file.mkdir();
	}

	@Override
	public boolean moveDirectory(String path, String newName)
			throws InvalidAccessException, EntryAlreadyExistsException, EntryNotFoundException, NotADirectoryException {
		File file = new File(rootPath + File.separator + path);
		File newFile;

		if (!isFileOrDirInsidePath(file))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$

		if (!file.exists())
			throw new EntryNotFoundException(String.format(StringTable.getString("LocalFileProvider.DIRECTORY_NOT_EXISTS"), path)); //$NON-NLS-1$

		if (!file.isDirectory())
			throw new NotADirectoryException(String.format(StringTable.getString("LocalFileProvider.PATH_NOT_A_DIRECTORY"), path)); //$NON-NLS-1$

		// construye el nuevo nombre de archivo
		newFile = new File(file.getParentFile().getPath() + File.separator + newName);
		if (!isFileOrDirInsidePath(newFile))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$
		if (newFile.exists())
			throw new EntryAlreadyExistsException(String.format(StringTable.getString("LocalFileProvider.DIRECTORY_ALREADY_EXISTS"), path)); //$NON-NLS-1$
		return file.renameTo(newFile);
	}

	@Override
	public boolean deleteDirectory(String path) throws InvalidAccessException, EntryNotFoundException,
			NotADirectoryException, DirectoryNotEmptyException, EntryInUseException {
		return deleteDirectory(path, false);
	}

	@Override
	public boolean deleteDirectory(String path, boolean recurse) throws InvalidAccessException, EntryInUseException,
			EntryNotFoundException, NotADirectoryException, DirectoryNotEmptyException {
		File file = new File(rootPath + File.separator + path);

		if (!isFileOrDirInsidePath(file))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$

		if (file.equals(new File(rootPath)))
			throw new InvalidAccessException(StringTable.getString("LocalFileProvider.CANNOT_DELETE_ROOT")); //$NON-NLS-1$

		if (!file.exists())
			throw new EntryNotFoundException(String.format(StringTable.getString("LocalFileProvider.DIRECTORY_NOT_EXISTS"), path)); //$NON-NLS-1$

		if (!file.isDirectory())
			throw new NotADirectoryException(String.format(StringTable.getString("LocalFileProvider.PATH_NOT_A_DIRECTORY"), path)); //$NON-NLS-1$

		if (file.list().length > 0 && !recurse)
			throw new DirectoryNotEmptyException(StringTable.getString("LocalFileProvider.DIRECTORY_NOT_EMPTY")); //$NON-NLS-1$

		if (recurse) {
			for (File childItem : file.listFiles()) {
				if (childItem.isDirectory()) {
					deleteDirectory(getRelativePath(childItem), true);
				} else {
					try {
						deleteFile(getRelativePath(childItem));
					} catch (NotAFileException ex) {
					} // no debiera ocurrir esto
				}
			}
			file.delete();
			return true;
		} else {
			return file.delete();
		}
	}

	/**
	 * Escribe en el archivo los bytes recibidos
	 * 
	 * @param file   Archivo a escribir.
	 * @param buffer Stream de entrada de bytes.
	 * @return El número de bytes escritos.
	 */
	private long writeFile(File file, long size, InputStream buffer) throws FileProviderException {
		int count;
		long bytesReceived = 0;
		byte[] bytes = new byte[BUFFER_SIZE];
		FileOutputStream writer = null;
		try {
			writer = new FileOutputStream(file);
			while ((count = buffer.read(bytes)) >= 0) {
				try {
					writer.write(bytes, 0, count);
				} catch (IOException ex) {
					tryCloseStreams(buffer, writer);
					throw new FileProviderInternalException(StringTable.getString("LocalFileProvider.FILE_WRITE_ERROR")); //$NON-NLS-1$
				}
				bytesReceived += count;
			}
			writer.close();
			buffer.close();
		} catch (IOException ex) {
			tryCloseStreams(buffer, writer);
			throw new FileProviderInternalException(StringTable.getString("LocalFileProvider.SOCKET_RECEIVE_ERROR")); //$NON-NLS-1$
		}
		return bytesReceived;
	}

	@Override
	public boolean putFile(String path, long size, InputStream buffer)
			throws InvalidAccessException, EntryInUseException, FileProviderException {
		File file = new File(rootPath + File.separator + path);
		long receivedBytes;

		if (!isFileOrDirInsidePath(file))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$

		if (file.isDirectory())
			throw new EntryInUseException(String.format(StringTable.getString("LocalFileProvider.PATH_IS_A_DIRECTORY"), path)); //$NON-NLS-1$

		/*
		 * Recibir el nuevo como .partial y eliminar el viejo cuando se
		 * termine. Se sobreescribe todo .partial que exista.
		 */
		File tmpFile = new File(rootPath + File.separator + path + ".partial"); //$NON-NLS-1$
		receivedBytes = writeFile(tmpFile, size, buffer);

		if (receivedBytes < size)
			return false;

	    if (file.exists())
			file.delete();
		tmpFile.renameTo(file);

		return receivedBytes == size;
	}

	/**
	 * Lee bytes del archivo
	 * 
	 * @param file   Archivo a escribir.
	 * @param buffer Stream de salida de bytes.
	 * @return El número de bytes enviados.
	 */
	private long readFile(File file, long size, OutputStream buffer) throws FileProviderException {
		int count;
		long bytesSent = 0;
		byte[] bytes = new byte[BUFFER_SIZE];
		FileInputStream reader = null;
		try {
			reader = new FileInputStream(file);
			while ((count = reader.read(bytes)) >= 0) {
				try {
					buffer.write(bytes, 0, count);
				} catch (IOException e) {
					tryCloseStreams(buffer, reader);
					throw new FileProviderInternalException(StringTable.getString("LocalFileProvider.SOCKET_SEND_ERROR")); //$NON-NLS-1$
				}
				bytesSent += count;
			}
			reader.close();
			buffer.flush();
			buffer.close();
		} catch (IOException ex) {
			tryCloseStreams(buffer, reader);
			throw new FileProviderInternalException(StringTable.getString("LocalFileProvider.FILE_READ_ERROR")); //$NON-NLS-1$
		}
		return bytesSent;
	}

	@Override
	public boolean retrieveFile(String path, OutputStream buffer) throws EntryInUseException, NotAFileException,
			EntryNotFoundException, InvalidAccessException, FileProviderException {
		File file = new File(rootPath + File.separator + path);
		long sentBytes;

		if (!isFileOrDirInsidePath(file))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$

		if (file.isDirectory())
			throw new EntryInUseException(String.format(StringTable.getString("LocalFileProvider.PATH_IS_A_DIRECTORY"), path)); //$NON-NLS-1$

		if (!file.exists())
			throw new EntryNotFoundException(String.format(StringTable.getString("LocalFileProvider.FILE_NOT_EXISTS"), path)); //$NON-NLS-1$

		sentBytes = readFile(file, file.length(), buffer);
		return sentBytes == file.length();
	}

	@Override
	public boolean moveFile(String path, String newName)
			throws InvalidAccessException, EntryNotFoundException, NotAFileException, EntryAlreadyExistsException {
		File file = new File(rootPath + File.separator + path);
		File newFile;

		if (!isFileOrDirInsidePath(file))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$

		if (!file.exists())
			throw new EntryNotFoundException(String.format(StringTable.getString("LocalFileProvider.FILE_NOT_EXISTS"), path)); //$NON-NLS-1$

		if (!file.isFile())
			throw new NotAFileException(String.format(StringTable.getString("LocalFileProvider.PATH_NOT_A_FILE"), path)); //$NON-NLS-1$

		// construye el nuevo nombre de archivo
		newFile = new File(file.getParentFile().getPath() + File.separator + newName);
		if (!isFileOrDirInsidePath(newFile))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$
		if (newFile.exists())
			throw new EntryAlreadyExistsException(String.format(StringTable.getString("LocalFileProvider.FILE_ALREADY_EXISTS"), path)); //$NON-NLS-1$

		return file.renameTo(newFile);
	}

	@Override
	public boolean deleteFile(String path)
			throws EntryInUseException, NotAFileException, EntryNotFoundException, InvalidAccessException {
		File file = new File(rootPath + File.separator + path);

		if (!isFileOrDirInsidePath(file))
			throw new InvalidAccessException(
					String.format(StringTable.getString("LocalFileProvider.PATH_OUTSIDE_SANDBOX"), path)); //$NON-NLS-1$

		if (!file.exists())
			throw new EntryNotFoundException(String.format(StringTable.getString("LocalFileProvider.FILE_NOT_EXISTS"), path)); //$NON-NLS-1$

		if (!file.isFile())
			throw new NotAFileException(String.format(StringTable.getString("LocalFileProvider.PATH_NOT_A_FILE"), path)); //$NON-NLS-1$

		if (!file.delete())
			throw new EntryInUseException(String.format(StringTable.getString("LocalFileProvider.FILE_IS_LOCKED"), path)); //$NON-NLS-1$

		return true;
	}

	private void tryCloseStreams(Closeable... streams) {
		try {
			for (Closeable c : streams)
				if (c != null)
					c.close();
		} catch (IOException e) {
		}
	}

}
