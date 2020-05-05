/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import groovy.text.GStringTemplateEngine
import groovy.util.logging.Slf4j

/**
 * General-purpose text manipulation utilities.
 */
@Slf4j
class TextUtils {

  /**
   * A generic separator used in some places to concatenate values.  <b>Never</b> use this for parsing of these
   * concatenated strings.  This value is not a reserved value and can actually exist in the key strings themselves.
   * This makes parsing difficult. Value: <b>/ (slash)</b>.
   */
  public static final String VALUE_SEPARATOR = '/'


  /**
   * Builds the default toString() for the object (class and hashcode).
   * @param obj The object.
   * @return The base object toString()
   */
  static String toStringBase(Object obj) {
    if (obj == null) {
      return 'null'
    }
    return "${obj.getClass().name}@${Integer.toHexString(obj.hashCode())}"
  }

  /**
   * Evaluates a given GString with the given parameters.  This is needed because the normal GStrings are limited
   * to the scope of the current method/class.  This would force the user to use strings like "${parameters.day}" instead
   * of the simpler "$day".<p/>
   * This method uses simple replacement logic for simple expressions, but falls back to the slower GStringTemplateEngine
   * approach if the more complex forms are used such as "${object.method()}".
   * <p>
   * <b>Note:</b> The special parameter <code>all</code> is added to the possible parameters for convenience.  It is
   * only added if the parameters does not contain <code>all</code> already.
   * <p>
   * <b>Note:</b> This includes a fix for Spring messages.properties handling of escaped brackets (& #123;).  If no arguments
   * are passed to the message source, then it does not un-escape the &#123;.  This method
   * will replace any '& #123;' with &#123;.
   * @param gString The Groovy String with possible replaceable parameters.
   * @param parameters The parameters to replace this with.
   * @return The replaced string.
   */
  @SuppressWarnings(['GStringExpressionWithinString', "ParameterReassignment"])
  static String evaluateGString(String gString, Map parameters) {
    // First, find all bare variables (e.g. A$day) and replace them with the bracket syntax 'A${$day}'
    gString = gString.replaceAll(/\$([\w.]+)/, /\$\{$0\}/)

    // Now get rid of the $ inside of the ${}.  'A${$day}' becomes 'A${day}'
    gString = gString.replaceAll(/\$\{\$/, /\$\{/)

    // Need to un-escape any brackets that did not get fixed during the lookup.
    if (gString.contains("'")) {
      gString = gString.replaceAll(/'\{'/, /\{/)
      gString = gString.replaceAll(/'\}'/, /\}/)
    }

    // See if the special parameter ${all} is used and should be added.
    if (!parameters.all && gString.contains('${all}')) {
      def allString = parameters.toString()
      parameters.all = allString
    }

    // Now, replace all simple forms of the ${} (e.g. ${day} but not ${day.method()}
    gString = gString.replaceAll(/\$\{(\w+)\}/) { m, k -> fixGStringArgument(parameters[k]) }

    // Now see if we have any complex expressions.  If so, then use the slower (much slower) template approach
    if (gString.contains('$')) {
      def engine = new GStringTemplateEngine()
      gString = engine.createTemplate(gString).make(parameters).toString()
    }

    return gString
  }

  protected static String fixGStringArgument(Object o) {
    if (o == null) {
      return o
    }
    def s = o?.toString()
    s = s.replace('$', '\\$')
    return s
  }

  /**
   * Utility method to find the line in the output with the given text.
   * @param page The total output to the page.
   * @param text The text to search for.  Finds first occurrence.
   * @return The whole line.  Null if not found.
   */
  static String findLine(String page, String text) {
    if (!page) {
      return null
    }
    int firstIdx = page.indexOf(text)
    if (firstIdx < 0) {
      return null
    }

    // Look backwards until we find another new line.
    int startOfLineIdx = firstIdx
    while (startOfLineIdx > 0) {
      if (page[startOfLineIdx] == '\n') {
        startOfLineIdx++
        break
      }
      startOfLineIdx--
    }

    int endOfLineIdx = page.indexOf('\n', startOfLineIdx + 1)
    // might go all way to the end (-1) case.
    return page[startOfLineIdx..endOfLineIdx]
  }

  /**
   * Parses the name/value pairs in a string into a Map.
   *
   * @param s The source.  For example, "required='true' label='ABC' "
   * @return The name/value pairs in a map.
   */
  static Map<String, String> parseNameValuePairs(String s) {
    Map<String, String> res = [:]
    s = s?.trim()
    if (!s) {
      return res
    }
    def done = false
    def loc = 0
    while (!done) {
      // Find end of the current parameter name (=)
      def nameEnd = s.indexOf('=', loc)
      if (nameEnd < 0) {
        throw new IllegalArgumentException("parseNameValuePairs() badly formed name/value pair near($loc). '=' not found. Source = $s")
      }
      def name = s[loc..(nameEnd - 1)]
      loc = nameEnd + 1
      def quote = s[loc]
      // Look for end of quotes string
      loc++
      def valueEnd = s.indexOf(quote, loc)
      if (valueEnd < 0) {
        throw new IllegalArgumentException("parseNameValuePairs() badly formed name/value pair near($loc). End quote not found. Source = $s")
      }

      def value = s[loc..(valueEnd - 1)]
      //println "$name quote = $quote, value = `$value`, loc,end = $loc,$valueEnd"
      if (loc == valueEnd) {
        // Detect empty string case
        value = ''
      }

      res[name] = value

      loc = valueEnd + 1
      done = loc >= s.length()
    }

    return res
  }

  /**
   * Adds a single field to the output buffer, formatted for display.  Enforces the field limit.
   *
   * <h3>Options</h3>
   * <ul>
   *   <li><b>highlight</b> - If true, then highlight the field names with HTML bold.  (<b>default:</b> false) </li>
   *   <li><b>maxLength</b> - The max length of the string.  If adding a field will expand beyond this length,
   *                          then the field will not be added, but a '...' will be added. (<b>default:</b> 100) </li>
   *   <li><b>newLine</b> - If true, then an HTML &lt;br&gt; is added between each name/value pair  (<b>default:</b> false).  </li>
   * </ul>

   *
   * @param sb The output buffer.
   * @param label The field label.
   * @param value The value for the field. If null or empty, then does nothing.
   * @param options The options (see above).
   * @param
   * @param
   */
  static StringBuilder addFieldForDisplay(StringBuilder sb, String label, String value, Map options = null) {
    if (!value) {
      return sb
    }

    def highlightFieldNames = options?.highlight ?: false
    def newLine = options?.newLine ?: false
    def maxLength = (Integer) options?.maxLength ?: 100

    def prefix = highlightFieldNames ? '<b>' : ''
    def suffix = highlightFieldNames ? '</b>' : ''
    def length = sb.length()
    if (length) {
      sb.append(' ')
      if (newLine) {
        sb.append('<br>')
      }
    }
    def s = prefix + label + suffix + ": " + value
    if (length == 0 || (length + s.length()) < maxLength) {
      // Still some room left or there is only one field
      sb.append(s)
    } else {
      // No room left
      sb.append('...')
    }
    return sb
  }
}
