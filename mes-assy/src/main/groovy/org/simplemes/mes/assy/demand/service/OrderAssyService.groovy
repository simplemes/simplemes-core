package org.simplemes.mes.assy.demand.service

import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.domain.FlexField
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.search.AdjustQueryInterface
import org.simplemes.eframe.search.SearchHelper
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.mes.assy.demand.AddOrderAssembledComponentRequest
import org.simplemes.mes.assy.demand.AssembledComponentStateEnum
import org.simplemes.mes.assy.demand.ComponentRemoveUndoRequest
import org.simplemes.mes.assy.demand.FindComponentAssemblyStateRequest
import org.simplemes.mes.assy.demand.OrderComponentState
import org.simplemes.mes.assy.demand.OrderComponentStateEnum
import org.simplemes.mes.assy.demand.RemoveOrderAssembledComponentRequest
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.demand.domain.OrderBOMComponent
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.OrderReleaseResponse
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.OrderReleasePoint
import org.simplemes.mes.product.domain.Product

import javax.inject.Singleton
import javax.transaction.Transactional

/**
 * This service provides business logic used for the Assembly module that
 * provides additions to the core Order service.
 * <p>
 * This also provides the main entry point for maintaining the assembled components for an order/LSN.
 * This includes adding and removing the component information.
 */
@Slf4j
@Singleton
@Transactional
class OrderAssyService implements OrderReleasePoint, AdjustQueryInterface {

  /**
   * The maximum length of the assembly data string total. (Default: 300).  Each single assembly record
   * can contribute 100 chars to this string.
   */
  static final Integer MAX_ASSY_DATA_STRING_LENGTH = 300

  /**
   * Pre-release extension method for the order release process.
   * <p>
   * <b>Note</b>: This method is part of the Stable API.
   * @param orderReleaseRequest The details of the release request (order and qty).
   */

  @Override
  void preRelease(OrderReleaseRequest orderReleaseRequest) {

  }

  /**
   * After an order is released by the mes-core module, this addition method will copy any ProductComponent (BOM)
   * records to the order.
   * <p>
   * <b>Note:</b> Do not call this directly.  Call the mes-core OrderService.release() method instead.
   * <p>
   * @param coreResponse The response from the core method (or from other extensions that provide a response).
   * @param orderReleaseRequest The details of the release request (order and qty).
   * @return Return a new response if your extension needs to return a different value. Return null to use core response.
   *         You can also modify the coreResponse if it is aPOGO that allows modification.
   */
  @Override
  OrderReleaseResponse postRelease(OrderReleaseResponse coreResponse, OrderReleaseRequest orderReleaseRequest) {
    def order = orderReleaseRequest?.order
    def count = 0
    def components = order?.product?.getFieldValue('components')
    if (components) {
      for (component in (components)) {
        def orderComponent = new OrderBOMComponent(component as ProductComponent)
        order.getFieldValue('components') << orderComponent
        count++
      }
    }

    // Now, force a save, just in case the session close does not flush it.
    if (count) {
      orderReleaseRequest.order.save()
    }
    return null
  }


  /**
   * Adds a single component to the order/LSN.  Will auto-assign a unique sequence.
   * <p/>
   * This method is part of the <b>Stable API</b>.
   * @param request The component details to add to the order/LSN.
   * @return The component record added to the order.
   */
  OrderAssembledComponent addComponent(AddOrderAssembledComponentRequest request) {
    ArgumentUtils.checkMissing(request, 'request')
    def order = request.order
    ArgumentUtils.checkMissing(order, 'request.order')

    resolveComponentForRequest(request)

    def orderAssembledComponent = new OrderAssembledComponent()
    orderAssembledComponent.component = request.component
    orderAssembledComponent.userName = SecurityUtils.currentUserName
    orderAssembledComponent.qty = request.qty ?: 1.0
    orderAssembledComponent.assemblyData = request.assemblyData
    orderAssembledComponent.fields = request.fields
    orderAssembledComponent.bomSequence = request.bomSequence ?: 0
    orderAssembledComponent.lsn = request.lsn
    orderAssembledComponent.workCenter = request.workCenter
    orderAssembledComponent.location = request.location
    // Assign a unique sequence to this new record.
    def maxSequence = 0
    order.assembledComponents.each { maxSequence = it.sequence > maxSequence ? it.sequence : maxSequence }
    orderAssembledComponent.sequence = maxSequence + 1
    order.assembledComponents << orderAssembledComponent
    order.dateUpdated = new Date()
    order.save()
    return orderAssembledComponent
  }

