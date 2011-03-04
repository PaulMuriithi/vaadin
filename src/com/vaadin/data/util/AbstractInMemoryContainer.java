package com.vaadin.data.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.vaadin.data.Container;
import com.vaadin.data.Container.ItemSetChangeNotifier;
import com.vaadin.data.Item;

/**
 * Abstract {@link Container} class that handles common functionality for
 * in-memory containers. Concrete in-memory container classes can either inherit
 * this class, inherit {@link AbstractContainer}, or implement the
 * {@link Container} interface directly.
 * 
 * Features:
 * <ul>
 * <li> {@link Container.Ordered}
 * <li> {@link Container.Indexed}
 * <li> {@link Filterable} (internal implementation, does not implement the
 * interface directly)
 * <li> {@link Sortable} (internal implementation, does not implement the
 * interface directly)
 * </ul>
 * 
 * @param <ITEMIDTYPE>
 *            the class of item identifiers in the container, use Object if can
 *            be any class
 * @param <PROPERTYIDCLASS>
 *            the class of property identifiers for the items in the container,
 *            use Object if can be any class
 * @param <ITEMCLASS>
 *            the (base) class of the Item instances in the container, use
 *            {@link Item} if unknown
 * 
 * @since 6.6
 */
public abstract class AbstractInMemoryContainer<ITEMIDTYPE, PROPERTYIDCLASS, ITEMCLASS extends Item>
        extends AbstractContainer implements ItemSetChangeNotifier,
        Container.Indexed {

    /**
     * Filter interface for item filtering in in-memory containers, including
     * for implementing {@link Filterable}.
     * 
     * An ItemFilter must implement {@link #equals(Object)} and
     * {@link #hashCode()} correctly to avoid duplicate filter registrations
     * etc.
     * 
     * TODO this may change for 6.6 with the new filtering API
     * 
     * @since 6.6
     */
    public interface ItemFilter extends Serializable {
        /**
         * Check if an item passes the filter.
         * 
         * @param item
         * @return true if the item is accepted by this filter
         */
        public boolean passesFilter(Item item);

        /**
         * Check if a change in the value of a property can affect the filtering
         * result. May always return true, at the cost of performance.
         * 
         * @param propertyId
         * @return true if the filtering result may/does change based on changes
         *         to the property identified by propertyId
         */
        public boolean appliesToProperty(Object propertyId);
    }

    /**
     * An ordered {@link List} of all item identifiers in the container,
     * including those that have been filtered out.
     * 
     * Must not be null.
     */
    private List<ITEMIDTYPE> allItemIds;

    /**
     * An ordered {@link List} of item identifiers in the container after
     * filtering, excluding those that have been filtered out.
     * 
     * This is what the external API of the {@link Container} interface and its
     * subinterfaces shows (e.g. {@link #size()}, {@link #nextItemId(Object)}).
     * 
     * If null, the full item id list is used instead.
     */
    private List<ITEMIDTYPE> filteredItemIds;

    /**
     * Filters that are applied to the container to limit the items visible in
     * it
     */
    private Set<ItemFilter> filters = new HashSet<ItemFilter>();

    /**
     * The item sorter which is used for sorting the container.
     */
    private ItemSorter itemSorter = new DefaultItemSorter();

    // Constructors

    /**
     * Constructor for an abstract in-memory container.
     */
    protected AbstractInMemoryContainer() {
        setAllItemIds(new ListSet<ITEMIDTYPE>());
    }

    // Container interface methods with more specific return class

    public abstract ITEMCLASS getItem(Object itemId);

    /**
     * Get an item even if filtered out.
     * 
     * For internal use only.
     * 
     * @param itemId
     * @return
     */
    protected abstract ITEMCLASS getUnfilteredItem(Object itemId);

    // cannot override getContainerPropertyIds() and getItemIds(): if subclass
    // uses Object as ITEMIDCLASS or PROPERTYIDCLASS, Collection<Object> cannot
    // be cast to Collection<MyInterface>

    // public abstract Collection<PROPERTYIDCLASS> getContainerPropertyIds();
    // public abstract Collection<ITEMIDCLASS> getItemIds();

    // Container interface method implementations

    public int size() {
        return getVisibleItemIds().size();
    }

    public boolean containsId(Object itemId) {
        // only look at visible items after filtering
        if (itemId == null) {
            return false;
        } else {
            return getVisibleItemIds().contains(itemId);
        }
    }

    public Collection<?> getItemIds() {
        return Collections.unmodifiableCollection(getVisibleItemIds());
    }

    // Container.Ordered

    public ITEMIDTYPE nextItemId(Object itemId) {
        int index = indexOfId(itemId);
        if (index >= 0 && index < size() - 1) {
            return getIdByIndex(index + 1);
        } else {
            // out of bounds
            return null;
        }
    }

    public ITEMIDTYPE prevItemId(Object itemId) {
        int index = indexOfId(itemId);
        if (index > 0) {
            return getIdByIndex(index - 1);
        } else {
            // out of bounds
            return null;
        }
    }

    public ITEMIDTYPE firstItemId() {
        if (size() > 0) {
            return getIdByIndex(0);
        } else {
            return null;
        }
    }

    public ITEMIDTYPE lastItemId() {
        if (size() > 0) {
            return getIdByIndex(size() - 1);
        } else {
            return null;
        }
    }

    public boolean isFirstId(Object itemId) {
        if (itemId == null) {
            return false;
        }
        return itemId.equals(firstItemId());
    }

    public boolean isLastId(Object itemId) {
        if (itemId == null) {
            return false;
        }
        return itemId.equals(lastItemId());
    }

    // Container.Indexed

    public ITEMIDTYPE getIdByIndex(int index) {
        return getVisibleItemIds().get(index);
    }

    public int indexOfId(Object itemId) {
        return getVisibleItemIds().indexOf(itemId);
    }

    // ItemSetChangeNotifier

    @Override
    public void addListener(Container.ItemSetChangeListener listener) {
        super.addListener(listener);
    }

    @Override
    public void removeListener(Container.ItemSetChangeListener listener) {
        super.removeListener(listener);
    }

    // internal methods

    // Filtering support

    /**
     * Filter the view to recreate the visible item list from the unfiltered
     * items, and send a notification if the set of visible items changed in any
     * way.
     */
    protected void filterAll() {
        if (doFilterContainer(!getFilters().isEmpty())) {
            fireItemSetChange();
        }
    }

    /**
     * Filters the data in the container and updates internal data structures.
     * This method should reset any internal data structures and then repopulate
     * them so {@link #getItemIds()} and other methods only return the filtered
     * items.
     * 
     * @param hasFilters
     *            true if filters has been set for the container, false
     *            otherwise
     * @return true if the item set has changed as a result of the filtering
     */
    protected boolean doFilterContainer(boolean hasFilters) {
        if (!hasFilters) {
            boolean changed = getAllItemIds().size() != getVisibleItemIds()
                    .size();
            setFilteredItemIds(null);
            return changed;
        }

        // Reset filtered list
        List<ITEMIDTYPE> originalFilteredItemIds = getFilteredItemIds();
        if (originalFilteredItemIds == null) {
            originalFilteredItemIds = Collections.emptyList();
        }
        setFilteredItemIds(new ListSet<ITEMIDTYPE>());

        // Filter
        boolean equal = true;
        Iterator<ITEMIDTYPE> origIt = originalFilteredItemIds.iterator();
        for (final Iterator<ITEMIDTYPE> i = getAllItemIds().iterator(); i
                .hasNext();) {
            final ITEMIDTYPE id = i.next();
            if (passesFilters(id)) {
                // filtered list comes from the full list, can use ==
                equal = equal && origIt.hasNext() && origIt.next() == id;
                getFilteredItemIds().add(id);
            }
        }

        return !equal || origIt.hasNext();
    }

    /**
     * Checks if the given itemId passes the filters set for the container. The
     * caller should make sure the itemId exists in the container. For
     * non-existing itemIds the behavior is undefined.
     * 
     * @param itemId
     *            An itemId that exists in the container.
     * @return true if the itemId passes all filters or no filters are set,
     *         false otherwise.
     */
    protected boolean passesFilters(Object itemId) {
        ITEMCLASS item = getUnfilteredItem(itemId);
        if (getFilters().isEmpty()) {
            return true;
        }
        final Iterator<ItemFilter> i = getFilters().iterator();
        while (i.hasNext()) {
            final ItemFilter f = i.next();
            if (!f.passesFilter(item)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add a container filter and re-filter the view
     * 
     * This can be used to implement
     * {@link Filterable#addContainerFilter(Object, String, boolean, boolean)}.
     */
    protected void addFilter(ItemFilter filter) {
        getFilters().add(filter);
        filterAll();
    }

    /**
     * Remove all container filters for all properties and re-filter the view.
     * 
     * This can be used to implement
     * {@link Filterable#removeAllContainerFilters()}.
     */
    protected void removeAllFilters() {
        if (getFilters().isEmpty()) {
            return;
        }
        getFilters().clear();
        filterAll();
    }

    /**
     * Checks if there is a filter that applies to a given property.
     * 
     * @param propertyId
     * @return true if there is an active filter for the property
     */
    protected boolean isPropertyFiltered(Object propertyId) {
        if (getFilters().isEmpty() || propertyId == null) {
            return false;
        }
        final Iterator<ItemFilter> i = getFilters().iterator();
        while (i.hasNext()) {
            final ItemFilter f = i.next();
            if (f.appliesToProperty(propertyId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove all container filters for a given property identifier and
     * re-filter the view. This also removes filters applying to multiple
     * properties including the one identified by propertyId.
     * 
     * This can be used to implement
     * {@link Filterable#removeContainerFilters(Object)}.
     * 
     * @param propertyId
     * @return Collection<Filter> removed filters
     */
    protected Collection<ItemFilter> removeFilters(Object propertyId) {
        if (getFilters().isEmpty() || propertyId == null) {
            return Collections.emptyList();
        }
        List<ItemFilter> removedFilters = new LinkedList<ItemFilter>();
        for (Iterator<ItemFilter> iterator = getFilters().iterator(); iterator
                .hasNext();) {
            ItemFilter f = iterator.next();
            if (f.appliesToProperty(propertyId)) {
                removedFilters.add(f);
                iterator.remove();
            }
        }
        if (!removedFilters.isEmpty()) {
            filterAll();
            return removedFilters;
        }
        return Collections.emptyList();
    }

    // sorting

    /**
     * Returns the ItemSorter used for comparing items in a sort. See
     * {@link #setItemSorter(ItemSorter)} for more information.
     * 
     * @return The ItemSorter used for comparing two items in a sort.
     */
    protected ItemSorter getItemSorter() {
        return itemSorter;
    }

    /**
     * Sets the ItemSorter used for comparing items in a sort. The
     * {@link ItemSorter#compare(Object, Object)} method is called with item ids
     * to perform the sorting. A default ItemSorter is used if this is not
     * explicitly set.
     * 
     * @param itemSorter
     *            The ItemSorter used for comparing two items in a sort (not
     *            null).
     */
    protected void setItemSorter(ItemSorter itemSorter) {
        this.itemSorter = itemSorter;
    }

    /**
     * Sort base implementation to be used to implement {@link Sortable}.
     * 
     * Subclasses should override this with a public
     * {@link #sort(Object[], boolean[])} method calling this superclass method
     * when implementing Sortable.
     * 
     * @see com.vaadin.data.Container.Sortable#sort(java.lang.Object[],
     *      boolean[])
     */
    protected void sort(Object[] propertyId, boolean[] ascending) {
        if (!(this instanceof Sortable)) {
            throw new UnsupportedOperationException(
                    "Cannot sort a Container that does not implement Sortable");
        }

        // Set up the item sorter for the sort operation
        getItemSorter().setSortProperties((Sortable) this, propertyId,
                ascending);

        // Perform the actual sort
        doSort();

        // Post sort updates
        if (getFilteredItemIds() != null) {
            filterAll();
        } else {
            fireItemSetChange();
        }

    }

    /**
     * Perform the sorting of the data structures in the container. This is
     * invoked when the <code>itemSorter</code> has been prepared for the sort
     * operation. Typically this method calls
     * <code>Collections.sort(aCollection, getItemSorter())</code> on all arrays
     * (containing item ids) that need to be sorted.
     * 
     */
    protected void doSort() {
        Collections.sort(getAllItemIds(), getItemSorter());
    }

    /**
     * Returns the sortable property identifiers for the container. Can be used
     * to implement {@link Sortable#getSortableContainerPropertyIds()}.
     */
    protected Collection<?> getSortablePropertyIds() {
        LinkedList<Object> sortables = new LinkedList<Object>();
        for (Object propertyId : getContainerPropertyIds()) {
            Class<?> propertyType = getType(propertyId);
            if (Comparable.class.isAssignableFrom(propertyType)
                    || propertyType.isPrimitive()) {
                sortables.add(propertyId);
            }
        }
        return sortables;
    }

    // removing items

    /**
     * Removes all items from the internal data structures of this class. This
     * can be used to implement {@link #removeAllItems()} in subclasses.
     * 
     * No notification is sent, the caller has to fire a suitable item set
     * change notification.
     */
    protected void internalRemoveAllItems() {
        // Removes all Items
        getAllItemIds().clear();
        if (getFilteredItemIds() != null) {
            getFilteredItemIds().clear();
        }
    }

    /**
     * Removes a single item from the internal data structures of this class.
     * This can be used to implement {@link #removeItem(Object)} in subclasses.
     * 
     * No notification is sent, the caller has to fire a suitable item set
     * change notification.
     * 
     * @param itemId
     *            the identifier of the item to remove
     * @return true if an item was successfully removed, false if failed to
     *         remove or no such item
     */
    protected boolean internalRemoveItem(Object itemId) {
        if (itemId == null) {
            return false;
        }

        boolean result = getAllItemIds().remove(itemId);
        if (result && getFilteredItemIds() != null) {
            getFilteredItemIds().remove(itemId);
        }

        return result;
    }

    // adding items

    /**
     * Adds the bean to all internal data structures at the given position.
     * Fails if an item with itemId is already in the container. Returns a the
     * item if it was added successfully, null otherwise.
     * 
     * <p>
     * Caller should initiate filtering after calling this method.
     * </p>
     * 
     * For internal use only - subclasses should use
     * {@link #internalAddItemAtEnd(Object, Item, boolean)},
     * {@link #internalAddItemAt(int, Object, Item)} and
     * {@link #internalAddItemAfter(Object, Object, Item)} instead.
     * 
     * @param position
     *            The position at which the item should be inserted in the
     *            unfiltered collection of items
     * @param itemId
     *            The item identifier for the item to insert
     * @param item
     *            The item to insert
     * 
     * @return ITEMCLASS if the item was added successfully, null otherwise
     */
    private ITEMCLASS internalAddAt(int position, ITEMIDTYPE itemId,
            ITEMCLASS item) {
        if (position < 0 || position > getAllItemIds().size() || itemId == null
                || item == null) {
            return null;
        }
        // Make sure that the item has not been added previously
        if (getAllItemIds().contains(itemId)) {
            return null;
        }

        // "filteredList" will be updated in filterAll() which should be invoked
        // by the caller after calling this method.
        getAllItemIds().add(position, itemId);
        registerNewItem(position, itemId, item);

        return item;
    }

    /**
     * Add an item at the end of the container, and perform filtering if
     * necessary. An event is fired if the filtered view changes.
     * 
     * The new item is added at the beginning if previousItemId is null.
     * 
     * @param newItemId
     * @param item
     *            new item to add
     * @param filter
     *            true to perform filtering and send event after adding the
     *            item, false to skip them
     * @return item added or null if no item was added
     */
    protected ITEMCLASS internalAddItemAtEnd(ITEMIDTYPE newItemId,
            ITEMCLASS item, boolean filter) {
        ITEMCLASS newItem = internalAddAt(getAllItemIds().size(), newItemId,
                item);
        if (newItem != null && filter) {
            // TODO filter only this item, use fireItemAdded()
            filterAll();
            if (getFilteredItemIds() == null) {
                // TODO hack: does not detect change in filterAll() in this case
                fireItemAdded(indexOfId(newItemId), newItemId, item);
            }
        }
        return newItem;
    }

    /**
     * Add an item after a given (visible) item, and perform filtering. An event
     * is fired if the filtered view changes.
     * 
     * The new item is added at the beginning if previousItemId is null.
     * 
     * @param previousItemId
     *            item id of a visible item after which to add the new item, or
     *            null to add at the beginning
     * @param newItemId
     * @param item
     *            new item to add
     * @return item added or null if no item was added
     */
    protected ITEMCLASS internalAddItemAfter(ITEMIDTYPE previousItemId,
            ITEMIDTYPE newItemId, ITEMCLASS item) {
        // only add if the previous item is visible
        ITEMCLASS newItem = null;
        if (previousItemId == null) {
            newItem = internalAddAt(0, newItemId, item);
        } else if (containsId(previousItemId)) {
            newItem = internalAddAt(
                    getAllItemIds().indexOf(previousItemId) + 1, newItemId,
                    item);
        }
        if (newItem != null) {
            // TODO filter only this item, use fireItemAdded()
            filterAll();
            if (getFilteredItemIds() == null) {
                // TODO hack: does not detect change in filterAll() in this case
                fireItemAdded(indexOfId(newItemId), newItemId, item);
            }
        }
        return newItem;
    }

    /**
     * Add an item at a given (visible) item index, and perform filtering. An
     * event is fired if the filtered view changes.
     * 
     * @param index
     *            position where to add the item (visible/view index)
     * @param newItemId
     * @return item added or null if no item was added
     * @return
     */
    protected ITEMCLASS internalAddItemAt(int index, ITEMIDTYPE newItemId,
            ITEMCLASS item) {
        if (index < 0 || index > size()) {
            return null;
        } else if (index == 0) {
            // add before any item, visible or not
            return internalAddItemAfter(null, newItemId, item);
        } else {
            // if index==size(), adds immediately after last visible item
            return internalAddItemAfter(getIdByIndex(index - 1), newItemId,
                    item);
        }
    }

    /**
     * Registers a new item as having been added to the container. This can
     * involve storing the item or any relevant information about it in internal
     * container-specific collections if necessary, as well as registering
     * listeners etc.
     * 
     * The full identifier list in {@link AbstractInMemoryContainer} has already
     * been updated to reflect the new item when this method is called.
     * 
     * @param position
     * @param itemId
     * @param item
     */
    protected void registerNewItem(int position, ITEMIDTYPE itemId,
            ITEMCLASS item) {
    }

    // item set change notifications

    /**
     * Notify item set change listeners that an item has been added to the
     * container.
     * 
     * Unless subclasses specify otherwise, the default notification indicates a
     * full refresh.
     * 
     * @param postion
     *            position of the added item in the view (if visible)
     * @param itemId
     *            id of the added item
     * @param item
     *            the added item
     */
    protected void fireItemAdded(int position, ITEMIDTYPE itemId, ITEMCLASS item) {
        fireItemSetChange();
    }

    /**
     * Notify item set change listeners that an item has been removed from the
     * container.
     * 
     * Unless subclasses specify otherwise, the default notification indicates a
     * full refresh.
     * 
     * @param postion
     *            position of the removed item in the view prior to removal (if
     *            was visible)
     * @param itemId
     *            id of the removed item, of type {@link Object} to satisfy
     *            {@link Container#removeItem(Object)} API
     */
    protected void fireItemRemoved(int position, Object itemId) {
        fireItemSetChange();
    }

    // visible and filtered item identifier lists

    /**
     * Returns the internal list of visible item identifiers after filtering.
     * 
     * For internal use only.
     */
    protected List<ITEMIDTYPE> getVisibleItemIds() {
        if (getFilteredItemIds() != null) {
            return getFilteredItemIds();
        } else {
            return getAllItemIds();
        }
    }

    /**
     * TODO Temporary internal helper method to set the internal list of
     * filtered item identifiers.
     * 
     * @param filteredItemIds
     */
    protected void setFilteredItemIds(List<ITEMIDTYPE> filteredItemIds) {
        this.filteredItemIds = filteredItemIds;
    }

    /**
     * TODO Temporary internal helper method to get the internal list of
     * filtered item identifiers.
     * 
     * @return List<ITEMIDTYPE>
     */
    protected List<ITEMIDTYPE> getFilteredItemIds() {
        return filteredItemIds;
    }

    /**
     * TODO Temporary internal helper method to set the internal list of all
     * item identifiers. Should not be used outside this class except for
     * implementing clone(), may disappear from future versions.
     * 
     * @param allItemIds
     */
    protected void setAllItemIds(List<ITEMIDTYPE> allItemIds) {
        this.allItemIds = allItemIds;
    }

    /**
     * TODO Temporary internal helper method to get the internal list of all
     * item identifiers. Should not be used outside this class without
     * exceptional justification, may disappear in future versions.
     * 
     * @return List<ITEMIDTYPE>
     */
    protected List<ITEMIDTYPE> getAllItemIds() {
        return allItemIds;
    }

    /**
     * TODO Temporary internal helper method to set the internal list of
     * filters.
     * 
     * @param filters
     */
    protected void setFilters(Set<ItemFilter> filters) {
        this.filters = filters;
    }

    /**
     * TODO Temporary internal helper method to get the internal list of
     * filters.
     * 
     * @return Set<ItemFilter>
     */
    protected Set<ItemFilter> getFilters() {
        return filters;
    }

}