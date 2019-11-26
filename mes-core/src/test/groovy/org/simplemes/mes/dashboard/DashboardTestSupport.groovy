package org.simplemes.mes.dashboard

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Support utilities and values for dashboard-related mes-core tests.
 */
class DashboardSpecSupport {

  /**
   * A simple dashboard activity that will display dashboard events (as JSON) as they are received.
   */
  public static final DISPLAY_EVENT_ACTIVITY = '''
      <@efForm id="logFailure" dashboard=true>
      <@efHTML>
        <h4"">Events</h4>
        <span id="events"></span>
      </@efHTML>
      </@efForm>
      ${params._variable}.handleEvent = function(event) { 
        document.getElementById("events").innerHTML += JSON.stringify(event)+"<br>";
      }
    '''
}
