package org.simplemes.mes.demand.controller

import groovy.util.logging.Slf4j
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
import org.simplemes.mes.demand.CompleteRequest
import org.simplemes.mes.demand.StartRequest
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.WorkService

import javax.annotation.Nullable
import javax.inject.Inject
import java.security.Principal

/**
 * Provides access to the WorkService for starting and completing work on Orders and LSN.
 * Provides API access and client GUI access.
 * <p>
 * The {@link org.simplemes.mes.demand.service.WorkService} is part of the <b>Stable API</b>.
 *
 */
@Slf4j
@Secured('OPERATOR')
@Controller("/work")
class WorkController extends BaseController {

  /**
   * The actual work service needed to process requests.
   */
  @Inject
  WorkService workService

  /**
   * Handle start WorkService requests.  The parameters/content matches the fields of the request object
   * {@link org.simplemes.mes.demand.StartRequest}.
   * <p>
   * <b>Response</b>: JSON list {@link org.simplemes.mes.demand.StartResponse}
   */
  @Post('/start')
  HttpResponse start(@Body String body, @Nullable Principal principal) {
    def res = null
    Order.withTransaction {
      def mapper = Holders.objectMapper
      StartRequest startRequest = mapper.readValue(body, StartRequest)
      log.debug('startRequest() {}', startRequest)
      def startResponse = workService.start(startRequest)
      res = mapper.writeValueAsString(startResponse)
    }
    return HttpResponse.ok(res)
  }

  /**
   * Displays the core start activity page.
   * @param request The request.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Get("/startActivity")
  @Produces(MediaType.TEXT_HTML)
  StandardModelAndView startActivity(@Nullable Principal principal) {
    return new StandardModelAndView("demand/work/start", principal, this)
  }

  /**
   * Handle complete WorkService requests.  The parameters/content matches the fields of the request object
   * {@link org.simplemes.mes.demand.CompleteRequest}.
   */
  @Post('/complete')
  HttpResponse complete(@Body String body, @Nullable Principal principal) {
    def res = null
    Order.withTransaction {
      def mapper = Holders.objectMapper
      CompleteRequest completeRequest = mapper.readValue(body, CompleteRequest)
      log.debug('completeRequest() {}', completeRequest)
      def completeResponse = workService.complete(completeRequest)
      res = mapper.writeValueAsString(completeResponse)
    }
    return HttpResponse.ok(res)
  }

  /**
   * The complete activity for the dashboard.
   */
  def completeActivity() {
  }

  /**
   * Displays the core complete activity page.
   * @param request The request.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Get("/completeActivity")
  @Produces(MediaType.TEXT_HTML)
  StandardModelAndView completeActivity(@Nullable Principal principal) {
    return new StandardModelAndView("demand/work/complete", principal, this)
  }

  /**
   * Handle reverse start WorkService requests.  The parameters/content matches the fields of the request object
   * {@link org.simplemes.mes.demand.StartRequest}.
   */
  @Post('/reverseStart')
  HttpResponse reverseStart(@Body String body, @Nullable Principal principal) {
    def res = null
    Order.withTransaction {
      def mapper = Holders.objectMapper
      StartRequest startRequest = mapper.readValue(body, StartRequest)
      log.debug('reverseStart: {}', startRequest)
      def startResponse = workService.reverseStart(startRequest)
      res = mapper.writeValueAsString(startResponse)
    }
    return HttpResponse.ok(res)
  }

  /**
   * Displays the core reverse start activity page.
   * @param request The request.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Get("/reverseStartActivity")
  @Produces(MediaType.TEXT_HTML)
  StandardModelAndView reverseStartActivity(@Nullable Principal principal) {
    return new StandardModelAndView("demand/work/reverseStart", principal, this)
  }


}
