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
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.numbering.CodeSequenceTrait

import javax.persistence.Column

/**
 * Defines the sequence used to generate new lot/serial numbers (LSN).
 * <p/>
 * The default LSN naming sequence is 'SERIAL'.
 *
 */

@Slf4j
@MappedEntity('lsn_sequence')
@DomainEntity
@ToString(includeNames = true, includePackage = false)
class LSNSequence implements CodeSequenceTrait {

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

  /**
   * The custom field holder.  Max size: {@link FieldSizes#MAX_CUSTOM_FIELDS_LENGTH}
   */
  @ExtensibleFieldHolder
  @Column(length = FieldSizes.MAX_CUSTOM_FIELDS_LENGTH, nullable = true)
  @SuppressWarnings("unused")
  String customFields

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

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
  static Map<String, List<String>> initialDataRecords = [LSNSequence: ['SERIAL']]

  /**
   * Loads initial naming sequence(s).  Default sequence is:
   * <ul>
   *   <li> SERIAL - <b>format:</b> <i>'SN$currentSequence'</i></li>
   * </ul>
   *
   */
  static Map<String, List<String>> initialDataLoad() {
    LSNSequence s
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
    // Note: This is functionally the same as the CodeSequenceTrait method, but we need one here to limit
    //       the search to just LSNSequences.
    findByDefaultSequence(true)
  }

}
