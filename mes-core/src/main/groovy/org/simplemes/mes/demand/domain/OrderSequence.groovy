package org.simplemes.mes.demand.domain

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity

/**
 * Defines the sequence used to generate new order numbers.
 * <p/>
 * The default ordering naming sequence is 'ORDER'.
 *
 */
@Slf4j
@MappedEntity
@DomainEntity
@ToString(includeNames = true, includePackage = false)
class OrderSequence /* TODO: move from extends CodeSequence*/ {

  /**
   * The primary key for this sequence.
   */
  String sequence

  /**
   * If true, then this sequence is the default used for new orders.
   */
  boolean defaultSequence = false

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE') Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  Integer version = 0

  @Id @AutoPopulated UUID uuid

  /**
   * Internal constraints.
   */
  static constraints = {
  }

  /**
   * A list of the records created by the initial data load.
   * Used only for test cleanup by {@link org.simplemes.eframe.test.BaseSpecification}.
   */
  static Map<String, List<String>> initialDataRecords = [CodeSequence: ['ORDER'], OrderSequence: ['ORDER']]

  /**
   * Loads initial naming sequence(s).  Default sequence is:
   * <ul>
   *   <li> ORDER - <b>format:</b> <i>'M$currentSequence'</i></li>
   * </ul>
   *
   */
  static Map<String, List<String>> initialDataLoad() {
    // TODO: Restore
/*
    OrderSequence s

    //noinspection UnnecessaryQualifiedReference
    s = OrderSequence.findBySequence('ORDER')
    if (!s) {
      new OrderSequence(sequence: 'ORDER', title: 'Order Sequence',
                        formatString: 'M$currentSequence', currentSequence: 1000, defaultSequence: true).save()
      log.info("Loaded OrderNameSequence ORDER")
    }
*/
    return initialDataRecords
  }

  /**
   * Returns the order name sequence that is flagged as the default.
   * @return The sequence that is the default code.
   *
   */
  static OrderSequence findDefaultSequence() {
    // Note: This is functionally the same as the CodeSequence method, but we need one here to limit
    //       the search to just OrderSequences.
    findByDefaultSequence(true)
  }

}
