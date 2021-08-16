package cl.ulagos.icinf.rmi.client.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import cl.ulagos.icinf.rmi.client.StringTable;

public class FileManagerFrame extends JFrame {

	private JPanel contentPane, headerPanel, mainPanel, serverFileOpsPanel, clientFileOpsPanel, sessionPanel;
	private FileInfoPanel clientInfoPanel, serverInfoPanel;
	private JLabel lblServerAddress, lblUserName;
	private FileTreePanel clientFSView, serverFSView;
	private JButton btnClientNewFolder, btnClientRename, btnClientDelete, btnClientCopy;
	private JButton btnServerNewFolder, btnServerRename, btnServerDelete, btnServerCopy;
	private JButton btnChangePassword;

	/**
	 * Panel que permite ocultar sus elementos desde fuera con solo una operación.
	 */
	private class RecursiveEnablePanel extends JPanel {
		private static final long serialVersionUID = 1L;

		@Override
		public void setEnabled(boolean enabled) {
			for (Component c : getComponents())
				c.setEnabled(enabled);
		}
	}

	public void resetPanels() {
		serverInfoPanel.setInfoVisibility(false);
		serverFSView.clearTree();
		clientInfoPanel.setInfoVisibility(false);
		clientFSView.clearTree();
	}

	public void setEnabledStatus(boolean status) {
		// Creían que iba a manipular los componentes uno por uno? JA!
		for (Component c : headerPanel.getComponents()) {
			c.setEnabled(status);
		}
		for (Component c : mainPanel.getComponents()) {
			c.setEnabled(status);
		}
		for (Component c : sessionPanel.getComponents()) {
			c.setEnabled(status);
		}
	}

	public FileTreePanel getClientPanel() {
		return clientFSView;
	}

	public FileTreePanel getServerPanel() {
		return serverFSView;
	}

	public FileInfoPanel getClientInfoPanel() {
		return clientInfoPanel;
	}

	public FileInfoPanel getServerInfoPanel() {
		return serverInfoPanel;
	}

	public JButton getBtnClientNewFolder() {
		return btnClientNewFolder;
	}

	public JButton getBtnClientRename() {
		return btnClientRename;
	}

	public JButton getBtnClientDelete() {
		return btnClientDelete;
	}

	public JButton getBtnClientCopy() {
		return btnClientCopy;
	}

	public JButton getBtnServerNewFolder() {
		return btnServerNewFolder;
	}

	public JButton getBtnServerRename() {
		return btnServerRename;
	}

	public JButton getBtnServerDelete() {
		return btnServerDelete;
	}

	public JButton getBtnServerCopy() {
		return btnServerCopy;
	}

	public JButton getBtnChangePassword() {
		return btnChangePassword;
	}

	public void setUsername(String username) {
		lblUserName.setText(String.format(StringTable.getString("FileManagerFrame.SESSION_USERNAME"), username)); //$NON-NLS-1$
	}

	public void setServerHostname(String hostname) {
		lblServerAddress.setText(String.format(StringTable.getString("FileManagerFrame.SESSION_HOSTNAME"), hostname)); //$NON-NLS-1$
	}

	public void setServerButtonsEnable(boolean enabled) {
		for (Component c : serverFileOpsPanel.getComponents())
			c.setEnabled(enabled);
	}

	public void setClientButtonsEnable(boolean enabled) {
		for (Component c : clientFileOpsPanel.getComponents())
			c.setEnabled(enabled);
	}

