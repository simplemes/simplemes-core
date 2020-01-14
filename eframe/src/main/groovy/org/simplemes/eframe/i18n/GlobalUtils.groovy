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
    def res = null
    try {
      res = getMessageSource().getMessage(key, locale, args)
    } catch (NoSuchMessageException | MissingResourceException ignored) {
    }
    if (!res) {
      res = key
      if (key.contains('.') && Holders.configuration.localizationTest) {
        res = "-==$key==-"
      }
    }
    return res

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
