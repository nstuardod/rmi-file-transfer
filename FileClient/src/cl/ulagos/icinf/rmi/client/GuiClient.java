package cl.ulagos.icinf.rmi.client;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.thirdparty.ThreadFactoryWithNamePrefix;

import cl.ulagos.icinf.rmi.client.gui.ChangePasswordDialog;
import cl.ulagos.icinf.rmi.client.gui.ClientDirEntryTreeNode;
import cl.ulagos.icinf.rmi.client.gui.FileManagerFrame;
import cl.ulagos.icinf.rmi.client.gui.LoginDialog;
import cl.ulagos.icinf.rmi.client.gui.ServerDirEntryTreeNode;
import cl.ulagos.icinf.rmi.client.gui.TransferProgressDialog;
import cl.ulagos.icinf.rmi.providers.DirEntry;
import cl.ulagos.icinf.rmi.providers.EntryType;
import cl.ulagos.icinf.rmi.server.FileManagementException;
import cl.ulagos.icinf.rmi.server.FileServerException;
import cl.ulagos.icinf.rmi.server.InvalidLoginException;
import cl.ulagos.icinf.rmi.server.InvalidTokenException;

/**
 * Representa al cliente gráfico para manejo de archivos.
 * 
 * Esta clase realiza todo el trabajo pesado de contactar con el servidor
 * mediante GuiClientRemote y desde acá maneja la ventana del programa
 * FileManagerFrame. Ningún frame o dialog contiene lógica de programa. Se
 * intenta realizar el máximo de operaciones de GUI en el hilo EDT y las
 * llamadas a funciones del GuiRemoteClient se hacen mediante hilos para no
 * parar el EDT tanto tiempo.
 * 
 * @author Nicolás Stuardo
 */
public class GuiClient {
	public static final int BUFFER_SIZE = 1400; // espero que no se acabe la memoria con esto xD
	private GuiClientRemote client;
	private FileManagerFrame fileManager;
	private LoginDialog loginDialog;
	private File clientRootDirectory;

	private ExecutorService serverRequestExecutor = Executors
			.newSingleThreadExecutor(new ThreadFactoryWithNamePrefix("ServerRequestPool")); //$NON-NLS-1$

