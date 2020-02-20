/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.i18n

import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.ArgumentUtils

import javax.inject.Singleton
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.text.MessageFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A bean that provides the message bundle access to lookup localized text.
 * Currently finds the localized text from the messages.properties and similar files, using the
 * UTF8 character encoding.
 * <p>
 * The message key/values are stored in a cache that can be cleared.  This avoids creation of MissingResourceExceptions
 * and reduces access to the raw ResourceBundles.
 * <p>
 * This relies on the normal classpath searches to find all matching .properties files.
 * This works with .jar files too.
 * <p>
 * Portions of the logic were inspired by the Spring framework.
 */
@Slf4j
@Singleton
class MessageSource {

  /**
   * Cache to hold loaded ResourceBundles.   Stored by base file name, then a map stored by locale.
   * The List<ResourceBundle> contains all bundles from all classpath .jar/folders that apply for the
   * given baseName/locale.
   */
  protected Map<String, Map<Locale, List<ResourceBundle>>> cachedResourceBundles

  /**
   * Cache to hold already generated lookup values from the .properties files.
   */
  protected Map<String, Map<Locale, MessageFormat>> cachedMessageFormats

  /**
   * Cache to hold the bundle for each unique URL.  This avoid re-reading the fallback bundles.
   */
  protected Map<URL, ResourceBundle> cachedBundles

  /**
   * The list of bundle base names to use
   */
  protected Collection<String> baseNames

  /**
   * The control used to load the bundle.
   */
  protected ResourceBundle.Control bundleControl

  /**
   * A dev/test mode option to clear the caches when the i18n/messages.properties changes.
   */
  protected boolean clearCacheOnFileChanged = false

  /**
   * Empty constructor.
   */
  MessageSource() {
    cachedResourceBundles = new ConcurrentHashMap<>()
    cachedMessageFormats = new ConcurrentHashMap<>()
    cachedBundles = new ConcurrentHashMap<>()

    baseNames = new ConcurrentLinkedQueue<>()
    baseNames.add("i18n/messages")
    baseNames.add("i18n/sample")

    if (Holders.environmentDev || Holders.environmentTest) {
      clearCacheOnFileChanged = true
    }
  }

  /**
   * Constructor to add other base names.  Mainly used for testing.
   *
   * @param baseNames The extra base names to search.
   */
  MessageSource(Collection<String> baseNames) {
    this()
    this.baseNames.addAll(baseNames)
  }

  /**
   * Looks up the given string from the supported resource bundle(s) and formats the arguments into the message for
   * a localized display string.
   *
   * @param key The look up key.
   * @param locale The locale.  Can be null.
   * @param args The optional arguments.
   * @return The localized, looked up message with the arguments replaced.  Null if not found.
   */
  String getMessage(String key, Locale locale, Object... args) {
    devModeCheckForChanges()
    if (locale == null) {
      locale = Locale.getDefault()
    }
    MessageFormat format = getMessageFormat(key, locale)

    if (format != null) {
      return format.format(args)
    }

    return null
  }

  /**
   * Checks gets the message format for the given key/locale.
   * Also caches the MessageFormat if found.
   *
   * @param key The look up key.
   * @param locale The locale.
   * @return The looked up message with the arguments replaced.
   */
  protected MessageFormat getMessageFormat(String key, Locale locale) {
    MessageFormat res = null
    Map<Locale, MessageFormat> map = cachedMessageFormats.get(key)
    if (map != null) {
      res = map.get(locale)
    }
    if (res == null) {
      // Not in cache
      map = cachedMessageFormats.get(key)
      if (map == null) {
        map = new ConcurrentHashMap<>()
        cachedMessageFormats.putIfAbsent(key, map)
      }

      // Now, find the msg/pattern to store in the cache
      String s = lookupKey(key, locale)
      if (s != null) {
        res = new MessageFormat(s)
        cachedMessageFormats.get(key).putIfAbsent(locale, res)
      }
    }

    return res
  }

  /**
   * Looks up the given string from the supported resource bundle(s).
   *
   * @param key The look up key.
   * @param locale The locale.  Can be null.
   * @return The localized, looked up text.
   */
  protected String lookupKey(String key, Locale locale) {
    for (String baseName : baseNames) {
      List<ResourceBundle> bundles = getResourceBundles(baseName, locale)
      for (bundle in bundles) {
        try {
          return bundle.getString(key)
        } catch (MissingResourceException ignored) {
          // Try next bundle and baseName defined.
        }
      }
    }
    return null
  }


