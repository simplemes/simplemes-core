package org.simplemes.mes.assy.demand.controller

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
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.web.task.TaskMenuItem
import org.simplemes.mes.assy.demand.AddOrderAssembledComponentRequest
import org.simplemes.mes.assy.demand.ComponentRemoveUndoAction
import org.simplemes.mes.assy.demand.ComponentRemoveUndoRequest
import org.simplemes.mes.assy.demand.ComponentReportDetail
import org.simplemes.mes.assy.demand.FindComponentAssemblyStateRequest
import org.simplemes.mes.assy.demand.OrderComponentStateEnum
import org.simplemes.mes.assy.demand.RemoveOrderAssembledComponentRequest
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.demand.service.OrderAssyService
import org.simplemes.mes.demand.DemandObject
import org.simplemes.mes.demand.domain.Order

import javax.annotation.Nullable
import javax.inject.Inject
import java.security.Principal

/**
 * The OrderAssy controller.  Provides basic access to the OrderAssyService.
 * Does NOT provide CRUD access to any domain class.
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs. </li>
 * </ul>

 */
@Slf4j
@Secured('OPERATOR')
@Controller("/orderAssy")
class OrderAssyController extends BaseController {


  //def searchService

  /**
   * Defines the standard end-user task entry points that this controller handles.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  def taskMenuItems = [new TaskMenuItem(folder: 'assembly:1000', name: 'assemblyReport', uri: '/orderAssy/assemblyReport', displayOrder: 1010),
                       new TaskMenuItem(folder: 'assembly:1000', name: 'componentReport', uri: '/orderAssy/componentReport', displayOrder: 1020)]

  /**
   * The actual order assy service needed to process requests.
   */
  @Inject
  OrderAssyService orderAssyService

  /**
   * This method adds a component to the order/LSN.
   * This exposes the OrderAssyService method of the same name.
   * The argument is parsed from the request input body (JSON).
   * <p>
   * <b>Input</b>: {@link AddOrderAssembledComponentRequest} (JSON format).
   * <p>
   * <b>Response</b>: {@link org.simplemes.mes.assy.demand.domain.OrderAssembledComponent} (JSON format)
   *
   */
  @Post('/addComponent')
  @SuppressWarnings("unused")
  HttpResponse addComponent(@Body String body, @Nullable Principal principal) {
    def mapper = Holders.objectMapper
    AddOrderAssembledComponentRequest request = mapper.readValue(body, AddOrderAssembledComponentRequest)
    log.debug('addComponent() body={}, addRequest={} ', body, request)
    def response = orderAssyService.addComponent(request)
    def res = mapper.writeValueAsString(response)
    return HttpResponse.ok(res)
  }


