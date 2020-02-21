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
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.numbering.CodeSequenceTrait

import javax.persistence.Column

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
class OrderSequence implements CodeSequenceTrait {

  /**
   * The primary key for this sequence.
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String sequence

  /**
   * The title (short description) of this object.  This is usually visible users.
   */
  @Column(length = FieldSizes.MAX_TITLE_LENGTH, nullable = true)
  String title

  /**
   * The Format string.  Supports these replaceable parameters:
   *
   * $currentSequence
   */
  @Column(length = FieldSizes.MAX_LONG_STRING_LENGTH, nullable = false)
  String formatString = 'SN$currentSequence'

  /**
   * The current sequence.
   */
  long currentSequence = 1

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
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['sequence', 'title', 'formatString', 'currentSequence', 'defaultSequence']

  /**
   * A list of the records created by the initial data load.
   * Used only for test cleanup by {@link org.simplemes.eframe.test.BaseSpecification}.
   */
  static Map<String, List<String>> initialDataRecords = [OrderSequence: ['ORDER']]

  /**
   * Loads initial naming sequence(s).  Default sequence is:
   * <ul>
   *   <li> ORDER - <b>format:</b> <i>'M$currentSequence'</i></li>
   * </ul>
   *
   */
  static Map<String, List<String>> initialDataLoad() {
    OrderSequence s

    s = OrderSequence.findBySequence('ORDER')
    if (!s) {
      new OrderSequence(sequence: 'ORDER', title: 'Order Sequence',
                        formatString: 'M$currentSequence', currentSequence: 1000, defaultSequence: true).save()
      log.info("Loaded OrderNameSequence ORDER")
    }
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
    OrderSequence.findByDefaultSequence(true)
  }

}
