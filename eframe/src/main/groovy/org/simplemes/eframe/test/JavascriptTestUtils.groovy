/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import groovy.util.logging.Slf4j

import javax.script.Compilable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import java.text.ParseException

/**
 * Some utilities for testing generated Javascript code.  Basic testing only.
 * Uses the Java built-in ECMAScript engine (Rhino).
 *
 */
@Slf4j
class JavascriptTestUtils {

  /**
   * Checks the given page text for all &lt;script&gt; sections and compiles them in Javascript.
   * The ECMAScript engine throws parsing exceptions when the javascript is badly formed.
   * <p/>
   * If no &lt;script&gt; sections, then this method does nothing.
   * @param page The HTML Page with &lt;script&gt; tags.  Must be paired.
   */
  @SuppressWarnings('CatchThrowable')
  static boolean checkScriptsOnPage(String page) {
    //println "engine = $engine"
      for (script in extractScriptsFromPage(page)) {
        checkScript(script)
      }
    return true
  }

  /**
   * Checks the syntax for the given valid javascript formatting.  This is used on fragments that typically
   * used as values in a variable or in a javascript object created for the GUI toolkit.
   * @param fragment The fragment (e.g. ' { view: "button", label: "label"} '
   */
  @SuppressWarnings('CatchThrowable')
  static boolean checkScriptFragment(String fragment) {
    // Wrap with 'var x =' in case the fragment is meant to be used in an expression.
    return checkScript("var x = $fragment;")
  }

  /**
   * Checks the syntax for the given valid javascript formatting.  This is used on the actual Javascript.
   * @param script The javascript.
   * @return True if passed.
   */
  @SuppressWarnings('CatchThrowable')
  static boolean checkScript(String script) {
    ScriptEngineManager factory = new ScriptEngineManager()
    ScriptEngine engine = factory.getEngineByName("ECMAScript")
    if (engine instanceof Compilable) {
      log.trace('checkScripts() checking {}', script)
      engine.compile(new StringReader(script))
    }
    return true
  }

  /**
   * Extracts the scripts from the given page text.  Finds all &lt;script&gt; sections and returns them as
   * a list of scripts.<p/>
   * Currently does not handle &lt;/script&gt; embedded in a quoted string in the script section.
   * This will cause a parse exception.
   * @param page The HTML Page with &lt;script&gt; tags.  Must be paired.
   * @return the scripts (list).
   */
  static List<String> extractScriptsFromPage(String page) {
    List<String> res = []
    def includeCount = 0

    def start = page.indexOf('<script')
    while (start >= 0) {
      // Figure out if the script is an include (from web location).
      def isInclude = page.startsWith('<script src=', start)

      // Find end of this script section
      start += 8
      def end = page.indexOf('</script>', start)
      if (end < 0) {
        throw new ParseException("Can't find end </script> tag.", start)
      }
      end--
      def s = page[start..end]
      //println "s($isInclude) = $s"
      if (isInclude) {
        includeCount++
      } else {
        res << s
      }
      start = page.indexOf('<script', end)
    }

    // A simple test to find malformed <script></script> pairs.
    // This is meant to catch when </script> is inside of a quoted string in the JS.  This should fail.
    // Count up the </script> elements.  Should equal the number of scripts found.
    def count = 0
    start = page.indexOf('</script>')
    while (start >= 0) {
      count++
      start = page.indexOf('</script>', start + 1)
    }
    def expectedCount = res.size() + includeCount
    if (expectedCount != count) {
      throw new ParseException("Too many($count) </script> tags.  Expected ($expectedCount).  Perhaps a <script> tag is in a quoted string (not supported).", start)
    }

    return res
  }

