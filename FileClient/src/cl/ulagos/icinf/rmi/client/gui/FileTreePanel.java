package cl.ulagos.icinf.rmi.client.gui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import cl.ulagos.icinf.rmi.client.StringTable;

/**
 * Muestra el listado de archivos.
 * 
 * @author Nicolás Stuardo
 *
 */
public class FileTreePanel extends JPanel {

	private JLabel lblViewName;
	private JScrollPane scrollPane;
	private JTree fileTree;

	public FileTreePanel() {
		setLayout(new BorderLayout(0, 10));

		lblViewName = new JLabel("<nombre>"); //$NON-NLS-1$
		add(lblViewName, BorderLayout.NORTH);

		scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);

		fileTree = new JTree();
		fileTree.setShowsRootHandles(true);
		// Solo seleccionar un nodo a la vez.
		fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		scrollPane.setViewportView(fileTree);

		clearTree();
	}

	/**
	 * Establece el nombre de la vista a mostrar.
	 * 
	 * @param text El nombre a mostrar en pantalla.
	 */
	public void setHeaderText(String text) {
		lblViewName.setText(text + ":"); //$NON-NLS-1$
	}

	/**
	 * Limpia el árbol y coloca un elemento vacío.
	 */
	public void clearTree() {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(StringTable.getString("FileTreePanel.NO_CONTENTS")); //$NON-NLS-1$
		DefaultTreeModel model = new DefaultTreeModel(top);
		fileTree.setModel(model);
	}

	@Override
	public void setEnabled(boolean enabled) {
		scrollPane.setEnabled(enabled);
		fileTree.setEnabled(enabled);
		lblViewName.setEnabled(enabled);
	}

	public JTree getTree() {
		return fileTree;
	}
}
