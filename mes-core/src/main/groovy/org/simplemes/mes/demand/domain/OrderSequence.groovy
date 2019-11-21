package org.simplemes.mes.demand.domain

import grails.gorm.annotation.Entity
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.mes.numbering.domain.CodeSequence

/**
 * Defines the sequence used to generate new order numbers.
 * <p/>
 * The default ordering naming sequence is 'ORDER'.
 *
 */
@Slf4j
@Entity
@ToString(includeNames = true, includePackage = false)
class OrderSequence extends CodeSequence {

  /**
   * If true, then this sequence is the default used for new orders.
   */
  boolean defaultSequence = false

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
    OrderSequence s

    //noinspection UnnecessaryQualifiedReference
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
    findByDefaultSequence(true)
  }

}
