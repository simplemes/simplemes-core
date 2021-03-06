/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.jar.JarFile
import java.util.regex.Pattern

/**
 * Utility to scan the class path for folder with resources from all modules (.jar files) and class path directories.
 * Supports only the format: 'folder/*.ext'.
 *
 */
@CompileStatic
@Slf4j
class ClassPathScanner {

  /**
   * The factory to build a class path scanner.  This allows tests to provide specific results
   * from the scan() method.
   */
  static ClassPathScannerFactory factory = new ClassPathScannerFactory()

  /**
   * The path format to search for.  Supports only the format: 'folder/*.ext'.
   */
  String searchSpec

  /**
   * The Matching pattern for the given search spec.
   */
  Pattern searchPattern

  /**
   * The classloader to use
   */
  ClassLoader classLoader = getClass().getClassLoader()

  /**
   * Builds a scanner for the given search spec.
   * @param searchSpec The path format to search for.  Supports only the format: 'folder/*.ext'.
   */
  ClassPathScanner(String searchSpec) {
    this.searchSpec = searchSpec
    def s = searchSpec?.replace('*', '[\\w/]+')   // reports/[\w/]+.jxrml$
    //searchPattern = ~"reports/[\\w/]+.jrxml\$"
    searchPattern = ~"$s\$"

    if (!searchSpec?.contains('/')) {
      throw new IllegalArgumentException("Search Spec '$searchSpec' is not valid.  Must contains a '/'.")
    }

  }

  /**
   * Scans the classpath for matching resources.
   *
   * @return The resource URLs or relative paths.
   */
  List scan() {
    def res = []

    def resources = classLoader.getResources(getRootPath())
    for (r in resources) {
      res.addAll(scanResource(r))
    }

    log.trace("scan(): res = {}", res)
    return res
  }

  /**
   * Returns the root path for the desired resources.
   * @return The root path. 'folder/*.ext' is converted to 'folder'.
   */
  String getRootPath() {
    def idx = searchSpec.lastIndexOf('/')
    return searchSpec[0..idx - 1]
  }

  /**
   * Scans a single resource for sub-elements that match the pattern.
   * @param url The resource to check.
   * @return Any sub-folder resources found.
   */
  List scanResource(URL url) {
    def res = []
    log.trace("scanResource(): url = {}", url)

    if (url.protocol == 'file') {
      res.addAll(scanFileFolder(new File(url.path)))
    } else if (url.protocol == 'jar') {
      res.addAll(scanJarFolder(new File(getJarFilePath(url))))
    }

    return res
  }

  /**
   * Scan a file folder, including sub-folders.
   * @param folder The folder to check.
   * @return The matching resources found in the folder.
   */
  List scanFileFolder(File folder) {
    def res = []
    for (f in folder.listFiles()) {
      if (f.directory) {
        res.addAll(scanFileFolder(f))
      } else {
        def url = f.toURI().toURL()
        def match = match(url)
        if (match) {
          res << match
        }
      }
    }

    return res
  }

  /**
   * Scan a JAR file folder, including sub-folders.
   * @param url The URL of the jar file to check.
   * @return The matching resources found in the folder.
   */
  List scanJarFolder(File file) {
    def res = []
    def jarFile = new JarFile(file)
    for (entry in jarFile.entries()) {
      if (match(entry.toString())) {
        res << buildJarFileUrl(file, entry.toString())
      }
    }

    return res
  }

  /**
   * Builds a jar file URL for the given jar file and path.
   * @param file The .jar file.
   * @param path The
   * @return
   */
  URL buildJarFileUrl(File file, String path) {
    return new URL("jar:${file.toURI()}!/$path")
  }

  /**
   * Gets the file name for the given JAR file URL.
   * @param url The URL of the jar file to get.
   * @return The path for the file.
   */
  String getJarFilePath(URL url) {
    def fileName = url.path
    def idx = fileName.indexOf('!')
    if (idx > 0) {
      fileName = fileName[0..(idx - 1)]
    }
    fileName = fileName - 'file:'

    return fileName
  }

  /**
   * Returns the portion of the given URL matches the desired search spec.
   * @param url The URL to check.
   * @return The matching portion.
   */
  String match(URL url) {
    return match(url.toString())
  }

  /**
   * Returns the portion of the given URL matches the desired search spec.
   * @param url The string form of URL to check.
   * @return The matching portion.
   */
  String match(String url) {
    def matches = searchPattern.matcher(url)
    if (matches.count == 1) {
      return matches[0]
    }
    return null
  }

}

/**
 * A simple factory to build a class path scanner.
 */
class ClassPathScannerFactory {

  /**
   * Builds a scanner.
   * @param path The path for the scanner.
   * @return The scanner.
   */
  ClassPathScanner buildScanner(String path) {
    return new ClassPathScanner(path)
  }

}