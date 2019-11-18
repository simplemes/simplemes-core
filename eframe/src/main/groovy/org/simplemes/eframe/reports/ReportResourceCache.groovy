package org.simplemes.eframe.reports

import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * This provides a mechanism to hold the report engine images and other resources for later display within the browser.
 * This is done by using a global cache to hold the resources by report name and image name.
 */
class ReportResourceCache {

  /**
   * A static instance for this cache handler.
   */
  static ReportResourceCache instance = new ReportResourceCache()

  /**
   * The cache (of soft references) used for these resources.
   */
  static cache = new ConcurrentHashMap()

  /**
   * Puts a resource in the cache, with a soft reference.
   * @param name The name.
   * @param value The value.
   */
  void putResource(String name, Object value) {
    cache.put(name, new SoftReference(value))
  }

  /**
   * Retrieves a given resource from the cache.
   * @param name The name of the resource.
   * @return The resource.
   */
  Object getResource(String name) {
    return cache.get(name)?.get()
  }
}
