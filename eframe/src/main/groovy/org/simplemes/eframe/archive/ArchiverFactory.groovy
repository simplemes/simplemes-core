/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.archive

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.TypeUtils

import javax.inject.Singleton

/**
 * Returns the configured Archiver.  Can be controlled by the configuration option
 */
@Singleton
class ArchiverFactory implements ArchiverFactoryInterface {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static ArchiverFactory instance = new ArchiverFactory()

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
