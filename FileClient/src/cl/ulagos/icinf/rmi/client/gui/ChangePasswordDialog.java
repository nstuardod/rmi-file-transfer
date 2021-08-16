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
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import cl.ulagos.icinf.rmi.client.StringTable;

public class ChangePasswordDialog extends JDialog {
	private JPasswordField tfOldPassword, tfNewPassword, tfNewPassword2;
	private JButton btnSubmit, btnCancel;
	private JPanel changePasswordPanel;

	public JButton getBtnSubmit() {
		return btnSubmit;
	}

	public JButton getBtnCancel() {
		return btnCancel;
	}

	public boolean checkNewPassword() {
		// Que ingrese la actual
		if ((tfOldPassword.getPassword().length == 0)) {
			JOptionPane.showMessageDialog(this, StringTable.getString("ChangePasswordDialog.EMPTY_CURRENT_PASSWORD_MESSAGE"), StringTable.getString("ChangePasswordDialog.EMPTY_CURRENT_PASSWORD_TITLE"), //$NON-NLS-1$ //$NON-NLS-2$
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		// Que no esté en blanco
		if ((tfNewPassword.getPassword().length == 0)) {
			JOptionPane.showMessageDialog(this, StringTable.getString("ChangePasswordDialog.NO_NEW_PASSWORD_MESSAGE"), StringTable.getString("ChangePasswordDialog.NO_NEW_PASSWORD_TITLE"), //$NON-NLS-1$ //$NON-NLS-2$
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		// Que confirme la contraseña
		if ((tfNewPassword2.getPassword().length == 0)) {
			JOptionPane.showMessageDialog(this, StringTable.getString("ChangePasswordDialog.UNCONFIRMED_PASSWORD_MESSAGE"), StringTable.getString("ChangePasswordDialog.UNCONFIRMED_PASSWORD_TITLE"), //$NON-NLS-1$ //$NON-NLS-2$
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		// Que sean iguales
		if (!Arrays.equals(tfNewPassword.getPassword(), tfNewPassword2.getPassword())) {
			JOptionPane.showMessageDialog(this,
					StringTable.getString("ChangePasswordDialog.PASSWORDS_DONT_MATCH_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("ChangePasswordDialog.PASSWORDS_DONT_MATCH_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
			return false;
		}
		// Que no sea la misma ingresada.
		if (Arrays.equals(tfNewPassword.getPassword(), tfOldPassword.getPassword())) {
			JOptionPane.showMessageDialog(this, StringTable.getString("ChangePasswordDialog.SAME_PASSWORD_MESSAGE"), //$NON-NLS-1$
					StringTable.getString("ChangePasswordDialog.SAME_PASSWORD_TITLE"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	public String getOldPassword() {
		return new String(tfOldPassword.getPassword());
	}

	public String getNewPassword() {
		return new String(tfNewPassword.getPassword());
	}

	public void initComponents() {
		setResizable(false);
		setTitle(StringTable.getString("ChangePasswordDialog.CHANGE_PASSWORD_TITLE")); //$NON-NLS-1$
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel headerPane = new JPanel();
		getContentPane().add(headerPane, BorderLayout.NORTH);

		JLabel heading = new JLabel(StringTable.getString("ChangePasswordDialog.CHANGE_PASSWORD_HEADER")); //$NON-NLS-1$
		headerPane.add(heading);
		heading.setFont(heading.getFont().deriveFont(heading.getFont().getSize() + 5f));

		changePasswordPanel = new JPanel();
		changePasswordPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(changePasswordPanel, BorderLayout.CENTER);
		GridBagLayout gbl_changePasswordPanel = new GridBagLayout();
		gbl_changePasswordPanel.columnWidths = new int[] { 0, 0, 0 };
		gbl_changePasswordPanel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_changePasswordPanel.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_changePasswordPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		changePasswordPanel.setLayout(gbl_changePasswordPanel);

		JTextArea taInstructions = new JTextArea(2, 16);
		taInstructions.setLineWrap(true);
		taInstructions.setBackground(UIManager.getColor("Label.background")); //$NON-NLS-1$
		taInstructions.setEditable(false);
		taInstructions.setWrapStyleWord(true);
		taInstructions.setFont(UIManager.getFont("Label.font")); //$NON-NLS-1$
		taInstructions.setOpaque(false);
		taInstructions.setText(StringTable.getString("ChangePasswordDialog.CHANGE_PASSWORD_MESSAGE")); //$NON-NLS-1$
		GridBagConstraints gbc_taInstructions = new GridBagConstraints();
		gbc_taInstructions.anchor = GridBagConstraints.NORTH;
		gbc_taInstructions.gridwidth = 2;
		gbc_taInstructions.insets = new Insets(0, 0, 5, 0);
		gbc_taInstructions.fill = GridBagConstraints.HORIZONTAL;
		gbc_taInstructions.gridx = 0;
		gbc_taInstructions.gridy = 0;
		changePasswordPanel.add(taInstructions, gbc_taInstructions);

		JLabel lblCurrentPassword = new JLabel(StringTable.getString("ChangePasswordDialog.CURRENT_PASSWORD")); //$NON-NLS-1$
		GridBagConstraints gbc_lblServer = new GridBagConstraints();
		gbc_lblServer.anchor = GridBagConstraints.WEST;
		gbc_lblServer.insets = new Insets(0, 0, 5, 5);
		gbc_lblServer.gridx = 0;
		gbc_lblServer.gridy = 1;
		changePasswordPanel.add(lblCurrentPassword, gbc_lblServer);

		tfOldPassword = new JPasswordField(16);
		lblCurrentPassword.setLabelFor(tfOldPassword);
		GridBagConstraints gbc_tfOldPassword = new GridBagConstraints();
		gbc_tfOldPassword.fill = GridBagConstraints.HORIZONTAL;
		gbc_tfOldPassword.insets = new Insets(0, 0, 5, 0);
		gbc_tfOldPassword.gridx = 1;
		gbc_tfOldPassword.gridy = 1;
		changePasswordPanel.add(tfOldPassword, gbc_tfOldPassword);

		JLabel lblNewPassword = new JLabel(StringTable.getString("ChangePasswordDialog.NEW_PASSWORD")); //$NON-NLS-1$
		GridBagConstraints gbc_lblNewPassword = new GridBagConstraints();
		gbc_lblNewPassword.anchor = GridBagConstraints.WEST;
		gbc_lblNewPassword.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewPassword.gridx = 0;
		gbc_lblNewPassword.gridy = 2;
		changePasswordPanel.add(lblNewPassword, gbc_lblNewPassword);

		tfNewPassword = new JPasswordField(16);
		lblNewPassword.setLabelFor(tfNewPassword);
		GridBagConstraints gbc_newPassword = new GridBagConstraints();
		gbc_newPassword.fill = GridBagConstraints.HORIZONTAL;
		gbc_newPassword.insets = new Insets(0, 0, 5, 0);
		gbc_newPassword.gridx = 1;
		gbc_newPassword.gridy = 2;
		changePasswordPanel.add(tfNewPassword, gbc_newPassword);

		JLabel lblNewPassword2 = new JLabel(StringTable.getString("ChangePasswordDialog.CONFIRM_PASSWORD")); //$NON-NLS-1$
		GridBagConstraints gbc_lblNewPassword2 = new GridBagConstraints();
		gbc_lblNewPassword2.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewPassword2.anchor = GridBagConstraints.WEST;
		gbc_lblNewPassword2.gridx = 0;
		gbc_lblNewPassword2.gridy = 3;
		changePasswordPanel.add(lblNewPassword2, gbc_lblNewPassword2);

		tfNewPassword2 = new JPasswordField(16);
		lblNewPassword2.setLabelFor(tfNewPassword2);
		GridBagConstraints gbc_tfNewPassword2 = new GridBagConstraints();
		gbc_tfNewPassword2.fill = GridBagConstraints.HORIZONTAL;
		gbc_tfNewPassword2.gridx = 1;
		gbc_tfNewPassword2.gridy = 3;
		changePasswordPanel.add(tfNewPassword2, gbc_tfNewPassword2);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, 5, 5));

		btnSubmit = new JButton(StringTable.getString("ChangePasswordDialog.BTN_CHANGE")); //$NON-NLS-1$
		btnSubmit.setMnemonic('b');
		buttonsPanel.add(btnSubmit);

		btnCancel = new JButton(StringTable.getString("ChangePasswordDialog.BTN_CANCEL")); //$NON-NLS-1$
		btnCancel.setMnemonic('c');
		buttonsPanel.add(btnCancel);

		pack();

		// Enfocar primer textField al abrir
		addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				tfOldPassword.requestFocusInWindow();
			}
		});

		// Enter en password -> click en login
		tfNewPassword2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				btnSubmit.doClick();
			}
		});
	}

	public ChangePasswordDialog() {
		initComponents();
	}
}
