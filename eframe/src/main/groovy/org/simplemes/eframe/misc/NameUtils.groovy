/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import io.micronaut.data.model.naming.NamingStrategy

/**
 * Miscellaneous utilities for manipulating names in the application and for the framework.
 * <p/>
 * Original Author: mph
 *
 */
class NameUtils {

  /**
   * This is the string value used for a number of default records that are created on startup.
   * This is '@' to avoid translation and searching problems.
   */
  public static final String DEFAULT_KEY = '@'

  /**
   * Force the first letter to uppercase.  Used mainly for naming conventions in the framework.
   * @param value The string to force the first character to uppercase.
   * @return The string with the first character forced to uppercase.
   */
  static String uppercaseFirstLetter(String value) {
    if (value?.length() > 0) {
      def remainder = ''
      if (value.length() > 1) {
        remainder = value[1..-1]
      }
      value = value[0].toUpperCase() + remainder
    }
    return value
  }

  /**
   * Converts the given simple class name to a domain name convention. (lower-case first letter).
   * Also supports use from controller by removing 'Controller' from the string.
   * @param value The class to adjust to a domain name.
   * @return The domain name (initial lowercase).
   */
  static String toDomainName(Object value) {
    if (!value) {
      return null
    }
    if (value instanceof Class) {
      value = value.simpleName
    }
    return lowercaseFirstLetter(value - 'Controller')
  }

  /**
   * Converts the given field name to a standard column name.  Does not check the @Column annotation.
   * Use the default naming strategy.
   * @param fieldName The field name.
   * @return The column name.
   */
  static String toColumnName(String fieldName) {
    return NamingStrategy.DEFAULT.mappedName(fieldName)
  }

  /**
   * Force the first letter to lowercase.  Used mainly for naming conventions for the framework.
   * @param value The string to force the first character to lowercase.
   * @return The string with the first character forced to lowercase.
   */
  static String lowercaseFirstLetter(String value) {
    if (!value) {
      return value
    }
    def len = value.length()
    return value[0].toLowerCase() + (len > 2 ? value[1..-1] : '')
  }

  /**
   * Force the first word to lowercase.  This will convert a string like 'LSNSequence' to 'lsnSequence'.
   * This is useful with camel-case class names that start with an acronym.
   * @param value The string to force the first word to lowercase.  If all uppercase, the force all letters to lowercase.
   * @return The string with the first word forced to lowercase.  Unchanged in some scenarios
   */
  @SuppressWarnings('UnnecessarySubstring')
  static String lowercaseFirstWord(String value) {
    if (value?.length() > 0) {
      def matches = (value =~ /^[A-Z]*/)
      String startingWord = matches[0]
      if (startingWord) {
        if (startingWord.length() == 1) {
          // Only first character is uppercase, so just shift it.
          value = value[0].toLowerCase(Locale.US) + value.substring(1)
        } else {
          if (startingWord.length() == value.length()) {
            // Whole word is uppercase
            value = value.toLowerCase(Locale.US)
          } else {
            // Lowercase all but last letter (first letter of next word).
            startingWord = startingWord[0..-2]
            value = startingWord.toLowerCase(Locale.US) + value[startingWord.length()..-1]
          }
        }
      }
    }
    return value
  }

  /**
   * Determines if the string has any lowercase characters.  This is used mainly for field/property names, so the language is ignored.
   * @param value The string to check.
   * @return True if if any lowercase values are found.  Numbers and punctuation don't affect this check.
   */
  static boolean hasAnyLowerCase(String value) {
    if (value?.length() > 0) {
      for (int i = 0; i < value.length(); i++) {
        if (value.charAt(i).isLowerCase()) {
          return true
        }
      }
      return false
    }
    return false
  }

  /**
   * Converts a key field value (name) from a domain class to a legal HTML ID.  This allows key field with embedded
   * spaces or invalid characters to be converted to a legal HTML name.  For example, a key field of 'ABC %$#@ 123' will
   * be converted to 'ABC123'.
   * @param name The key field value to be sanitized for an HTML ID usage.
   * @return the sanitized HTML ID.
   */
  static String convertToHTMLID(String name) {
    if (name == null) {
      return null
    }
    return name.replaceAll(~/[\W\s]+/, '')
  }

  /**
   * Determines if the given string is a legal Java identifier.
   * @param id The identifier to check.
   * @return True if legal.
   */
  static boolean isLegalIdentifier(String id) {
    if (!id) {
      return false
    }
    if (!Character.isJavaIdentifierStart(id[0] as Character)) {
      return false
    }
    for (c in id) {
      if (!Character.isJavaIdentifierPart(c as Character)) {
        return false
      }
    }
    return true
  }

  /**
   * Converts a given DB column name to the standard mixed case field name in a domain class.
   * Only works with proper name mapping.
   * @param columnName The DB column name (e.g. 'REPORT_TIME_INTERVAL').
   * @return The field name (e.g. 'reportTimeInterval').
   */
  static String convertFromColumnName(String columnName) {
    if (!columnName) {
      return columnName
    }
    // This is the inverse of data layer's NamingStrategy.  They do not have a method to handle the case
    def s = columnName.toLowerCase()
    def sb = new StringBuilder()
    def underscoreFound = false
    for (c in s) {

      if (c == '_') {
        underscoreFound = true
      } else {
        if (underscoreFound) {
          sb << c.toUpperCase()
          underscoreFound = false
        } else {
          sb << c
        }
      }
    }

    return sb.toString()
  }

  /**
   * Builds a field name for a display version of a field.  This is used to create display versions
   * for list displays and related features.
   * @param fieldName The field name to build a synthetic field name from for the display version of the field.
   * @return The field name (e.g. '_statusDisplay_').
   */
  static String buildDisplayFieldNameForJSON(String fieldName) {
    return "_${fieldName}Display_"
  }

  /**
   * Determines if the given field name is a display field name (e.g. '_statusDisplay_').
   * @param fieldName The field name to determine if it is a display field name.
   * @return The field name (e.g. '_statusDisplay_' is true).
   */
  static boolean isDisplayFieldNameForJSON(String fieldName) {
    return fieldName?.startsWith('_') && fieldName?.endsWith("Display_")
  }

}
