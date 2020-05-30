package com.github.manolo8.darkbot.core.objects.swf;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.itf.UpdatableAuto;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Represents a collection of pointers in SWF.
 * Contains an utility method for syncing the pointers with java object collections.
 */
public abstract class SwfPtrCollection extends Updatable {
    private long lastPointer;

    /**
     * @return size of the SWF collection
     */
    public abstract int getSize();

    /**
     * @param i The index to search
     * @return The pointer the index points to to
     */
    public abstract long getPtr(int i);

    public void forEachIndexed(Consumer<Long> consumer) {
        for (int i = indexOf(lastPointer) + 1; i < getSize(); i++)
            consumer.accept(lastPointer = getPtr(i));
    }

    public void forEach(Consumer<Long> consumer) {
        for (int i = 0; i < getSize(); i++)
            consumer.accept(getPtr(i));
    }

    public int indexOf(long value) {
        for (int i = getSize() - 1; i >= 0; i--)
            if (value == getPtr(i)) return i;
        return -1;
    }

    /**
     * Syncs a java list to this SWF collection.
     * @param list The java list to sync
     * @param constructor The constructor for new instances of the object
     * @param filter The filter to apply, if any objects should be ignored from the list
     * @param <T> The type the pointer is mapped to in java
     * @return The leftover items that didn't match the filter
     */
    public  <T extends UpdatableAuto> List<T> sync(List<T> list,
                                                   Supplier<T> constructor,
                                                   Predicate<T> filter) {
        int currSize = getSize(), listIdx = 0;
        List<T> ignored = new ArrayList<>();
        for (int arrIdx = 0; arrIdx < currSize; listIdx++, arrIdx++) {
            boolean newItem = list.size() <= listIdx;
            T item = newItem ? constructor.get() : list.get(listIdx);
            item.update(getPtr(arrIdx));
            if (filter != null && !filter.test(item)) {
                if (!newItem) list.remove(listIdx);
                ignored.add(item);
                listIdx--;
                continue;
            }
            if (newItem) list.add(item);
        }
        while (list.size() > listIdx)
            list.remove(list.size() - 1);
        return ignored;
    }
}
