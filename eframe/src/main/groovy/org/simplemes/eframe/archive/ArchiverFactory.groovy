package org.simplemes.eframe.archive

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.TypeUtils

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Returns the configured Archiver.  Can be controlled by the configuration option
 */
@Singleton
class ArchiverFactory implements ArchiverFactoryInterface {

  /**
   * Builds an archiver for a single use.
   * @return The archiver.
   */
  @Override
  ArchiverInterface getArchiver() {
    def s = Holders.configuration.archive.archiver
    def clazz = TypeUtils.loadClass(s)
    return (ArchiverInterface) clazz.newInstance()
  }
}
