package org.simplemes.eframe.misc


import groovy.util.logging.Slf4j

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Utility to scan the class path for specific implementations of an interface/parent class.
 */
//@CompileStatic
@Slf4j
class ClassPathScanner {
  // look at https://github.com/grails/grails-data-mapping/blob/ef1730a42bc515cfb96cf9175d81e344d8a08a61/grails-datastore-gorm/src/main/groovy/org/grails/datastore/gorm/utils/ClasspathEntityScanner.groovy

  Class searchForClass

  /**
   * The classloader to use
   */
  ClassLoader classLoader = getClass().getClassLoader()

  /**
   * Packages that won't be scanned for performance reasons
   */
  List<String> ignoredPackages = ['com', 'net', '', 'org', 'java', 'javax', 'groovy']

  ClassPathScanner(Class searchForClass) {
    this.searchForClass = searchForClass
  }

  /**
   * Scans the classpath for entities for the given packages
   *
   * @param packages The packages
   * @return The entities
   */
  Class[] scan(Package... packages) {
/*
    ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(false)
    //componentProvider.setMetadataReaderFactory(new AnnotationMetadataReaderFactory(classLoader))
    componentProvider.addIncludeFilter(new AssignableTypeFilter(searchForClass))

    Collection<Class> classes = new HashSet<>()
    for (Package p in packages) {
      def packageName = p.name
      if (ignoredPackages.contains(packageName)) {
        log.error("Package [$packageName] will not be scanned as it is too generic and will slow down startup time. Use a more specific package")
      } else {
        for (BeanDefinition candidate in componentProvider.findCandidateComponents(packageName)) {
          Class persistentEntity = Class.forName(candidate.beanClassName, false, classLoader)
          classes.add persistentEntity
        }
      }
    }
*/
    return [] as Class[]
  }
}
