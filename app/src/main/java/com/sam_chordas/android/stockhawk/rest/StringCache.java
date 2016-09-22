package com.sam_chordas.android.stockhawk.rest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Owen LaRosa on 9/22/16.
 */

public class StringCache {

    // maximum number of total bytes for all string values combined
    private int capacity;
    // Total size of all stored string values
    private int currentSize = 0;

    // Ordered record of when keys were added
    private ArrayDeque<String> stringQueue = new ArrayDeque<String>();
    // mapping of keys with their corresponding values
    private ConcurrentHashMap<String, String> stringHashMap = new ConcurrentHashMap<String, String>();

    /**
     * Initialize with default capacity of 1 MB
     */
    public StringCache() {
        this.capacity = 1024000; // default of 1 MB
    }

    /**
     * Initialize with custom capacity
     * @param capacity Cumulative size of values in bytes
     */
    public StringCache(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Set string in the cache
     * @param key Key of the string to set
     * @param value value of the string
     */
    public void putString(String key, String value) {
        String previousEntry = stringHashMap.get(key);
        // if the key already has an entry, consider this when adjusting the current size
        int previousSize = previousEntry != null ? numberOfBytes(previousEntry) : 0;
        stringHashMap.put(key, value);
        stringQueue.push(key);
        currentSize += (numberOfBytes(value) - previousSize);
        while (currentSize > capacity) {
            String oldKey = stringQueue.pop();
            removeString(oldKey);
        }
    }

    /**
     * Retrieve entry from the cache
     * @param key Key of the item to retrieve
     * @return String with the corresponding key
     */
    public String getString(String key) {
        return stringHashMap.get(key);
    }

    /**
     * Remove entry with the specified key if present
     * @param key Key of the string to be removed
     */
    public void removeString(String key) {
        if (key != null) {
            currentSize -= numberOfBytes(stringHashMap.get(key));
            stringHashMap.remove(key);
        }
    }

    /**
     * Reset contents of the cache
     */
    public void clear() {
        stringHashMap.clear();
        currentSize = 0;
    }

    // get the number of bytes for the string in UTF-8 encoding
    private int numberOfBytes(String input) {
        try {
            return input.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            return 0;
        }
    }

}