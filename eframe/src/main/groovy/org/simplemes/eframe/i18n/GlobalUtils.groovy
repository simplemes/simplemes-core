package org.simplemes.eframe.i18n

import io.micronaut.context.exceptions.NoSuchMessageException
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.ArgumentUtils

/**
 * Miscellaneous globalization utils.  Most are static methods for convenience but work with
 * thread context values as needed.
 *
 */
class GlobalUtils {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  //static GlobalUtils instance = new GlobalUtils()

  /**
   * The default locale to use when the request locale is not available.
   */
  static Locale defaultLocale = Locale.default

  /**
   * The run-time message source bean.
   */
  static protected MessageSource messageSource

  /**
   * Convenience method for general message.properties lookup.
   * If the message is not found, then the original key will be returned.  If the configuration option
   * {@link org.simplemes.eframe.application.EFrameConfiguration#localizationTest} is true, then the
   * value returned is "-==${key}==-" for keys with a period.  This will help flag missing properties
   * entries.
   * @param key The key to lookup.
   * @param locale The locale to use for the message. (<b>Default</b>: Request Locale)
   * @param args The replaceable arguments used by the message (if any).
   * @return The looked up message.
   */
  @SuppressWarnings('UnnecessaryGetter')
  static String lookup(String key, Locale locale = null, Object... args) {
    locale = locale ?: getRequestLocale()
    try {
      return getMessageSource().getMessage(key, args, locale)
    } catch (NoSuchMessageException | MissingResourceException ignored) {
      def res = key
      if (key.contains('.') && Holders.configuration.localizationTest) {
        res = "-==$key==-"
      }
      return res
    }
  }

  /**
   * Convenience method to lookup a label and a tooltip based on inputs from parameters.
   * If no tooltip is given, will use the label key as the basis for a tooltip lookup.
   * If no tooltip is given and the generated key is not found, then the tooltip returned is null.
   * @param labelKey The key to lookup the label with (<b>Required</b>).
   * @param tooltipKey The key to lookup the tooltip with with (<b>Optional</b>).
   * @return The looked up values (first is label, second is the tooltip).
   */
  static Tuple2 lookupLabelAndTooltip(String labelKey, String tooltipKey) {
    ArgumentUtils.checkMissing(labelKey, 'labelKey')

    def label = lookup(labelKey)

    def tooltip
    if (tooltipKey) {
      tooltip = lookup(tooltipKey)
    } else {
      // Build the tooltip from the label.
      tooltipKey = labelKey - '.label' + '.tooltip'
      tooltip = lookup(tooltipKey)
      if (tooltipKey == tooltip) {
        // Not found, so don't return the generated tooltip.
        tooltip = null
      }
    }

    return [label, tooltip]
  }

  /**
   * Loops through the validation errors for the domain object and creates the human readable string for display.
   * @param domainObject The domain object with validation errors.
   * @param locale The locale to use for the message.
   * @return The map of field/error values.  This is a map for each field that failed with a list of error codes for each.
   */
  static Map<String, List<String>> lookupValidationErrors(Object domainObject, Locale locale = null) {
    return lookupValidationErrors(domainObject.errors, locale)
  }

  /**
   * Loops through the validation errors for the domain object and creates the human readable string for display.
   * @param errors The errors from the domain object that failed validation.
   * @param locale The locale to use for the message.
   * @return The map of field/error values.  This is a map for each field that failed with a list of error codes for each.
   */
  static Map<String, List<String>> lookupValidationErrors(Map errors, Locale locale = null) {
    locale = locale ?: getRequestLocale()
    def stringsByField = [:]
    for (fieldErrors in errors) {
      for (error in fieldErrors.allErrors) {
        def field = error.hasProperty('field') ? error.field : error.objectName
        def resolved = stringsByField[field]
        if (!resolved) {
          resolved = []
          stringsByField[field] = resolved
        }
        error = enhanceErrorArguments((Object) error)

        def messageSource = getMessageSource()
        //messageSource.setBasename('messages')
        try {
          def s = messageSource.getMessage(error, locale)
          resolved << s
        } catch (NoSuchMessageException ignored) {
          resolved << "${error.toString()} - $field"
        }
      }
    }
    return stringsByField
  }

  /**
   * Enhance the error arguments by adding a short version of the domain class name to the end of the
   * arguments array.  This short name can be used in the error messages.
   * This method also adjusts the first argument by looking up the value in the messages.properties as if
   * it was a field name (e.g. adds '.label' and uses that as the argument, if found).
   * @param error The error to enhance.
   * @return A new error with the argument added.
   */
  // TODO: Replace with non-hibernate alternative
  static Object enhanceErrorArguments(Object error) {
    // Attempt to lookup the first argument as a field label.
    if (error.arguments) {
      def labelKey = error.arguments[0] + '.label'
      def s = lookup(labelKey, null)
      if (s != labelKey) {
        error.arguments[0] = s
      }
    }

    def simpleName = null
    for (arg in error.arguments) {
      if (arg instanceof Class) {
        simpleName = arg.simpleName
      }
    }
    if (!simpleName) {
      return error
    }
    Object[] newArray = new Object[error.arguments.length + 1]
    System.arraycopy(error.arguments, 0, newArray, 0, error.arguments.length)
    newArray[error.arguments.length] = simpleName
/*
    return new FieldError(error.objectName, error.field, error.rejectedValue, error.isBindingFailure(),
                          error.codes, newArray, error.defaultMessage)
*/

  }


  /**
   * Determines the locale to be used from the request.  This relies on the ThreadLocal Spring
   * RequestContextHolder to find the request.
   * @param locale The locale for the various status text values. (<b>Default:</b> Request locale, or Locale.default).
   * @return The request locale, falls back to the system default.
   */
  static Locale getRequestLocale(Locale locale = null) {
    if (locale) {
      // No need to look up a locale.
      return locale
    }
    // Get the current if we can.
    def o = Holders.currentRequest?.locale
    if (o) {
      locale = o.get()
      //println "locale = $locale"
    }
    locale = locale ?: defaultLocale  // Fallback to a fixed locale for unit tests and such.
    return locale
  }

  /**
   * Gets the message source bean (if possible).  Will fallback to a hard-coded instance of the bean (for unit tests).
   * @return The source.
   */
  static protected MessageSource getMessageSource() {
    if (!messageSource) {
      messageSource = Holders.applicationContext?.getBean(MessageSource)
      if (!messageSource) {
        // Fall back to a non-bean copy for unit testing.
        messageSource = new MessageSource()
      }
    }
    return messageSource
  }

  /**
   * Safely call toString() on an object, using the locale if the object supports it.
   * @param o The object to get a string form.
   * @param locale The locale. Defaults to the request locale. (<b>Optional:</b>).
   * @return The string value (localized if supported).  '' if o is null.
   */
  @SuppressWarnings('UnnecessaryGetter')
  static String toStringLocalized(Object o, Locale locale = null) {
    if (o == null) {
      return ''
    }
    // See if the object supports localized toString().
    if (o.metaClass.pickMethod('toStringLocalized', [Locale] as Class[])) {
      return o.toStringLocalized(locale ?: getRequestLocale())
    }
    return o.toString()
  }


}
