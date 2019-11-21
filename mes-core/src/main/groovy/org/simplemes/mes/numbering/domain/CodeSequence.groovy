package org.simplemes.mes.numbering.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Slf4j
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.mes.misc.FieldSizes

/**
 * Defines the code sequence numbering policy for generating object code (primary key fields) on the shop floor.
 * These are typically used for Orders and LSNs.
 *
 */
@Slf4j
@Entity
@EqualsAndHashCode(includes = ['sequence'])
abstract class CodeSequence {
  // *******************************************************************
  //
  // Properties
  //
  // *******************************************************************

  /**
   * The primary key for this sequence.
   */
  String sequence

  /**
   * The title (short description) of this object.  This is usually visible users.
   */
  String title

  /**
   * The Format string.  Supports these replaceable parameters:
   *
   * $currentSequence
   */
  String formatString = 'SN$currentSequence'
  /**
   * The current sequence.
   */
  long currentSequence = 1

  /**
   * If true, then this is the default sequence to use if one is not specified.
   * Only used by concrete sub-classes.
   */
  boolean defaultSequence = false


  static constraints = {
    sequence(maxSize: FieldSizes.MAX_CODE_LENGTH, unique: true, nullable: false, blank: false)
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, nullable: true)
    formatString(maxSize: FieldSizes.MAX_LONG_STRING_LENGTH)
  }

  // *******************************************************************
  //
  // Business methods (public, stable methods)
  //
  // *******************************************************************

  /**
   * Format the current number for test/display purposes.  Does not increment the current sequence or other properties.
   * @see #formatValues
   * @param params Optional list of parameters to be used in the formatting.
   * @return The formatted next number
   */
  String formatTest(Map params = null) {
    def fullParams = [currentSequence: currentSequence, date: new Date()]
    if (params) {
      fullParams.putAll(params)
    }

    return TextUtils.evaluateGString(formatString, fullParams)
  }

  /**
   * Format one or more values using the current sequence.  Updates the current sequence as needed.
   * <p/>
   * <b>Note:</b> For locking reasons, this method discards the original Sequence instance, so the caller should not rely on it.
   * @param nValues The number of new numbers to generate.
   * @param params Optional list of parameters to be used in the formatting.
   * @return The formatted next number
   */
  List<String> formatValues(int nValues, Map params = null) {
    if (nValues <= 0) {
      throw new IllegalArgumentException("nValues must be > 0")
    }
    // Delegate to the static method so it can discard this instance from the Hibernate cache before locking the record.
    return formatValuesStatic(this, nValues, params)

  }

  /**
   * Private internal format method.  Used to work around record locking issues.
   * <p/>
   * <b>Note:</b> Not to be called by client code under any circumstances.
   * @param nValues The number of new numbers to generate.
   * @param params Optional list of parameters to be used in the formatting.
   * @return The formatted next number
   */
  protected List<String> formatValuesInternal(int nValues, Map params = null) {
    List<String> res = new ArrayList<String>(nValues)
    for (int i = 0; i < nValues; i++) {
      res[i] = formatTest(params)
      setCurrentSequence(currentSequence + 1)
    }
    log.debug('formatValuesInternal(): seq = {}, res = {}', this, res)
    // Temporary test for contention issues.
    //if (debugFlag) {
    //  Thread.sleep(debugFlag)
    //}
    save(flush: false)

    return res
  }

  /**
   * Format one or more values using the current sequence.  This method locks the sequence record and then performs
   * the actual format.
   * @param params Optional list of parameters to be used in the formatting.
   * @return The formatted next number
   */
  private static List<String> formatValuesStatic(CodeSequence sequence, int nValues, Map params = null) {
    def id = sequence.id
    // Force the originally read record to be re-read
    sequence.discard()
    sequence = lock(id)
    // This discard/lock logic avoids the error:
    // ERROR org.hibernate.internal.SessionImpl - HHH000346: Error during managed flush [Batch update returned unexpected row count from update [0]; actual row count: 0; expected: 1]
    return sequence.formatValuesInternal(nValues, params)
  }

  String toShortString() {
    return sequence
  }

}
