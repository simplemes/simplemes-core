package org.simplemes.eframe.archive

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the interface for an archiver.  Provides ways to remove/restore old records from the
 * database.
 */
interface ArchiverInterface {
  /**
   * Adds the given object to the archive.
   * This method can be called several times.
   * @param object The domain object to archive.
   */
  void archive(Object object)

  /**
   * Closes the current archive and removes the archived records from the database.
   * @return The archive reference for the saved archive (e.g. a file for the FileArchiver).
   */
  String close()

  /**
   * Cancels the archive action.  Removes the archive (file) and does not delete the object archived.
   */
  void cancel()

  /**
   * Unarchive the given file and save all of the domain objects in the archive.
   * @param ref The file reference provided by the archive process.
   * @param save If true, then the unarchived records will be automatically saved by the unarchive logic.
   * @return A list of domain objects that were created.
   */
  Object[] unarchive(String ref, Boolean save)


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
  String makeArchiveRefBase(Object domainObject)


}
