package org.simplemes.eframe.test

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A mock File that can be used to write text to string for later use.
 * The file reader will rely on the text written to the mock file by previous calls to the
 * writer.
 */
@SuppressWarnings("UnusedMethodParameter")
class MockFile extends File {

  /**
   * The path.
   */
  String path

  /**
   * The writer to use for this mock file.
   */
  Writer writer

  /**
   * Set to true if the delete() method was called.
   */
  boolean deleted = false

  /**
   * Set to true if the mock file exists (maybe something written).
   */
  boolean fileExists = false

  /**
   * The list of Mocked files in this mocked file.  If given, then this is assumed to be a directory.
   */
  List files

  /**
   * The simulated contents of the file.
   */
  String contents

  /**
   * The factory that created this mock file.  Used to control how many times exists() should return true.
   */
  MockFileFactory factory

  /**
   * Dummy constructor.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  MockFile() {
    super('')
  }

  /**
   * Creates a new <code>File</code> instance by converting the given
   * pathname string into an abstract pathname.  If the given string is
   * the empty string, then the result is the empty abstract pathname.
   *
   * @param pathname A pathname string
   * @throws NullPointerException*          If the <code>pathname</code> argument is <code>null</code>
   */
  MockFile(String path, Writer writer, MockFileFactory factory) {
    super(path)
    this.path = path
    this.writer = writer
    this.factory = factory
    if (factory.writesShouldFail) {
      // Provide a writer that always fails
      //this.writer = {Object[] args -> fileExists=true; throw new IOException('Disk full')} as Writer
      this.writer = [
        write: { Object[] args -> fileExists = true; throw new IOException('Disk full') },
        close: { Object[] args -> },  // Does nothing so the writer can be closed without error.
      ] as Writer
      // And pretend the file exists so the delete() method will be called
    }
  }

  /**
   * Tests whether the file or directory denoted by this abstract pathname
   * exists.
   */
  @Override
  boolean exists() {
    // Allow the first n calls to return true.
    factory.existCountTrue--
    return factory.existCountTrue >= 0 || fileExists
  }

  /**
   * Builds a new writer (uses the provided writer).
   * @param codec Ignored.
   * @return The writer.
   */
  Writer newWriter(String codec) {
    return writer
  }

  /**
   * Builds a new writer (uses the provided writer).
   * @return The writer.
   */
  Writer newWriter() {
    return writer
  }

  /**
   * Builds a new reader for the mock file.  Uses the string from the provided writer.
   * @param codec Ignored.
   * @return The reader.
   */
  Reader newReader() {
    if (contents) {
      return new StringReader(contents)
    } else {
      return new StringReader(writer.toString())
    }
  }

  /**
   * Builds a new reader for the mock file.  Uses the string from the provided writer.
   * @param codec Ignored.
   * @return The reader.
   */
/*
  Reader newReader(String codec) {
    if (contents) {
      return new StringReader(contents)
    } else {
      return new StringReader(writer.toString())
    }
  }
*/

  /**
   * Does nothing.
   */
  @Override
  boolean delete() {
    deleted = true
    return true
  }

  /**
   * Mock method to simulate a real directory, when needed.
   */
  @Override
  boolean isDirectory() {
    return files?.size() > 0
  }

  /**
   * Mock method to simulate a directory (when needed).
   */
  @Override
  File[] listFiles() {
    def res = []
    for (String file in files) {
      def s = file
      if (!file.startsWith(path)) {
        s = "$path/$file"
      }
      res << factory.newFile(s)
    }
    return res
  }
}