  /**
   * Resolves the component for the given request.
   * Updates the request's component, bomSequence and qty, if possible.
   * <p>
   * Uses these values (in order of precedence):
   * <p> bomSequence
   * <p> component
   *
   *
   * @param request The request.  Modified.
   */
  void resolveComponentForRequest(AddOrderAssembledComponentRequest request) {
    def order = request.order
    if (request.bomSequence) {
      // See if a bomSequence can be used to find the component requirements
      def bomSequence = request.bomSequence
      def orderBOMComponent = order.components?.find() { it.sequence == bomSequence }
      if (!orderBOMComponent) {
        //error.10001.message=Could not find component to remove in order {0} sequence {1}.
        throw new BusinessException(10001, [order.order, bomSequence])
      }
      request.component = (Product) orderBOMComponent.component
      request.qty = request.qty ?: orderBOMComponent.qty
    } else {
      // Use the component to find the right BOM record (if possible)
      ArgumentUtils.checkMissing(request.component, 'request.component')
      def component = request.component
      def orderBOMComponent = order.components?.find() { it.component == component }
      if (orderBOMComponent) {
        request.bomSequence = orderBOMComponent.sequence
        request.qty = request.qty ?: orderBOMComponent.qty
      }
    }
  }

  /**
   * Marks a single component as removed.
   * <p/>
   * This method is part of the <b>Stable API</b>.
   * @param request The component to remove.  The Sequence is used to update the record.
   * @return The component record after the removal.
   */
  OrderAssembledComponent removeComponent(RemoveOrderAssembledComponentRequest request) {
    ArgumentUtils.checkMissing(request, 'request')
    def order = request.order
    def sequence = request.sequence
    ArgumentUtils.checkMissing(order, 'request.order')
    ArgumentUtils.checkMissing(sequence, 'request.sequence')

    def orderAssembledComponent = order.assembledComponents.find() {
      it.sequence == sequence
    } as OrderAssembledComponent
    if (!orderAssembledComponent) {
      //error.10001.message=Could not find component to remove in order {0} sequence {1}.
      throw new BusinessException(10001, [order.order, sequence])
    }

    if (orderAssembledComponent.state == AssembledComponentStateEnum.REMOVED) {
      //error.10002.message=Component {0} has already been removed from order {1} by {2} at {3}
      throw new BusinessException(10002, [orderAssembledComponent.component, order.order,
                                          orderAssembledComponent.removedByUserName,
                                          DateUtils.formatDate(orderAssembledComponent.removedDate)])
    }
    orderAssembledComponent.state = AssembledComponentStateEnum.REMOVED
    orderAssembledComponent.removedByUserName = SecurityUtils.currentUserName
    orderAssembledComponent.removedDate = new Date()

    orderAssembledComponent.save()

    return orderAssembledComponent

  }

  /**
   * Un-removes a single component.  Used to undo a removal from the client.
   * <p/>
   * This method is part of the <b>Stable API</b>.
   * @param request The component to restore.  The Sequence is used to update the record.
   * @return The component record after the restore.
   */
  OrderAssembledComponent undoComponentRemove(ComponentRemoveUndoRequest request) {
    ArgumentUtils.checkMissing(request, 'request')
    def order = request.order
    def sequence = request.sequence
    ArgumentUtils.checkMissing(order, 'request.order')
    ArgumentUtils.checkMissing(sequence, 'request.sequence')

    def orderAssembledComponent = order.assembledComponents.find() {
      it.sequence == sequence
    } as OrderAssembledComponent
    if (!orderAssembledComponent) {
      //error.10001.message=Could not find component to remove in order {0} sequence {1}.
      throw new BusinessException(10001, [order.order, sequence])
    }

    if (orderAssembledComponent.state != AssembledComponentStateEnum.REMOVED) {
      //error.10003.message=Component {0} has already been un-removed on order {1}
      throw new BusinessException(10003, [orderAssembledComponent.component, order.order])
    }
    orderAssembledComponent.state = AssembledComponentStateEnum.ASSEMBLED
    orderAssembledComponent.removedByUserName = null
    orderAssembledComponent.removedDate = null

    orderAssembledComponent.save()

    return orderAssembledComponent

  }

