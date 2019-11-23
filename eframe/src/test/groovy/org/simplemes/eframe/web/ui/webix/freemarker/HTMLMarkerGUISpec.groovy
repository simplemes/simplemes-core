package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.page.HomePage
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of the efHTML marker basic ability in a GUI.
 */
@IgnoreIf({ !sys['geb.env'] })
class HTMLMarkerGUISpec extends BaseDashboardSpecification {

  def "verify that basic marker displays HTML correctly"() {
    given: 'a dashboard with a simple submit page'
    def activity = """
      <@efForm id="logFailure" dashboard=true>
        <@efField field="rma" value="RMA1001" width=20>
          <@efHTML><a href='./' id='aLink'>link</a></@efHTML>
        </@efField>
      </@efForm>
    """
    buildDashboard(defaults: [activity])

    when: 'the dashboard is displayed'
    displayDashboard()

    then: 'the link is displayed'
    $('#aLink').text() == 'link'

    when: 'the link is clicked'
    $('#aLink').click()

    then: 'the link works'
    at HomePage
  }


}
