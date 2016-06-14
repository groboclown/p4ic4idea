/**
 * 
 */
package com.perforce.p4java.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;

/**
 * Defines the common operations to Perforce view maps. View maps are
 * normally used in Perforce clients, labels, branches, etc., to map one type
 * of path (a depot path, for example) to a different type of path (e.g. a
 * client path).<p>
 * 
 * View maps work in a manner that's described in the main Perforce documentation
 * for the basic client view, but in summary, map entries can be inclusive,
 * exclusive, or overlays, and map entry order is (of course) deeply significant.<p>
 * 
 * This implementation of view maps does not (yet) include advanced Perforce
 * functionality (such as translation or testing the map to see whether a
 * path is mapped or not), but future versions will; the emphasis here is on
 * setting up a common basis for P4Java view maps.
 */

public class ViewMap<E extends IMapEntry> implements Iterable<E> {
	
	protected List<E> entryList = null;
	
	/**
	 * Default constructor. Creates a new ViewMap with an
	 * empty (but not null) entry list.
	 */
	public ViewMap() {
		this.entryList = new ArrayList<E>();
	}
	
	/**
	 * Constructs a new ViewMap from the passed-in entry list. The passed-in
	 * list is inspected for consistency before being used.
	 * 
	 * @param entryList non-null (but possibly-empty) entry list.
	 */
	public ViewMap(List<E> entryList) {
		checkEntryList(entryList);
		this.entryList = entryList;
	}
	
	/**
	 * Return the number of elements in the associated entry list.
	 */
	public int getSize() {
		return this.entryList.size();
	}

	/**
	 * Delete the entry at the specified position. Will throw a P4JavaError
	 * if order is out of bounds. The order field of the deleted entry
	 * will be set to ORDER_UNKNOWN; the order fields of any entries
	 * "below" the deletion will be updated with their new order.
	 * 
	 * @param position order of entry to be deleted
	 */
	public synchronized void deleteEntry(int position) {
		if ((position < 0) || (position >= this.entryList.size())) {
			throw new P4JavaError("Position out of range: "
					+ position + "; list size: " + this.entryList.size());
		}
		updateEntryListPositions();
	}

	/**
	 * Get the map entry at the specified position. Will throw a P4JavaError
	 * if order is out of bounds.
	 * 
	 * @param position list position to use
	 */
	public synchronized E getEntry(int position) {
		if ((position < 0) || (position >=this.entryList.size())) {
			throw new P4JavaError("Position out of range: "
					+ position + "; list size: " + this.entryList.size());
		}
		
		E entry = this.entryList.get(position);
		if (entry == null) {
			throw new NullPointerError("Null entry in ViewMap list");
		} else if (entry.getOrder() != position) {
			throw new P4JavaError(
					"Entry internal order does not match list order");
		}
		return entry;
	}

	/**
	 * Get the entry list associated with this view map.
	 * 
	 * @return non-null entry list
	 */
	public List<E> getEntryList() {
		return this.entryList;
	}

	/**
	 * Add a map new entry at the end of the view map. The value of the entry's
	 * order field will be set to the order in the entry list.
	 * 
	 * @param entry non-null map entry.
	 */
	public synchronized void addEntry(E entry) {
		if (entry == null) {
			throw new NullPointerError(
					"Null entry passed to ViewMap insertEntry method");
		}
		this.entryList.add(entry);
		entry.setOrder(this.getSize() - 1);
	}

	/**
	 * Set (replace) a specific map position.<p>
	 *  
	 * Will throw a P4JavaError if order is out of bounds or if the new entry
	 * is null. The value of the entry's order field will be set to the
	 * order in the entry list; the value of the replaced entry's
	 * order field will be set to ORDER_UNKNOWN.
	 * 
	 * @param position list order of replacement
	 * @param entry non-null replacement entry
	 */
	public synchronized void setEntry(int position, E entry) {
		if ((position < 0) || (position >= this.entryList.size())) {
			throw new P4JavaError("Position out of range: "
					+ position + "; list size: " + this.entryList.size());
		}
		if (entry == null) {
			throw new NullPointerError(
					"Null entry passed to ViewMap replaceEntry method");
		}
		this.entryList.get(position).setOrder(IMapEntry.ORDER_UNKNOWN);
		this.entryList.set(position, entry);
		entry.setOrder(position);
	}

	/**
	 * Set the entry list associated with this view map.
	 * 
	 * @param entryList non-null entry list
	 */
	public void setEntryList(List<E> entryList) {
		checkEntryList(entryList);
		this.entryList = entryList;
	}
	
	/**
	 * Do some sanity checks on the passed-in entry list. This includes
	 * checking for null list, null entries, and whether each entry's order
	 * field matches its actual order in the list. Throws NullPointerError
	 * or P4JavaError as appropriate.
	 */
	
	public synchronized void checkEntryList(List<E> entryList) {
		if (entryList == null) {
			throw new NullPointerError(
					"Null entryList passed to checkEntryList");
		} else {
			int pos = 0;
			for (IMapEntry entry : entryList) {
				if (entry == null) {
					throw new NullPointerError(
							"Null entry in list passed to checkEntryList");
				}
				if (entry.getOrder() != pos) {
					throw new P4JavaError(
							"Inconsistent view map entry order in entry list check");
				}
				pos++;
			}
		}
	}
	
	/**
	 * Update the entry list entry positions after an update by reassigning entry-internal
	 * positions as appropriate.
	 */
	protected void updateEntryListPositions() {
		int pos = 0;
		for (E entry : entryList) {
			if (entry == null) {
				throw new NullPointerError("Null entry in entryList");
			}
			entry.setOrder(pos++);
		}
	}

	/**
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<E> iterator() {
		return this.entryList.iterator();
	}
}
