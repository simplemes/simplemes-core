package org.simplemes.eframe.test

import org.xml.sax.ErrorHandler
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Some utilities for testing generated HTML code.  Basic testing only.
 *
 *
 */
class HTMLTestUtils {

  /**
   * Checks the given HTML page text that all tags are balance and properly formed.
   * Uses the XML parser, so the HTML code should be XML-compliant.
   * @param page The HTML Page with the tags.  Can be empty string or null.
   * @param ignoreLines A list of strings used to ignore lines for the HTML check.  This drops the lines that contain this string.
   *                    Used when the HTML contains Javascript that has embedded HTML in the Javascript.
   */
  static boolean checkHTML(String page, List<String> ignoreLines = null) {
    if (page) {
      page = "<XYZ>$page</XYZ>"
      // filter out any lines that should be ignored.
      if (ignoreLines) {
        def lines = page.readLines()
        for (ignoreLine in ignoreLines) {
          // Strip out the lines to be ignored, one at a time.
          lines = lines.findAll() { !it.contains(ignoreLine) }
        }
        page = lines.join('\n')
      }

      // Now, remove any trouble-some values that fail XHTML scrutiny.
      if (page.contains('&nbsp;')) {
        page = page - '&nbsp;'
      }

      def parser = new XmlParser()
      // Create a new error handle to avoid the default System.error.print() of the error.
      parser.setErrorHandler(new ErrorHandler() {
        @Override
        void warning(SAXParseException e) throws SAXException {
        }

        @Override
        void fatalError(SAXParseException e) throws SAXException {
          throw e
        }

        @Override
        void error(SAXParseException e) throws SAXException {
          throw e
        }
      })
      parser.parseText("<XHTML>$page</XHTML>")
    }
    return true
  }

  /**
   * Extract the first HTML tag/content from the input HTML.
   * <p/>
   * Does <b>not</b> support ending HTML brackets (/&gt;) inside of attribute values.
   *
   * @param html The full HTML to search.
   * @param tag The tag search for (e.g. 'input').
   * @param extractToEndTag If true, then extracts to the end tag (e.g. &lt;div&gt; ... &lt;/div&gt;).  If false, then
   *        only extracts the tag text up to the tag start block (up to the first '/&gt;').
   *        Use false for simple tags such as '&lt;input ... /&gt;'.
   *        Use true for begin/end tags such as '&lt;a ... &gt;..&lt;/a&gt;'.
   *        <b>Default:</b> false.
   * @return The entire tag of code.  Can be null.
   */
  static String extractTag(String html, String tag, Boolean extractToEndTag = false) {
    if (!html) {
      return null
    }
    // Find the start of the block we are interested in.
    def startText = tag
    if (!startText.startsWith('<')) {
      startText = '<' + startText
    }
    def startIndex = html.indexOf(startText)
    if (startIndex < 0) {
      return null
    }

    // Now, figure out the raw tag name, dropping any attributes
    def rawTag = tag
    if (rawTag.startsWith('<')) {
      rawTag = rawTag[1..-1]
    }
    if (rawTag.contains(' ')) {
      // Ignore any attributes
      def tokens = rawTag.tokenize(' ')
      rawTag = tokens[0]
    }

    // Figure out the end tag we will be looking for
    def endTag
    if (extractToEndTag) {
      endTag = "</$rawTag>"
    } else {
      endTag = "/>"
    }

    def endIndex = html.indexOf(endTag, startIndex)
    if (endIndex < 0) {
      endIndex = html.indexOf(">", startIndex)
      if (endIndex < 0) {
        return null
      }
      endIndex--
    } else {
      // Make sure we get the whole end tag if asked for
      if (extractToEndTag) {
        endIndex = endIndex + rawTag.length() + 1
      }
    }

    return html[startIndex..endIndex + 1]
  }

  /**
   * Parses the given tag text and determines if it has the given class.
   * @param tag The Tag text (e.g. <div id='ABC' class='field required'>)
   * @param cssClass The CSS class name.
   */
  static void assertTagHasClass(String tagText, String cssClass) {
    assert tagText, "TagText is empty"
    def classLoc = tagText.indexOf('class=')
    assert classLoc > 0, "Tag '$tagText' does not have a class"
    // Find the class string contents
    def quote = tagText[classLoc + 6]
    def quoteEnd = tagText.indexOf(quote, classLoc + 7)
    assert quoteEnd > 0, "Tag '$tagText' does not close the class string with a quote"
    def classString = tagText[classLoc + 6..quoteEnd]
    assert classString.contains(cssClass)
  }

  /**
   * Make sure that the given list of strings occur in the same order in the given text.
   * Used to check order of generate HTML/script.
   * @param text The text to check for order in.
   * @param matchList The list of strings to check for matches.  Should contain 2 or more entries.
   */
  static void assertOrderInText(text, List<String> matchList) {
    def lastIndex = -1
    def lastString = ''
    for (s in matchList) {
      def loc = text.indexOf(s)
      assert loc > lastIndex, "Value '$s' comes before '$lastString'"
      lastIndex = loc
      lastString = s
    }
  }

}
