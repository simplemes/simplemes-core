/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.javascript

import org.simplemes.eframe.test.BaseDashboardSpecification
import sample.controller.OrderController
import spock.lang.IgnoreIf

/**
 * Tests of the eframe_toolkit.js methods related to text field suggestions (mainly setSuggestURI).
 */
@IgnoreIf({ !sys['geb.env'] })
class ToolkitJSSuggestGUISpec extends BaseDashboardSpecification {

  def "verify that setSuggestURI calls server-side without duplicating the number of calls"() {
    given: 'a dashboard with an input field with suggest enabled and focus set to the field'
    def activity = """
      <@efForm id="logFailure" dashboard=true>
        <@efField field="order" id="order" label="Order/LSN" value="M1008" suggest="/order/suggestOrder?workCenter=WC1"
                  width=20 labelWidth='35%' />
      </@efForm>
      \${params._variable}.postScript = 'ef.focus("order")';
    """
    buildDashboard(defaults: [activity])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the counters are reset for this run'
    OrderController.suggestCount = 0
    OrderController.suggestLatestParameters = null

    and: 'the suggest URI is changed'
    js.exec("tk.setSuggestURI('order','/order/suggestOrder?workCenter=WC2')")

    and: 'a key is pressed to trigger the suggest'
    sendKey("x")
    waitFor {
      $('div.webix_popup', view_id: 'order_suggest').displayed
    }

    then: 'the controller was called with the right URI and only once'
    OrderController.suggestCount == 1
    OrderController.suggestLatestParameters.workCenter == 'WC2'
  }

}
