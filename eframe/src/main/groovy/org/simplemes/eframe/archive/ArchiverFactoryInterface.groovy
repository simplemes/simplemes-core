package org.simplemes.eframe.archive

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the interface for a Archiver Factory.  This produces an archive instance needed to archive
 * one or more domain objects (e.g. to a file).
 */
interface ArchiverFactoryInterface {

  /**
   * Builds an archiver for a single use.
   * @return The archiver.
   */
  ArchiverInterface getArchiver()

}
