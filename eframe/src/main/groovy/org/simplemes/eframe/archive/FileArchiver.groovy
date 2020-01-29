/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.archive


import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.domain.ArchiveLog
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.exception.MessageBasedException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.json.TypeableMapper
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.misc.FileFactory
import org.simplemes.eframe.misc.FileUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.misc.TypeUtils

import javax.inject.Singleton

/**
 * Archives one or more domain objects to a file and removes them from the database.
 */
@Slf4j
@Singleton
class FileArchiver implements ArchiverInterface {
  // TODO: Implement filename checker to prevent absolute paths and relative paths above current folder ( too many ..).

  /**
   * The JSON File the domain object is written to.
   */
  protected File jsonFile

  /**
   * The writer used to write the JSON to the file.
   */
  protected Writer writer

  /**
   * The relative path for the XML file.  Can be used to unarchive the records in the future.
   */
  protected String fileReference

  /**
   * The list of domain objects to delete when successfully finished writing the archive.
   */
  protected List objectsToDelete = []

  /**
   * The name of the key field of the first domain object.
   */
  protected String keyName

  /**
   * Set to true if the archive file has been verified.
   */
  boolean verified = false

  /**
   * Adds the given object to the archive.
   * This method can be called several times.
   * @param domainObject The domain object to archive.
   */
  @Override
  void archive(Object domainObject) {
    ArgumentUtils.checkMissing(domainObject, 'domainObject')
    if (!DomainUtils.instance.isDomainEntity(domainObject.class)) {
      throw new IllegalArgumentException("Cannot archive a ${domainObject.class}.  It is not a Hibernate domain class.")
    }

    // Make sure the domain has been saved and has a record in the db.
    if (domainObject.uuid == null) {
      //error.128.message=The domain {0} {1} has no UUID.  This record must be saved before it can be processed.
      throw new MessageBasedException(128, [TypeUtils.toShortString(domainObject), domainObject.class.simpleName])
    }

    // Write the object(s)
    def first = (jsonFile == null)
    try {
      if (jsonFile == null) {
        keyName = DomainUtils.instance.getPrimaryKeyField(domainObject.class)
        setupFile(domainObject)
        TypeableMapper.instance.start(writer)
      }
      TypeableMapper.instance.writeOne(writer, domainObject, first)
      objectsToDelete += domainObject
      // Now, look for any related objects.
      //println "domainObject = ${domainObject.class.declaredMethods}"
      for (o in DomainUtils.instance.findRelatedRecords(domainObject)) {
        // Just follow normal archive for each related object.
        archive(o)
      }
    } catch (Exception e) {
      // Make sure the file is deleted if the archive failed.
      if (writer) {
        //println "closing writer = $writer"
        writer.close()
        if (jsonFile.exists()) {
          jsonFile.delete()
          //println "deleted jsonFile = $jsonFile, res = ${res}"
        }
      }
      throw e
    }
  }

  /**
   * Builds an archiver for a given domain object.  The domain object is used to name the archive file.
   * This archive will delete the domain object(s) from the database upon successful writing of the file.
   * @param domainObject The domain object to archive.
   *
   */
  protected void setupFile(Object domainObject) {
    String top = determineArchiveDirectory()

    // Build the file name by starting with a core portion and checking for another existing file.
    def baseRef = makeArchiveRefBase(domainObject)
    //println "baseRef = ${baseRef}"
    // Build the directory to write to.
    FileUtils.createArchiveDirsIfNeeded("${top}/${baseRef}")

    // Now, create the real file name, looping as needed if it already exists.
    def ref
    def count = 0
    while (true) {
      if (count > 0) {
        ref = "${baseRef}-${count}.arc"
      } else {
        ref = "${baseRef}.arc"
      }
      jsonFile = FileFactory.instance.newFile(FileUtils.convertToOSPath("${top}/${ref}"))
      if (!jsonFile.exists()) {
        break
      }
      count++
    }

    writer = jsonFile.newWriter()
    fileReference = ref
  }

  /**
   * Closes the current archive and removes the archived records from the database.
   * @return The archive reference for the saved archive (e.g. a file for the FileArchiver).
   */
  @Override
  String close() {
    TypeableMapper.instance.finish(writer)
    writer.close()

    // Verify the archive if desired.
    if (Holders.configuration.archive.verify) {
      verifyArchive(fileReference)
      verified = true
    }

    // Now, delete the objects since the .arc appears to be Ok.
    //println "Parent.list()1 = ${Parent.list()}"
    for (o in objectsToDelete) {
      o.delete()
      //noinspection GroovyAssignabilityCheck
      log.trace("Deleting Archived Object {} {}", o.class.simpleName, TypeUtils.toShortString(o))
    }
    //println "Parent.list()2 = ${Parent.list()}"
    if (log.debugEnabled) {
      log.debug("Archived (file: $jsonFile.path) ${LogUtils.limitedLengthList(objectsToDelete)}")
    }
    // for Debug, write the file to console.
    //xmlFile.eachLine {println "${it}" }
    //xmlFile.close()

    // Now, log the archive to a long-term log file for historical reasons.
    if (Holders.configuration.archive.log) {
      def domainObject = objectsToDelete[0]
      ArchiveLog archiveLog = new ArchiveLog()
      archiveLog.recordUUID = domainObject.uuid
      archiveLog.className = domainObject.class.name
      archiveLog.archiveReference = fileReference.take(FieldSizes.MAX_PATH_LENGTH)
      if (keyName) {
        archiveLog.keyValue = domainObject[keyName]?.toString()?.take(FieldSizes.MAX_KEY_LENGTH)
      }
      archiveLog.save()
    }
    // Make sure any later calls won't affect the data from this call.
    objectsToDelete = []
    jsonFile = null
    return fileReference
  }