  /**
   * Finds the state of all the components needed for the given order/LSN.
   * If LSN is given, then the logic will attempt to reconcile order-based components
   * with this single LSN.  This will cause problems when mixing order-based and LSN-based
   * component assembly for a given component.
   * <p>
   * This method does no pagination or filtering, but it does sort by sequence.  It does group by the
   * BOM component record.
   *
   * @param request The find request.
   * @return The component states.
   */
  @SuppressWarnings("AbcMetric")
  List<OrderComponentState> findComponentAssemblyState(FindComponentAssemblyStateRequest request) {
    // TODO: Add support for paging and use large page size in GUI (100)
    List<OrderComponentState> res = []
    if (!request || !request.demand) {
      return res
    }

    Order order
    LSN lsn = null
    if (request.demand instanceof Order) {
      order = request.demand as Order
    } else {
      lsn = request.demand as LSN
      order = lsn.order
    }

    // Find the BOM requirements
    List<OrderBOMComponent> orderComponents = order.components
    log.trace('findComponentAssemblyState: orderComponents = {}', orderComponents)

    // and add them to the result, with the current assembly state.
    for (orderComponent in orderComponents) {
      // Calculate the required qty for the requested element
      def qtyRequired = orderComponent.qty * (lsn?.qty ?: order?.qtyToBuild)
      def orderComponentState = new OrderComponentState(component: orderComponent.component.product,
                                                        componentAndTitle: TypeUtils.toShortString(orderComponent.component, true),
                                                        sequence: orderComponent.sequence,
                                                        qtyRequired: qtyRequired,
                                                        qtyAssembled: 0.0)
      orderComponentState.assemblyData = orderComponent.component.assemblyDataType
      if (orderComponentState.assemblyData?.fields?.size()) {
        orderComponentState.firstAssemblyDataField = orderComponentState.assemblyData.fields[0].fieldName
      }
      res << orderComponentState
    }

    // Now, figure out how much has been assembled for each bom component record.
    // Only find the ones still assembled and for the LSN (if LSN is passed in) or for the entire order.
    for (orderComponentState in res) {
      List<OrderAssembledComponent> matches = order.assembledComponents.findAll() { OrderAssembledComponent comp ->
        comp.state == AssembledComponentStateEnum.ASSEMBLED &&
          comp.bomSequence == orderComponentState.sequence &&
          (comp.lsn == null || comp.lsn == lsn)
      }
      def qtyAssembled = matches.sum() { it.qty } as BigDecimal
      orderComponentState.qtyAssembled = qtyAssembled ?: 0.0
      //println "qtyAssembled = $qtyAssembled for $matches"
      //println "orderComponentState = $orderComponentState"
      // Now, copy the assy data values to the output record.
      if (matches) {
        // Use only the first Assy data type (only one is valid).
        orderComponentState.assemblyData = matches[0].assemblyData
        orderComponentState.fields = matches[0].fields
        // Now, add the unique assy data values to the output string.
        for (match in matches) {
          addAssemblyDataValues(orderComponentState, match)
          orderComponentState.sequencesForRemoval << match.sequence
          orderComponentState.removalLabels << formatForRemoval(match)
        }
        // If a single assembled component was found, then make sure the sequenceForRemoval is set.
        // Only a single entry can be removed with quick removal.
      } else {
        // Force a blank instead of null.  Null causes problems with client-side display toolkit grid.
        orderComponentState.assemblyDataAsString = ''
      }
    }

    // Now, fill in the pre-calculated values for the client display.
    for (orderComponentState in res) {
      determineStateValues(orderComponentState)
    }

    // Now, filter out the ones that are fully assembled if the user only wants to see missing components
    if (request.hideAssembled) {
      res = res.findAll() {
        it.overallState == OrderComponentStateEnum.EMPTY || it.overallState == OrderComponentStateEnum.PARTIAL
      }
    } else {
      // Now, add any non-BOM components currently assembled.
      List<OrderAssembledComponent> nonBOMs = order.assembledComponents.findAll() { OrderAssembledComponent comp ->
        comp.state == AssembledComponentStateEnum.ASSEMBLED &&
          comp.bomSequence == 0
      }
      // Now, sort the nonBOMs by product name for consistent sorting and find next sequence to use for nonBOMs.
      def currentSequence = 0
      if (nonBOMs) {
        nonBOMs = nonBOMs.sort() { it.component.product }
        def max = res.max() { it.sequence }
        currentSequence = max?.sequence ?: 0
      }

      for (nonBOM in nonBOMs) {
        currentSequence += 10
        def orderComponentState = new OrderComponentState(component: nonBOM.component.product,
                                                          componentAndTitle: TypeUtils.toShortString(nonBOM.component, true),
                                                          sequence: currentSequence,
                                                          sequencesForRemoval: [nonBOM.sequence],
                                                          qtyRequired: 0.0,
                                                          qtyAssembled: nonBOM.qty)
        orderComponentState.assemblyData = nonBOM.assemblyData
        orderComponentState.fields = nonBOM.fields
        orderComponentState.canBeRemoved = true
        orderComponentState.canBeAssembled = false
        addAssemblyDataValues(orderComponentState, nonBOM)
        determineStateValues(orderComponentState)
        res << orderComponentState
      }
    }

    res.sort() { it.sequence }

    log.trace('findComponentAssemblyState: res = {}', res)
    return res
  }

