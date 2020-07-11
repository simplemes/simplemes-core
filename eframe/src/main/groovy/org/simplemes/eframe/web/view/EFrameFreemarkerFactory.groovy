/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.view

import freemarker.template.Configuration
import io.micronaut.context.env.Environment
import io.micronaut.views.ViewsConfiguration
import io.micronaut.views.freemarker.FreemarkerViewsRendererConfiguration
import io.micronaut.views.freemarker.FreemarkerViewsRendererConfigurationProperties
import org.simplemes.eframe.web.ui.webix.freemarker.FreemarkerDirectiveConfiguration

/**
 * Defines a freemarker factory to create the configuration for the enterprise framework.
 */
@SuppressWarnings("unused")
//@Replaces(FreemarkerViewsRenderer)
// Originally in a .java file, but now @replaces works better in Gradle tests when in a .groovy file/
// For some reason, the embedded server startup in Gradle misses these .java beans with @Replaces.
// TODO: Delete
class EFrameFreemarkerFactory /*extends FreemarkerViewsRenderer*/ {
  EFrameFreemarkerFactory(ViewsConfiguration viewsConfiguration, FreemarkerViewsRendererConfigurationProperties freemarkerConfiguration) {
    //super(viewsConfiguration, freemarkerConfiguration)
  }

  /**
   * Constructs a freemarker configuration.
   *
   * @param freemarkerConfiguration The freemarker configuration properties
   * @param viewsConfiguration The views configuration
   * @param environment The environment
   * @return The freemarker configuration
   */
  //@Singleton
  //@Replaces(Configuration.class)
  Configuration getConfiguration(FreemarkerViewsRendererConfiguration freemarkerConfiguration,
                                 ViewsConfiguration viewsConfiguration,
                                 Environment environment) {
    Configuration configuration = new Configuration(freemarkerConfiguration.getIncompatibleImprovements())
    FreemarkerDirectiveConfiguration.addSharedVariables(configuration)
    configuration.setClassLoaderForTemplateLoading(environment.getClassLoader(), "/" + viewsConfiguration.getFolder())
    return configuration
  }

}
