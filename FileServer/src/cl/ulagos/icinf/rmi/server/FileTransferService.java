package cl.ulagos.icinf.rmi.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import cl.ulagos.icinf.rmi.StringTable;
import cl.ulagos.icinf.rmi.providers.FileProvider;
import cl.ulagos.icinf.rmi.providers.FileProviderException;

/**
 * Servicio responsable del envío y recepción de archivos.
 * 
 * @author Nicolás Stuardo
 *
 */
public class FileTransferService implements Callable<Void> {
	private ServerSocket socket;
	private final FileProvider provider;
	private final Logger logger = Logger.getLogger("FileTransferService"); //$NON-NLS-1$
	private String path;
	private long size;
	private boolean result;

	public enum TransferMode {
		SEND, RECEIVE
	};

	private TransferMode mode;

	public FileTransferService(FileProvider provider) {
		this.provider = provider;
	}

	/**
	 * Configura el servidor para el envío o recepción de un archivo.
	 * 
	 * @param path Ruta del archivo
	 * @param mode Modo de transferencia.
	 */
	public void configure(String path, long size, TransferMode mode) {
		this.path = path;
		this.size = size;
		this.mode = mode;
	}

	// https://www.mkyong.com/java/java-genaerate-random-integers-in-a-range/
	// No ando de ganas de reinventar todo
	private int randomInRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException(StringTable.getString("FileTransferService.RANDOM_MIN_MAX_REVERSED")); //$NON-NLS-1$
		}

		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}

	/**
	 * Crea el socket para la transferencia.
	 * 
	 * @return Puerto asignado para la transferencia.
	 * @throws IOException
	 */
	public int createSocket() throws IOException {
		int port = randomInRange(49152, 65535);
		if (socket == null)
			socket = new ServerSocket(port);
		else {
			logger.warn(StringTable.getString("FileTransferService.SOCKET_ALREADY_EXISTS")); //$NON-NLS-1$
			socket.close();
			socket = new ServerSocket(port);
		}
		logger.info(String.format(StringTable.getString("FileTransferService.SOCKET_CREATED"), port)); //$NON-NLS-1$
		return port;
	}

	/**
	 * Entra en modo de espera de conexión e inicia la transferencia cuando el
	 * cliente conecte.
	 */
	@Override
	public Void call() throws Exception {
		if (socket == null)
			throw new Exception(StringTable.getString("FileTransferService.NO_SOCKET_AVAILABLE")); //$NON-NLS-1$

		logger.info(StringTable.getString("FileTransferService.WAITING_FOR_CLIENT")); //$NON-NLS-1$
		Socket clientSocket = socket.accept();
		switch (mode) {
		case RECEIVE:
			try {
				result = receiveFile(clientSocket);
			} catch (Exception e) {
				logger.error(StringTable.getString("FileTransferService.RECEIVE_RESULT") + StringTable.getString("FileTransferService.FAIL"), e); //$NON-NLS-1$ //$NON-NLS-2$
				clientSocket.close();
			}
			logger.info(StringTable.getString("FileTransferService.RECEIVE_RESULT") + (result ? StringTable.getString("FileTransferService.SUCCESS") : StringTable.getString("FileTransferService.FAIL"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			break;
		case SEND:
			try {
				result = sendFile(clientSocket);
			} catch (Exception e) {
				logger.error(StringTable.getString("FileTransferService.SEND_RESULT") + StringTable.getString("FileTransferService.FAIL"), e); //$NON-NLS-1$ //$NON-NLS-2$
				clientSocket.close();
			}
			logger.info(StringTable.getString("FileTransferService.SEND_RESULT") + (result ? StringTable.getString("FileTransferService.SUCCESS") : StringTable.getString("FileTransferService.FAIL"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			break;
		default:
			throw new Exception(StringTable.getString("FileTransferService.UNKNOWN_TRANSFER_MODE")); //$NON-NLS-1$
		}
		clientSocket.close();
		return null;
	}

	/**
	 * Envía el archivo al usuario.
	 * 
	 * @param clientSocket Socket de cliente.
	 * @return
	 * @throws IOException
	 */
	private boolean sendFile(Socket clientSocket) throws IOException, FileManagementException {
		logger.info(String.format(StringTable.getString("FileTransferService.SENDING_FILE"), path, socket.getLocalPort())); //$NON-NLS-1$
		OutputStream outStream = clientSocket.getOutputStream();
		try {
			return provider.retrieveFile(path, outStream);
		} catch (FileProviderException e) {
			throw new FileManagementException(e.getMessage());
		}
	}

	/**
	 * Recibe el archivo del usuario.
	 * 
	 * @param clientSocket Socket de cliente.
	 * @return
	 */
	private boolean receiveFile(Socket clientSocket) throws IOException, FileManagementException {
		logger.info(String.format(StringTable.getString("FileTransferService.RECEIVING_FILE"), path, socket.getLocalPort())); //$NON-NLS-1$
		InputStream inStream = clientSocket.getInputStream();

		try {
			return provider.putFile(path, size, inStream);
		} catch (FileProviderException e) {
			throw new FileManagementException(e.getMessage());
		}
	}

	public boolean getTransferResult() {
		return result;
	}
}
