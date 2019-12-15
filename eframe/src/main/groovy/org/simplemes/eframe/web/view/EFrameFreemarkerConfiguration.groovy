package org.simplemes.eframe.web.view

import freemarker.template.Configuration
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.core.io.scan.ClassPathResourceLoader
import io.micronaut.views.ViewsConfiguration
import io.micronaut.views.freemarker.FreemarkerViewsRendererConfigurationProperties
import org.simplemes.eframe.web.ui.webix.freemarker.FreemarkerDirectiveConfiguration

import javax.annotation.Nullable

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
@Replaces(FreemarkerViewsRendererConfigurationProperties)
@Requires(classes = Configuration)
@ConfigurationProperties(PREFIX)
class EFrameFreemarkerConfiguration extends FreemarkerViewsRendererConfigurationProperties {

  /**
   * Default contructor.
   *
   * @param viewsConfiguration The views configuration
   * @param version The minimum version
   * @param resourceLoader The resource loader
   */
  public EFrameFreemarkerConfiguration(
    ViewsConfiguration viewsConfiguration,
    @Property(name = "micronaut.views.freemarker.incompatible-improvements") @Nullable String version,
    @Nullable ClassPathResourceLoader resourceLoader) {
    super(viewsConfiguration, version, resourceLoader)
    println "EFrameFreemarkerConfiguration viewsConfiguration = $viewsConfiguration"
    setSharedVariable("abc", 'xyz')
    FreemarkerDirectiveConfiguration.addSharedVariables(this)
    //this.setClassLoaderForTemplateLoading(environment.getClassLoader(), "/" + viewsConfiguration.getFolder())

/*
    super(version != null ? new Version(version) : Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    if (resourceLoader != null) {
      setClassLoaderForTemplateLoading(
        resourceLoader.getClassLoader(), "/" + viewsConfiguration.getFolder()
      );
    }
*/
  }
}
