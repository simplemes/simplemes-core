package org.simplemes.mes.demand.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.mes.demand.CompleteRequest
import org.simplemes.mes.demand.DemandObject
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.OrderStatus
import org.simplemes.mes.demand.WorkStateTrait
import org.simplemes.mes.demand.WorkableInterface
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.tracking.domain.ActionLog

import javax.persistence.OneToMany

/*
import org.simplemes.mes.numbering.LSNSequence
import org.simplemes.mes.numbering.OrderSequence
import org.simplemes.mes.product.Product
import org.simplemes.mes.system.LSNTrackingOption
import org.simplemes.mes.tracking.ActionLog
*/

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * This is a manufacturing order that is used to produce a product on the shop floor.  This is the primary
 * object that a production operator interacts with on the shop floor.
 * <p/>
 * <b>Note:</b> This object is stored in the table <b>'ORDR'</b>
 */
@MappedEntity
@DomainEntity
@ToString(includeNames = true, includePackage = false, excludes = ['dateCreated', 'dateUpdated'])
@EqualsAndHashCode(includes = ["order"])
class Order implements WorkStateTrait, WorkableInterface, DemandObject {
  /**
   * The order's name as it is known to the users.  Upon creation, if an order is not provided,
   * then one is generated from the Sequence.  <b>Primary Key Field</b>.
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_CODE_LENGTH}.
   * <p/>
   * This value is stored in the column <b>'ORDR'</b>
   */
  String order

  /**
   * The Order's overall status.  This is one of the pre-defined OrderStatus codes.
   */
  OrderStatus overallStatus

  /**
   * The number of pieces to produce for this order.
   * <p/>
   * Default value=1.0
   * <p/>
   * Minimum value=0.000000001
   */
  BigDecimal qtyToBuild = 1.0

  /**
   * The number of released to production. This is released manually in the GUI or by using the OrderService.
   * <p/><b>Non-CRUD Field</b>.
   */
  BigDecimal qtyReleased = 0.0

  /**
   * The product to be produced by this order.
   * <b>(Optional)</b>
   */
  Product product

  /**
   * Defines the 'has' elements.  These are child elements of this order that are deleted when the
   * order is deleted.<p/>
   * This orderRouting is the routing used only by this order.  The effective routing is copied to the order upon release.
   */
  static hasOne = [orderRouting: OrderRouting]

  /**
   * This is the list operation states that corresponds to the current orderRouting's operations.  This holds the quantities
   * in queue, in work, etc for a given operation. This list is sorted in the same order as the orderRouting's operations
   *
   */
  @OneToMany(mappedBy = "order")
  List<OrderOperState> operationStates
  // This duplicate definition is needed since the normal hasMany injection uses a Set.  A List is easier to use.

  /**
   * The Order can have child lot/serial numbers (LSNs) that can be processed for many actions.
   * Order is not guaranteed. <b>(Optional)</b>
   */
  @OneToMany(mappedBy = "order")
  List<LSN> lsns
  // This duplicate definition is needed since the normal hasMany injection uses a Set.  A List is easier to use.

  // Automatically updated values.
  /**
   * Copied from Product on creation to determine what type of tracking is needed.  If product is not given,
   * then defaults to Order only.
   */
  LSNTrackingOption lsnTrackingOption = LSNTrackingOption.ORDER_ONLY

  /**
   * The date this order was released.
   */
  Date dateReleased

  /**
   * The date this order was completed or marked as done.
   */
  Date dateCompleted

  /**
   * The number of pieces waiting to be worked (in queue) for this object.
   * <p/><b>WorkStateTrait field</b>.
   */
  BigDecimal qtyInQueue = 0.0

  /**
   * The number of pieces actively in work for this object.
   * <p/><b>WorkStateTrait field</b>.
   */
  BigDecimal qtyInWork = 0.0

  /**
   * The number of pieces to completed (done) on this object.
   * <p/><b>WorkStateTrait field</b>.
   */
  BigDecimal qtyDone = 0.0

  /**
   * The date/time any quantity was queued at this point (operation or top-level).
   * Can be null if the nothing is in queue.
   * <p/><b>WorkStateTrait field</b>.
   */
  Date dateQtyQueued

  /**
   * The date/time any quantity was started at this point (operation or top-level).
   * Can be null if the nothing is in work.
   * <p/><b>WorkStateTrait field</b>.
   */
  Date dateQtyStarted

  /**
   * The date/time any quantity was first queued at this point (operation or top-level).
   * <p/><b>WorkStateTrait field</b>.
   */
  Date dateFirstQueued

  /**
   * The date/time any quantity was first started at this point (operation or top-level).
   * <p/><b>WorkStateTrait field</b>.
   */
  Date dateFirstStarted

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE') Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  Integer version = 0

  @Id @AutoPopulated UUID uuid

