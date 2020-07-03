package org.simplemes.mes.demand.domain

import com.fasterxml.jackson.annotation.JsonFilter
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.mes.demand.CompleteRequest
import org.simplemes.mes.demand.DemandObject
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.OrderStatus
import org.simplemes.mes.demand.WorkStateTrait
import org.simplemes.mes.demand.WorkableInterface
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.OperationTrait
import org.simplemes.mes.product.RoutingTrait
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.tracking.domain.ActionLog

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

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
@MappedEntity('ordr')
@DomainEntity
@JsonFilter("searchableFilter")
@ToString(includeNames = true, includePackage = false, excludes = ['dateCreated', 'dateUpdated'])
@EqualsAndHashCode(includes = ["order"])
class Order implements WorkStateTrait, WorkableInterface, DemandObject, RoutingTrait {
  /**
   * The order's name as it is known to the users.  Upon creation, if an order is not provided,
   * then one is generated from the Sequence.  <b>Primary Key Field</b>.
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_CODE_LENGTH}.
   * <p/>
   * This value is stored in the column <b>'ORDR'</b>
   */
  @Column(name = 'ordr', length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String order
  // TODO: Add unique index to DDL.

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
  @Nullable
  @ManyToOne(targetEntity = Product)
  Product product

  /**
   * This is the list of operations this order should be processed at.
   *
   */
  @OneToMany(targetEntity = OrderOperation, mappedBy = "order")
  List<OperationTrait> operations

  /**
   * This is the list operation states that corresponds to the current operations.  This holds the quantities
   * in queue, in work, etc for a given operation.
   *
   */
  @OneToMany(mappedBy = "order")
  List<OrderOperState> operationStates

  /**
   * The Order can have child lot/serial numbers (LSNs) that can be processed for many actions.
   * Order is not guaranteed. <b>(Optional)</b>
   */
  @OneToMany(mappedBy = "order")
  List<LSN> lsns

  /**
   * Copied from Product on creation to determine what type of tracking is needed.  If product is not given,
   * then defaults to Order only.
   */
  LSNTrackingOption lsnTrackingOption = LSNTrackingOption.ORDER_ONLY

  /**
   * The date this order was released.
   */
  @Nullable
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateReleased

  /**
   * The date this order was completed or marked as done.
   */
  @Nullable
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
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
  @Nullable @SuppressWarnings("unused")
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateQtyQueued

  /**
   * The date/time any quantity was started at this point (operation or top-level).
   * Can be null if the nothing is in work.
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @SuppressWarnings("unused")
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateQtyStarted

  /**
   * The date/time any quantity was first queued at this point (operation or top-level).
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @SuppressWarnings("unused")
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateFirstQueued

  /**
   * The date/time any quantity was first started at this point (operation or top-level).
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @SuppressWarnings("unused")
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateFirstStarted

  /**
   * The custom field holder.  Unlimited size.
   */
  @ExtensibleFieldHolder
  @Column(nullable = true)
  @MappedProperty(type = DataType.STRING, definition = 'TEXT')
  @SuppressWarnings("unused")
  String customFields

  @DateCreated @SuppressWarnings("unused")
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated @SuppressWarnings("unused")
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  @SuppressWarnings("unused")
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
  @SuppressWarnings("unused")
  static searchable = true

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['order', 'overallStatus', 'qtyToBuild', 'product',
                       'group:state', 'qtyReleased', 'qtyInQueue', 'qtyInWork', 'qtyDone',
                       'dateCompleted',
                       'group:lsns', 'lsnTrackingOption', 'lsns']

  /**
   * Called before validate happens.  Used to set the order if needed.
   */
  @SuppressWarnings("unused")
  def beforeValidate() {
    // Set the order name if none provided.
    if (!order) {
      def values = OrderSequence.findDefaultSequence()?.formatValues(1)
      if (values) {
        order = values[0]
      } else {
        throw new IllegalArgumentException("No default OrderSequence")
      }
    }

    // Set the status if one is not provided.
    overallStatus = overallStatus ?: OrderStatus.getDefault()

    // Now handle any stuff to be done on creation only.
    if (!uuid) {
      if (product) {
        // Copy the LSN tracking option on creation only.
        lsnTrackingOption = product.lsnTrackingOption
      }
    }
  }

  /**
   * Validates the record before save.
   * @return The list of errors.
   */
  List<ValidationError> validate() {
    def res = []
    res.addAll(validateQtyToBuild())
    res.addAll(validateQtyReleased())
    res.addAll(validateLSNs())

    return res
  }

  /**
   * Validates that the qtyToBuild is legal for the given Order.
   */
  List<ValidationError> validateQtyToBuild() {
    int nValues = calculateLSNCount(qtyToBuild)
    if (lsns) {
      // If LSNs are provided, then make sure they match the qty to build.
      if (nValues != lsns.size()) {
        //error.3013.message=Wrong number of LSNs {1} provided for order {2}.  Should be {3}.
        return [new ValidationError(3013, 'lsns', lsns.size(), order, nValues)]
      }
    }
    if (qtyToBuild <= 0) {
      //error.137.message=Invalid Value "{1}" for "{0}". Value should be greater than {2}.
      return [new ValidationError(137, 'qtyToBuild', qtyToBuild, 0)]
    }
    return []
  }

  /**
   * Validates that the qtyReleased is legal for the given Order.
   */
  List<ValidationError> validateQtyReleased() {
    if (qtyReleased < 0) {
      //error.136.message=Invalid Value "{1}" for "{0}". Value should be greater than or equal to {2}.
      return [new ValidationError(136, 'qtyReleased', qtyReleased, 0)]
    }
    return []
  }

  /**
   * Validates that the LSNs are legal for the given Order.
   */
  List<ValidationError> validateLSNs() {
    // Make sure LSNs are allowed (if we have any)
    if (product?.lsnTrackingOption) {
      if ((lsns?.size() > 0) && !(product?.lsnTrackingOption?.isLSNAllowed())) {
        //error.3014.message=LSNs not allowed for tracking option {1} provided for order {2}.
        return [new ValidationError(3014, 'lsns', product?.lsnTrackingOption?.toString(), order)]
      }
    }

    // Make sure there are no duplicates.
    String dup = null
    if (lsns) {
      lsns.each { orig ->
        if (lsns.count { orig.lsn == it.lsn } > 1) {
          dup = orig.lsn
        }
      }
    }
    if (dup) {
      //error.3015.message=Duplicate LSN {1} provided for order {2}.
      return [new ValidationError(3015, 'lsns', dup, order)]
    }

    return []

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

    int nValues = calculateLSNCount(qtyToAdd)

    if (nValues == 0) {
      // Nothing left to generate.
      return
    }

    // Find the right LSN Sequence to create.
    def seq = product?.lsnSequence
    seq = seq ?: LSNSequence.findDefaultSequence()

    if (!seq) {
      // Still no sequence found, then throw a configuration error.
      //error.102.message=Could not find expected default value for {0}.
      throw new BusinessException(102, [LSNSequence.simpleName])
    }

    // Build the parameters available to the sequence generator.
    def params = [order: this, product: product]

    def lotSize = product?.lotSize ?: 1.0
    List<String> numbers = seq.formatValues(nValues, params)
    lsns = lsns ?: []
    numbers.each { number ->
      // Figure out the qty for this LSN.  Full Lot size until the last LSN (maybe).
      def qty = lotSize
      if (qtyToAdd < qty) {
        qty = qtyToAdd
      }
      qtyToAdd -= qty
      LSN lsn = new LSN(lsn: number, qty: qty)
      lsns << lsn
    }
  }

  /**
   * Utility method to calculate the number of whole LSNs needed for a given qty.
   * @param qty The quantity needed on the LSNs.  Typically, the qty to release.
   * @return The number of LSNs needed.
   */
  private int calculateLSNCount(BigDecimal qty) {
    if (!qty) {
      return 0
    }
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
   * Determines the next workable to be performed after the given operation is done.
   * @param currentOperState The current operation to find the next operation for.
   * @return The workable.  Null if there is no other workable left to be processed.
   */
  WorkableInterface determineNextWorkable(OrderOperState currentOperState) {
    int nextSeq = determineNextOperation(currentOperState.sequence)
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
    save()
  }

  /**
   * Checks to determine if the order is done and sets the right flags/statuses as needed.
   * This is called when any workable on the order is completed.
   * @param request The Complete request that triggered this possible order done processing.
   */
  void checkForOrderDone(CompleteRequest request) {
    // TODO: handle LSN
    if (operations) {
      // Has some routing steps to check
      qtyDone += request.qty
    }
    if (qtyToBuild == qtyDone) {
      markAsDone()
    } else if (operations) {
      // Just a qtyDone update, so force the save.
      save()
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