  /**
   * Displays the core assembly activity page.
   * @param request The request.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @SuppressWarnings("unused")
  @Produces(MediaType.TEXT_HTML)
  @Get("/assemblyActivity")
  StandardModelAndView assemblyActivity(HttpRequest request, @Nullable Principal principal) {
    def view = "assy/demand/assembly"
    def modelAndView = new StandardModelAndView(view, principal, this)
    //def params = ControllerUtils.instance.convertToMap(request.parameters)

    // No model is used here.
    return modelAndView
  }

  /**
   * Displays the assemble component dialog for a single component with Flex Type.
   *
   * <h3>HTTP Parameters</h3>
   * The supported parameters are:
   * <ul>
   *   <li><b>order</b> - The order to add the components for (<b>order or lsn is required</b>). </li>
   *   <li><b>lsn</b> - The lsn to add the components for (<b>order or lsn is required</b>). </li>
   *   <li><b>component</b> - The component to assembled (<b>required</b>). </li>
   *   <li><b>bomSequence</b> - The BOM Sequence for the component to assemble (<b>Optional</b>). </li>
   *   <li><b>assemblyData</b> - The FlexType used to define the data fields to collect (<b>Optional</b>). </li>
   * </ul>
   * @param request The request.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @SuppressWarnings("unused")
  @Produces(MediaType.TEXT_HTML)
  @Get("/assembleComponentDialog")
  StandardModelAndView assembleComponentDialog(HttpRequest request, @Nullable Principal principal) {
    def view = "assy/demand/assembleComponentDialog"
    def modelAndView = new StandardModelAndView(view, principal, this)
    def params = ControllerUtils.instance.convertToMap(request.parameters)

    modelAndView['componentModel'] = resolveComponent(params)

    return modelAndView
  }

  /**
   * Resolves the assembleComponentDialog() HTTP parameters into a prototype OrderAssembledComponent object
   * that is used for the dialog field's model.
   * @param params The HTTP params.
   * @return The OrderAssembledComponent prototype for the planned component.
   */
  OrderAssembledComponent resolveComponent(Map params) {
    def res = new OrderAssembledComponent()
    ArgumentUtils.checkMissing(params.order, 'order')
    def order = Order.findByOrder(params.order as String)

    // Build an add request so the service can fill in some details.
    def bomSequence = ArgumentUtils.convertToInteger(params.bomSequence)
    def component = null
    if (!bomSequence && params.component) {
      def bomComponent = order.components.find { it.component.product == params.component }
      component = bomComponent?.component
    }
    def addRequest = new AddOrderAssembledComponentRequest(order: order, bomSequence: bomSequence,
                                                           component: component)
    if (addRequest.order && (addRequest.bomSequence || addRequest.component)) {
      orderAssyService.resolveComponentForRequest(addRequest)
    }

    component = addRequest.component
    if (component) {
      res.component = component
      res.qty = addRequest.qty
      res.assemblyData = component.assemblyDataType
    } else {
      def s = params.bomSequence ? "(sequence $params.bomSequence)" : params.component
      //error.10000.message=Could not find component {1} for order {0}.
      throw new BusinessException(10000, [params.order, s])
    }

    return res
  }


  /**
   * Provides the removal single component dialog for a removing a single component.
   *
   * <h3>HTTP Parameters</h3>
   * The supported parameters are:
   * <ul>
   *   <li><b>order</b> - The order to add the components for (<b>order or lsn is required</b>). </li>
   *   <li><b>lsn</b> - The lsn to add the components for (<b>order or lsn is required</b>). </li>
   *   <li><b>sequences</b> - The list of OrderAssembledComponent sequence value(s) to display for possible removal (<b>required</b>). </li>
   * </ul>
   */
  def removeComponentDialog() {
    log.debug('assembleComponentDialog: params = {}', params)
    //println "params = $params"
    ArgumentUtils.checkMissing(params.order, 'params.order')
  }

  /**
   * This method removes a component to the order/LSN.
   * This exposes the OrderAssyService method of the same name.
   * The argument is parsed from the request input body (JSON).
   * <p>
   * <b>Input</b>: {@link RemoveOrderAssembledComponentRequest} (JSON format).
   * <p>
   * <b>Response</b>:
   * The response includes these two top-level elements in a map (JSON):
   * <ul>
   *   <li><b>orderAssembledComponent</b> - The OrderAssembledComponent that was removed for this request.
   * {@link org.simplemes.mes.assy.demand.domain.OrderAssembledComponent}.</li>
   *   <li><b>undoActions</b> - The list of undo actions needed to reverse this removal. </li>
   * </ul>
   *
   */
  @Post('/removeComponent')
  @SuppressWarnings("unused")
  HttpResponse removeComponent(@Body String body, @Nullable Principal principal) {
    def mapper = Holders.objectMapper
    RemoveOrderAssembledComponentRequest request = mapper.readValue(body, RemoveOrderAssembledComponentRequest)
    log.debug('removeComponent() {}', request)
    def orderAssembledComponent = orderAssyService.removeComponent(request)

    // reversedAssemble.message=Removed component {0} from {1}.
    def infoMsg = GlobalUtils.lookup('reversedAssemble.message', orderAssembledComponent.component.product, request.order.order)
    def res = [orderAssembledComponent: orderAssembledComponent,
               infoMsg                : infoMsg,
               undoActions            : [new ComponentRemoveUndoAction(orderAssembledComponent, request.order)]]

    return HttpResponse.ok(Holders.objectMapper.writeValueAsString(res))
  }

