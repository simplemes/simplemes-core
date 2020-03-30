package org.simplemes.mes.system.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.mes.system.ScanRequest
import org.simplemes.mes.system.ScanRequestInterface
import org.simplemes.mes.system.service.ScanService

import javax.annotation.Nullable
import javax.inject.Inject
import java.security.Principal

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The controller for the core MES scan dashboard activities.  Handles the basic barcode scan action.
 *
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs. </li>
 * </ul>
 */
@Slf4j
@Secured('OPERATOR')
@Controller("/scan")
class ScanController extends BaseController {

  /**
   * The actual scan service needed to process requests.
   */
  @Inject
  ScanService scanService

  /**
   * Handle scan requests from the client (HTTP POST endpoint = '/scan').
   * <p>
   * <b>Body (JSON)</b>: {@link org.simplemes.mes.system.ScanRequest}
   * <p>
   * <b>Response</b>: JSON list {@link org.simplemes.mes.system.ScanResponseInterface}
   */
  @Post("/scan")
  HttpResponse scan(@Body String body, @Nullable Principal principal) {
    ScanRequestInterface scanRequest = Holders.objectMapper.readValue(body, ScanRequest) as ScanRequestInterface
    def res = scanService.scan(scanRequest)
    return HttpResponse.ok(Holders.objectMapper.writeValueAsString(res))
  }

  /**
   * Displays the core scan activity page.
   * @param request The request.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Get("/scanActivity")
  @Produces(MediaType.TEXT_HTML)
  StandardModelAndView scanActivity(HttpRequest request, @Nullable Principal principal) {
    return new StandardModelAndView("system/scan", principal, this)
  }
/*
  HttpResponse scanActivity(HttpRequest request, @Nullable Principal principal) {
    def src = """
<script>

_A.provideParameters = function() {
  var order= document.getElementById("order").value;
  //console.log(wc+order);
  return {
    order: order
  }
}
_A.handleEvent = function(event) {
  if (event.type == 'ORDER_LSN_CHANGED') {
  }
}



_A.display = {
  view: 'form', id: 'scanArea', type: 'clean', margin: 0,paddingX: 10,
  rows: [
    {height: 10}
    ,{margin: 8,view: "label", template: "&nbsp;"},
    {type: "clean", width: tk.pw("10%"),  
      template: "    <div id=\\"scan\\">      <span class=\\"scanHeader\\">Scan Order/LSN&nbsp;</span><span id=\\"scanText\\"></span>      <div class=\\"orderBlock\\">"+
        "        <span id=\\"order\\" class=\\"orderDisplay\\">M1001</span>"+
        "          <button type=\\"button\\" id=\\"undoButton\\" class=\\"undo-button-disabled\\" onclick=\\"dashboard.undoAction();\\""+
        "                  title=\\"Undo Last Action\\" ></button>"+
        "      <br>"+
        "        <span id=\\"orderStatus\\" class=\\"orderStatusDisplay\\">In Work...</span>"+
        "      </div>"+
        "      <div id=\\"buttonLayout\\" style=\\"text-align: center\\">"+
        "        <div id=\\"DashboardButtons\\" style=\\"padding: 8px 10px 4px\\">"+
        "        </div>"+
        "      </div>"+
        "    </div>"}
    ,{view: "form", id: "ButtonsA", type: "clean", borderless: true,  elements: [{view: "template", id: "ButtonsContentA" , template: "-"}]}
    ,{height: 10}
  ]
};




<script>
eframe._addPreloadedMessages([
  {"inWork.status.message": "inWork.status.message"}
  ,      {"inQueue.status.message": "inQueue.status.message"}
  ,      {"scanDashboard.couldNotFind.message": "Could not find {0}."}
  ,      {"_decimal_": "."}    ]);
</script>

</script>
"""

    return HttpResponse.ok(src)
  }

*/


}
