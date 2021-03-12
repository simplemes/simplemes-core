/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.web.asset

import groovy.transform.ToString

/**
 * Defines a single asset for the client.
 * Provides fields to get the URL for the asset and create the input stream to read it.
 */
@ToString(includeNames = true, includePackage = false)
class WebClientAsset {
  boolean gzipExists = false
  boolean exists = false
  URL resource
  URL gzipResource
  boolean isDirectory = false
  Long fileSize
  Long gzipFileSize
  Date lastModified

}
