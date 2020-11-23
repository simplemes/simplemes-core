package org.simplemes.mes.app

import groovy.util.logging.Slf4j
import io.micronaut.runtime.Micronaut

@Slf4j
class Application {
  static void main(String[] args) {
    log.debug('args = {}, env = {}', args, System.getenv().keySet())
    log.debug('application.yml resources found = {}', this.classLoader.getResources('application.yml')*.path)
    def map = [:] as Map
    def url = System.getenv('DATABASE_URL')

    if (url) {
      // handle heroku-style URLs by parsing and rebuilding as needed for runtime.
      //"postgresql://mph:mh1234@192.168.1.77:5432/mes_dev"
      def parts = url.tokenize(':/@')
      if (parts.size() == 6) {
        def userName = parts[1]
        def pw = parts[2]
        def server = parts[3]
        def port = parts[4]
        def db = parts[5]

        def newUrl = "jdbc:postgresql://$server:$port/$db?user=$userName&password=$pw"

        map['DATABASE_URL'] = newUrl //'jdbc:postgresql://192.168.1.77:5432/mes_dev?user=mph&password=mh1234'
        //println "map = $map"
      }

    }
    //Micronaut.run(Application)
    Micronaut.build(args)
      .mainClass(Application)
      .properties(map)
      .start();

  }
}
