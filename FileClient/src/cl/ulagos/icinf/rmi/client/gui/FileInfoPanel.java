package cl.ulagos.icinf.rmi.client.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import cl.ulagos.icinf.rmi.client.StringTable;
import cl.ulagos.icinf.rmi.providers.DirEntry;
import cl.ulagos.icinf.rmi.providers.EntryType;

public class FileInfoPanel extends JPanel {

	private JPanel promptPanel, infoPanel, emptyPanel;
	private JLabel lblFilename, lblType, lblSize;

	public void initComponents() {
		CardLayout cards = new CardLayout();
		setLayout(cards);

		// Este panel muestra el mensaje.
		promptPanel = new JPanel(new BorderLayout());
		JLabel lblPrompt = new JLabel(StringTable.getString("FileInfoPanel.PROMPT_SELECT_ITEM")); //$NON-NLS-1$
		lblPrompt.setHorizontalAlignment(SwingConstants.CENTER);
		promptPanel.add(lblPrompt, BorderLayout.CENTER);

		add(promptPanel, "Prompt"); //$NON-NLS-1$

		emptyPanel = new JPanel(new BorderLayout());
		JLabel lblEmpty = new JLabel(StringTable.getString("FileInfoPanel.EMPTY_DIRECTORY")); //$NON-NLS-1$
		lblPrompt.setHorizontalAlignment(SwingConstants.CENTER);
		emptyPanel.add(lblEmpty, BorderLayout.CENTER);

		add(emptyPanel, "Empty"); //$NON-NLS-1$

		// Y este el contenido
		infoPanel = new JPanel();
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		infoPanel.setLayout(gridBagLayout);

		JLabel lblHeaderFilename = new JLabel(StringTable.getString("FileInfoPanel.INFO_NAME")); //$NON-NLS-1$
		GridBagConstraints gbc_lblHeaderFilename = new GridBagConstraints();
		gbc_lblHeaderFilename.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblHeaderFilename.insets = new Insets(0, 0, 5, 5);
		gbc_lblHeaderFilename.gridx = 0;
		gbc_lblHeaderFilename.gridy = 0;
		infoPanel.add(lblHeaderFilename, gbc_lblHeaderFilename);

		lblFilename = new JLabel();
		GridBagConstraints gbc_lblFilename = new GridBagConstraints();
		gbc_lblFilename.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblFilename.insets = new Insets(0, 0, 5, 0);
		gbc_lblFilename.gridx = 1;
		gbc_lblFilename.gridy = 0;
		infoPanel.add(lblFilename, gbc_lblFilename);

		JLabel lblHeaderType = new JLabel(StringTable.getString("FileInfoPanel.INFO_TYPE")); //$NON-NLS-1$
		GridBagConstraints gbc_lblHeaderType = new GridBagConstraints();
		gbc_lblHeaderType.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblHeaderType.insets = new Insets(0, 0, 5, 5);
		gbc_lblHeaderType.gridx = 0;
		gbc_lblHeaderType.gridy = 1;
		infoPanel.add(lblHeaderType, gbc_lblHeaderType);

		lblType = new JLabel();
		GridBagConstraints gbc_lblType = new GridBagConstraints();
		gbc_lblType.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblType.insets = new Insets(0, 0, 5, 0);
		gbc_lblType.gridx = 1;
		gbc_lblType.gridy = 1;
		infoPanel.add(lblType, gbc_lblType);

		JLabel lblHeaderSize = new JLabel(StringTable.getString("FileInfoPanel.INFO_SIZE")); //$NON-NLS-1$
		GridBagConstraints gbc_lblHeaderSize = new GridBagConstraints();
		gbc_lblHeaderSize.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblHeaderSize.insets = new Insets(0, 0, 0, 5);
		gbc_lblHeaderSize.gridx = 0;
		gbc_lblHeaderSize.gridy = 2;
		infoPanel.add(lblHeaderSize, gbc_lblHeaderSize);

		lblSize = new JLabel();
		GridBagConstraints gbc_lblSize = new GridBagConstraints();
		gbc_lblSize.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblSize.gridx = 1;
		gbc_lblSize.gridy = 2;
		infoPanel.add(lblSize, gbc_lblSize);

		add(infoPanel, "Detail"); //$NON-NLS-1$
	}

	public FileInfoPanel() {
		initComponents();
		setInfoVisibility(false);
	}

	public void setInfoVisibility(boolean visible) {
		CardLayout cl = (CardLayout) (getLayout());
		if (visible)
			cl.show(this, "Detail"); //$NON-NLS-1$
		else
			cl.show(this, "Prompt"); //$NON-NLS-1$

	}

	public void setFilename(String filename) {
		lblFilename.setText(filename);
	}

	public void setType(EntryType type) {
		switch (type) {
		case DIRECTORY:
			lblType.setText(StringTable.getString("FileInfoPanel.TYPE_DIRECTORY")); //$NON-NLS-1$
			break;
		case FILE:
			lblType.setText(StringTable.getString("FileInfoPanel.TYPE_FILE")); //$NON-NLS-1$
			break;
		default:
			lblType.setText(StringTable.getString("FileInfoPanel.TYPE_UNKNOWN")); //$NON-NLS-1$
			break;
		}
	}

	public void setSize(long size) {
		NumberFormat formatter = NumberFormat.getNumberInstance();
		lblSize.setText(String.format(StringTable.getString("FileInfoPanel.SIZE_FORMAT"), formatter.format(size))); //$NON-NLS-1$
	}

	public void setDetails(DirEntry entry) {
		if (entry == null) {
			((CardLayout) (getLayout())).show(this, "Empty"); //$NON-NLS-1$
		} else {
			setFilename(entry.getName());
			setType(entry.getType());
			setSize(entry.getSize());
		}
	}

	@Override
	public void setEnabled(boolean enable) {
		for (Component c : promptPanel.getComponents())
			c.setEnabled(enable);
		for (Component c : infoPanel.getComponents())
			c.setEnabled(enable);
		for (Component c : emptyPanel.getComponents())
			c.setEnabled(enable);
	}

}