	/**
	 * Fija los actionlisteners para el login.
	 */
	private void setLoginDialogListeners() {
		// (LoginDialog) Cancelar -> Cerrar dialogo
		ActionListener cancelClickedListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loginDialog.setVisible(false);
				loginDialog.dispose();
			}
		};

		// (LoginDialog) Cerrar dialogo -> Cerrar app
		WindowAdapter closedListener = new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				fileManager.setVisible(false);
				fileManager.dispose();
			};
		};

		// Cerrar dialogo con escape
		loginDialog.getRootPane().registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loginDialog.setVisible(false);
				loginDialog.dispose();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

		loginDialog.getBtnCancel().addActionListener(cancelClickedListener);
		loginDialog.addWindowListener(closedListener);

		loginDialog.getBtnLogin().addActionListener(new ActionListener() {
			class LoginTask extends SwingWorker<Void, Void> {
				String hostname, username, password;

				@SuppressWarnings("unused")
				private LoginTask() {}

				public LoginTask(String hostname, String username, String password) {
					this.hostname = hostname;
					this.username = username;
					this.password = password;
				}

				@Override
				protected Void doInBackground() throws InvalidLoginException, UnknownHostException, ConnectException,
						MalformedURLException, NotBoundException, RemoteException {
					client.connect(hostname, username, password);
					return null;
				}

				@Override
				protected void done() {
					// Ejecutado en EDT
					try {
						get();
						loginDialog.getBtnCancel().removeActionListener(cancelClickedListener);
						loginDialog.removeWindowListener(closedListener);
						loginDialog.setVisible(false);
						loginDialog.dispose();
					} catch (ExecutionException e) {
						Exception trueReason = (Exception) e.getCause();
						if (trueReason instanceof UnknownHostException) {
							JOptionPane.showMessageDialog(loginDialog,
									StringTable.getString("GuiClient.LOGIN_SERVER_NOT_EXISTS_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.LOGIN_SERVER_CONNECT_ERROR_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						} else if (trueReason instanceof ConnectException) {
							JOptionPane.showMessageDialog(loginDialog,
									StringTable.getString("GuiClient.LOGIN_CONNECTION_REFUSED_MESSAGE"), //$NON-NLS-1$
									"Servidor no responde", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
						} else if (trueReason instanceof MalformedURLException) {
							JOptionPane.showMessageDialog(loginDialog,
									StringTable.getString("GuiClient.LOGIN_SERVER_MALFORMED_URL"), //$NON-NLS-1$
									StringTable.getString("GuiClient.LOGIN_SERVER_CONNECT_ERROR_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						} else if (trueReason instanceof NotBoundException) {
							JOptionPane.showMessageDialog(loginDialog,
									StringTable.getString("GuiClient.LOGIN_SERVICE_NOT_BOUND_TO_SERVER"), //$NON-NLS-1$
									StringTable.getString("GuiClient.LOGIN_SERVER_CONNECT_ERROR_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						} else if (trueReason instanceof InvalidLoginException) {
							JOptionPane.showMessageDialog(loginDialog,
									StringTable.getString("GuiClient.LOGIN_INVALID_CREDENTIALS_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.LOGIN_INVALID_CREDENTIALS_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						} else {
							e.printStackTrace();
							JOptionPane.showMessageDialog(loginDialog,
									StringTable.getString("GuiClient.SOMETHING_HAPPENED_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.SOMETHING_HAPPENED_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						}
					} catch (InterruptedException e) {
						JOptionPane.showMessageDialog(loginDialog,
								StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_TITLE"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
					}
					loginDialog.getBtnLogin().setEnabled(true);
					loginDialog.getBtnCancel().setEnabled(true);
				}
			};

			@Override
			public void actionPerformed(ActionEvent e) {
				// Bloquea los botones para que no molesten 2 veces al server.
				loginDialog.getBtnLogin().setEnabled(false);
				loginDialog.getBtnCancel().setEnabled(false);
				// loginTask se debe realizar fuera del EDT para no pegar la GUI.
				LoginTask task = new LoginTask(loginDialog.getServer(), loginDialog.getUsername(),
						loginDialog.getPassword());
				task.execute();
			}
		});

	}

	private String getRemoteFilePath(TreePath path) {
		if (path == null)
			return null;
		// Convertir el path del árbol a un path textual
		StringBuilder sbPathName = new StringBuilder(32);
		Object[] pathList = path.getPath();
		for (int i = 0; i < pathList.length; i++) { // omitir la raíz
			ServerDirEntryTreeNode currentNode = (ServerDirEntryTreeNode) pathList[i];
			if (!currentNode.getDirectoryEntry().getName().equals("/")) //$NON-NLS-1$
				sbPathName.append(currentNode.getDirectoryEntry().getName());
			if (i < pathList.length - 1)
				sbPathName.append('/');
		}
		return sbPathName.toString();
	}

	private List<DirEntry> loadServerListing(String filePath) throws RemoteException, InvalidTokenException,
			FileManagementException, ExecutionException, InterruptedException {
		try {
			ServerListingFetcher fetcher = new ServerListingFetcher(client, filePath);
			Future<List<DirEntry>> result = serverRequestExecutor.submit(fetcher);
			List<DirEntry> subdirEntries = result.get();
			return subdirEntries;
		} catch (ExecutionException e) {
			Exception trueReason = (Exception) e.getCause();
			if (trueReason instanceof RemoteException) {
				throw (RemoteException) trueReason;
			} else if (trueReason instanceof InvalidTokenException) {
				throw (InvalidTokenException) trueReason;
			} else if (trueReason instanceof FileManagementException) {
				throw (FileManagementException) trueReason;
			} else {
				throw e;
			}
		}
	}

	private void refreshServerTree(TreePath path) {
		DefaultTreeModel model = (DefaultTreeModel) fileManager.getServerPanel().getTree().getModel();

		if (path == null) {// HACK: null apuntará a la raíz
			TreeNode[] rootNode = model.getPathToRoot((DefaultMutableTreeNode) model.getRoot());
			path = new TreePath(rootNode);
		}

		ServerDirEntryTreeNode node = (ServerDirEntryTreeNode) path.getLastPathComponent();
		String filePath = getRemoteFilePath(path);

		ServerDirEntryTreeNode newNode = new ServerDirEntryTreeNode(node.getDirectoryEntry());
		try {
			List<DirEntry> subdirEntries;
			// Reemplaza la entrada por una nueva, si falla podemos dejar un 'cargando...'
			if (node.getParent() == null) {
				model.setRoot(newNode);
				subdirEntries = loadServerListing(filePath);
			} else {
				model.insertNodeInto(newNode, (MutableTreeNode) node.getParent(), node.getParent().getIndex(node));
				model.removeNodeFromParent(node);
				subdirEntries = loadServerListing(filePath);
			}
			if (subdirEntries.size() > 0) {
				newNode.loadSubentries(subdirEntries);
				model.nodeStructureChanged(newNode);
			} else {
				newNode.loadSubentries(null);
				model.nodeStructureChanged(newNode.getParent());
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(fileManager, StringTable.getString("GuiClient.REMOTE_EXCEPTION_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("GuiClient.REMOTE_EXCEPTION_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		} catch (InvalidTokenException e) {
			JOptionPane.showMessageDialog(fileManager, StringTable.getString("GuiClient.SESSION_EXPIRED_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("GuiClient.SESSION_EXPIRED_TITLE"), //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE);
		} catch (FileManagementException e) {
			JOptionPane.showMessageDialog(fileManager, e.getMessage(),
					StringTable.getString("GuiClient.FILE_MANAGEMENT_EXCEPTION_TITLE"), //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE);
		} catch (ExecutionException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(fileManager, StringTable.getString("GuiClient.SOMETHING_HAPPENED_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("GuiClient.SOMETHING_HAPPENED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
			System.exit(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(fileManager, StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_TITLE"), //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE);
		} finally {
			TreeNode[] nodeToReopen = model.getPathToRoot(newNode);
			TreePath newPath = new TreePath(nodeToReopen);
			fileManager.getServerPanel().getTree().expandPath(newPath);
			fileManager.getServerPanel().getTree().setSelectionPath(newPath);
			fileManager.getServerPanel().getTree().scrollPathToVisible(newPath);
		}
	}

	private void refreshClientTree(TreePath path) {
		JTree tree = fileManager.getClientPanel().getTree();
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

		if (path == null) {// HACK: null apuntará a la raíz
			TreeNode[] rootNode = model.getPathToRoot((DefaultMutableTreeNode) model.getRoot());
			path = new TreePath(rootNode);
		}

		ClientDirEntryTreeNode node = (ClientDirEntryTreeNode) path.getLastPathComponent();
		File file = node.getDirectoryEntry();

		ClientDirEntryTreeNode newNode = new ClientDirEntryTreeNode(file);
		try {
			File[] subdirEntries;
			// Reemplaza la entrada por una nueva, si falla podemos dejar un 'cargando...'
			if (node.getParent() == null) {
				model.setRoot(newNode);
				subdirEntries = file.listFiles();
			} else {
				model.insertNodeInto(newNode, (MutableTreeNode) node.getParent(), node.getParent().getIndex(node));
				model.removeNodeFromParent(node);
				subdirEntries = file.listFiles();
			}
			if (subdirEntries.length > 0) {
				newNode.loadSubentries(subdirEntries);
				model.nodeStructureChanged(newNode);
			} else {
				newNode.loadSubentries(null);
				model.nodeStructureChanged(newNode.getParent());
			}
		} catch (SecurityException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(fileManager,
					StringTable.getString("GuiClient.REFRESH_CLIENT_TREE_PERMISSION_ERROR_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("GuiClient.REFRESH_CLIENT_TREE_PERMISSION_ERROR_TITLE"), //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE);
		} finally {
			TreeNode[] nodeToReopen = model.getPathToRoot(newNode);
			TreePath newPath = new TreePath(nodeToReopen);
			tree.expandPath(newPath);
			tree.setSelectionPath(newPath);
			tree.scrollPathToVisible(newPath);
		}
	}

	private void setupServerView() {
		ServerDirEntryTreeNode rootNode = new ServerDirEntryTreeNode(new DirEntry("/", EntryType.DIRECTORY)); //$NON-NLS-1$
		DefaultTreeModel model = new DefaultTreeModel(rootNode);
		fileManager.getServerPanel().getTree().setModel(model);
		try {
			List<DirEntry> rootEntries = loadServerListing("/"); //$NON-NLS-1$
			if (rootEntries.size() > 0)
				rootNode.loadSubentries(rootEntries);
			else
				rootNode.loadSubentries(null);
			model.nodeStructureChanged(rootNode);
		} catch (RemoteException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(fileManager, StringTable.getString("GuiClient.REMOTE_EXCEPTION_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("GuiClient.REMOTE_EXCEPTION_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		} catch (InvalidTokenException e) {
			JOptionPane.showMessageDialog(fileManager, StringTable.getString("GuiClient.SESSION_EXPIRED_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("GuiClient.SESSION_EXPIRED_TITLE"), //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE);
		} catch (FileManagementException e) {
			JOptionPane.showMessageDialog(fileManager, e.getMessage(),
					StringTable.getString("GuiClient.FILE_MANAGEMENT_EXCEPTION_TITLE"), //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE);
		} catch (ExecutionException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(fileManager, StringTable.getString("GuiClient.SOMETHING_HAPPENED_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("GuiClient.SOMETHING_HAPPENED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
			System.exit(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(fileManager, StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
			System.exit(1);
		}

		fileManager.getServerPanel().getTree().addTreeExpansionListener(new TreeExpansionListener() {

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				TreePath path = event.getPath();
				ServerDirEntryTreeNode node = (ServerDirEntryTreeNode) path.getLastPathComponent();

				String filePath = getRemoteFilePath(path);

				try {
					List<DirEntry> subdirEntries = loadServerListing(filePath);
					if (subdirEntries.size() > 0)
						node.loadSubentries(subdirEntries);
					else
						node.loadSubentries(null);
					model.nodeStructureChanged(node);
				} catch (RemoteException e) {
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.REMOTE_EXCEPTION_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.REMOTE_EXCEPTION_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
				} catch (InvalidTokenException e) {
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.SESSION_EXPIRED_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.SESSION_EXPIRED_TITLE"), //$NON-NLS-1$
							JOptionPane.ERROR_MESSAGE);
				} catch (FileManagementException e) {
					JOptionPane.showMessageDialog(fileManager, e.getMessage(),
							StringTable.getString("GuiClient.FILE_MANAGEMENT_EXCEPTION_TITLE"), //$NON-NLS-1$
							JOptionPane.ERROR_MESSAGE);
				} catch (ExecutionException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.SOMETHING_HAPPENED_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.SOMETHING_HAPPENED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					System.exit(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					System.exit(1);
				}
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				((ServerDirEntryTreeNode) event.getPath().getLastPathComponent()).removeSubentries();
			}
		});

		// Si se hace clic en un ítem se debe mostrar su detalle en el panel de abajo.
		fileManager.getServerPanel().getTree().addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
				// Ocultar detalle si es vacío.
				if (selectedNode == null) {
					fileManager.getServerInfoPanel().setInfoVisibility(false);
					fileManager.setServerButtonsEnable(false);
				} else if (selectedNode instanceof ServerDirEntryTreeNode) {
					DirEntry detail = ((ServerDirEntryTreeNode) selectedNode).getDirectoryEntry();
					fileManager.getServerInfoPanel().setDetails(detail);
					fileManager.getServerInfoPanel().setInfoVisibility(true);
					fileManager.setServerButtonsEnable(true);
					// ojo que no se pueden crear carpetas en un archivo
					fileManager.getBtnServerNewFolder().setEnabled(detail.getType() == EntryType.DIRECTORY);

					// Ni eliminar ni renombrar la raíz
					boolean isRoot = selectedNode.isRoot();
					fileManager.getBtnServerDelete().setEnabled(!isRoot);
					fileManager.getBtnServerRename().setEnabled(!isRoot);

					/*-
					 * Activar el botón copiar del lado de servidor si:
					 * - tenemos un directorio seleccionado en el cliente.
					 * - tenemos un archivo seleccionado en el servidor
					 * De lo contrario se apaga
					 */
					TreePath clientSelection = fileManager.getClientPanel().getTree().getSelectionPath();
					if (clientSelection == null) {
						fileManager.getBtnServerCopy().setEnabled(false);
					} else {
						if (!(clientSelection.getLastPathComponent() instanceof ClientDirEntryTreeNode)) {
							fileManager.getBtnServerCopy().setEnabled(false);
							return;
						}
						ClientDirEntryTreeNode selectionValue = (ClientDirEntryTreeNode) clientSelection
								.getLastPathComponent();

						boolean isClientSelectionADir = (selectionValue.getDirectoryEntry()).isDirectory();
						boolean isServerSelectionAFile = detail.getType() == EntryType.FILE;
						fileManager.getBtnServerCopy().setEnabled(isClientSelectionADir & isServerSelectionAFile);
						fileManager.getBtnClientCopy().setEnabled(!isClientSelectionADir & !isServerSelectionAFile);
					}
				} else {
					fileManager.getServerInfoPanel().setDetails(null);
					fileManager.setServerButtonsEnable(false);
				}
			}
		});

		setServerPanelButtonListeners();

	}

	private void setupClientView() {
		ClientDirEntryTreeNode rootNode = new ClientDirEntryTreeNode(clientRootDirectory);
		DefaultTreeModel model = new DefaultTreeModel(rootNode);
		fileManager.getClientPanel().getTree().setModel(model);

		// Lista los contenidos de la "raíz"
		rootNode.loadSubentries(clientRootDirectory.listFiles());
		model.nodeStructureChanged(rootNode);
		fileManager.getClientPanel().getTree().addTreeExpansionListener(new TreeExpansionListener() {

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				TreePath path = event.getPath();
				ClientDirEntryTreeNode node = (ClientDirEntryTreeNode) path.getLastPathComponent();
				File file = node.getDirectoryEntry();

				File[] subentries = file.listFiles();
				if (subentries.length > 0)
					node.loadSubentries(subentries);
				else
					node.loadSubentries(null);
				model.nodeStructureChanged(node);
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				((ClientDirEntryTreeNode) event.getPath().getLastPathComponent()).removeSubentries();
			}
		});

		fileManager.getClientPanel().getTree().addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
				// Ocultar detalle si es vacío.
				if (selectedNode == null) {
					fileManager.getClientInfoPanel().setInfoVisibility(false);
					fileManager.setClientButtonsEnable(false);
				} else if (selectedNode instanceof ClientDirEntryTreeNode) {
					File detail = ((ClientDirEntryTreeNode) selectedNode).getDirectoryEntry();
					fileManager.getClientInfoPanel().setDetails(new DirEntry(detail.getName(),
							detail.isDirectory() ? EntryType.DIRECTORY : EntryType.FILE, detail.length()));
					fileManager.getClientInfoPanel().setInfoVisibility(true);
					fileManager.setClientButtonsEnable(true);
					fileManager.getBtnClientNewFolder().setEnabled(detail.isDirectory());

					boolean isRoot = selectedNode.isRoot();
					fileManager.getBtnClientDelete().setEnabled(!isRoot);
					fileManager.getBtnClientRename().setEnabled(!isRoot);

					/*-
					 * Activar el botón copiar del lado de cliente si:
					 * - tenemos un directorio seleccionado en el servidor.
					 * - tenemos un archivo seleccionado en el cliente
					 * De lo contrario se apaga
					 */
					TreePath serverSelection = fileManager.getServerPanel().getTree().getSelectionPath();
					if (serverSelection == null) {
						fileManager.getBtnClientCopy().setEnabled(false);
					} else {
						if (!(serverSelection.getLastPathComponent() instanceof ServerDirEntryTreeNode)) {
							fileManager.getBtnClientCopy().setEnabled(false);
							return;
						}
						ServerDirEntryTreeNode selectionValue = (ServerDirEntryTreeNode) serverSelection
								.getLastPathComponent();

						// Aquí activamos el otro lado
						boolean isServerSelectionADir = selectionValue.getDirectoryEntry()
								.getType() == EntryType.DIRECTORY;
						boolean isClientSelectionAFile = detail.isFile();
						fileManager.getBtnClientCopy().setEnabled(isServerSelectionADir & isClientSelectionAFile);
						fileManager.getBtnServerCopy().setEnabled(!isServerSelectionADir & !isClientSelectionAFile);
					}
				} else {
					fileManager.getClientInfoPanel().setDetails(null);
					fileManager.setClientButtonsEnable(false);
				}
			}
		});

		setClientPanelButtonListeners();
	}

	private class FileTransferWorker extends SwingWorker<Long, Void> {
		File file;
		String remotePath;
		boolean sending;
		long size;
		TransferProgressDialog dialog;
		Socket transferSocket = null;
		InputStream is = null;
		OutputStream os = null;
		Runnable callback = null;

		public FileTransferWorker(TransferProgressDialog dialog, File localFile, String remotePath, long size,
				boolean sending) {
			this.file = localFile;
			this.remotePath = remotePath;
			this.size = size;
			this.sending = sending;
			this.dialog = dialog;

			if (sending) {
				dialog.setSourceFile(localFile.getAbsolutePath());
				dialog.setDestinationFile(remotePath);
			} else {
				dialog.setSourceFile(remotePath);
				dialog.setDestinationFile(localFile.getAbsolutePath());
			}
			addPropertyChangeListener(dialog.new ProgressListener());

			dialog.getBtnCancel().addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					cancel(true);
				}
			});
			dialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					cancel(true);
				};
			});
		}

		public void setCallback(Runnable callback) {
			this.callback = callback;
		}

		@Override
		protected Long doInBackground() throws SocketTimeoutException, SocketException, IOException, RemoteException,
				InvalidTokenException, FileManagementException, FileServerException {
			int port, count, progress;
			long transferredBytes = 0;
			byte[] buffer;

			if (sending)
				port = client.putFile(remotePath, size);
			else
				port = client.getFile(remotePath);

			try {
				transferSocket = new Socket(client.getServerHostname(), port);
				transferSocket.setSoTimeout(10000);

				if (sending) {
					is = new FileInputStream(file);
					os = new BufferedOutputStream(transferSocket.getOutputStream());
				} else {
					is = new BufferedInputStream(transferSocket.getInputStream());
					os = new FileOutputStream(file, false);
				}

				buffer = new byte[BUFFER_SIZE];
				while (!isCancelled() && (transferredBytes < size)) {
					count = is.read(buffer);
					os.write(buffer, 0, count);
					transferredBytes += count;
					progress = (int) (100L * transferredBytes / size);
					setProgress(progress);
				}
				/*
				 * gracias BufferedOutputStream por no hacer flush al cerrar (inserte emoji de
				 * enojo)
				 */
				os.flush();
				tryCloseResources();
			} catch (SocketTimeoutException e) {
				tryCloseResources();
				throw e;
			} catch (SocketException e) {
				tryCloseResources();
				throw e;
			} catch (IOException e) {
				tryCloseResources();
				throw e;
			}
			return transferredBytes;
		}

		private void tryCloseResources() {
			try {
				if (transferSocket != null)
					transferSocket.close();
				if (os != null)
					os.close();
				if (is != null)
					is.close();
			} catch (IOException e) {}
		}

		@Override
		protected void done() {
			try {
				long transferred = get();
				JOptionPane.showMessageDialog(dialog,
						String.format(StringTable.getString("GuiClient.TRANSFER_RESULT_MESSAGE"), //$NON-NLS-1$
								NumberFormat.getNumberInstance().format(transferred)),
						StringTable.getString("GuiClient.TRANSFER_RESULT_TITLE"), JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$
			} catch (ExecutionException e) {
				Exception trueReason = (Exception) e.getCause();
				if (trueReason instanceof SocketTimeoutException) {
					JOptionPane.showMessageDialog(dialog,
							StringTable.getString("GuiClient.TRANSFER_SOCKET_TIMEOUT_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.TRANSFER_FAILED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
				} else if (trueReason instanceof SocketException) {
					trueReason.printStackTrace();
					JOptionPane.showMessageDialog(dialog,
							StringTable.getString("GuiClient.TRANSFER_SOCKET_EXCEPTION_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.TRANSFER_FAILED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
				} else if (trueReason instanceof IOException) {
					trueReason.printStackTrace();
					JOptionPane.showMessageDialog(dialog,
							StringTable.getString("GuiClient.TRANSFER_IO_EXCEPTION_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.TRANSFER_FAILED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
				} else if (trueReason instanceof InvalidTokenException) {
					JOptionPane.showMessageDialog(dialog, StringTable.getString("GuiClient.SESSION_EXPIRED_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.SESSION_EXPIRED_TITLE"), //$NON-NLS-1$
							JOptionPane.ERROR_MESSAGE);
				} else if (trueReason instanceof FileManagementException || trueReason instanceof FileServerException) {
					trueReason.printStackTrace();
					JOptionPane.showMessageDialog(dialog,
							StringTable.getString("GuiClient.TRANSFER_SERVER_ERROR_MESSAGE") + trueReason.getMessage(), //$NON-NLS-1$
							StringTable.getString("GuiClient.SESSION_EXPIRED_TITLE"), //$NON-NLS-1$
							JOptionPane.ERROR_MESSAGE);
				} else {
					e.printStackTrace();
					JOptionPane.showMessageDialog(dialog, StringTable.getString("GuiClient.SOMETHING_HAPPENED_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.SOMETHING_HAPPENED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
				}
			} catch (CancellationException e) {
				JOptionPane.showMessageDialog(dialog, StringTable.getString("GuiClient.TRANSFER_CANCELLED_MESSAGE"), //$NON-NLS-1$
						StringTable.getString("GuiClient.TRANSFER_CANCELLED_TITLE"), JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$
			} catch (InterruptedException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(dialog, StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_MESSAGE"), //$NON-NLS-1$
						StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
			} finally {
				if (callback != null)
					new Thread(callback).start();
				dialog.setVisible(false);
				dialog.dispose();
			}
		}

	};

	private void setServerPanelButtonListeners() {
		class ServerRenameRequest implements Callable<Void> {
			String path, newName;
			boolean isDirectory;

			@SuppressWarnings("unused")
			private ServerRenameRequest() {}

			public ServerRenameRequest(String path, String newName, EntryType type) throws IllegalArgumentException {
				this.path = path;
				// Validar el nombre de archivo
				if (!ClientUtils.checkNewName(newName))
					throw new IllegalArgumentException(
							StringTable.getString("GuiClient.INVALID_FILENAME_EXCEPTION_MESSAGE")); //$NON-NLS-1$
				this.newName = newName;
				this.isDirectory = type == EntryType.DIRECTORY;
			}

			@Override
			public Void call() throws RemoteException, InvalidTokenException, FileManagementException {
				if (isDirectory)
					client.moveDirectory(path, newName);
				else
					client.moveFile(path, newName);
				return null;
			}
		}

		class ServerMakeDirRequest implements Callable<Void> {
			private String path;

			@SuppressWarnings("unused")
			private ServerMakeDirRequest() {}

			public ServerMakeDirRequest(String name, String path) throws IllegalArgumentException {
				if (!ClientUtils.checkNewName(name))
					throw new IllegalArgumentException(
							StringTable.getString("GuiClient.INVALID_FILENAME_EXCEPTION_MESSAGE")); //$NON-NLS-1$
				this.path = path + "/" + name; //$NON-NLS-1$
			}

			@Override
			public Void call() throws RemoteException, InvalidTokenException, FileManagementException {
				client.newDirectory(path);
				return null;
			}
		}

		class ServerDeleteRequest implements Callable<Void> {
			private String path;
			private boolean recurse, isDirectory;

			@SuppressWarnings("unused")
			private ServerDeleteRequest() {}

			public ServerDeleteRequest(String path, boolean recurse, EntryType type) {
				this.path = path;
				this.recurse = recurse;
				this.isDirectory = type == EntryType.DIRECTORY;
			}

			@Override
			public Void call() throws RemoteException, InvalidTokenException, FileManagementException {
				if (isDirectory)
					client.deleteDirectory(path, recurse);
				else
					client.deleteFile(path);
				return null;
			}
		}

		/*
		 * Para no duplicar código, todos los botones del servidor comparten este
		 * ActionListener. La acción a ejecutar dependera del actionCommand definido al
		 * crear el objeto JButton en el Frame.
		 */
		ActionListener serverActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Callable<Void> action = null;
				FileTransferWorker worker = null;
				TransferProgressDialog progressDialog = null;
				String cmd = e.getActionCommand();
				JTree serverTree = fileManager.getServerPanel().getTree();

				// Obtener la ruta a operar
				TreePath selectionPath = serverTree.getSelectionPath();
				if (selectionPath == null) {
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.NO_ELEMENT_SELECTED_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.NO_ELEMENT_SELECTED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					return;
				}

				if (!(selectionPath.getLastPathComponent() instanceof ServerDirEntryTreeNode)) {
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.INVALID_SELECTION_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.INVALID_SELECTION_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					return;
				}
				String path = getRemoteFilePath(selectionPath);
				if (path == null) {
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.NO_ELEMENT_SELECTED_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.NO_ELEMENT_SELECTED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					return;
				}
				ServerDirEntryTreeNode lastPathComponent = (ServerDirEntryTreeNode) selectionPath
						.getLastPathComponent();
				DirEntry entry = lastPathComponent.getDirectoryEntry();
				ServerDirEntryTreeNode root = (ServerDirEntryTreeNode) lastPathComponent.getRoot();

				if ((cmd.equals("delete") || cmd.equals("rename")) && root.equals(lastPathComponent)) { //$NON-NLS-1$ //$NON-NLS-2$
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.CANNOT_PERFORM_ON_ROOT_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.CANNOT_PERFORM_ON_ROOT_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					return;
				}

				if (cmd.equals("rename")) { //$NON-NLS-1$
					String newName;
					do {
						newName = null;
						Object oNewName = JOptionPane.showInputDialog(fileManager,
								StringTable.getString("GuiClient.RENAME_DIALOG_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.RENAME_DIALOG_TITLE"), JOptionPane.PLAIN_MESSAGE, null, //$NON-NLS-1$
								null, entry.getName());

						if (oNewName == null)
							return;
						newName = oNewName.toString();
						if (newName.isEmpty())
							JOptionPane.showMessageDialog(fileManager,
									StringTable.getString("GuiClient.RENAME_EMPTY_ERROR_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.RENAME_EMPTY_ERROR_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
					} while (newName.isEmpty());

					action = new ServerRenameRequest(path, newName, entry.getType());
				} else if (cmd.equals("new_dir")) { //$NON-NLS-1$
					String newName;
					do {
						Object oNewName = JOptionPane.showInputDialog(fileManager,
								StringTable.getString("GuiClient.MKDIR_DIALOG_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.MKDIR_DIALOG_TITLE"), JOptionPane.PLAIN_MESSAGE); //$NON-NLS-1$

						if (oNewName == null)
							return;
						newName = oNewName.toString();
						if (newName.isEmpty())
							JOptionPane.showMessageDialog(fileManager,
									StringTable.getString("GuiClient.MKDIR_EMPTY_ERROR_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.MKDIR_EMPTY_ERROR_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
					} while (newName.isEmpty());
					action = new ServerMakeDirRequest(newName, path);
				} else if (cmd.equals("delete")) { //$NON-NLS-1$
					// La típica confirmación
					// WHAT! Yes = 0; No = 1; Cerrar = -1
					if (0 != JOptionPane.showConfirmDialog(fileManager,
							String.format(StringTable.getString("GuiClient.DELETE_CONFIRM_MESSAGE"), entry.getName()), //$NON-NLS-1$
							StringTable.getString("GuiClient.DELETE_CONFIRM_TITLE"), //$NON-NLS-1$
							JOptionPane.YES_NO_OPTION))
						return;
					// Confirmar si eliminar contenidos de un directorio.
					int doRecurse = 1;
					if (entry.getType().equals(EntryType.DIRECTORY)) {
						doRecurse = JOptionPane.showConfirmDialog(fileManager,
								String.format(StringTable.getString("GuiClient.DELETE_RECURSIVE_CONFIRM_MESSAGE"), //$NON-NLS-1$
										entry.getName()),
								StringTable.getString("GuiClient.DELETE_RECURSIVE_CONFIRM_TITLE"), //$NON-NLS-1$
								JOptionPane.YES_NO_OPTION);
						if (doRecurse != 0)
							return;
					}
					action = new ServerDeleteRequest(path, doRecurse == 0, entry.getType());
				} else if (cmd.equals("copy")) { //$NON-NLS-1$
					// Se debe crear la ruta al archivo antes de pasarla al worker.
					ClientDirEntryTreeNode node = (ClientDirEntryTreeNode) fileManager.getClientPanel().getTree()
							.getSelectionPath().getLastPathComponent();
					File localFile = new File(node.getDirectoryEntry(), entry.getName());
					if (localFile.exists()) {
						if (0 != JOptionPane.showConfirmDialog(fileManager,
								StringTable.getString("GuiClient.TRANSFER_OVERWRITE_CONFIRM_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.TRANSFER_OVERWRITE_CONFIRM_TITLE"), //$NON-NLS-1$
								JOptionPane.YES_NO_OPTION))
							return;
					}
					progressDialog = new TransferProgressDialog();
					progressDialog.setLocationRelativeTo(fileManager);
					progressDialog.setModalityType(ModalityType.APPLICATION_MODAL);
					worker = new FileTransferWorker(progressDialog, localFile, path, entry.getSize(), false);
					worker.setCallback(new Runnable() {
						@Override
						public void run() {
							refreshClientTree(new TreePath(node.getPath()));
						}
					});
				} else {
					return;
				}

				try {
					if (!cmd.equals("copy")) { //$NON-NLS-1$
						Future<Void> result = serverRequestExecutor.submit(action);
						result.get();
						// Actualizar el nodo padre del item modificado.
						refreshServerTree(selectionPath.getParentPath());
					} else {
						worker.execute();
						progressDialog.setVisible(true);
					}
				} catch (IllegalArgumentException e1) {
					JOptionPane.showMessageDialog(fileManager, e1.getMessage(),
							StringTable.getString("GuiClient.GENERIC_ERROR_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
				} catch (ExecutionException e1) {
					Exception trueReason = (Exception) e1.getCause();
					if (trueReason instanceof RemoteException) {
						trueReason.printStackTrace();
						JOptionPane.showMessageDialog(fileManager,
								StringTable.getString("GuiClient.REMOTE_EXCEPTION_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.REMOTE_EXCEPTION_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					} else if (trueReason instanceof InvalidTokenException) {
						JOptionPane.showMessageDialog(fileManager,
								StringTable.getString("GuiClient.SESSION_EXPIRED_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.SESSION_EXPIRED_TITLE"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
					} else if (trueReason instanceof FileManagementException) {
						JOptionPane.showMessageDialog(fileManager, trueReason.getMessage(),
								StringTable.getString("GuiClient.FILE_MANAGEMENT_EXCEPTION_TITLE"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
					} else {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(fileManager,
								StringTable.getString("GuiClient.SOMETHING_HAPPENED_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.SOMETHING_HAPPENED_TITLE"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
						System.exit(1);
					}
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_TITLE"), //$NON-NLS-1$
							JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		fileManager.getBtnServerRename().addActionListener(serverActionListener);
		fileManager.getBtnServerDelete().addActionListener(serverActionListener);
		fileManager.getBtnServerNewFolder().addActionListener(serverActionListener);
		fileManager.getBtnServerCopy().addActionListener(serverActionListener);
	}

	private void setClientPanelButtonListeners() {

		class ClientRenameRequest implements Callable<Boolean> {
			File file;
			String newName;

			@SuppressWarnings("unused")
			private ClientRenameRequest() {}

			public ClientRenameRequest(File file, String newName) throws IllegalArgumentException {
				this.file = file;
				// Validar el nombre de archivo
				if (!ClientUtils.checkNewName(newName))
					throw new IllegalArgumentException(
							StringTable.getString("GuiClient.INVALID_FILENAME_EXCEPTION_MESSAGE")); //$NON-NLS-1$
				this.newName = newName;
			}

			@Override
			public Boolean call() throws SecurityException {
				File newFile = new File(file, newName);
				return file.renameTo(newFile);
			}
		}
		class ClientMakeDirRequest implements Callable<Boolean> {
			private String name;
			private File parent;

			@SuppressWarnings("unused")
			private ClientMakeDirRequest() {}

			public ClientMakeDirRequest(String name, File parent) throws IllegalArgumentException {
				if (!ClientUtils.checkNewName(name))
					throw new IllegalArgumentException(
							StringTable.getString("GuiClient.INVALID_FILENAME_EXCEPTION_MESSAGE")); //$NON-NLS-1$
				this.name = name;
				this.parent = parent;
			}

			@Override
			public Boolean call() throws SecurityException {
				File newDir = new File(parent, name);
				return newDir.mkdir();
			}
		}

		class ClientDeleteRequest implements Callable<Boolean> {
			private File file;
			private boolean recurse;

			@SuppressWarnings("unused")
			private ClientDeleteRequest() {}

			public ClientDeleteRequest(File file, boolean recurse) {
				this.file = file;
				this.recurse = recurse;
			}

			private boolean recursiveDelete(File item) throws IOException, SecurityException {
				for (File childItem : item.listFiles()) {
					if (childItem.isDirectory()) {
						recursiveDelete(childItem);
					} else {
						childItem.delete();
					}
				}
				item.delete();
				return true;
			}

			@Override
			public Boolean call() throws Exception, IOException, SecurityException {
				if (file.isFile()) {
					return file.delete();
				} else {
					if (file.listFiles().length > 0 && !recurse)
						throw new Exception(String.format(
								StringTable.getString("GuiClient.DIRECTORY_NOT_EMPTY_EXCEPTION"), file.getName())); //$NON-NLS-1$
					else
						return recursiveDelete(file);
				}
			}
		}

		ActionListener clientActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Callable<Boolean> action = null;
				FileTransferWorker worker = null;
				TransferProgressDialog progressDialog = null;
				String cmd = e.getActionCommand();

				// Obtener la ruta a operar
				TreePath selection = fileManager.getClientPanel().getTree().getSelectionPath();
				if (selection == null) {
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.INVALID_SELECTION_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.INVALID_SELECTION_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					return;
				}

				if (!(selection.getLastPathComponent() instanceof ClientDirEntryTreeNode)) {
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.NO_ELEMENT_SELECTED_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.NO_ELEMENT_SELECTED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					return;
				}

				File file = ((ClientDirEntryTreeNode) selection.getLastPathComponent()).getDirectoryEntry();

				// No permitr eliminar ni renombrar la carpeta raíz
				if ((cmd.equals("rename") || cmd.equals("delete")) && file.equals(clientRootDirectory)) { //$NON-NLS-1$//$NON-NLS-2$
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.CANNOT_PERFORM_ON_ROOT_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.CANNOT_PERFORM_ON_ROOT_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					return;
				}

				if (cmd.equals("rename")) { //$NON-NLS-1$
					String newName;
					do {
						newName = null;
						Object oNewName = JOptionPane.showInputDialog(fileManager,
								StringTable.getString("GuiClient.RENAME_DIALOG_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.RENAME_DIALOG_TITLE"), JOptionPane.PLAIN_MESSAGE, null, //$NON-NLS-1$
								null, file.getName());

						if (oNewName == null)
							return;
						newName = oNewName.toString();
						if (newName.isEmpty())
							JOptionPane.showMessageDialog(fileManager,
									StringTable.getString("GuiClient.RENAME_EMPTY_ERROR_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.RENAME_EMPTY_ERROR_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
					} while (newName.isEmpty());

					try {
						action = new ClientRenameRequest(file, newName);
					} catch (IllegalArgumentException e1) {
						JOptionPane.showMessageDialog(fileManager, e1.getMessage(),
								StringTable.getString("GuiClient.GENERIC_ERROR_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
						return;
					}
				} else if (cmd.equals("new_dir")) { //$NON-NLS-1$
					String newName;
					do {
						Object oNewName = JOptionPane.showInputDialog(fileManager,
								StringTable.getString("GuiClient.MKDIR_DIALOG_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.MKDIR_DIALOG_TITLE"), JOptionPane.PLAIN_MESSAGE); //$NON-NLS-1$

						if (oNewName == null)
							return;
						newName = oNewName.toString();
						if (newName.isEmpty())
							JOptionPane.showMessageDialog(fileManager,
									StringTable.getString("GuiClient.MKDIR_EMPTY_ERROR_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.MKDIR_EMPTY_ERROR_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
					} while (newName.isEmpty());
					try {
						action = new ClientMakeDirRequest(newName, file);
					} catch (IllegalArgumentException e1) {
						JOptionPane.showMessageDialog(fileManager, e1.getMessage(),
								StringTable.getString("GuiClient.GENERIC_ERROR_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
						return;
					}
				} else if (cmd.equals("delete")) { //$NON-NLS-1$
					if (0 != JOptionPane.showConfirmDialog(fileManager,
							String.format(StringTable.getString("GuiClient.DELETE_CONFIRM_MESSAGE"), file.getName()), //$NON-NLS-1$
							StringTable.getString("GuiClient.DELETE_CONFIRM_TITLE"), //$NON-NLS-1$
							JOptionPane.YES_NO_OPTION))
						return;
					// Confirmar si eliminar contenidos de un directorio.
					int doRecurse = 1;
					if (file.isDirectory()) {
						doRecurse = JOptionPane.showConfirmDialog(fileManager,
								String.format(StringTable.getString("GuiClient.DELETE_RECURSIVE_CONFIRM_MESSAGE"), //$NON-NLS-1$
										file.getName()),
								StringTable.getString("GuiClient.DELETE_RECURSIVE_CONFIRM_TITLE"), //$NON-NLS-1$
								JOptionPane.YES_NO_OPTION);
						if (doRecurse != 0)
							return;
					}
					action = new ClientDeleteRequest(file, doRecurse == 0);
				} else if (cmd.equals("copy")) { //$NON-NLS-1$
					// Obtener la ruta del servidor
					ServerDirEntryTreeNode node = (ServerDirEntryTreeNode) fileManager.getServerPanel().getTree()
							.getSelectionPath().getLastPathComponent();
					DirEntry entry = node.getDirectoryEntry();
					String remotePath = getRemoteFilePath(new TreePath(node.getPath()));
					if (entry.getType() == EntryType.FILE && file.getName().equals(entry.getName())) {
						if (0 != JOptionPane.showConfirmDialog(fileManager,
								StringTable.getString("GuiClient.TRANSFER_OVERWRITE_CONFIRM_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.TRANSFER_OVERWRITE_CONFIRM_TITLE"), //$NON-NLS-1$
								JOptionPane.YES_NO_OPTION))
							return;
					} else {
						remotePath += "/" + file.getName(); //$NON-NLS-1$
					}
					progressDialog = new TransferProgressDialog();
					progressDialog.setLocationRelativeTo(fileManager);
					progressDialog.setModalityType(ModalityType.APPLICATION_MODAL);
					worker = new FileTransferWorker(progressDialog, file, remotePath, file.length(), true);
					worker.setCallback(new Runnable() {
						@Override
						public void run() {
							refreshServerTree(new TreePath(node.getPath()));
						}
					});
				} else {
					return;
				}

				try {
					if (!cmd.equals("copy")) { //$NON-NLS-1$
						Future<Boolean> result = serverRequestExecutor.submit(action);
						if (!result.get())
							JOptionPane.showMessageDialog(fileManager,
									String.format(StringTable.getString("GuiClient.FILE_MANAGEMENT_ERROR_MESSAGE"), //$NON-NLS-1$
											file.getName()),
									StringTable.getString("GuiClient.FILE_MANAGEMENT_ERROR_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						refreshClientTree(selection.getParentPath());
					} else {
						worker.execute();
						progressDialog.setVisible(true);
					}
				} catch (ExecutionException e1) {
					Exception trueReason = (Exception) e1.getCause();
					if (trueReason instanceof IOException) {
						JOptionPane.showMessageDialog(fileManager,
								String.format(
										StringTable
												.getString("GuiClient.TRANSFER_FILE_MANAGEMENT_IO_EXCEPTION_MESSAGE"), //$NON-NLS-1$
										trueReason.getMessage()),
								StringTable.getString("GuiClient.TRANSFER_FILE_MANAGEMENT_IO_EXCEPTION_TITLE"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
					} else if (trueReason instanceof SecurityException) {
						JOptionPane.showMessageDialog(fileManager,
								StringTable.getString("GuiClient.TRANSFER_SECURITY_EXCEPTION_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.TRANSFER_SECURITY_EXCEPTION_TITLE"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
					} else if (trueReason instanceof Exception) {
						JOptionPane.showMessageDialog(fileManager, trueReason.getMessage(),
								StringTable.getString("GuiClient.SOMETHING_HAPPENED_TITLE"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
					} else {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(fileManager,
								StringTable.getString("GuiClient.SOMETHING_HAPPENED_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.SOMETHING_HAPPENED_TITLE"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
						System.exit(1);
					}
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(fileManager,
							StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_MESSAGE"), //$NON-NLS-1$
							StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_TITLE"), //$NON-NLS-1$
							JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		fileManager.getBtnClientRename().addActionListener(clientActionListener);
		fileManager.getBtnClientDelete().addActionListener(clientActionListener);
		fileManager.getBtnClientNewFolder().addActionListener(clientActionListener);
		fileManager.getBtnClientCopy().addActionListener(clientActionListener);
	}

	private void setupPasswordChange() {
		class ChangePasswordRequest implements Callable<Void> {
			String password, newPassword;

			@SuppressWarnings("unused")
			private ChangePasswordRequest() {}

			public ChangePasswordRequest(String password, String newPassword) {
				this.password = password;
				this.newPassword = newPassword;
			}

			@Override
			public Void call()
					throws RemoteException, InvalidLoginException, InvalidTokenException, FileServerException {
				client.changePassword(password, newPassword);
				return null;
			}
		}

		ActionListener btnChangeListener = new ActionListener() {
			ChangePasswordDialog chgPwdDialog;
			ActionListener submitListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (!chgPwdDialog.checkNewPassword())
						// no quiero más código flecha del que hay
						return;
					String oldPassword = chgPwdDialog.getOldPassword();
					String newPassword = chgPwdDialog.getNewPassword();
					Future<Void> result = serverRequestExecutor
							.submit(new ChangePasswordRequest(oldPassword, newPassword));
					try {
						result.get();
						JOptionPane.showMessageDialog(chgPwdDialog,
								StringTable.getString("GuiClient.PASSWORD_CHANGE_RESULT_SUCCESS_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.PASSWORD_CHANGE_RESULT_SUCCESS_TITLE"), //$NON-NLS-1$
								JOptionPane.INFORMATION_MESSAGE);
						chgPwdDialog.setVisible(false);
						chgPwdDialog.dispose();
					} catch (ExecutionException e1) {
						Exception trueReason = (Exception) e1.getCause();
						if (trueReason instanceof RemoteException) {
							trueReason.printStackTrace();
							JOptionPane.showMessageDialog(chgPwdDialog,
									StringTable.getString("GuiClient.REMOTE_EXCEPTION_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.REMOTE_EXCEPTION_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						} else if (trueReason instanceof InvalidTokenException) {
							JOptionPane.showMessageDialog(fileManager,
									StringTable.getString("GuiClient.SESSION_EXPIRED_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.SESSION_EXPIRED_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						} else if (trueReason instanceof InvalidLoginException) {
							JOptionPane.showMessageDialog(chgPwdDialog,
									StringTable.getString("GuiClient.PASSWORD_CHANGE_RESULT_CURRENT_INVALID_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.PASSWORD_CHANGE_RESULT_CURRENT_INVALID_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						} else if (trueReason instanceof FileServerException) {
							JOptionPane.showMessageDialog(chgPwdDialog, trueReason.getMessage(),
									StringTable.getString("GuiClient.PASSWORD_CHANGE_RESULT_SERVER_ERROR_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
						} else {
							e1.printStackTrace();
							JOptionPane.showMessageDialog(chgPwdDialog,
									StringTable.getString("GuiClient.SOMETHING_HAPPENED_MESSAGE"), //$NON-NLS-1$
									StringTable.getString("GuiClient.SOMETHING_HAPPENED_TITLE"), //$NON-NLS-1$
									JOptionPane.ERROR_MESSAGE);
							System.exit(1);
						}
					} catch (InterruptedException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(chgPwdDialog,
								StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_MESSAGE"), //$NON-NLS-1$
								StringTable.getString("GuiClient.INTERRUPTED_EXECUTION_TITLE"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
					}
				}
			};

			ActionListener cancelClickedListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					chgPwdDialog.setVisible(false);
					chgPwdDialog.dispose();
				}
			};

			@Override
			public void actionPerformed(ActionEvent e) {
				chgPwdDialog = new ChangePasswordDialog();
				chgPwdDialog.getBtnSubmit().addActionListener(submitListener);
				chgPwdDialog.setLocationRelativeTo(fileManager);
				chgPwdDialog.setModalityType(ModalityType.APPLICATION_MODAL);

				chgPwdDialog.getBtnCancel().addActionListener(cancelClickedListener);
				chgPwdDialog.getRootPane().registerKeyboardAction(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						chgPwdDialog.setVisible(false);
						chgPwdDialog.dispose();
					}
				}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

				chgPwdDialog.setVisible(true);
			}

		};

		fileManager.getBtnChangePassword().addActionListener(btnChangeListener);
	}

	public void start() {
		System.out.println(StringTable.getString("GuiClient.USING_GUI_CLIENT")); //$NON-NLS-1$
		client = new GuiClientRemote();

		// Inicializar la raíz del cliente ahora
		clientRootDirectory = new File(System.getProperty("user.home")); //$NON-NLS-1$
		// Estilo de ventanas
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) { // Mucha excepción así que vamos a lo general...
			JOptionPane.showMessageDialog(null, StringTable.getString("GuiClient.COULD_NOT_LOAD_LOOKANDFEEL_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("GuiClient.COULD_NOT_LOAD_LOOKANDFEEL_TITLE"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
		}

		// Poner un título decente
		// https://stackoverflow.com/a/10986679
		try {
			java.awt.Toolkit xToolkit = java.awt.Toolkit.getDefaultToolkit();
			java.lang.reflect.Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName"); //$NON-NLS-1$
			awtAppClassNameField.setAccessible(true);
			awtAppClassNameField.set(xToolkit, StringTable.getString("GuiClient.AWT_APPLICATION_NAME")); //$NON-NLS-1$

		} catch (Exception e) {}
		// Lanzar la IU
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fileManager = new FileManagerFrame();
				fileManager.setLocationRelativeTo(null);
				fileManager.setVisible(true);
				fileManager.setEnabledStatus(false);
			}
		});
		// Mostrar el login
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					loginDialog = new LoginDialog();
					setLoginDialogListeners();
					loginDialog.setLocationRelativeTo(fileManager);
					loginDialog.setModalityType(ModalityType.APPLICATION_MODAL);
					loginDialog.setVisible(true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(fileManager, e.getMessage(),
					StringTable.getString("GuiClient.SOMETHING_HAPPENED_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
			System.exit(1);
		}

		if (!client.isConnected())
			System.exit(0);

		// Desbloquear la IU y mostrar sistema de archivos.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setupServerView();
				setupClientView();
				setupPasswordChange();
				fileManager.setUsername(client.getUsername());
				fileManager.setServerHostname(client.getServerHostname());
				fileManager.setEnabledStatus(true);
				fileManager.setServerButtonsEnable(false);
				fileManager.setClientButtonsEnable(false);
			}
		});
	}
}

// (Este debe ser el intento de "MVC" más rasca del mundo, si es que califica como uno.
// Responder por interno
