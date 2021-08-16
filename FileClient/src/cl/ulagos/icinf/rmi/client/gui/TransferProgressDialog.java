package cl.ulagos.icinf.rmi.client.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import cl.ulagos.icinf.rmi.client.StringTable;

public class TransferProgressDialog extends JDialog {

	private final JPanel headerPanel = new JPanel();
	private JLabel lblSource;
	private JLabel lblDestination;
	private JLabel lblProgress;
	private JButton btnCancel;
	private JProgressBar progressBar;

	private static int clamp(int value, int min, int max) {
		return (value > max) ? max : (value < min) ? min : value;
	}

	private void setProgress(int value) {
		int progress = clamp(value, 0, 100);
		lblProgress.setText(progress + "%"); //$NON-NLS-1$
		progressBar.setValue(progress);
	}

	public void setDestinationFile(String value) {
		lblDestination.setText(value);
	}

	public void setSourceFile(String value) {
		lblSource.setText(value);
	}

	public JButton getBtnCancel() {
		return btnCancel;
	}

	private void initComponents() {
		setTitle(StringTable.getString("TransferProgressDialog.FILE_TRANSFER_TITLE")); //$NON-NLS-1$
		setMinimumSize(new Dimension(440, 140));
		setResizable(false);
		getContentPane().setLayout(new BorderLayout(5, 5));
		headerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(headerPanel, BorderLayout.NORTH);
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

		Box hbSource = Box.createHorizontalBox();
		hbSource.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.add(hbSource);

		JLabel lblTransferring = new JLabel(StringTable.getString("TransferProgressDialog.HEADER_TITLE")); //$NON-NLS-1$
		hbSource.add(lblTransferring);

		lblSource = new JLabel("<archivo>"); //$NON-NLS-1$
		hbSource.add(lblSource);

		Box hbDestination = Box.createHorizontalBox();
		hbDestination.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.add(hbDestination);

		JLabel lblFileDestination = new JLabel(StringTable.getString("TransferProgressDialog.LABEL_DESTINATION")); //$NON-NLS-1$
		hbDestination.add(lblFileDestination);

		lblDestination = new JLabel("<destino>"); //$NON-NLS-1$
		hbDestination.add(lblDestination);

		Box verticalBox = Box.createVerticalBox();
		hbDestination.add(verticalBox);

		JPanel statusPanel = new JPanel();
		getContentPane().add(statusPanel, BorderLayout.CENTER);
		GridBagLayout gbl_statusPanel = new GridBagLayout();
		gbl_statusPanel.columnWeights = new double[] { 1.0, 0.0 };
		gbl_statusPanel.rowWeights = new double[] { 0.0 };
		statusPanel.setLayout(gbl_statusPanel);

		progressBar = new JProgressBar();
		progressBar.setValue(0);
		GridBagConstraints gbc_progressBar = new GridBagConstraints();
		gbc_progressBar.ipadx = 5;
		gbc_progressBar.fill = GridBagConstraints.BOTH;
		gbc_progressBar.insets = new Insets(0, 5, 0, 5);
		gbc_progressBar.gridx = 0;
		gbc_progressBar.gridy = 0;
		statusPanel.add(progressBar, gbc_progressBar);

		lblProgress = new JLabel("X %"); //$NON-NLS-1$
		GridBagConstraints gbc_lblX = new GridBagConstraints();
		gbc_lblX.ipadx = 5;
		gbc_lblX.anchor = GridBagConstraints.WEST;
		gbc_lblX.gridx = 1;
		gbc_lblX.gridy = 0;
		statusPanel.add(lblProgress, gbc_lblX);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		btnCancel = new JButton(StringTable.getString("TransferProgressDialog.BTN_CANCEL")); //$NON-NLS-1$
		buttonPane.add(btnCancel);
	}

	public TransferProgressDialog() {
		initComponents();
	}

	public class ProgressListener implements PropertyChangeListener {

		public ProgressListener() {
			setProgress(0);
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("progress")) { //$NON-NLS-1$
				setProgress((int) evt.getNewValue());
			}
		}
	}
}