	public FileManagerFrame() {
		setMinimumSize(new Dimension(640, 440));
		setTitle(StringTable.getString("FileManagerFrame.FILE_MANAGER_TITLE")); //$NON-NLS-1$
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		contentPane = new JPanel(new BorderLayout(0, 10));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		headerPanel = new JPanel();
		contentPane.add(headerPanel, BorderLayout.NORTH);
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

		JLabel lblAppName = new JLabel(StringTable.getString("FileManagerFrame.FILE_MANAGER_HEADER")); //$NON-NLS-1$
		headerPanel.add(lblAppName);
		lblAppName.setFont(lblAppName.getFont().deriveFont(lblAppName.getFont().getSize() + 4f));

		lblServerAddress = new JLabel(); //$NON-NLS-1$
		headerPanel.add(lblServerAddress);

		mainPanel = new JPanel();
		contentPane.add(mainPanel, BorderLayout.CENTER);
		GridBagLayout gbl_mainPanel = new GridBagLayout();
		gbl_mainPanel.columnWidths = new int[] { 0, 0, 0 };
		gbl_mainPanel.rowHeights = new int[] { 0, 0, 0, 0 };
		gbl_mainPanel.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_mainPanel.rowWeights = new double[] { 0.0, 0.0, 1.0, Double.MIN_VALUE };
		mainPanel.setLayout(gbl_mainPanel);

		clientFSView = new FileTreePanel();
		clientFSView.setHeaderText(StringTable.getString("FileManagerFrame.CLIENT_PANEL_HEADER")); //$NON-NLS-1$
		GridBagConstraints gbc_clientFSView = new GridBagConstraints();
		gbc_clientFSView.insets = new Insets(0, 0, 5, 5);
		gbc_clientFSView.weighty = 5.0;
		gbc_clientFSView.fill = GridBagConstraints.BOTH;
		gbc_clientFSView.gridx = 0;
		gbc_clientFSView.gridy = 0;
		mainPanel.add(clientFSView, gbc_clientFSView);

		serverFSView = new FileTreePanel();
		serverFSView.setHeaderText(StringTable.getString("FileManagerFrame.SERVER_PANEL_HEADER")); //$NON-NLS-1$
		GridBagConstraints gbc_serverFSView = new GridBagConstraints();
		gbc_serverFSView.insets = new Insets(0, 0, 5, 0);
		gbc_serverFSView.weighty = 5.0;
		gbc_serverFSView.fill = GridBagConstraints.BOTH;
		gbc_serverFSView.gridx = 1;
		gbc_serverFSView.gridy = 0;
		mainPanel.add(serverFSView, gbc_serverFSView);

		clientInfoPanel = new FileInfoPanel();
		GridBagConstraints gbc_clientInfoPanel = new GridBagConstraints();
		gbc_clientInfoPanel.anchor = GridBagConstraints.WEST;
		gbc_clientInfoPanel.insets = new Insets(0, 0, 5, 5);
		gbc_clientInfoPanel.gridx = 0;
		gbc_clientInfoPanel.gridy = 1;
		mainPanel.add(clientInfoPanel, gbc_clientInfoPanel);

		serverInfoPanel = new FileInfoPanel();
		GridBagConstraints gbc_serverInfoPanel = new GridBagConstraints();
		gbc_serverInfoPanel.anchor = GridBagConstraints.WEST;
		gbc_serverInfoPanel.insets = new Insets(0, 0, 5, 0);
		gbc_serverInfoPanel.gridx = 1;
		gbc_serverInfoPanel.gridy = 1;
		mainPanel.add(serverInfoPanel, gbc_serverInfoPanel);

		clientFileOpsPanel = new RecursiveEnablePanel();
		clientFileOpsPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		GridBagConstraints gbc_clientFileOpsPanel = new GridBagConstraints();
		gbc_clientFileOpsPanel.insets = new Insets(0, 0, 0, 5);
		gbc_clientFileOpsPanel.fill = GridBagConstraints.BOTH;
		gbc_clientFileOpsPanel.gridx = 0;
		gbc_clientFileOpsPanel.gridy = 2;
		mainPanel.add(clientFileOpsPanel, gbc_clientFileOpsPanel);
		FlowLayout fl_clientFileOpsPanel = new FlowLayout(FlowLayout.LEADING, 0, 5);
		clientFileOpsPanel.setLayout(fl_clientFileOpsPanel);

		btnClientNewFolder = new JButton(StringTable.getString("FileManagerFrame.BTN_NEW_DIRECTORY")); //$NON-NLS-1$
		btnClientRename = new JButton(StringTable.getString("FileManagerFrame.BTN_RENAME")); //$NON-NLS-1$
		btnClientDelete = new JButton(StringTable.getString("FileManagerFrame.BTN_DELETE")); //$NON-NLS-1$
		btnClientCopy = new JButton(StringTable.getString("FileManagerFrame.BTN_CLIENT_COPY")); //$NON-NLS-1$
		
		btnClientNewFolder.setActionCommand("new_dir"); //$NON-NLS-1$
		btnClientRename.setActionCommand("rename"); //$NON-NLS-1$
		btnClientDelete.setActionCommand("delete"); //$NON-NLS-1$
		btnClientCopy.setActionCommand("copy"); //$NON-NLS-1$

		clientFileOpsPanel.add(btnClientNewFolder);
		clientFileOpsPanel.add(btnClientRename);
		clientFileOpsPanel.add(btnClientDelete);
		clientFileOpsPanel.add(btnClientCopy);

		serverFileOpsPanel = new RecursiveEnablePanel();
		serverFileOpsPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		GridBagConstraints gbc_serverFileOpsPanel = new GridBagConstraints();
		gbc_serverFileOpsPanel.fill = GridBagConstraints.BOTH;
		gbc_serverFileOpsPanel.gridx = 1;
		gbc_serverFileOpsPanel.gridy = 2;
		mainPanel.add(serverFileOpsPanel, gbc_serverFileOpsPanel);
		FlowLayout fl_serverFileOpsPanel = new FlowLayout(FlowLayout.LEADING, 0, 5);
		serverFileOpsPanel.setLayout(fl_serverFileOpsPanel);

		btnServerNewFolder = new JButton(StringTable.getString("FileManagerFrame.BTN_NEW_DIRECTORY")); //$NON-NLS-1$
		btnServerRename = new JButton(StringTable.getString("FileManagerFrame.BTN_RENAME")); //$NON-NLS-1$
		btnServerDelete = new JButton(StringTable.getString("FileManagerFrame.BTN_DELETE")); //$NON-NLS-1$
		btnServerCopy = new JButton(StringTable.getString("FileManagerFrame.BTN_SERVER_COPY")); //$NON-NLS-1$
		
		btnServerNewFolder.setActionCommand("new_dir"); //$NON-NLS-1$
		btnServerRename.setActionCommand("rename"); //$NON-NLS-1$
		btnServerDelete.setActionCommand("delete"); //$NON-NLS-1$
		btnServerCopy.setActionCommand("copy"); //$NON-NLS-1$

		serverFileOpsPanel.add(btnServerNewFolder);
		serverFileOpsPanel.add(btnServerRename);
		serverFileOpsPanel.add(btnServerDelete);
		serverFileOpsPanel.add(btnServerCopy);

		sessionPanel = new JPanel();
		contentPane.add(sessionPanel, BorderLayout.SOUTH);
		sessionPanel.setLayout(new BoxLayout(sessionPanel, BoxLayout.X_AXIS));

		lblUserName = new JLabel(); //$NON-NLS-1$
		sessionPanel.add(lblUserName);

		Component horizontalGlue = Box.createHorizontalGlue();
		sessionPanel.add(horizontalGlue);

		btnChangePassword = new JButton(StringTable.getString("FileManagerFrame.BTN_CHANGE_PASSWORD")); //$NON-NLS-1$
		sessionPanel.add(btnChangePassword);

		setUsername(StringTable.getString("FileManagerFrame.NOT_CONNECTED")); //$NON-NLS-1$
		setServerHostname(StringTable.getString("FileManagerFrame.NOT_CONNECTED")); //$NON-NLS-1$
		pack();
	}
}