  /**
   * Sets the various state values and flags based on the quantities in the state POGO.
   * This updates the value in place.
   * @param orderComponentState The component state POGO to update.
   */
  static void determineStateValues(OrderComponentState orderComponentState) {
    if (orderComponentState.qtyAssembled == 0.0) {
      orderComponentState.overallState = OrderComponentStateEnum.EMPTY
      orderComponentState.canBeAssembled = true
      orderComponentState.canBeRemoved = false
    } else if (orderComponentState.qtyAssembled == orderComponentState.qtyRequired) {
      orderComponentState.overallState = OrderComponentStateEnum.FULL
      orderComponentState.canBeAssembled = false
      orderComponentState.canBeRemoved = true
    } else if (orderComponentState.qtyAssembled > orderComponentState.qtyRequired) {
      orderComponentState.overallState = OrderComponentStateEnum.OVER
      orderComponentState.canBeAssembled = false
      orderComponentState.canBeRemoved = true
    } else {
      orderComponentState.overallState = OrderComponentStateEnum.PARTIAL
      orderComponentState.canBeAssembled = true
      orderComponentState.canBeRemoved = true
    }
    orderComponentState.overallStateString = orderComponentState.overallState.toStringLocalized()

    if (orderComponentState.qtyRequired > 0.0) {
      //noinspection GroovyAssignabilityCheck
      orderComponentState.percentAssembled = 100 * orderComponentState.qtyAssembled / orderComponentState.qtyRequired
    } else {
      orderComponentState.percentAssembled = 0
    }
    def qa = NumberUtils.formatNumber(orderComponentState.qtyAssembled)
    def qr = NumberUtils.formatNumber(orderComponentState.qtyRequired)

    orderComponentState.qtyAndStateString = "$qa/$qr $orderComponentState.overallStateString"
  }

  /**
   * Adds the assy data type's field values to the assemblyDataAsString for display.  It will all the assy data up to
   * 300 chars total, eliminating duplicates sets.
   * This updates the orderComponentState in place.
   * @param orderComponentState Component state POGO to add the assy data field values to.
   * @param orderAssembledComponent The assembled component to pull the assy data from.
   */
  static void addAssemblyDataValues(OrderComponentState orderComponentState, OrderAssembledComponent orderAssembledComponent) {
    def original = orderComponentState.assemblyDataAsString
    if (original == null) {
      original = ''
    }
    def s = ExtensibleFieldHelper.instance.formatConfigurableTypeValues('assemblyData',
                                                                        orderAssembledComponent, [highlight: true])
    if (!s) {
      // No flex type fields defined.  See if there are any left-over values in the custom fields.
      s = ExtensibleFieldHelper.instance.formatConfigurableTypeValues(orderAssembledComponent, [highlight: true])
    }
    // Now, if the new data is not already in the list, add it to it (up to 300 total chars).
    if (!original.contains(s)) {
      if ((original.length() + s.length()) < MAX_ASSY_DATA_STRING_LENGTH) {
        if (original) {
          // If there is another set of data, then add a delimiter.
          original += '; '
        }
        original += s
      } else {
        original += '...'
      }
    }
    orderComponentState.assemblyDataAsString = original
  }

