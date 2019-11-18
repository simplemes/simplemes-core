package org.simplemes.eframe.misc
/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This is a pluggable File object creator.  It is used in production to build a standard Java File
 * object.  In tests, it can be replaced with a MockFileFactory to test using a simple string contents.
 */
@SuppressWarnings("JavaIoPackageAccess")
class FileFactory {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static FileFactory instance = new FileFactory()


  /**
   * The factory method to return a new File() instance for the given path.
   * @param path The path (in local OS format).
   * @return The File instance.
   */
  File newFile(String path) {
    return new File(path)
  }
}
