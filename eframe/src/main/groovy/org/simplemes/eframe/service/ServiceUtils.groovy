package org.simplemes.eframe.service

import groovy.util.logging.Slf4j
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import org.simplemes.eframe.application.Holders

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

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
   * This looks for classes that use the @Singleton annotation and end with Service.
   * @return The list of service classes.
   */
  List<Class> getAllServices() {
    Collection<BeanDefinition> beans = Holders.applicationContext?.getBeanDefinitions(Qualifiers.byStereotype(Singleton))
    Collection<BeanDefinition> services = beans?.findAll { it.beanType.name.endsWith('Service') }
    return services*.beanType
  }


}
