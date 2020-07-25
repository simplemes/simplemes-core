/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.system

/**
 * Defines the interface for loading demo data.  See {@link org.simplemes.eframe.system.controller.DemoDataController}
 * for access to this loading feature.
 * This is designed to be used by modules to quickly load demo data for that module's feature.
 * <p>
 * Any bean that implements this interface will be called to load the demo data.
 */
interface DemoDataLoaderInterface {

  /**
   * Loads the demo data.
   *
   * <h3>Results Map</h3>
   * The elements in the list of Maps includes:
   * <ul>
   *   <li><b>name</b> - The name of the data loaded.  Usually a Class.simpleName. </li>
   *   <li><b>count</b> - The number of records actually loaded. </li>
   *   <li><b>total</b> - The number of records that could be potentially loaded. </li>
   *   <li><b>uri</b> - The URI to a definition page (if available) that will show the the loaded records (e.g. '/flexType'). </li>
   * </ul>
   *
   * @return A list of Maps with the elements defined above.
   */
  List<Map<String, Object>> loadDemoData()
}