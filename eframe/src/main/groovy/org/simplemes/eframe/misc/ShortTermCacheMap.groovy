package org.simplemes.eframe.misc

import groovy.util.logging.Slf4j

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This is a map that will be used to temporarily cache some data for a very short time.  Typically <10 seconds.
 * This is used to act as a level 1 cache for expensive logic that will not change in a very short time.
 * <p>
 * One typical use is to define the field definitions for a domain class.  These are usually short in the
 * domain with ExtensibleFields (in the complex field map).  This avoids expensive checks for field extensions
 * and configurable type fields.
 * <p>
 * If a cache element has exceeded its lifetime, then the get() method will return null.
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>trace</b> - Logs cache hits/misses.</li>
 * </ul>
 *
 * <b>Note:</b> This method does not support concurrent access.  This should only be used in
 *              short-lived areas.
 */
@Slf4j
class ShortTermCacheMap extends HashMap {

  /**
   * The lifetime of elements in the cache (in ms).  Default: 1000ms.
   */
  Long lifeTime

  /**
   * The time the given element was most recently added.
   */
  protected Map<Object, Long> addedTime = [:]

  /**
   * Basic constructor.
   * @param lifeTime The lifetime of the elements in the cache (in ms).
   */
  ShortTermCacheMap(Long lifeTime = 1000) {
    this.lifeTime = lifeTime ?: 1000
  }

  /**
   * Returns the value to which the specified key is mapped,
   * or {@code null} if this map contains no mapping for the key.
   *
   */
  @Override
  Object get(Object key) {
    def res = super.get(key)
    def timeAdded = addedTime[key] ?: 0
    def age = System.currentTimeMillis() - timeAdded
    if (res != null && age <= lifeTime) {
      // The value exists and was added within the lifetime allowed.
      log.trace('hit: {} age = {} res = {}', key, age, res)
      return res
    }
    log.trace('miss: {}', key)
    return null
  }

  /**
   * Associates the specified value with the specified key in this map.
   * If the map previously contained a mapping for the key, the old
   * value is replaced.
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with <tt>key</tt>, or
   *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
   *         (A <tt>null</tt> return can also indicate that the map
   *         previously associated <tt>null</tt> with <tt>key</tt>.)
   */
  @Override
  Object put(Object key, Object value) {
    def millis = System.currentTimeMillis()
    addedTime[key] = millis
    log.trace('put: {} time = {} res = {}', key, millis, value)
    return super.put(key, value)
  }

}
