package org.simplemes.eframe.misc

import org.simplemes.eframe.i18n.GlobalUtils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Some general-purpose number utilities.  Provides large number formatting and other utils.
 *
 */
class NumberUtils {

  /**
   * Build a human-readable large number with M, K, G, etc.  3,543,725 is displayed as '3.1M' and 54,543,725 is displayed as '54M'.
   * Makes sure at least 2 significant digits are displayed if the whole number is <10.
   * Supports K (1000), M, M, G, T (tera).  This method does <b>not</b> round to nearest whole number and uses the
   * 'normal' definition of kilo, mega, etc (K=1000,M=1000000, etc).
   * @param value The value to display the shortened readable number.
   * @param locale The locale to use for display (<b>Default:</b> Request Locale, or Locale.default).
   * @return The string. (e.g. 3,543,725 is returned as '3.1M').
   */
  static String formatLargeNumberReadable(long value, Locale locale = null) {
    // Calculate the actual values with decimals for the units we support.
    BigDecimal k = value / 1000.0
    BigDecimal m = value / 1000000.0
    BigDecimal g = value / 1000000000.0
    BigDecimal t = value / 1000000000000.0

    // Figure out which is the lowest unit that is greater than 1.0
    String key = null
    def displayValue = value
    if (t >= 1.0) {
      key = 'number.tera.label'
      displayValue = t
    } else if (g >= 1.0) {
      key = 'number.giga.label'
      displayValue = g
    } else if (m >= 1.0) {
      key = 'number.mega.label'
      displayValue = m
    } else if (k >= 1.0) {
      key = 'number.kilo.label'
      displayValue = k
    }

    if (key) {
      // Get the precision and decimal place correct.
      if (displayValue > 10) {
        displayValue = displayValue.setScale(0, BigDecimal.ROUND_DOWN)
      } else {
        displayValue = displayValue.setScale(1, BigDecimal.ROUND_DOWN)
      }
      displayValue = formatNumber(displayValue, locale)
      return GlobalUtils.lookup(key, GlobalUtils.getRequestLocale(locale), [displayValue] as Object[])
    }
    return value.toString()
  }

  /**
   * Build a human-readable form of the given Number with the specified locale.
   * @param value The value to format.
   * @param locale The locale to use for formatting (<b>Default:</b> Request Locale, or Locale.default).
   * @param groupingUsed True if the grouping should be used on large numbers (<b>Default:</b> true).
   * @return The string. (e.g. 3.1 in German is '3,1').
   */
  static String formatNumber(Number value, Locale locale = null, boolean groupingUsed = true) {
    def nf = DecimalFormat.getInstance(GlobalUtils.getRequestLocale(locale))
    nf.setGroupingUsed(groupingUsed)
    return nf.format(value)
  }

  /**
   * Parses a human-readable form of the given Number with the specified locale.
   * @param stringValue The value to format.
   * @param locale The locale to use for parsing (<b>Default:</b> Request Locale, or Locale.default).
   * @return The string. (e.g. 3.1 in German is '3,1').
   */
  static Number parseNumber(String stringValue, Locale locale = null) {
    def nf = DecimalFormat.getInstance(GlobalUtils.getRequestLocale(locale))
    nf.setGroupingUsed(true)  // Always allow grouping (thousands)
    nf.setParseBigDecimal(true)
    return nf.parse(stringValue)
  }

  /**
   * Returns the decimal separator character for a locale.
   * @param locale The locale to use for parsing (<b>Default:</b> Request Locale, or Locale.default).
   * @return The separator character.
   */
  static String determineDecimalSeparator(Locale locale = null) {
    return DecimalFormatSymbols.getInstance(GlobalUtils.getRequestLocale(locale)).decimalSeparator
  }

  /**
   * Returns the grouoping (thousands) separator character for a locale.
   * @param locale The locale to use for parsing (<b>Default:</b> Request Locale, or Locale.default).
   * @return The separator character.
   */
  static String determineGroupingSeparator(Locale locale = null) {
    return DecimalFormatSymbols.getInstance(GlobalUtils.getRequestLocale(locale)).groupingSeparator
  }

  /**
   * Returns true if the class is a number class (primitive ot sub-class of Number).
   * @param clazz The class to test.
   * @return True if a number.
   */
  static boolean isNumberClass(Class clazz) {
    if (Number.isAssignableFrom(clazz)) {
      return true
    }
    return (clazz == long || clazz == int || clazz == short)
  }

  /**
   * Divides the top by the bottom, round up for any fraction.
   * @param top The top.
   * @param bottom The bottom.
   * @return The rounded up result.
   */
  static int divideRoundingUp(int top, int bottom) {
    //noinspection GroovyAssignabilityCheck
    int res = top / bottom
    if (top % bottom) {
      res++
    }

    return res
  }

  /**
   * Determines if the given value is a number string.  Treats ',' and '.' as always valid.
   * @param s The string to test.
   */
  static boolean isNumber(String s) {
    if (!s) {
      return false
    }
    return s ==~ /[0-9\.,]*/

  }

  /**
   * Trims the trailing zeros, after the decimal character.
   * @param s The string to trim the trailing zeros from.
   * @param locale The locale to use for parsing (<b>Default:</b> Request Locale, or Locale.default).
   * @return The trimmed value.
   */
  static String trimTrailingZeros(String s, Locale locale = null) {
    def decimal = '' + DecimalFormatSymbols.getInstance(GlobalUtils.getRequestLocale(locale)).decimalSeparator
    if (s?.contains(decimal)) {
      while (s.endsWith('0')) {
        def loc = s.lastIndexOf('0') - 1
        if (s[loc] == decimal) {
          break
        }
        s = s[0..loc]
      }
    }
    return s
  }
}
