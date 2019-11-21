package org.simplemes.mes.application

import groovy.transform.CompileStatic
import io.micronaut.runtime.Micronaut
import org.simplemes.eframe.application.StartupHandler
import org.simplemes.eframe.domain.DomainFinder

@CompileStatic
class Application {
  static void main(String[] args) {
    StartupHandler.preStart()

    def list = DomainFinder.instance.topLevelDomainClasses
    list << Application
    Micronaut.run(list as Class[])

    //Micronaut.run([Application, Order, Role, UserPreference] as Class[])

    //Micronaut.run(Application)
  }
}