/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.test

import groovy.transform.ToString
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils

/**
 * A support class for GUI tests that uses the client sub-module's localization files for lookups.
 * <p/>
 * <b>Note: </b>Not supported for production.
 */
@ToString(includePackage = false, includeNames = true, includes = ['map'])
class WebClientLookup {

  /**
   * If true, then all client modules' locales have been loaded.
   */
  static boolean loadedJSFiles = false

  /**
   * The cached lookups.  Contains one entry for each language.  Each entry is a list of lookups.
   */
  static Map<String, List<WebClientLookup>> cache = [:]

  static String fallbackLanguage = 'en'

  /**
   * The list of default folders to check for locales.
   * Use addLocaleFolder() to add client-specific locales.
   */
  static List<String> localeFolders = ["src/client/eframe/src/eframe-lib/locales"]


  /**
   * The map for this .js file.
   */
  Map map

  /**
   * The name of the file this lookup content was extracted from.
   */
  String jsFileName

  WebClientLookup(String jsFileName) {
    this.jsFileName = jsFileName
    if (Holders.environmentProduction) {
      throw new IllegalAccessException('This class is not supported in production')
    }
    map = parseFile(jsFileName)
  }


  /**
   * Parses the given JS file for message definitions.
   * @param fileName The .js file name.
   * @return The parsed values.
   */
  protected Map parseFile(String fileName) {
    def file = new File(fileName)
    def line
    def inGetMessagesBlock = false
    def currentNestingDepth = 0
    def prefixes = []
    def map = [:]
    file.withReader('UTF-8') {
      while ((line = it.readLine()) != null) {
        if (line.contains('return {')) {
          inGetMessagesBlock = true
        } else {
          if (inGetMessagesBlock) {
            if (line.contains('{')) {
              // Check for block nesting start.
              def blocks = line.tokenize(': ') as List
              if (blocks.size() == 2) {
                currentNestingDepth++
                prefixes.push(blocks[0])
              }
            } else if (line.contains('}')) {
              // Check for block nesting end.
              if (currentNestingDepth) {
                currentNestingDepth--
                prefixes.pop()
              }
            } else if (line.contains(':')) {
              def (String shortKey, String value) = parseLine(line as String)
              def key = prefixes.join('.') + '.' + shortKey
              map[key] = value
            }
          }
        }
      }
    }

    return map
  }

  /**
   * Parses the given text line into valid key/value pair.  Supports format valid for Javascript object entry.
   * @param line The line to parse.
   * @return A Tuple with the key/value.  Can be null.
   */
  protected Tuple2<String, String> parseLine(String line) {
    def key = null, value = null

    def colonLoc = line.indexOf(':')
    if (colonLoc && colonLoc < line.size()) {
      key = line[0..(colonLoc - 1)].trim()
      def s = line[(colonLoc + 1)..-1]?.trim()
      if (s) {
        def quote = s[0]
        if (quote == '"' || quote == "'") {
          def quoteEndLoc = s.lastIndexOf(quote)
          if (quoteEndLoc > 0) {
            s = s[1..(quoteEndLoc - 1)]
            value = s
          }
        }
      }
    }

    return [key, value]
  }

  /**
   * Finds the given key from the locale-specific resources.  Uses the current locale.
   * @param key The lookup key.
   * @return The string.
   */
  protected String findString(String key) {
    return map[key]
  }

  /**
   * Loads the locales from the client sub-module(s).
   */
  static void loadLocales() {
    if (loadedJSFiles) {
      return
    }
    for (s in localeFolders) {
      for (file in new File(s).listFiles()) {
        def locale = determineLocale(file.path)
        if (cache[locale.language] == null) {
          cache[locale.language] = []
        }
        cache[locale.language] << new WebClientLookup(file.path)
      }
    }
    loadedJSFiles = true
  }

  /**
   * Convenience method for general client localization lookup.
   * <p/>
   * <b>Note: </b>Not supported for production.
   * <p>
   * <b>Note:</b> This lookup will flag missing .properties entries.
   * @param key The key to lookup.  If it starts with '*', then the return value will start with a '*'.
   *            The lookup will take place without the '*'.  This is used to support required field labels.
   * @param locale The locale to use for the message. (<b>Default</b>: Request Locale)
   * @param args The replaceable arguments used by the message (if any).
   * @return The looked up message.
   */
  @SuppressWarnings('unused')
  static String lookup(String key, Locale locale = null, Object... args) {
    loadLocales()
    if (locale == null && GlobalUtils.defaultLocale) {
      locale = GlobalUtils.defaultLocale
    }
    def prefix = ''
    if (key.startsWith('*')) {
      key = key - '*'
      prefix = '*'
    }
    String lang = locale?.language ?: Locale.default.language
    for (lookup in cache[lang]) {
      String value = lookup?.findString(key)
      if (value) {
        return prefix + value
      }
    }

    // Now, try the fallback language
    for (lookup in cache[fallbackLanguage]) {
      String value = lookup?.findString(key)
      if (value) {
        return prefix + value
      }
    }

    return "${key}.not.found"
  }

  /**
   * Determines the locale for the given file.  Uses the base file name.
   * @param fileName The file name.
   * @return The locale.  Uses default if not able to determine the file's locale.
   */
  static protected Locale determineLocale(String fileName) {
    def locale = Locale.default
    fileName = fileName.replace(File.separator, '/')
    def loc = fileName.lastIndexOf('/')
    if (loc) {
      def s = fileName[(loc + 1)..-1] - '.js'
      locale = Locale.forLanguageTag(s) ?: Locale.default
    }
    return locale
  }

  /**
   * Adds the locale folder to the web client lookup.  Use this when the lookups needed are not in the
   * the default list (e.g. not in "src/client/eframe/src/eframe-lib/locales").
   * @param folder The folder to add.
   */
  static addLocaleFolder(String folder) {
    if (!localeFolders.contains(folder)) {
      loadedJSFiles = false
      localeFolders << folder
    }
  }

}