  /**
   * This is a list of fields that are not allowed in the standard CRUD (Create, Read, Update, Delete) APIs provided by the standard
   * controllers.  The specific fields that are not allowed in the CRUD calls are explicitly noted in the field documentation (Javadoc)
   * with the notation: <b>Non-CRUD Field</b>.
   * These fields are updated by business logic, normally in a related service.
   */
  //@SuppressWarnings("GroovyUnusedDeclaration")
  // TODO:  static nonCRUDFields = ['qtyReleased', 'qtyInQueue', 'qtyInWork', 'qtyDone']

  /**
   * This domain is a top-level searchable element.
   */
  // TODO: static searchable = true

  /**
   * <i>Internal definitions for GORM framework.</i>
   */
  static constraints = {
    order(maxSize: FieldSizes.MAX_CODE_LENGTH, unique: true, nullable: false, blank: false)
    product(nullable: true)
    qtyToBuild(min: 0.0001, scale: FieldSizes.STANDARD_DECIMAL_SCALE, validator: { val, obj -> checkQtyToBuild(val, obj) })
    qtyReleased(min: 0.000, scale: FieldSizes.STANDARD_DECIMAL_SCALE)
    overallStatus(nullable: false)  // Look at maxSize: OrderStatus.ID_LENGTH,
    lsns(validator: { val, obj -> checkLSNs(val, obj) })
    orderRouting(nullable: true)
    dateCompleted(nullable: true)
    dateReleased(nullable: true)

    // Fields added by the WorkStateTrait
    qtyInQueue(scale: FieldSizes.STANDARD_DECIMAL_SCALE)
    qtyInWork(scale: FieldSizes.STANDARD_DECIMAL_SCALE)
    qtyDone(scale: FieldSizes.STANDARD_DECIMAL_SCALE)
    dateQtyQueued(nullable: true)
    dateQtyStarted(nullable: true)
    dateFirstQueued(nullable: true)
    dateFirstStarted(nullable: true)
  }

  /**
   * <i>Internal definitions for GORM framework.</i>
   */
  static mapping = {
    table 'ordr'           // ORDER is not a legal table or column name, so use ORDR
    order column: 'ordr', index: 'Ordr_Idx'
    autoTimestamp true
  }

  /**
   * <i>Internal definitions for GORM framework.</i>
   */
  static hasMany = [lsns: LSN, operationStates: OrderOperState]

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['order', 'overallStatus', 'qtyToBuild', 'product', 'orderRouting',
                       'group:state', 'qtyReleased', 'qtyInQueue', 'qtyInWork', 'qtyDone',
                       'dateCompleted',
                       'group:lsn', 'lsnTrackingOption', 'lsns']

  /**
   * Called before validate happens.  Used to set the order if needed.
   */
  def beforeValidate() {
    // Set the order name if none provided.
    if (!order) {
      def values = OrderSequence.findDefaultSequence()?.formatValues(1)
      if (values) {
        order = values[0]
      } else {
        throw new IllegalArgumentException("No default OrderNameSequence")
      }
    }

    // Set the status if one is not provided.
    overallStatus = overallStatus ?: OrderStatus.getDefault()

    // Now handle any stuff to be done on creation only.
    if (!id) {
      if (product) {
        // Copy the LSN tracking option on creation only.
        lsnTrackingOption = product.lsnTrackingOption
      }
    }

    //Finally, a work around for GORM bug: http://jira.grails.org/browse/GRAILS-8158
    // Call beforeValidate() on the SN explicitly
    lsns?.each { lsn ->
      lsn.beforeValidate()
    }
  }

  /**
   * Validates that the qtyToBuild is legal for the given Order.
   * @param val The qtyToBuild to be validated.
   * @param obj The overall order object.
   */
  static checkQtyToBuild(val, obj) {
    Order order = obj
    BigDecimal qtyToBuild = val
    int nValues = calculateLSNCount(qtyToBuild, order?.product)
    if (obj.lsns) {
      // If LSNs are provided, then make sure they match the qty to build.
      if (nValues != obj.lsns.size()) {
        //order.qtyToBuild.wrongLSNCount.error=Wrong number of LSNs {3} provided for order {4}.  Should be {5}.
        return ['wrongLSNCount.error', obj.lsns.size(), obj.order, nValues]
      }
    }
    return true
  }

  /**
   * Validates that the LSNs are legal for the given Order.
   * @param val The LSNs to be validated.
   * @param obj The overall order object.
   */
  static checkLSNs(val, obj) {
    Order order = obj
    // Make sure LSNs are allowed (if we have any)
    if (order?.product?.lsnTrackingOption) {
      if ((val?.size() > 0) && !(order?.product?.lsnTrackingOption?.isLSNAllowed())) {
        //order.lsns.notAllowed.error=LSNs not allowed for tracking option {3} provided for order {4}.
        return ['notAllowed.error', order?.product?.lsnTrackingOption?.toString(), obj.order]
      }
    }
    // Make sure there are no duplicates.
    String dup = null
    if (val?.size() > 0) {
      val.each { orig ->
        if (val.count { orig.lsn == it.lsn } > 1) {
          dup = orig.lsn
        }
      }
    }
    if (dup) {
      //order.lsns.duplicate.error=Duplicate LSN {3} provided for order {4}.
      return ['duplicate.error', dup, obj.order]
    } else {
      return true
    }

  }

