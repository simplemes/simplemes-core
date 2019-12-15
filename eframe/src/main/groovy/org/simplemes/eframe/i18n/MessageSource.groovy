package org.simplemes.eframe.i18n

import io.micronaut.context.i18n.ResourceBundleMessageSource

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A bean that provides the message bundle access to lookup localized text.
 * Currently finds the localized text from the messages.properties file, using the
 * UTF8 character encoding.
 * <p>
 * This is a thin wrapper on the Spring MessageSource
 */
// TODO: Replace with non-hibernate alternative
@Singleton
class MessageSource extends ResourceBundleMessageSource {
  private static final String PROPERTIES_SUFFIX = ".properties"
  //private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()

  /**
   * Basic constructor.
   */
  MessageSource() {
    super()
    //setDefaultEncoding("UTF-8")
    //setBasenames('classpath*:i18n/messages', 'i18n/messages', 'i18n/sample')
  }

  /**
   * Calculate all filenames for the given bundle basename and Locale.
   * This sub-class supports the use of 'classpath*:i18n/messages' to allow modules to provide properties.
   * This will return a 'jar:' format if the .properties file is found in a .jar file.
   * @param basename the basename of the bundle
   * @param locale the locale
   * @return the List of filenames to check
   */
  protected List<String> calculateAllFilenames(String basename, Locale locale) {
    def filenames = super.calculateAllFilenames(basename, locale)
    def res = []

/*
    for (filename in filenames) {
      if (filename.startsWith(PathMatchingResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)) {
        try {
          Resource[] resources = resolver.getResources(filename + PROPERTIES_SUFFIX)
          for (Resource resource : resources) {
            String sourcePath = resource.getURI().toString().replace(PROPERTIES_SUFFIX, "")
            res << sourcePath
          }
        } catch (IOException ignored) {
        }
      } else {
        res << filename
      }
    }
*/

    return res
  }

}
