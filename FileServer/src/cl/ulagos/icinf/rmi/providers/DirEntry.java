package cl.ulagos.icinf.rmi.providers;

import java.io.Serializable;

/**
 * Representa una entrada de directorio.
 * 
 * @author Nicolás Stuardo
 *
 */
public class DirEntry implements Cloneable, Serializable {
	/**
	 * Nombre de la entrada
	 */
	protected String name;
	/**
	 * Tipo de entrada
	 */
	protected EntryType type;
	/**
	 * Tamaño de la entrada
	 */
	protected long size;

	public DirEntry(String name, EntryType type, long size) {
		this.name = name;
		this.type = type;
		this.size = size;
	}

	public DirEntry(String name, EntryType type) {
		this(name, type, 0L);
	}

	protected DirEntry() {
	}

	/**
	 * Obtiene el nombre de la entrada de directorio.
	 * 
	 * @return El nombre de la entrada.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Obtiene el tipo de entrada.
	 * 
	 * @return La constante enum que indica el tipo de entrada
	 */
	public EntryType getType() {
		return type;
	}

	/**
	 * Obtiene el tamaño del archivo.
	 * 
	 * @return El tamaño del archivo en bytes.
	 */
	public long getSize() {
		return size;
	}

	@Override
	public Object clone() {
		DirEntry clone;
		try {
			clone = (DirEntry) super.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String toString() {
		return name;
	}
}
