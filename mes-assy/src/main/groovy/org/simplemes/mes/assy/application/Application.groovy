package org.simplemes.mes.assy.application

import groovy.transform.CompileStatic
import io.micronaut.runtime.Micronaut

/**
 * The main class for Assembly module testing.  Not used in production.
 */
@CompileStatic
class Application {
    static void main(String[] args) {
        Micronaut.run(Application)
    }
}