  /**
   * Cancels the archive action.  Removes the archive (file) and does not delete the object archived.
   */
  @Override
  void cancel() {
    if (writer) {
      writer.close()
    }
    if (jsonFile) {
      jsonFile.delete()
    }

    // Make sure any later calls won't affect the data from this call.
    objectsToDelete = []
    jsonFile = null

  }

  /**
   * Unarchive the given file and save all of the domain objects in the archive.
   * @param ref The file reference provided by the archive process.
   * @param save If true, then the unarchived records will be automatically saved by the unarchive logic.
   * @return A list of domain objects that were created.
   */
  Object[] unarchive(String ref, Boolean save = true) {
    ArgumentUtils.checkMissing(ref, 'ref')
    def res = []

    // Parse the JSON from the file.
    def reader = null
    String fName = 'unknown archive file'
    try {
      fName = makePathFromReference(ref)
      File file = FileFactory.instance.newFile(fName)
      reader = file.newReader()
      def list = TypeableMapper.instance.read(reader)
      for (o in list) {
        if (save) {
          if (!o.validate()) {
            //error.104.message=Could not process {0} due to error {1}
            throw new BusinessException(104, [fName, GlobalUtils.lookupValidationErrors(o)])
          }
          o.save()
        }
        res << o
      }

      if (log.debugEnabled) {
        log.debug("Archive: Unarchived (ref: $ref) ${LogUtils.limitedLengthList(res)}")
      }
    } catch (Exception e) {
      if (e.toString().contains(fName)) {
        // File name is in the error, so just re-throw it
        throw e
      } else {
        // Make sure the file name is in the error.
        //error.104.message=Could not process {0} due to error {1}
        throw new BusinessException(104, [fName, e])
      }
    } finally {
      // Make sure input stream is closed.
      // This is needed since exceptions in XML parsing will leave the file open.
      reader?.close()
    }

    return res
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
  String makeArchiveRefBase(Object domainObject) {
    // Find the 'normal' name of the given class.
    ArgumentUtils.checkMissing(domainObject, 'domainObject')
    def objectName = NameUtils.toDomainName(domainObject.class)

    // Use the date details as replaceable parameters.
    def c = new GregorianCalendar()

    // Build a map for parameter replacements.
    String folderFormat = Holders.configuration.archive.folderName
    String fileFormat = Holders.configuration.archive.fileName

    //  Work around bug in config file for $
    // Combine the folder and file in the ref GString.
    def refFormat = folderFormat.replace('#', '$') + '/' + fileFormat.replace('#', '$')
    def params = [:]
    params.year = c.get(Calendar.YEAR)
    params.month = String.format('%02d', c.get(Calendar.MONTH) + 1)
    params.day = String.format('%02d', c.get(Calendar.DAY_OF_MONTH))
    params.hour = String.format('%02d', c.get(Calendar.HOUR_OF_DAY))
    params.now = c
    params.key = TypeUtils.toShortString(domainObject)
    params.object = domainObject
    params."${objectName}" = domainObject

    //println "refFormat = $refFormat"
    return TextUtils.evaluateGString(refFormat, params)
  }

  /**
   * Creates a archive file's path name using the given reference.  This is only useful for
   * file archives.
   *
   * @param ref The archive reference returned by the archive() method.
   * @return The path where the archive file is located.
   */
  static String makePathFromReference(String ref) {
    String s = determineArchiveDirectory() + File.separator + ref
    return s.replace('/', File.separator)
  }

  /**
   * Creates a archive file's reference from the given path name.  This is only useful for
   * file archives.
   *
   * @param path The path that the archive is stored in.
   * @return The reference used for the archive.
   */
  @SuppressWarnings("ParameterReassignment")
  static String makeReferenceFromPath(String path) {
    def prefix = determineArchiveDirectory() + File.separator
    prefix = prefix.replace(File.separator, '/')
    path = path.replace(File.separator, '/')
    return path - prefix
  }

  /**
   * Determines the archive main directory from the configuration element: org.simplemes.eframe.archive.top.
   * @return The directory (blank string if not found).
   */
  static String determineArchiveDirectory() {
    def top = Holders.configuration.archive.topFolder
    log.trace('determineArchiveDirectory: archive directory = {}', top)
    return top
  }

  /**
   * Verify that the given archive file was written correctly and contains valid XML.  Just checks
   * the XML format.
   * @param ref The file reference to check.
   */
  void verifyArchive(String ref) {
    // Parse the XML from the file.
    String fName = makePathFromReference(ref)
    def reader = null
    try {
      File file = FileFactory.instance.newFile(fName)
      reader = file.newReader()
      //println "reader = $reader"
      TypeableMapper.instance.read(reader)
    } finally {
      reader?.close()
    }
  }

  /**
   * Gets a list of archive file references that exist in the active archive area (folder).
   * @return The list of file references.
   */
  List<String> findAllArchives() {
    def topFolder = FileFactory.instance.newFile(determineArchiveDirectory())
    def list = []
    return findAllArchives(topFolder, list)
  }

  /**
   * Gets a list of archive file references that exist in the active archive area (folder).
   * @return The list of file references.
   */
  protected List<String> findAllArchives(File folder, List list) {
    for (file in folder.listFiles()) {
      if (file.directory) {
        findAllArchives(file, list)
      } else {
        list << makeReferenceFromPath(file.path)
      }
    }
    return list
  }


}
