package org.simplemes.mes.numbering

import org.simplemes.eframe.misc.TextUtils
import org.simplemes.mes.demand.domain.OrderSequence

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the code sequence numbering policy for generating an object code (primary key fields) on the shop floor.
 * These are typically used for Orders and LSNs.
 * This trait provides the common logic for most sequences.
 * Each concrete implementation of the sequence (e.g. LSNSequence) will need some fields defined:
 *
 *  <h3>Required Data Fields</h3>
 * Each concrete implementation of the sequence (e.g. LSNSequence) will need some fields defined:
 * <ul>
 *   <li><b>title</b> - The title (short description) of this object.  This is usually visible users. </li>
 *   <li><b>formatString</b> - The Format string.  Supports these replaceable parameters such as $currentSequence </li>
 *   <li><b>currentSequence</b> - The current sequence.  Incremented as value strings are generated. </li>
 *   <li><b>defaultSequence</b> - If true, then this is the default sequence to use if one is not specified. </li>
 * </ul>
 *
 */
trait CodeSequenceTrait {
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
  List<String> formatValuesInternal(int nValues, Map params = null) {
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
    save()

    return res
  }

  /**
   * Format one or more values using the current sequence.  This method locks the sequence record and then performs
   * the actual format.
   * @param params Optional list of parameters to be used in the formatting.
   * @return The formatted next number
   */
  private static List<String> formatValuesStatic(CodeSequenceTrait sequence, int nValues, Map params = null) {
    sequence = OrderSequence.findByUuidWithLock(sequence.uuid)
    return sequence.formatValuesInternal(nValues, params)
  }


}