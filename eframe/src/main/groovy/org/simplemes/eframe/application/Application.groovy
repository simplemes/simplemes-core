/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application

import groovy.transform.CompileStatic
import io.micronaut.runtime.Micronaut

/**
 * The main application class.  
 */
@CompileStatic
class Application {
  //static ApplicationContext applicationContext
  static void main(String[] args) {
    //TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    StartupHandler.preStart()

    Micronaut.run(Application)

    //Environment environment = applicationContext.getEnvironment()
    //println "environment = ${environment.activeNames}"

    //println "all = ${applicationContext.allBeanDefinitions*.name}"
    //def handlers = Holders.applicationContext.getBeansOfType(RejectionHandler) as RejectionHandler[]
    //println "handlers2 = $handlers"
/*
    Collection definitions = applicationContext.getBeanDefinitions(Qualifiers.byStereotype(Entity))
    //println "definitions = $definitions"
*/

  }


}