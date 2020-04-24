package org.simplemes.mes.assy.demand.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.web.task.TaskMenuItem
import org.simplemes.mes.assy.demand.AddOrderAssembledComponentRequest
import org.simplemes.mes.assy.demand.ComponentRemoveUndoAction
import org.simplemes.mes.assy.demand.ComponentRemoveUndoRequest
import org.simplemes.mes.assy.demand.ComponentReportDetail
import org.simplemes.mes.assy.demand.FindComponentAssemblyStateRequest
import org.simplemes.mes.assy.demand.OrderComponentStateEnum
import org.simplemes.mes.assy.demand.RemoveOrderAssembledComponentRequest
import org.simplemes.mes.assy.demand.service.OrderAssyService
import org.simplemes.mes.demand.DemandObject
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product

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
    log.debug('request() {}', request)
    def response = orderAssyService.addComponent(request)
    def res = mapper.writeValueAsString(response)
    return HttpResponse.ok(res)
  }


  /**
   * Gives the assembly state of a given order/LSN.
   */
  def assemblyScanActivity() {
    log.debug('assemblyScanActivity: params = {}', params)
  }

  /**
   * Provides the assemble component dialog for a single component with Flex Type.
   *
   * <h3>HTTP Parameters</h3>
   * The supported parameters are:
   * <ul>
   *   <li><b>order</b> - The order to add the components for (<b>order or lsn is required</b>). </li>
   *   <li><b>lsn</b> - The lsn to add the components for (<b>order or lsn is required</b>). </li>
   *   <li><b>component</b> - The component to assembled (<b>required</b>). </li>
   *   <li><b>flexType</b> - The FlexType used to define the data fields to collect (<b>Optional</b>). </li>
   * </ul>
   */
  def assembleComponentDialog() {
    log.debug('assembleComponentDialog: params = {}', params)
    ArgumentUtils.checkMissing(params.assemblyData, 'params.assemblyData')
    ArgumentUtils.checkMissing(params.component, 'params.component')
    ArgumentUtils.checkMissing(params.order, 'params.order')

    def flexType = FlexType.findByFlexType(params.assemblyData as String)
    def component = Product.findByProduct(params.component as String)
    def order = Order.findByOrder(params.order as String)
    def orderBOMComponent = determineOrderBOMComponent(params, order)
    def addComponentRequest = new AddOrderAssembledComponentRequest(order: order,
                                                                    component: component,
                                                                    assemblyData: flexType,
                                                                    orderBOMComponent: orderBOMComponent)
    respond addComponentRequest
    return addComponentRequest  // Return for easy Unit Testing
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
   * This method removes a single component from the order.  This marks the component as removed in the database.
   * This exposes the OrderAssyService method of the same name.
   * The argument is parsed from the request input (XML or JSON).
   *
   * <h3>HTTP Parameters</h3>
   * The supported parameters are:
   * <ul>
   *   <li><b>order</b> - The order to remove the components from (<b>required</b>). </li>
   *   <li><b>sequence</b> - The sequence of the record to remove (<b>required</b>). </li>
   * </ul>
   *
   * The response includes these two top-level elements in a map:
   * <ul>
   *   <li><b>orderAssembledComponent</b> - The OrderAssembledComponent that was removed for this request. </li>
   *   <li><b>undoActions</b> - The list of undo actions needed to reverse this removal. </li>
   * </ul>
   */
  @SuppressWarnings(['GroovyAssignabilityCheck', "GrReassignedInClosureLocalVar"])
  def removeComponent() {
    def removeComponentRequest = null
    request.withFormat {
      form {
        def order = Order.findByOrder(params.order)
        if (!order) {
          //error.110.message=Could not find {0} {1}
          throw new BusinessException(110, [GlobalUtils.lookup('order.label'), params.order])
        }
        def sequence = 0
        if (params.sequence) {
          sequence = Integer.valueOf(params.sequence)
        }
        removeComponentRequest = new RemoveOrderAssembledComponentRequest(order: order, sequence: sequence)
      }
    }
    if (!removeComponentRequest) {
      removeComponentRequest = ControllerUtils.parseContent(this, [removeOrderAssembledComponentRequest: RemoveOrderAssembledComponentRequest])
    }

    def orderAssembledComponent = orderAssyService.removeComponent(removeComponentRequest)

    // reversedAssemble.message=Removed component {0} from {1}.
    def infoMsg = GlobalUtils.lookup('reversedAssemble.message', orderAssembledComponent.component, removeComponentRequest.order)

    def res = [orderAssembledComponent: orderAssembledComponent,
               infoMsg                : infoMsg,
               undoActions            : [new ComponentRemoveUndoAction(orderAssembledComponent, removeComponentRequest.order)]]

    log.debug('removeComponentRequest: {}, {}', res, params)
    //println "addComponentRequest = $addComponentRequest"
    ControllerUtils.formatResponse(this, res)
  }

  /**
   * This method restores a removed component record for the order.
   * This exposes the OrderAssyService method of the same name.
   * The argument is parsed from the request input (XML or JSON).
   *
   * The response is the OrderAssembledComponent that was removed for this request.
   */
  @SuppressWarnings(['GroovyAssignabilityCheck', "GrReassignedInClosureLocalVar"])
  def undoComponentRemove() {
    def componentRemoveUndoRequest = null
    request.withFormat {
      form {
        def order = Order.findByOrder(params.order)
        if (!order) {
          //error.110.message=Could not find {0} {1}
          throw new BusinessException(110, [GlobalUtils.lookup('order.label'), params.order])
        }
        def sequence = 0
        if (params.sequence) {
          sequence = Integer.valueOf(params.sequence)
        }
        componentRemoveUndoRequest = new ComponentRemoveUndoRequest(order: order, sequence: sequence)
      }
    }
    if (!componentRemoveUndoRequest) {
      componentRemoveUndoRequest = ControllerUtils.parseContent(this, [componentRemoveUndoRequest: ComponentRemoveUndoRequest])
    }

    def orderAssembledComponent = orderAssyService.undoComponentRemove(componentRemoveUndoRequest)

    log.debug('removeComponentRequest: {}, {}', orderAssembledComponent, params)
    //println "addComponentRequest = $addComponentRequest"
    ControllerUtils.formatResponse(this, orderAssembledComponent)
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
      if (!demand) {
        // If no order/LSN is used, the don't flag as fully assembled.
        fullyAssembled = false
      } else {
        // Make sure there are components for the order
        order = order ?: demand?.order
        if (!order.components) {
          // No BOM components, so don't flag as fully assembled.
          fullyAssembled = false
        }
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

    def map = [totalAvailable: listSize, fullyAssembled: fullyAssembled, list: list]

    ControllerUtils.formatResponse(this, map)
  }

  /**
   * Returns the assembly state of a given order/LSN.
   * Returns a standard JSON list of OrderComponentState POGOs.  This includes the normal totalAvailable value.
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
   * <p>
   * <b>Response</b>: List<OrderComponentState> {@link org.simplemes.mes.assy.demand.OrderComponentState} (JSON format)
   *
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
    def s = Holders.objectMapper.writeValueAsString(totalAvailable: listSize, fullyAssembled: fullyAssembled, list: list)

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
    def res = [totalAvailable: 0, list: []]
    if (params.query) {
      def searchResults = searchService.globalSearch(params.query, params)
      res.totalAvailable = searchResults.totalHits
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
