package org.simplemes.mes.demand.domain

import grails.gorm.annotation.Entity
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.mes.numbering.domain.CodeSequence

/**
 * Defines the sequence used to generate new lot/serial numbers (LSN).
 * <p/>
 * The default LSN naming sequence is 'SERIAL'.
 *
 */

@Slf4j
@Entity
@ToString(includeNames = true, includePackage = false)
class LSNSequence extends CodeSequence {

  /**
   * If true, then this sequence is the default used for new orders.
   */
  boolean defaultSequence = false

  /**
   * <i>Internal definitions for GORM framework.</i>
   */
  static constraints = {
  }

  /**
   * A list of the records created by the initial data load.
   * Used only for test cleanup by {@link org.simplemes.eframe.test.BaseSpecification}.
   */
  static Map<String, List<String>> initialDataRecords = [CodeSequence: ['SERIAL'], LSNSequence: ['SERIAL']]

  /**
   * Loads initial naming sequence(s).  Default sequence is:
   * <ul>
   *   <li> SERIAL - <b>format:</b> <i>'SN$currentSequence'</i></li>
   * </ul>
   *
   */
  static Map<String, List<String>> initialDataLoad() {
    LSNSequence s

    //noinspection UnnecessaryQualifiedReference
    s = LSNSequence.findBySequence('SERIAL')
    if (!s) {
      new LSNSequence(sequence: 'SERIAL', title: 'Serial Number',
                      formatString: 'SN$currentSequence', currentSequence: 1000, defaultSequence: true).save()
      log.info("Loaded LSNSequence SERIAL")
    }
    return initialDataRecords
  }

  /**
   * Returns the order name sequence that is flagged as the default.
   * @return The sequence that is the default code.
   */
  static LSNSequence findDefaultSequence() {
    // Note: This is functionally the same as the CodeSequence method, but we need one here to limit
    //       the search to just LSNSequences.
    findByDefaultSequence(true)
  }

}