  /**
   * This method restores the removed component to the order.
   * This exposes the OrderAssyService method of the same name.
   * The argument is parsed from the request input body (JSON).
   * <p>
   * <b>Input</b>: {@link RemoveOrderAssembledComponentRequest} (JSON format).
   * <p>
   * <b>Response</b>: JSON formatted {@link org.simplemes.mes.assy.demand.domain.OrderAssembledComponent}.
   *
   */
  @Post('/undoRemoveComponent')
  @SuppressWarnings("unused")
  HttpResponse undoRemoveComponent(@Body String body, @Nullable Principal principal) {
    def mapper = Holders.objectMapper
    def request = mapper.readValue(body, ComponentRemoveUndoRequest)
    log.debug('undoRemoveComponent() {}', request)
    def orderAssembledComponent = orderAssyService.undoComponentRemove(request)

    return HttpResponse.ok(Holders.objectMapper.writeValueAsString(orderAssembledComponent))
  }


  /**
   * Finds the currently assembled components for the given order/LSN.  The results correspond to a single
   * OrderAssembledComponent record, so some of the fields are not useful.
   * <p>
   *
   * <b>Note:</b> The qtyRequired, overallStateString,overallState,percentAssembled,qtyAndStateString should be ignored for these results.
   * These values are not set correctly for this detail information.
   *
   * <h3>HTTP Parameters</h3>
   * The supported parameters are:
   * <ul>
   *   <li><b>order</b> - The order to find the components for (<b>order or lsn is required</b>). </li>
   *   <li><b>lsn</b> - The lsn to find the components for (<b>order or lsn is required</b>). </li>
   *   <li><b>sequence</b> - The sequence of the OrderAssembledComponent to return. (<b>Optional, List supported</b>). </li>
   * </ul>
   */
  def findAssembledComponents() {
    log.debug('findComponentAssemblyState: params = {}', params)
    DemandObject demand = null
    def order = null
    if (params.order) {
      order = Order.findByOrder(params.order as String)
      demand = order
    }
    if (params.lsn) {
      // If both given, then use order to find the LSN in case of duplicates.
      demand = order ? LSN.findByLsnAndOrder(params.lsn as String, order) : LSN.findByLsn(params.lsn as String)
    }

    def sequenceList = []
    def sequenceString = params.sequenceString
    if (sequenceString) {
      def l = sequenceString.tokenize(' ,')
      for (s in l) {
        sequenceList << Integer.valueOf(s as String)
      }
    }

    def list = orderAssyService.findAssembledComponents(demand, sequenceList)
    def listSize = list.size()

    def fullyAssembled = !list.find() { comp ->
      comp.overallState == OrderComponentStateEnum.EMPTY || comp.overallState == OrderComponentStateEnum.PARTIAL
    }
    // If we think it might be fully assembled, make sure it really is (order given and it has components).
    if (fullyAssembled) {
      if (demand) {
        // Make sure there are components for the order
        order = order ?: demand?.order
        if (!order.components) {
          // No BOM components, so don't flag as fully assembled.
          fullyAssembled = false
        }
      } else {
        // If no order/LSN is used, the don't flag as fully assembled.
        fullyAssembled = false
      }
    }

    // Now, we have the full list, so apply the sorting and then paging options.
    list = reSortList(params, list)

    //println "list = ${list*.component} ${list*.sequence}"

    // Now, apply offset/max from the params.
    def (offset, max) = ControllerUtils.calculateOffsetAndMaxForList(params)
    if (offset && max < listSize) {
      //noinspection GroovyAssignabilityCheck
      def end = Math.min(offset + max - 1, listSize - 1)
      list = list[(offset..end)]
    }

    def map = [total_count: listSize, fullyAssembled: fullyAssembled, list: list]

    ControllerUtils.formatResponse(this, map)
  }

