package cl.ulagos.icinf.rmi.client.gui;

import java.io.File;
import java.util.Comparator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import cl.ulagos.icinf.rmi.client.StringTable;
import cl.ulagos.icinf.rmi.providers.DirEntry;
import cl.ulagos.icinf.rmi.providers.EntryType;

public class ClientDirEntryTreeNode extends DefaultMutableTreeNode {

	private DefaultMutableTreeNode loadingNode = new DefaultMutableTreeNode(StringTable.getString("DirEntryTreeNode.LOADING_NODE")); //$NON-NLS-1$

	private static Comparator<ClientDirEntryTreeNode> FileNameComparator = new Comparator<ClientDirEntryTreeNode>() {

		@Override
		public int compare(ClientDirEntryTreeNode o1, ClientDirEntryTreeNode o2) {
			File f1 = (File) o1.getUserObject();
			File f2 = (File) o2.getUserObject();

			int result = f1.getName().compareTo(f2.getName());
			// Directorios primero
			if (f1.isFile() && f2.isDirectory() && result < 0)
				result *= -1;
			if (f1.isDirectory() && f2.isFile() && result > 0)
				result *= -1;

			return result;
		}
	};

	public ClientDirEntryTreeNode(File entry) {
		super(entry);
		if (entry.isDirectory()) {
			add(loadingNode);
		} else {
			setAllowsChildren(false);
		}
	}

	@Override
	public String toString() {
		// En esta clase aún hay oportunidad para cambiar la representación del nodo en
		// el árbol.
		if (isRoot())
			return ((File) getUserObject()).getAbsolutePath();
		else
			return ((File) getUserObject()).getName();
	}

	public void loadSubentries(File[] entries) {
		// Si, reemplazamos todos los contenidos si hay un cambio.
		removeAllChildren();
		if (entries == null) {
			add(new DefaultMutableTreeNode(StringTable.getString("DirEntryTreeNode.EMPTY_NODE"))); //$NON-NLS-1$
			return;
		}
		for (File e : entries) {
			add(new ClientDirEntryTreeNode(e));
		}
		children.sort(FileNameComparator);
	}

	public void removeSubentries() {
		// Si, reemplazamos todos los contenidos si hay un cambio.
		removeAllChildren();
		add(loadingNode);
	}

	public void replaceNode(DefaultMutableTreeNode oldNode, DefaultMutableTreeNode newNode) {
		int index = children.indexOf(oldNode);
		if (index == -1)
			throw new IllegalArgumentException(StringTable.getString("DirEntryTreeNode.REPLACED_NODE_NOT_EXISTS")); //$NON-NLS-1$

		children.set(index, newNode);
		newNode.setParent((MutableTreeNode) oldNode.getParent());
		oldNode.setParent(null);
		children.sort(FileNameComparator);
	}

	public File getDirectoryEntry() {
		if (this.equals(loadingNode))
			return null;
		else
			return (File) userObject;
	}
}
