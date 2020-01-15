package org.simplemes.eframe.i18n;

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
 */

import org.simplemes.eframe.application.Holders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
@Singleton
public class MessageSource {
  private static final Logger log = LoggerFactory.getLogger(MessageSource.class);

  /* TODO: Delete
  Sources
  PathMatchingResourcePatternResolver.java https://github.com/spring-projects/spring-framework/blob/3a0f309e2c9fdbbf7fb2d348be861528177f8555/spring-core/src/main/java/org/springframework/core/io/support/PathMatchingResourcePatternResolver.java
  AbstractMessageSource.java https://github.com/spring-projects/spring-framework/blob/master/spring-context/src/main/java/org/springframework/context/support/AbstractMessageSource.java
  Caching of bundles https://github.com/spring-projects/spring-framework/blob/master/spring-context/src/main/java/org/springframework/context/support/ResourceBundleMessageSource.java
   */

  /**
   * Cache to hold loaded ResourceBundles.   Stored by base file name, then a map of stored by locale.
   */
  protected Map<String, Map<Locale, ResourceBundle>> cachedResourceBundles;

  /**
   * Cache to hold already generated lookup values from the .properties files.
   */
  protected Map<String, Map<Locale, MessageFormat>> cachedMessageFormats;

  /**
   * The list of bundle base names to use
   */
  protected Collection<String> baseNames;

  /**
   * The control used to load the bundle.
   */
  protected ResourceBundle.Control bundleControl;

  /**
   * A dev/test mode option to clear the caches when the i18n/messages.properties changes.
   */
  protected boolean clearCacheOnFileChanged = false;

  /**
   * Empty constructor.
   */
  public MessageSource() {
    cachedResourceBundles = new ConcurrentHashMap<>();
    cachedMessageFormats = new ConcurrentHashMap<>();

    baseNames = new ConcurrentLinkedQueue<>();
    baseNames.add("i18n/messages");
    baseNames.add("i18n/sample");

    if (Holders.isEnvironmentDev() || Holders.isEnvironmentTest()) {
      clearCacheOnFileChanged = true;
    }
  }

  /**
   * Constructor to add other base names.  Mainly used for testing.
   *
   * @param baseNames The extra base names to search.
   */
  public MessageSource(Collection<String> baseNames) {
    this();
    this.baseNames.addAll(baseNames);
  }

  /**
   * Looks up the given string from the supported resource bundle(s) and formats the arguments into the message for
   * a localized display string.
   *
   * @param key    The look up key.
   * @param locale The locale.  Can be null.
   * @param args   The optional arguments.
   * @return The localized, looked up message with the arguments replaced.  Null if not found.
   */
  String getMessage(String key, Locale locale, Object... args) {
    devModeCheckForChanges();
    if (locale == null) {
      locale = Locale.getDefault();
    }
    MessageFormat format = getMessageFormat(key, locale);

    if (format != null) {
      return format.format(args);
    }

    return null;
  }

  /**
   * Checks gets the message format for the given key/locale.
   * Also caches the MessageFormat if found.
   *
   * @param key    The look up key.
   * @param locale The locale.
   * @return The looked up message with the arguments replaced.
   */
  protected MessageFormat getMessageFormat(String key, Locale locale) {
    MessageFormat res = null;
    Map<Locale, MessageFormat> map = cachedMessageFormats.get(key);
    if (map != null) {
      res = map.get(locale);
    }
    if (res == null) {
      // Not in cache
      map = cachedMessageFormats.get(key);
      if (map == null) {
        map = new ConcurrentHashMap<>();
        cachedMessageFormats.putIfAbsent(key, map);
      }

      // Now, find the msg/pattern to store in the cache
      String s = lookupKey(key, locale);
      if (s != null) {
        res = new MessageFormat(s);
        cachedMessageFormats.get(key).putIfAbsent(locale, res);
      }
    }

    return res;
  }

