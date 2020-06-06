/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import org.simplemes.eframe.misc.FileFactory

/**
 * A mock FileFactory that can be used to write text to string for later use.
 */
class MockFileFactory extends FileFactory {

  /**
   * The writer.
   */
  StringWriter stringWriter

  /**
   * This is the number of times the exists() method should return true.  Used to simulate
   * file name collisions.
   */
  int existCountTrue = 0

  /**
   * If true, then any writes to the file should fail.
   */
  boolean writesShouldFail = false

  /**
   * The last mock file to be created by this mock factory.
   */
  MockFile lastFileCreated

  /**
   * The simulated file system structure.
   * <p>
   * For Example:
   * <pre>
   * FileArchiver.fileFactory.simulatedFiles = ['../arch'                : ['../arch/2018-03-23'],
   *                                            '../arch/2018-03-23'     : ['ref1.arc', 'ref2.arc']]
   *
   * </pre>
   * This simulates a directory (<code>../arch</code>) with a sub-directory (<code>../arch/2018-03-23</code>)
   * that contains two files (<code>ref1, ref2</code>).
   */
  Map simulatedFiles

  /**
   * The simulated file contents for a simulated file system.
   * <p>
   * For Example:
   * <pre>
   * FileArchiver.fileFactory.simulatedContents["ref1.arc"] = '...'
   *
   * </pre>
   * This uses the given string as the contents of the Mock file <code>ref1.arc</code>.
   */
  Map simulatedContents

  /**
   * Creates a factory.
   */
  MockFileFactory() {
    this(null, 0)
  }

  /**
   * Creates a factory that uses the given string writer to hold the output data.
   * @param stringWriter The writer to hold the output data.
   * @param existCountTrue If >0, then return true for the exist() method on the MockFile for this many times.
   */
  MockFileFactory(StringWriter stringWriter, int existCountTrue = 0) {
    this.stringWriter = stringWriter
    this.existCountTrue = existCountTrue
  }

  /**
   * The factory method to return a new File() instance for the given path.
   * @param path The path (in local OS format).
   * @return The File instance.
   */
  @Override
  File newFile(String path) {
    lastFileCreated = new MockFile(path, stringWriter, this)

    // See if the given file is in the simulated list
    if (simulatedFiles) {
      def simulatedEntry = simulatedFiles[path]
      lastFileCreated.files = (List) simulatedEntry
    }

    // See if the given file has a specific simulated contents
    if (simulatedContents) {
      def key = lastFileCreated.name.replace(File.separatorChar, "/" as char)
      def simulatedContent = simulatedContents[key]
      //println "simulatedContent ${key} = $simulatedContent ${simulatedContents?.keySet()}"
      lastFileCreated.contents = simulatedContent
    }

    return lastFileCreated
  }
}