  /**
   * Gets/loads the given resource bundle for the given locale.  Uses the cachedResourceBundles.
   *
   * @param baseName The bundle base name.
   * @param locale The locale.
   * @return The bundle.
   */
  List<ResourceBundle> getResourceBundles(String baseName, Locale locale) {
    Map<Locale, List<ResourceBundle>> map = cachedResourceBundles.get(baseName)
    if (map != null) {
      List<ResourceBundle> bundles = map.get(locale)
      if (bundles != null) {
        return bundles
      }
    }

    List<ResourceBundle> bundles = loadBundles(baseName, locale)
    //ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale,this.class.classLoader, getBundleControl())
    if (map == null) {
      map = new ConcurrentHashMap<>()
      Map<Locale, List<ResourceBundle>> existing = cachedResourceBundles.putIfAbsent(baseName, map)
      if (existing != null) {
        map = existing
      }
    }
    map.put(locale, bundles)
    return bundles
  }


  /**
   * Loads the bundle(s) for the given locale.  This includes the specific locale-based .properties files
   * followed by less-specific files.  Up to the fallback .properties file.
   * @param baseName The bundle base name.
   * @param locale The locale.  Required.
   * @return The list of bundles, in order.  Most specific first (e.g. messages_US.properties,messages.properties).
   */
  List<ResourceBundle> loadBundles(String baseName, Locale locale) {
    ArgumentUtils.checkMissing(locale, 'locale')
    def control = getBundleControl()
    List<URL> urls = []

    // Check for the locale first.
    String resourceName = control.toResourceName(control.toBundleName(baseName, locale), "properties")
    def resources = this.class.classLoader.getResources(resourceName)
    for (resource in resources) {
      urls << resource
    }

    // Next, look for locale with country code
    if (locale.getCountry()) {
      def noCountryLocale = new Locale(locale.language)
      String noCountryName = control.toResourceName(control.toBundleName(baseName, noCountryLocale), "properties")
      resources = this.class.classLoader.getResources(noCountryName)
      for (resource in resources) {
        urls << resource
      }
    }

    // Finally, check the base, no locale .properties file.
    String fallBackResourceName = control.toResourceName(control.toBundleName(baseName, Locale.ROOT), "properties")
    resources = this.class.classLoader.getResources(fallBackResourceName)
    for (resource in resources) {
      urls << resource
    }

    // Load the bundles, some may be cached.
    return loadBundles(urls)
  }

  /**
   * Loads the bundles for the given URL(s).
   * @param urls The list of URLs.
   * @return The bundles.  May be from a cache.
   */
  List<ResourceBundle> loadBundles(List<URL> urls) {
    def res = []

    for (url in urls) {
      if (cachedBundles.get(url)) {
        res << cachedBundles.get(url)
      } else {
        InputStream stream = null
        if (url != null) {
          URLConnection connection = url.openConnection()
          if (connection != null) {
            connection.setUseCaches(false)
            stream = connection.getInputStream()
          }
        }
        if (stream != null) {
          try {
            def bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8))
            res << bundle
            cachedBundles.put(url, bundle)
          } finally {
            stream.close()
          }
        }
      }
    }

    res
  }


  /**
   * The control used to load the bundle.
   *
   * @return The control used to load the bundle.
   */
  ResourceBundle.Control getBundleControl() {
    if (bundleControl == null) {
      bundleControl = new Control()
    }
    return bundleControl
  }

  protected File messagesFile
  protected long messagesFileLastChanged = 0

  /**
   * Checks for changes to the i18n/messages.properties file.
   */
  protected void devModeCheckForChanges() {
    if (clearCacheOnFileChanged) {
      if (messagesFile == null) {
        // Need to find the messages file location.
        messagesFile = new File("i18n/messages.properties")
        try {
          Enumeration<URL> urls = getClass().getClassLoader().getResources("i18n/messages.properties")
          while (urls.hasMoreElements()) {
            URL url = urls.nextElement()
            if (url.getProtocol() == "file") {
              messagesFile = Paths.get(url.toURI()).toFile()
              messagesFileLastChanged = messagesFile.lastModified()
              break
            }
          }
        } catch (IOException | URISyntaxException e) {
          log.error("Error reading i18n/messages.properties", e)
        }
      }
      if (messagesFile != null) {
        if (messagesFileLastChanged > 0) {
          long current = messagesFile.lastModified()
          if (current != messagesFileLastChanged) {
            log.info("{} changed.  Clearing global resource bundle caches.", messagesFile)
            clearCaches()
          }
        }
      }
    }
  }

  /**
   * Clears the caches for the resource bundles.  Only use in test/dev modes.
   */
  private void clearCaches() {
    cachedResourceBundles = new ConcurrentHashMap<>()
    cachedMessageFormats = new ConcurrentHashMap<>()
    cachedBundles = new ConcurrentHashMap<>()
    ResourceBundle.clearCache()
  }


  @Override
  String toString() {
    return "MessageSource{baseNames=$baseNames, clear = $clearCacheOnFileChanged, cache = $cachedResourceBundles, $cachedMessageFormats }"
  }

}


/**
 * Bundle control to expose some internals of the core Control (file naming logic).
 */
class Control extends ResourceBundle.Control {
}
