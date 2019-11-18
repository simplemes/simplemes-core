package org.simplemes.eframe.reports

import net.sf.jasperreports.engine.JRPropertiesMap
import net.sf.jasperreports.engine.export.JRHyperlinkProducer
import net.sf.jasperreports.engine.export.JRHyperlinkProducerFactory
import net.sf.jasperreports.extensions.ExtensionsRegistry
import net.sf.jasperreports.extensions.ExtensionsRegistryFactory

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * This is the factory and registry to register the HyperLinkProducer for our reports.  This will point to the
 * hyperlink producer to generate links to other reports.
 */
class HyperlinkProducerFactory extends JRHyperlinkProducerFactory implements ExtensionsRegistryFactory, ExtensionsRegistry {
  /**
   * Returns the hyperlink producer associated with a specific hyperlink type.
   *
   * @param linkType the hyperlink type
   * @return an associated hyperlink producer, or <code>null</code> when none associated
   */
  @Override
  JRHyperlinkProducer getHandler(String linkType) {
    //println "linkType = $linkType"
    return new HyperlinkProducer()
  }

  /**
   * Instantiates an extensions registry.
   *
   * @param registryId the ID of the registry to instantiate.
   * The ID can be used to identify a set of properties to be used
   * when instantiating the registry.
   * @param properties the map of properties that can be used to configure
   * the registry instantiation process
   * @return an extensions registry
   */

  @Override
  ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties) {
    //println "registryId = $registryId, properties = $properties"
    return this
  }

  /**
   * Returns a list of extension objects for a specific extension type.
   *
   * @param extensionType the extension type
   * @return a list of extension objects
   */
  @Override
  <T> List<T> getExtensions(Class<T> extensionType) {
    if (extensionType == JRHyperlinkProducerFactory) {
      //println "extensionType2 = $extensionType"
      return [this]
    } else {
      return []
    }
  }
}
