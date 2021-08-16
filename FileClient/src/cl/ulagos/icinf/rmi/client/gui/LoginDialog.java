package cl.ulagos.icinf.rmi.client.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import cl.ulagos.icinf.rmi.client.GuiClient;
import cl.ulagos.icinf.rmi.client.StringTable;

public class LoginDialog extends JDialog {
	private JPasswordField tfPassword;
	private JTextField tfUsername;
	private JTextField tfServer;
	private JButton btnLogin;
	private JButton btnCancel;

	public JButton getBtnLogin() {
		return btnLogin;
	}

	public JButton getBtnCancel() {
		return btnCancel;
	}

	public String getPassword() {
		return new String(tfPassword.getPassword());
	}

	public String getUsername() {
		return tfUsername.getText();
	}

	public String getServer() {
		return tfServer.getText();
	}

	public void initComponents() {
		setResizable(false);
		setTitle(StringTable.getString("LoginDialog.LOGIN_HEADER")); //$NON-NLS-1$
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();

		JPanel headerPane = new JPanel();
		getContentPane().add(headerPane, BorderLayout.NORTH);

		JLabel label = new JLabel(StringTable.getString("LoginDialog.TITLE")); //$NON-NLS-1$
		headerPane.add(label);
		label.setFont(label.getFont().deriveFont(label.getFont().getSize() + 5f));

		JPanel loginPanel = new JPanel();
		loginPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(loginPanel, BorderLayout.CENTER);
		GridBagLayout gbl_loginPanel = new GridBagLayout();
		gbl_loginPanel.columnWidths = new int[] { 0, 0, 0 };
		gbl_loginPanel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_loginPanel.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_loginPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		loginPanel.setLayout(gbl_loginPanel);

		JTextArea taInstructions = new JTextArea(2, 16);
		taInstructions.setLineWrap(true);
		taInstructions.setBackground(UIManager.getColor("Label.background")); //$NON-NLS-1$
		taInstructions.setEditable(false);
		taInstructions.setWrapStyleWord(true);
		taInstructions.setFont(UIManager.getFont("Label.font")); //$NON-NLS-1$
		taInstructions.setOpaque(false);
		taInstructions.setText(StringTable.getString("LoginDialog.LOGIN_MESSAGE")); //$NON-NLS-1$
		GridBagConstraints gbc_taInstructions = new GridBagConstraints();
		gbc_taInstructions.anchor = GridBagConstraints.NORTH;
		gbc_taInstructions.gridwidth = 2;
		gbc_taInstructions.insets = new Insets(0, 0, 5, 0);
		gbc_taInstructions.fill = GridBagConstraints.HORIZONTAL;
		gbc_taInstructions.gridx = 0;
		gbc_taInstructions.gridy = 0;
		loginPanel.add(taInstructions, gbc_taInstructions);

		JLabel lblServer = new JLabel(StringTable.getString("LoginDialog.HOSTNAME")); //$NON-NLS-1$
		GridBagConstraints gbc_lblServer = new GridBagConstraints();
		gbc_lblServer.anchor = GridBagConstraints.WEST;
		gbc_lblServer.insets = new Insets(0, 0, 5, 5);
		gbc_lblServer.gridx = 0;
		gbc_lblServer.gridy = 1;
		loginPanel.add(lblServer, gbc_lblServer);

		tfServer = new JTextField(20);
		lblServer.setLabelFor(tfServer);
		GridBagConstraints gbc_tfServer = new GridBagConstraints();
		gbc_tfServer.fill = GridBagConstraints.HORIZONTAL;
		gbc_tfServer.insets = new Insets(0, 0, 5, 0);
		gbc_tfServer.gridx = 1;
		gbc_tfServer.gridy = 1;
		loginPanel.add(tfServer, gbc_tfServer);

		JLabel lblUsername = new JLabel(StringTable.getString("LoginDialog.USERNAME")); //$NON-NLS-1$
		GridBagConstraints gbc_lblUsername = new GridBagConstraints();
		gbc_lblUsername.anchor = GridBagConstraints.WEST;
		gbc_lblUsername.insets = new Insets(0, 0, 5, 5);
		gbc_lblUsername.gridx = 0;
		gbc_lblUsername.gridy = 2;
		loginPanel.add(lblUsername, gbc_lblUsername);

		tfUsername = new JTextField(16);
		lblUsername.setLabelFor(tfUsername);
		GridBagConstraints gbc_tfUsername = new GridBagConstraints();
		gbc_tfUsername.fill = GridBagConstraints.HORIZONTAL;
		gbc_tfUsername.insets = new Insets(0, 0, 5, 0);
		gbc_tfUsername.gridx = 1;
		gbc_tfUsername.gridy = 2;
		loginPanel.add(tfUsername, gbc_tfUsername);

		JLabel lblPassword = new JLabel(StringTable.getString("LoginDialog.PASSWORD")); //$NON-NLS-1$
		GridBagConstraints gbc_lblPassword = new GridBagConstraints();
		gbc_lblPassword.insets = new Insets(0, 0, 0, 5);
		gbc_lblPassword.anchor = GridBagConstraints.WEST;
		gbc_lblPassword.gridx = 0;
		gbc_lblPassword.gridy = 3;
		loginPanel.add(lblPassword, gbc_lblPassword);

		tfPassword = new JPasswordField(16);
		lblPassword.setLabelFor(tfPassword);
		GridBagConstraints gbc_tfPassword = new GridBagConstraints();
		gbc_tfPassword.fill = GridBagConstraints.HORIZONTAL;
		gbc_tfPassword.gridx = 1;
		gbc_tfPassword.gridy = 3;
		loginPanel.add(tfPassword, gbc_tfPassword);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, 5, 5));

		btnLogin = new JButton(StringTable.getString("LoginDialog.BTN_LOGIN")); //$NON-NLS-1$
		buttonsPanel.add(btnLogin);

		btnCancel = new JButton(StringTable.getString("LoginDialog.BTN_CANCEL")); //$NON-NLS-1$
		buttonsPanel.add(btnCancel);

		pack();

		// Enfocar primer textField al abrir
		addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				tfServer.requestFocusInWindow();
			}
		});

		// Enter en password -> click en login
		tfPassword.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				btnLogin.doClick();
			}
		});
	}

	public LoginDialog() {
		initComponents();
	}
}
