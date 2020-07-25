/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample

import groovy.util.logging.Slf4j
import org.simplemes.eframe.system.DemoDataLoaderInterface
import sample.domain.SampleParent

import javax.inject.Singleton

/**
 * Used to test the loading of demo data.  Loads two different tables (SampleParent and AllFieldsDomain records).
 */
@Slf4j
@Singleton
class SampleDemoDataLoader implements DemoDataLoaderInterface {
  /**
   * Loads the demo data.
   *
   * <h3>Results Map</h3>
   * The elements in the list of Maps includes:
   * <ul>
   *   <li><b>name</b> - The name of the data loaded.  Usually a Class.simpleName. </li>
   *   <li><b>count</b> - The number of records actually loaded. </li>
   *   <li><b>possible</b> - The number of records that could be potentially loaded. </li>
   *   <li><b>uri</b> - The URI to a definition page (if available) that will show the the loaded records (e.g. '/flexType'). </li>
   * </ul>
   *
   * @return A list of Maps with the elements defined above.
   */
  @Override
  List<Map<String, Object>> loadDemoData() {
    def res = []

    def possible = 1
    def count = 0
    def name = SampleParent.simpleName
    def uri = '/sampleParent'
    if (!SampleParent.findByName('SAMPLE1')) {
      def record = new SampleParent(name: 'SAMPLE1', title: "Demo Data", notes: "Demo record created by ${this.class.simpleName}").save()
      count++
      log.info("loadDemoData(): Loaded {}", record)
    }
    res << [name: name, uri: uri, count: count, possible: possible]

    return res
  }

}