  /**
   * This will change the query string if it detects an attempt to search on dynamic
   * assembly data field and adjusts the query with the right document path.
   * If the query string starts with a flex type, then it will be adjusted to
   * the right field name to find the data.
   * @param queryString The input query string from the user.
   * @param domainClass The domain class for domain-specific searches.  Null allowed.
   * @param coreQuery The results from the framework core adjustQuery() method.
   * @return The adjusted query string.
   */
  String adjustQueryAssemblyData(String queryString, Class domainClass, String coreQuery) {
    def results = coreQuery

    if (SearchHelper.isSimpleQueryString(coreQuery)) {
      // First, find the field name from the query.
      if (coreQuery.toLowerCase().startsWith('assy.')) {
        // We can ignore the assy. in this case.
        coreQuery = coreQuery[5..-1]
      }
      def loc = coreQuery.indexOf(':')
      if (loc > 0) {
        // Then it has a simple field name, so extract it and see if it matches a FlexField.
        def fieldName = coreQuery[0..(loc - 1)]
        def flexField = FlexField.findByFieldNameIlike(fieldName)
        if (flexField) {
          results = "order.assembledComponents.assemblyDataValues.${flexField.fieldName}${coreQuery[loc..-1]}"
        }
      }
    }

    return results
  }

  /**
   * ExtensionPoint pre method to adjust the query string to make the input more user friendly.
   * <p>
   * This implementation will change the query string if it detects an attempt to search on dynamic
   * assembly data field and adjusts the query with the right document path.
   * If the query string starts with a flex type, then it will be adjusted to
   * the right field name to find the data.
   *
   * @param queryString The input query string from the user. Null not allowed.
   * @param domainClass The domain class for domain-specific searches.  Null allowed.
   * @return The adjusted query string.
   */
  @Override
  void preAdjustQuery(String queryString, Class domainClass) {

  }

  /**
   * ExtensionPoint post method to adjust the query string to make the input more user friendly.
   * @param response The core response.
   * @param queryString The input query string from the user. Null not allowed.
   * @param domainClass The domain class for domain-specific searches.  Null allowed.
   * @return The adjusted query string.
   */
  @Override
  String postAdjustQuery(String response, String queryString, Class domainClass) {
    def results = response

    if (SearchHelper.instance.isSimpleQueryString(response)) {
      // First, find the field name from the query.
      if (response.toLowerCase().startsWith('assy.')) {
        // We can ignore the assy. in this case.
        response = response[5..-1]
      }
      def loc = response.indexOf(':')
      if (loc > 0) {
        // Then it has a simple field name, so extract it and see if it matches a FlexField.
        def fieldName = response[0..(loc - 1)]
        def list = FlexField.findAllByFieldNameIlike(fieldName)
        if (list) {
          def flexField = list[0]
          results = "assembledComponents.assemblyData_${flexField.fieldName}${response[loc..-1]}"
        }
      }
    }

    return results
  }

  /**
   * Formats the given assembled component, suitable for a removal confirmation.
   * @param orderAssembledComponent The component.
   * @return The
   */
  String formatForRemoval(OrderAssembledComponent orderAssembledComponent) {
    def orderComponentState = new OrderComponentState()
    addAssemblyDataValues(orderComponentState, orderAssembledComponent)
    def s = orderComponentState.assemblyDataAsString
    //removalLabel.label=Component :{0} Qty: {1} - {2}
    return GlobalUtils.lookup('removalLabel.label', orderAssembledComponent.component.product, orderAssembledComponent.qty, s)
  }
}