  /**
   * Returns the assembly state of a given order/LSN.
   * Returns a standard JSON list of OrderComponentState POGOs.  This includes the normal total_count value.
   * Also included is a boolean 'fullyAssembled' that is true if the order/lsn is fully assembled.
   *
   * <h3>HTTP Parameters</h3>
   * The supported parameters are:
   * <ul>
   *   <li><b>order</b> - The order to find the components for (<b>order or lsn is required</b>). </li>
   *   <li><b>lsn</b> - The lsn to find the components for (<b>order or lsn is required</b>). </li>
   *   <li><b>hideAssembled</b> - If 'true', then hide any completely assembled components from the results (<b>default</b>: false). </li>
   * </ul>
   *
   *
   * <h3>Response</h3>
   * A JSON formatted map with these elements:
   * <ul>
   *   <li><b>data</b> - List<OrderComponentState> {@link org.simplemes.mes.assy.demand.OrderComponentState} (JSON format) </li>
   *   <li><b>total_count</b> - The number of component records in the list. </li>
   *   <li><b>fullyAssembled</b> - 'true' if the order is fully assembled. </li>
   * </ul>

   */
  @Get('/findComponentAssemblyState')
  @SuppressWarnings("unused")
  HttpResponse findComponentAssemblyState(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    def findRequest = new FindComponentAssemblyStateRequest(params)

    def list = orderAssyService.findComponentAssemblyState(findRequest)
    def listSize = list.size()

    def fullyAssembled = !list.find() { comp ->
      comp.overallState == OrderComponentStateEnum.EMPTY || comp.overallState == OrderComponentStateEnum.PARTIAL
    }
    if (list.size() == 0) {
      // Missing order/lsn and no components found make sure the fullyAssembled is false.
      fullyAssembled = false
    }
    def s = Holders.objectMapper.writeValueAsString(total_count: listSize, fullyAssembled: fullyAssembled, data: list)

    return HttpResponse.ok(s)

  }

  // TODO: Support sorting to service when GUI is done.  Paging?
/*
  private static List<OrderComponentState> reSortList(Map params, List<OrderComponentState> list) {
    def (List sortFields, Map sortOrder) = ControllerUtils.calculateSortingForList(params)

    if (!sortFields) {
      return list
    }

    def sortField = sortFields[0]

    if (sortOrder[sortField] == 'desc') {
      list.sort() { a, b -> b[sortField] <=> a[sortField] }
    } else {
      list.sort() { a, b -> a[sortField] <=> b[sortField] }
    }

    return list
  }
*/

  /**
   * Displays the component report page.
   */
  def componentReport() {
  }

  /**
   * Does a full-text search on assembly related objects.  Uses most of the global search capabilities, but limits
   * the search to domains related to assembly (e.g. Order and Work Center setup).
   * Returns a standard JSON list of suitable for display in an ef:list tag.
   *
   * <h3>HTTP Parameters</h3>
   * The supported parameters are:
   * <ul>
   *   <li><b>query</b> - The query string (<b>Required</b>). </li>
   *   <li><b>max, offset</b> - The standard paging values  (<b>Default:</b> first page). </li>
   * </ul>
   */
  def componentReportList() {
    log.debug('componentReportList: params = {}', params)
    def res = [total_count: 0, list: []]
    if (params.query) {
      def searchResults = searchService.globalSearch(params.query, params)
      res.total_count = searchResults.totalHits
      for (hit in searchResults.hits) {
        def dtl = new ComponentReportDetail(searchHit: hit.displayValue)
        if (hit.object instanceof Order) {
          dtl._searchHitLink = "/orderAssy/assemblyReport?order=${hit.object.order}"
          dtl._searchHitText = hit.displayValue
          dtl.dateTime = hit.object.lastUpdated
        }
        res.list << dtl
      }
    }

    //render res as JSON
  }

  /**
   * Displays the assembly report for an order.  This page uses the findComponentAssemblyState() action
   * to display the state.
   */
  def assemblyReport() {

  }

}
