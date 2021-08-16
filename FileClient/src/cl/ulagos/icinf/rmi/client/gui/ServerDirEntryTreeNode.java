package cl.ulagos.icinf.rmi.client.gui;

import java.util.Comparator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import cl.ulagos.icinf.rmi.client.StringTable;
import cl.ulagos.icinf.rmi.providers.DirEntry;
import cl.ulagos.icinf.rmi.providers.EntryType;

public class ServerDirEntryTreeNode extends DefaultMutableTreeNode {

	private DefaultMutableTreeNode loadingNode = new DefaultMutableTreeNode(StringTable.getString("DirEntryTreeNode.LOADING_NODE")); //$NON-NLS-1$

	private static Comparator<ServerDirEntryTreeNode> DirEntryComparator = new Comparator<ServerDirEntryTreeNode>() {

		@Override
		public int compare(ServerDirEntryTreeNode o1, ServerDirEntryTreeNode o2) {
			DirEntry d1 = (DirEntry) o1.getUserObject();
			DirEntry d2 = (DirEntry) o2.getUserObject();

			int result = d1.getName().compareTo(d2.getName());
			// Directorios primero
			if (d1.getType() == EntryType.FILE && d2.getType() == EntryType.DIRECTORY && result < 0)
				result *= -1;
			if (d1.getType() == EntryType.DIRECTORY && d2.getType() == EntryType.FILE && result > 0)
				result *= -1;

			return result;
		}
	};

	public ServerDirEntryTreeNode(DirEntry entry) {
		super(entry);
		if (entry.getType() == EntryType.DIRECTORY) {
			add(loadingNode);
		} else {
			setAllowsChildren(false);
		}
	}

	public void loadSubentries(List<DirEntry> entries) {
		// Si, reemplazamos todos los contenidos si hay un cambio.
		removeAllChildren();
		if (entries == null) {
			add(new DefaultMutableTreeNode(StringTable.getString("DirEntryTreeNode.EMPTY_NODE"))); //$NON-NLS-1$
			return;
		}
		for (DirEntry e : entries) {
			add(new ServerDirEntryTreeNode(e));
		}
		children.sort(DirEntryComparator);
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
		children.sort(DirEntryComparator);
	}

	public DirEntry getDirectoryEntry() {
		if (this.equals(loadingNode))
			return new DirEntry("/", EntryType.DIRECTORY); //$NON-NLS-1$
		else
			return (DirEntry) userObject;
	}
}
