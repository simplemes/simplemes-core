package org.simplemes.eframe.web.view

import freemarker.template.Configuration
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.env.Environment
import io.micronaut.views.ViewsConfiguration
import io.micronaut.views.freemarker.FreemarkerFactory
import io.micronaut.views.freemarker.FreemarkerViewsRendererConfiguration
import org.simplemes.eframe.web.ui.webix.freemarker.FreemarkerDirectiveConfiguration

import javax.inject.Singleton


/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
 */

/**
 * Defines a freemarker factory to create the configuration for the enterprise framework.
 */
@SuppressWarnings("unused")
@Factory
@Replaces(FreemarkerFactory.class)
// Originally in a .java file, but now @replaces works better in Gradle tests when in a .groovy file/
// For some reason, the embedded server startup in Gradle misses these .java beans with @Replaces.
class EFrameFreemarkerFactory extends FreemarkerFactory {
  /**
   * Constructs a freemarker configuration.
   *
   * @param freemarkerConfiguration The freemarker configuration properties
   * @param viewsConfiguration The views configuration
   * @param environment The environment
   * @return The freemarker configuration
   */
  @Singleton
  @Replaces(Configuration.class)
  Configuration getConfiguration(FreemarkerViewsRendererConfiguration freemarkerConfiguration,
                                 ViewsConfiguration viewsConfiguration,
                                 Environment environment) {
    Configuration configuration = new Configuration(freemarkerConfiguration.getIncompatibleImprovements())
    FreemarkerDirectiveConfiguration.addSharedVariables(configuration)
    configuration.setClassLoaderForTemplateLoading(environment.getClassLoader(), "/" + viewsConfiguration.getFolder())
    return configuration
  }

}
