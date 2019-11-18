package org.simplemes.eframe.archive

import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ArchiverFactorySpec extends BaseSpecification {

  static specNeeds = [EMBEDDED]

  def "verify that the default FileArchiver is returned"() {
    when: 'the archiver is triggered'
    def archiver = Holders.applicationContext.getBean(ArchiverFactoryInterface)

    then: 'the right archiver is started'
    archiver.archiver instanceof FileArchiver
  }

  def "verify that the default archiver can be overridden"() {
    given: 'the configuration is set to use the custom archiver'
    Holders.configuration.archive.archiver = DummyArchiver.name

    when: 'the archiver is triggered'
    def archiver = Holders.applicationContext.getBean(ArchiverFactoryInterface)

    then: 'the right archiver is started'
    archiver.archiver instanceof DummyArchiver

    cleanup:
    Holders.configuration.archive = new EFrameConfiguration.Archive()
  }

  def "verify that the a bad Archiver is detected"() {
    given: 'the configuration is set to use the custom archiver'
    Holders.configuration.archive.archiver = String.name

    when: 'the archiver is triggered'
    Holders.applicationContext.getBean(ArchiverFactoryInterface).archiver

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['cast', 'string', 'ArchiverInterface'])

    cleanup:
    Holders.configuration.archive = new EFrameConfiguration.Archive()
  }

  /**
   * A dummy archiver for test.
   */
  class DummyArchiver implements ArchiverInterface {
    /**
     * Adds the given object to the archive.
     * This method can be called several times.
     * @param object The domain object to archive.
     */
    @Override
    void archive(Object object) {

    }

    /**
     * Closes the current archive and removes the archived records from the database.
     * @return The archive reference for the saved archive (e.g. a file for the FileArchiver).
     */
    @Override
    String close() {
      return null
    }

    /**
     * Cancels the archive action.  Removes the archive (file) and does not delete the object archived.
     */
    @Override
    void cancel() {

    }

    /**
     * Unarchive the given file and save all of the domain objects in the archive.
     * @param ref The file reference provided by the archive process.
     * @param save If true, then the unarchived records will be automatically saved by the unarchive logic.
     * @return A list of domain objects that were created.
     */
    @Override
    Object[] unarchive(String ref, Boolean save) {
      return new Object[0]
    }

    /**
     * Build the archive basic reference name.  This is a dynamic value that is intended to avoid
     * filling up a directory with .arc files.  The object's id can be used with this reference base
     * to make a legal path name.<p>
     * This is a relative path name from the archive topFolder location.
     * When used with {@link org.simplemes.eframe.archive.FileArchiver#makePathFromReference(String)}, the
     * actual file path can be found.
     * @param domainObject The domain object to build the archive file name from.
     * @return The base reference.
     */
    @Override
    String makeArchiveRefBase(Object domainObject) {
      return null
    }
  }

}

