package org.simplemes.eframe.test
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * General test support utilities.  These methods make common checks of unit tests easier.
 * <p/>
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
 */
class UnitTestUtils {

  /**
   * A date (milliseconds) useful for unit tests.  2010-06-15
   */
  public static final long SAMPLE_DATE_ONLY_MS = 1276560000000L

  /**
   * A DateOnly ISO string for a specific date.
   */
  public static final String SAMPLE_ISO_DATE_ONLY_STRING = "2010-06-15"

  /**
   * A date/time (milliseconds) useful for unit tests.  Feb 13 18:31:30.456 EST 2009
   */
  public static final long SAMPLE_TIME_MS = 1234567890456L

  /**
   * A Date ISO string for a specific date/time.  Feb 13 18:31:30.456 EST 2009
   */
  public static final String SAMPLE_ISO_TIME_STRING = "2009-02-13T23:31:30.456Z"

  /**
   * A date/time (milliseconds) with no fractions of a second, useful for unit tests.  Jun 14 21:34:38 EST 2010
   */
  public static final long SAMPLE_TIME_NO_FRACTION_MS = 1276565678000L

  /**
   * A Date ISO string for a specific date/time with no fractions.   Jun 14 21:34:38 EST 2010
   */
  public static final String SAMPLE_ISO_TIME_NO_FRACTION_STRING = "2010-06-15T01:34:38.000Z"


  /**
   * Sample unicode strings useful for testing Unicode support in a key field.
   */
  static final UNICODE_KEY_TEST_STRING = "\u00C0\u0370\u0400\u0530\u3040\u3300"
/*
  static final UNICODE_ACCENT_UPPER_A = "\u00C0"
  static final UNICODE_ACCENT_LOWER_A = "\u00E0"
  static final UNICODE_GREEK_UPPER_A = "\u0391"
  static final UNICODE_GREEK_LOWER_A = "\u03B1"
  static final UNICODE_RUSSIAN_UPPER_A = "\u0410"
  static final UNICODE_RUSSIAN_LOWER_A = "\u0430"
  // Chinese/Japanese/Korean set
  static final UNICODE_CJK1 = "\u4E10"
  static final UNICODE_CJK2 = "\u4F11"

  static
  final UNICODE_LOWER_STRING = "abc" + UNICODE_GREEK_LOWER_A + UNICODE_ACCENT_LOWER_A + UNICODE_RUSSIAN_LOWER_A + UNICODE_CJK1 + UNICODE_CJK2
  static
  final UNICODE_MIXED_STRING = "Abc" + UNICODE_GREEK_LOWER_A + UNICODE_ACCENT_UPPER_A + UNICODE_RUSSIAN_LOWER_A + UNICODE_CJK1 + UNICODE_CJK2
  static
  final UNICODE_UPPER_STRING = "ABC" + UNICODE_GREEK_UPPER_A + UNICODE_ACCENT_UPPER_A + UNICODE_RUSSIAN_UPPER_A + UNICODE_CJK1 + UNICODE_CJK2

*/

  /**
   * This method tests that the given exception's string value has all replaceable parameters replaced with a value.
   * @param e The exception.
   * @return True if all replaceable parameters have values.
   */
  static boolean allParamsHaveValues(Throwable e) {
    def s = e.toString()
    return !(s =~ /\{\d\}/)
  }

  /**
   * A convenience method to make sure the given exception has the right values.
   * @param e The exception.
   * @param expectedStrings These strings should be in the exception's toString() result.  Case insensitive check.
   * @param values The values to check.
   * @param errorCode If a BusinessException, then this is the expected code.  Optional.
   * @return true
   */
  static boolean assertExceptionIsValid(Throwable e, List<String> expectedStrings, Integer errorCode = 0) {
    assert allParamsHaveValues(e)
    assertContainsAllIgnoreCase(e, expectedStrings)
    if (errorCode) {
      assert e.code == errorCode
    }
    return true
  }

  /**
   * Tests if the given value is within the range of the given target.
   * @param value The value.
   * @param target The target.
   * @param valueName The name of the value for the message (optional).
   * @param range The range (<b>Default</b>: 1.0).
   */
  static assertClose(Number value, Number target, String valueName = '', Number range = 1.0) {
    //noinspection GroovyAssignabilityCheck
    assert Math.abs(value - target) <= range, "Value(${valueName}) $value is not in range of $target +- ($range)"
    return true
  }

  /**
   * Tests if the given value is within the range of the given target.
   * @param value The value.
   * @param target The target.
   * @param label The label to use in the assertion error message (e.g. 'Dialog Width').
   * @param tolerance The tolerance amount (<b>Default:</b> 10).
   */
  static assertClose(int value, int target, String label, int tolerance = 10) {
    def rangeString = "(${target - tolerance}..${target + tolerance})"
    assert Math.abs(value - target) <= tolerance, "'$label' ($value) is not in range of $rangeString"
    return true
  }

  /**
   * Asserts that the given string contains all of the values (in any order).
   * Comparison is case insensitive.
   * @param testString The string to check for the contained values.
   * @param values The values to check.
   * @return true if contains all.  Will throw an assertion exception otherwise.
   */
  static boolean assertContainsAllIgnoreCase(Object testString, List<String> values) {
    def testStringLC = testString.toString().toLowerCase()
    for (s in values) {
      assert testStringLC.contains(s.toLowerCase())
    }
    return true
  }

  /**
   * Verify that the given date/time is within a certain tolerance (2 seconds) of the current time.
   * This is typically used in tests of code that use the current date, but the test can't get the exact date/time for
   * comparison.
   * @param date The date to check (<b>Required</b>).
   * @param tolerance The tolerance for the check (milliseconds).(<b>Default: </b> 2000 (1sec))
   * @return True if the time is within 2 seconds.
   */
  static boolean dateIsCloseToNow(Date date, Long tolerance = 2000) {
    return compareDates(date, new Date(), tolerance)
  }

  /**
   * Verify that the given date/time is recent (within 2 seconds) of the given time.
   * This is typically used in tests of code that use the current date, but the test can't get the exact date/time for
   * comparison.
   * @param date The date to check (<b>Required</b>).
   * @param compareDate The date to compare to (<b>Required</b>).
   * @param tolerance The tolerance for the check (milliseconds).(<b>Default: </b> 2000 (2sec))
   * @return True if the time is within 2 seconds.
   */
  static boolean compareDates(Date date, Date compareDate, Long tolerance = 2000) {
    def millis = date?.time ?: 0
    def compareMillis = compareDate?.time ?: 0
    return Math.abs(millis - compareMillis) < tolerance
  }

}