  /**
   * Looks up the given string from the supported resource bundle(s).
   *
   * @param key    The look up key.
   * @param locale The locale.  Can be null.
   * @return The localized, looked up text.
   */
  protected String lookupKey(String key, Locale locale) {
    for (String baseName : baseNames) {
      ResourceBundle bundle = getResourceBundle(baseName, locale);
      try {
        return bundle.getString(key);
      } catch (MissingResourceException ignored) {
        // Try next base name for the value.
      }
    }
    return null;
  }


  /**
   * Gets/loads the given resource bundle for the given locale.  Uses the cachedResourceBundles.
   *
   * @param baseName The bundle base name.
   * @param locale   The locale.
   * @return The bundle.
   */
  ResourceBundle getResourceBundle(String baseName, Locale locale) {
    Map<Locale, ResourceBundle> map = cachedResourceBundles.get(baseName);
    if (map != null) {
      ResourceBundle bundle = map.get(locale);
      if (bundle != null) {
        return bundle;
      }
    }
    ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, getBundleControl());
    if (map == null) {
      map = new ConcurrentHashMap<>();
      Map<Locale, ResourceBundle> existing = cachedResourceBundles.putIfAbsent(baseName, map);
      if (existing != null) {
        map = existing;
      }
    }
    map.put(locale, bundle);
    return bundle;
  }


  /**
   * The control used to load the bundle.
   *
   * @return The control used to load the bundle.
   */
  ResourceBundle.Control getBundleControl() {
    if (bundleControl == null) {
      bundleControl = new UTF8Control();
    }
    return bundleControl;
  }

  protected File messagesFile;
  protected long messagesFileLastChanged = 0;

  /**
   * Checks for changes to the i18n/messages.properties file.
   */
  protected void devModeCheckForChanges() {
    if (clearCacheOnFileChanged) {
      if (messagesFile == null) {
        // Need to find the messages file location.
        messagesFile = new File("i18n/messages.properties");
        try {
          Enumeration<URL> urls = getClass().getClassLoader().getResources("i18n/messages.properties");
          while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (url.getProtocol().equals("file")) {
              messagesFile = Paths.get(url.toURI()).toFile();
              messagesFileLastChanged = messagesFile.lastModified();
              break;
            }
          }
        } catch (IOException | URISyntaxException e) {
          log.error("Error reading i18n/messages.properties", e);
        }
      }
      if (messagesFile != null) {
        if (messagesFileLastChanged > 0) {
          long current = messagesFile.lastModified();
          if (current != messagesFileLastChanged) {
            log.info(messagesFile + " changed.  Clearing global resource bundle caches.");
            clearCaches();
          }
        }
      }
    }
  }

  /**
   * Clears the caches for the resource bundles.  Only use in test/dev modes.
   */
  private void clearCaches() {
    cachedResourceBundles = new ConcurrentHashMap<>();
    cachedMessageFormats = new ConcurrentHashMap<>();
    ResourceBundle.clearCache();
  }


  @Override
  public String toString() {
    return "MessageSource{baseNames=" + baseNames + '}';
  }
}


/**
 * Bundle control to load a resource bundle as UTF-8 encoding.
 */
class UTF8Control extends ResourceBundle.Control {
  public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
      throws IOException {
    // The below is a copy of the default implementation.
    String bundleName = toBundleName(baseName, locale);
    String resourceName = toResourceName(bundleName, "properties");
    ResourceBundle bundle = null;
    InputStream stream = null;
    if (reload) {
      URL url = loader.getResource(resourceName);
      if (url != null) {
        URLConnection connection = url.openConnection();
        if (connection != null) {
          connection.setUseCaches(false);
          stream = connection.getInputStream();
        }
      }
    } else {
      stream = loader.getResourceAsStream(resourceName);
    }
    if (stream != null) {
      try {
        // Only this line is changed to make it to read properties files as UTF-8.
        bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
      } finally {
        stream.close();
      }
    }
    return bundle;
  }
}
