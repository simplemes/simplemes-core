/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.service

import groovy.util.logging.Slf4j
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import org.simplemes.eframe.application.Holders

import javax.inject.Singleton

/**
 * Service support utilities.
 * These class provide common service class utilities that simplify
 * how the services operate.
 *
 */
@Slf4j
class ServiceUtils {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static ServiceUtils instance = new ServiceUtils()

  /**
   * Returns all of the service classes defined in the system.
   * <p>
   * This looks for classes that use the @Singleton annotation and end with Service or ServiceDefinition$Intercepted.
   * @return The list of service classes.
   */
  List<Class> getAllServices() {
    Collection<BeanDefinition> beans = Holders.applicationContext?.getBeanDefinitions(Qualifiers.byStereotype(Singleton))
    def ending1 = 'Service'
    def ending2 = 'ServiceDefinition$Intercepted'
    Collection<BeanDefinition> services = beans?.findAll {
      it.beanType.name.endsWith(ending1) || it.beanType.name.endsWith(ending2)
    } as Collection<BeanDefinition>
    return services*.beanType
  }


}
