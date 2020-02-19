package org.simplemes.mes.application

import groovy.transform.CompileStatic
import io.micronaut.runtime.Micronaut
import org.simplemes.eframe.application.StartupHandler

@CompileStatic
class Application {
  static void main(String[] args) {
    StartupHandler.preStart()

    Micronaut.run(Application)

    //Micronaut.run([Application, Order, Role, UserPreference] as Class[])

    //Micronaut.run(Application)
  }
}