/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import sample.domain.Order
import spock.lang.IgnoreIf

/**
 * Tests for the BaseDashboardSpecification logic.
 */
@IgnoreIf({ !sys['geb.env'] })
class BaseDashboardSpec extends BaseDashboardSpecification {


  @SuppressWarnings("unused")
  static dirtyDomains = [Order]

  def "verify that the dirty domains work from the parent BaseDashboardSpecification class"() {
    given: 'some records to cleanup'
    Order.withTransaction {
      new Order(order: 'M1001').save()
    }

    and: 'a dashboard that should be cleaned up by the base-class'
    buildDashboard(defaults: ['/controller/dummy'])

    expect: 'a dummy test'
    Order.list().size() > 0
  }

  def "verify that displayDashboard supports a sub-class of DashboardPage"() {
    given: 'a dashboard with some page content'
    buildDashboard(defaults: ['<span id="ID237">Content B</span>'])

    and: 'a sub-class of the standard DashboardPage'
    def src = '''
    package sample
    
    import org.simplemes.eframe.custom.Addition
    import org.simplemes.eframe.custom.BaseAddition
    import org.simplemes.eframe.custom.AdditionConfiguration
    import org.simplemes.eframe.data.format.LongFieldFormat
    import sample.domain.SampleParent
    
    class TestDashboardPage extends org.simplemes.eframe.test.page.DashboardPage {
      static content = {
        testSpan {$('span#ID237')}
      }
    }
    '''
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the dashboard is displayed - with a a sub-class of the dashboard page'
    displayDashboard([page: clazz])

    then: 'the content defined in the sub-class page can be used'
    testSpan.text() == 'Content B'
  }

}
