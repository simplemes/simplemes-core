package org.simplemes.eframe.misc

import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainRefListFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.FieldFormatFactory
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.date.ISODate

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Miscellaneous utility methods for generating/using client-side javascript.
 */
class JavascriptUtils {

  /**
   * Formats the value for use in a standard javascript object block (e.g. : var o =&#123;name: 'value'&#125;).
   * This includes using quotes when needed and escaping HTML/Quotes inside of strings.
   * @param value The value to format.
   * @param format The field format.
   * @return The formatted string suitable for an object definition.  Can be null if value is null.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  static String formatForObject(Object value, FieldFormatInterface format) {
    if (value == null) {
      return null
    }
    switch (format) {
      case IntegerFieldFormat:
      case LongFieldFormat:
      case BigDecimalFieldFormat:
        return "${value.toString()}"
      case BooleanFieldFormat:
        return "${value.toString()}"
      case DateOnlyFieldFormat:
      case DateFieldFormat:
        return "\"${ISODate.format(value)}\""
      case DomainRefListFieldFormat:
      case DomainReferenceFieldFormat:
      case EnumFieldFormat:
      case EncodedTypeFieldFormat:
        def s = format.encode(value, null)
        return "\"${escapeForJavascript(s)}\""
      default:
        return "\"${escapeForJavascript(value.toString())}\""
    }
  }


  /**
   * Escape the string for safe use inside of a Javascript string variable.
   * This escapes (less than + forward slash) and double quotes.
   * @param value The value to escape.
   * @param labelMode If true, then this is in label mode.  This means '<' will be converted to '&lt;' to work
   *                  around an issue with display labels in the toolkit.
   * @return The escaped value.
   */
  static String escapeForJavascript(String value, Boolean labelMode = false) {
    if (value == null) {
      return ''
    }
    //return value?.replaceAll('/', '\\\\/')?.replaceAll('"', '\\\\"')
    def res = value?.replaceAll('</', '<\\\\/')?.replaceAll('"', '\\\\"')
    if (labelMode) {
      // For some reason, the escaped values as a label works different from a value in an editable field.
      // The editable field should not escape '<' or the field will show the '&lt;' in the input field.
      // But the label mode (readOnly) needs to escape it.
      res = res?.replaceAll('<', '&lt;')
    }

    return res
  }

  /**
   * Escape the HTML string for safe use inside of a Javascript string variable.
   * This escapes all script tags and double quotes.
   * @param value The value to escape.
   * @return The escaped value.
   */
  static String escapeHTMLForJavascript(String value) {
    if (value == null) {
      return ''
    }
    //def res = value?.replaceAll('</', '<\\\\/')?.replaceAll('"', '\\\\"')
    def res = value?.replaceAll('<[sS][cC][rR][iI][pP][tT]>', '&lt;script&gt;')?.replaceAll('"', '\\\\"')
    res = res?.replaceAll('</[sS][cC][rR][iI][pP][tT]>', '&lt;/script&gt;')

    return res
  }

  /**
   * Builds a Javascript code fragment from the list of maps with all of the properties.
   * @param list The list of maps to convert.
   * @return The code to build the object array in JS format.
   */
  static String buildJavascriptObject(List<Map> list) {
    StringBuilder sb = new StringBuilder()
    for (Map map in list) {
      if (sb) {
        sb << "\n,"
      }
      sb << '{'
      map.each { k, v ->
        def format = FieldFormatFactory.build(v.getClass())
        sb << "\"$k\":${formatForObject(v, format)},"
      }
      sb << '}'
    }


    return "[${sb.toString()}]"
  }

  /**
   * Formats the given string as a Javascript string, with multi-line support and double quote escaping.
   * @param s The input string.
   * @return The string literal for use in Javascript (e.g. '"abc"').
   */
  static String formatMultilineHTMLString(String s) {
    def sb = new StringBuilder()

    def lines = s.tokenize('\n\r')
    for (line in lines) {
      if (sb) {
        sb << "+\n"
      }
      sb << '"'
      sb << escapeHTMLForJavascript(line)
      sb << '"'
    }

    return sb.toString()
  }
}