  /**
   * Populate the LSN with a system generated sequence.  Uses the product's sequence and lotSize when possible.
   * A default lot size of 1.0 is used of product is not available.
   * <p/>
   * Does nothing if no unreleased quantity is available.
   */
  void populateLSNs() {
    // Calculate how much of the build qty is already in the LSN list.
    def qtyInLSNList = 0.0
    for (lsn in lsns) {
      qtyInLSNList += lsn.qty
    }

    BigDecimal qtyToAdd = qtyToBuild - qtyInLSNList
    if (qtyToAdd <= 0) {
      return
    }

    int nValues = calculateLSNCount(qtyToAdd, product)

    if (nValues == 0) {
      // Nothing left to generate.
      return
    }

    // Find the right LSN Sequence to create.
    def seq = product?.lsnSequence
    seq = seq ?: LSNSequence.findDefaultSequence()

    if (!seq) {
      // Still no sequence found, then throw a configuration error.
      //error.2009.message=Could not find expected default value for {0}.
      throw new BusinessException(2009, ['Default LSNSequence'])
    }

    // Build the parameters available to the sequence generator.
    def params = [order: this, product: product]

    def lotSize = product?.lotSize ?: 1.0
    List<String> numbers = seq.formatValues(nValues, params)
    numbers.each { number ->
      // Figure out the qty for this LSN.  Full Lot size until the last LSN (maybe).
      def qty = lotSize
      if (qtyToAdd < qty) {
        qty = qtyToAdd
      }
      qtyToAdd -= qty
      LSN lsn = new LSN(lsn: number, qty: qty)
      addToLsns(lsn)
    }
  }

  /**
   * Utility method to calculate the number of whole LSNs needed for a given qty.
   * @param qty The quantity needed on the LSNs.  Typically, the qty to release.
   * @param product The product to check the LSN count for.  Uses lotSize from the product if defined.  Uses lotSize=1.0 if no product.
   * @return The number of LSNs needed.
   */
  private static int calculateLSNCount(BigDecimal qty, Product product) {
    def lotSize = product?.lotSize ?: 1.0
    //noinspection GroovyAssignabilityCheck
    int nValues = qty / lotSize
    // Now, account for partial last LSN in case of a fraction.
    if ((qty.remainder(lotSize)) != 0.0) {
      nValues++
    }
    nValues
  }


  /**
   * Setter needed to save OrderRouting records in Unit Tests.
   * Mainly used to set the OrderRouting.order in the unit tests to work around an issue with the unit test mocks.
   * @param orderRouting The order routing.
   */
  void setOrderRouting(OrderRouting orderRouting) {
    this.orderRouting = orderRouting
    orderRouting?.order = this
  }

  /**
   * Determines the next workable to be performed after the given operation is done.
   * @param currentOperState The current operation to find the next operation for.
   * @return The workable.  Null if there is no other workable left to be processed.
   */
  WorkableInterface determineNextWorkable(OrderOperState currentOperState) {
    int nextSeq = orderRouting?.determineNextOperation(currentOperState.sequence)
    if (nextSeq == 0) {
      return null
    }
    return operationStates.find { it.sequence == nextSeq }
  }

  /**
   * Determines the next workable to be performed after this workable is completed.
   * @return The workable (null for this top-level tracking scenario).
   */
  WorkableInterface determineNextWorkable() {
    return null
  }

  /**
   * Saves any changes to this record.
   */
  void saveChanges() {
    // Force GORM/Hibernate to update the record
    setLastUpdated(new Date())
    save()
  }

  /**
   * Checks to determine if the order is done and sets the right flags/statuses as needed.
   * This is called when any workable on the order is completed.
   * @param request The Complete request that triggered this possible order done processing.
   */
  void checkForOrderDone(CompleteRequest request) {
    // TODO: handle LSN
    if (orderRouting) {
      // Has some routing steps to check
      qtyDone += request.qty
    }
    if (qtyToBuild == qtyDone) {
      markAsDone()
    }
  }

  /**
   * Mark the current order as Done (fully completed).
   */
  void markAsDone() {
    dateCompleted = new Date()
    overallStatus = OrderStatus.doneStatus
    save()
  }

  /**
   * Find the first N LSNs with a QTY in queue
   * @param qtyNeeded The qty needed to be satisfied by the LSN(s).
   * @return The list of LSNs that satisfy the need.
   */
  LSN[] resolveSpecificLSNs(BigDecimal qtyNeeded) {
    def res = []
    BigDecimal total = 0.0
    for (lsn in lsns) {
      if (lsn.qtyInQueue > 0) {
        res << lsn
        total += lsn.qtyInQueue
        if (total >= qtyNeeded) {
          break
        }
      }
    }

    return res
  }

  /**
   * Finds objects the related for archiving.
   * @return The list of objects.
   */
  List<Object> findRelatedRecords() {
    return ActionLog.findAllByOrder(this)
  }

}
