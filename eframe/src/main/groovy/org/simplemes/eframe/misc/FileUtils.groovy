package org.simplemes.eframe.misc

import org.simplemes.eframe.exception.BusinessException


/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Some general-purpose file utilities.  Provides OS-independent naming and directory creation mechanisms.
 * <p/>
 * Original Author: mph
 *
 */
@SuppressWarnings(["JavaIoPackageAccess"])
class FileUtils {
  /**
   * Converts the OS-independent path to OS-dependent.  Mostly just converts '/' to the appropriate File.separator.
   *
   * @param path The OS-independent path.
   * @return The OS-dependent path.
   */
  static String convertToOSPath(String path) {
    return path.replace('/', File.separator)
  }

/**
 * Create the dynamic Archive directory.  Creates sub-directories as needed.
 * Assumes last part of the path is a file name.
 *
 * @param path The path for the file name.
 */
  @SuppressWarnings(['FactoryMethodName', 'BuilderMethodWithSideEffects', "ParameterReassignment"])
  static void createArchiveDirsIfNeeded(String path) {
    if (!path) {
      throw new IllegalArgumentException("dir is null")
    }
    // Strip trailing file separator if needed.
    if (path.endsWith('/')) {
      path = path[0..path.length() - 1]
    }

    // Now, strip last element since that is a file name (usually).
    def lastSlashIdx = path.lastIndexOf('/')
    if (lastSlashIdx) {
      path = path[0..lastSlashIdx]
    }

    File f = new File(path)
    if (!f.exists()) {
      if (!f.mkdirs()) {
        //error.103.message=Error creating directory {0}
        throw new BusinessException(103, [path])
      }
    }
  }

  /**
   * Resolved the relative path elements ('..') to the specific path.  This handles a single
   * '..' to remove the previous element.  Only supports a single '..' ('../..' is <b>not</b> supported).
   * @param path The path.
   * @return The effective path.  Only supports '/' as a path separator).
   */
  static String resolveRelativePath(String path) {
    def res = path

    while (res?.contains('../')) {
      // 'abc/def/../ghi'
      //     i    j
      def j = res.indexOf('../')
      if (j == 0) {
        // Can't fix paths that start with a '../'
        break
      }
      def beginning = res[0..(j - 2)]
      def i = beginning.lastIndexOf('/')

      res = beginning[0..i] + res[(j + 3)..-1]
    }


    return res
  }

}