  /**
   * Extract a single block of javascript that starts with the given text.  Used to find a single block of code
   * that starts with the given text.  For example: find the script code that starts with 'function flexTypeEditor('
   * will find the first occurrence of the string, then finds the first bracket after this text.  Then finds the
   * corresponding end bracket and returns all of this text.
   * <p/>
   * Will fail with an exception if the brackets and quotes are not balanced.  Also does <b>not</b> support comments that have
   * quotes or brackets in the comment.
   * <p>
   * Supports using square brackets ([]) if the startText ends with [.  This will find all of an array's elements.
   * @param javascript The full javascript to search.
   * @param startText The starting text to search from (e.g. 'function flexTypeEditor(').
   * @return The entire block of code, including the start text.
   */
  static String extractBlock(String javascript, String startText) {
    if (!javascript) {
      return null
    }
    // Find the start of the block we are interested in.
    def startIndex = javascript.indexOf(startText)
    if (startIndex < 0) {
      return null
    }
    def bracketStart = '{'
    def bracketEnd = '}'
    // See if the caller wants an array block returned.
    if (startText.endsWith('[')) {
      bracketStart = '['
      bracketEnd = ']'
    }

    // Now, find the end by work through content, until we match the closing bracket.
    int bracketNestingDepth = 0
    boolean beginFound = false
    boolean inSingleQuote = false
    boolean inDoubleQuote = false
    int length = javascript.length()
    int i = startIndex
    // Check all characters after the starting text
    while (i < length) {
      def c = javascript[i]
      if (c == '\\') {
        // Skip the escaped character.
        i++
      }
      if (inSingleQuote || inDoubleQuote) {
        // See if the quote is terminated.
        if (c == "'") {
          inSingleQuote = false
        } else if (c == '"') {
          inDoubleQuote = false
        }
      } else {
        if (c == bracketStart) {
          bracketNestingDepth++
          beginFound = true
        } else if (c == bracketEnd) {
          bracketNestingDepth--
          if (beginFound && bracketNestingDepth == 0) {
            // Found closing bracket so return it all
            return javascript[startIndex..i]
          }
        } else if (c == "'") {
          inSingleQuote = true
        } else if (c == '"') {
          inDoubleQuote = true
        }
      }
      i++
    }
    // No bracket found or mismatched brackets, so return null.
    throw new IllegalArgumentException("Mismatched quotes or brackets.  If JavascriptUtils.checkScriptsOnPage() passes, then check comments with quotes on them.")
  }

  /**
   * The characters that are possible in a non-quoted value for the extractProperty() logic.
   */
  static final String NON_QUOTED_CHARACTERS = '0123456789true.false'

  /**
   * The characters are used to end a property value.
   */
  static final String PROPERTY_END_CHARACTERS = ',}'

  /**
   * Extracts the given property from a javascript segment.   Returns the value.
   * For example, extractProperty('{name: "ABC"}','name') will return "ABC" (without the quotes).
   * <p>
   * This is not a very flexible method.  It only supports simple quotes and does not allow escaped quotes in the value string.
   * @param page The javascript text for the page.
   * @param name The property name.
   * @return The value (not including quotes).
   */
  static String extractProperty(String page, String name) {
    if (!page) {
      return null
    }
    def loc = page.indexOf("$name:")
    if (loc >= 0) {
      loc += name.size() + 1
      // Skip past the space after the colon
      if (page[loc] == ' ') {
        loc++
      }
      if (loc >= page.size()) {
        return null
      }
      def quoteChar = page[loc]
      if (quoteChar == '"' || quoteChar == "'") {
        def endLoc = page.indexOf(quoteChar, loc + 1)
        if (endLoc > 0) {
          def res = page[(loc + 1)..(endLoc - 1)]
          if (res.size() > 0) {
            if (res == '""' || res == "''") {
              return ''
            }
            return res
          } else {
            return ''
          }
        }
      } else {
        StringBuilder sb = new StringBuilder()
        // No quote, so just return up to the next comma/bracket or EOS
        while (loc < page.size()) {
          def s = page[loc]
          if (PROPERTY_END_CHARACTERS.contains(s)) {
            // found next element, so return this.
            return sb.toString().trim()
          }
          sb << s
          loc++
        }
        // EOS found, so return this.
        return sb.toString()
      }
    }
    return null
  }


}
