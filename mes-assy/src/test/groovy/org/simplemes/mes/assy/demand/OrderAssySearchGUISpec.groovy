package org.simplemes.mes.assy.demand

import org.apache.http.HttpHost
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.search.SearchEngineClient
import org.simplemes.eframe.search.SearchEnginePoolExecutor
import org.simplemes.eframe.search.SearchHelper
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.assy.search.page.SearchIndexPage
import org.simplemes.mes.assy.test.AssyUnitTestUtils
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.tracking.domain.ActionLog
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for the assembly search capabilities in a live server.
 */
@IgnoreIf({ !sys['geb.env'] })
class OrderAssySearchGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, OrderAssembledComponent, Order, ProductComponent, Product, FlexType]

  /**
   * Waits for up to 5 seconds for the given search string to return a hit.
   * This is used to let the search engine process any request we just sent.
   * @param searchString The query string to wait for a hit on.
   */
  def waitForSearchHit(String searchString) {
    for (i in 1..20) {
      if (SearchHelper.instance.globalSearch(searchString).hits.size() > 0) {
        break
      }
      standardGUISleep()
      //println "waitForSearchHit $i"
    }
  }


  @SuppressWarnings(["GroovyAssignabilityCheck", 'UnnecessaryGetter'])
  @IgnoreIf({ !isSearchServerUp() })
  def "verify that search engine works end-to-end with assembly data fields"() {
    given: 'the live server is used'
    SearchHelper.instance.searchEngineClient = new SearchEngineClient(hosts: [new HttpHost('localhost', 9200)])
    SearchEnginePoolExecutor.startPool()

    and: 'an order has some components assembled'
    def uniqueName = "TestName${System.currentTimeMillis()}"
    def flexType = DataGenerator.buildFlexType(fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'], assemblyDataType: flexType)
    AssyUnitTestUtils.assembleComponent(order, [bomSequence     : 20, sequence: 20, qty: 0.1,
                                                assemblyDataType: flexType, assemblyDataValues: [LOT: "ACME1-$uniqueName"]])
    AssyUnitTestUtils.assembleComponent(order, [bomSequence     : 20, sequence: 21, qty: 0.13,
                                                assemblyDataType: flexType, assemblyDataValues: [LOT: "ACME2-$uniqueName"]])

    when: 'the search page is displayed and a search is started'
    login()
    to SearchIndexPage
    searchField.value("assy.LOT:*$uniqueName*")

    and: 'the search engine has finished indexing the unique object.'
    waitForSearchHit(uniqueName)
    searchButton.click()
    waitFor {
      searchResultsHeader.displayed
    }

    then: 'the results are displayed'
    searchResults[0].find('a').text().contains(TypeUtils.toShortString(order))

    cleanup:
    SearchHelper.instance.searchEngineClient = new SearchEngineClient()
    SearchEnginePoolExecutor.shutdownPool()
  }


}
