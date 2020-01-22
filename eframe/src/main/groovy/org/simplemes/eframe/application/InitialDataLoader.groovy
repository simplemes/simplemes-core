/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application


import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.AdditionHelper
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.InitialDataRecords

import javax.inject.Singleton

/**
 * Loads any initial data records needed for a new system.
 * Generally, this just calls each domain's initialDataLoad() method (if defined).
 * This also honors the static field  <code>static initialDataLoadAfter = ['org.simplemes.eframe.security.Role']</code>
 * to make sure this class is loaded after one or more other classes.
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>info</b> - Performance timing. </li>
 *   <li><b>debug</b> - Debugging information. Starting message.</li>
 * </ul>
 * @return The list of classes loaded, in the order loaded.
 */
@Slf4j
@Singleton
class InitialDataLoader {

  @SuppressWarnings("GroovyAssignabilityCheck")
  List<Class> dataLoad() {
    def classesLoaded = []
    def start = System.currentTimeMillis()
    def allDomains = DomainUtils.instance.allDomains
    if (!allDomains) {
      // Skip if hibernate is not started yet.
      return classesLoaded
    }
    addLoadersFromAdditions(allDomains)
    log.debug("Loading initial data.  Checking allDomains = {}", allDomains)
    //println "allDomains = $allDomains"
    List<IDL> idlList = []
    for (clazz in allDomains) {
      if (TypeUtils.doesClassHaveStaticMethod(clazz, 'initialDataLoad')) {
        def o = clazz.newInstance()
        def after = checkForPrecedence(o, 'initialDataLoadAfter')
        def before = checkForPrecedence(o, 'initialDataLoadBefore')
        idlList << new IDL(domainClass: clazz, afterClassNames: after, beforeClassNames: before)
      }
    }
    idlList = sortDomainsForLoading(idlList)
    for (idl in idlList) {
      try {
        idl.load()
        classesLoaded << idl.domainClass
      } catch (Exception e) {
        LogUtils.logStackTrace(log, e, "InitialDataLoad")
      }
    }

    log.info('InitialDataLoad time: {} ms for {} domains', (System.currentTimeMillis() - start), classesLoaded.size())
    return classesLoaded
  }

  /**
   * Adds any non-domain loaders from additions.
   * @param list The list (modified).
   */
  protected void addLoadersFromAdditions(List<Class> list) {
    for (addition in AdditionHelper.instance.additions) {
      if (addition.initialDataLoaders) {
        list.addAll(addition.initialDataLoaders)
      }
    }
  }

  /**
   * Checks for any precedence static lists in the given object.
   *
   *
   * @param o The domain object.
   * @param fieldName The static field name.  Usually 'initialDataLoadAfter' or 'initialDataLoadBefore'.
   * @return The list of class precedences.
   */
  protected List checkForPrecedence(Object o, String fieldName) {
    def list = null
    if (o.hasProperty(fieldName) && o[fieldName]) {
      // Convert any Class to its name.
      list = []
      for (predecessor in o[fieldName]) {
        if (predecessor instanceof Class) {
          predecessor = predecessor.name
        }
        list << predecessor
      }
    }
    return list
  }

  /**
   * Sorts the given list of Maps in the order needed for initial data loading.
   * @param list The list.  Contains elements: domain (a GrailsDomainClass) and after (a List<String>).
   * @return The sorted list.
   */
  protected List<IDL> sortDomainsForLoading(List<IDL> list) {
    // Sort by doing an insertion sort.
    List<IDL> res = []

    for (entry in list) {
      // Find the right place to store this new entry.
      def index = res.size()  // Default to end of list.

      // First, see if this new entry has an predecessors in the list.
      if (entry.afterClassNames) {
        // This entry has some predecessors
        for (afterClassName in entry.afterClassNames) {
          def newIndex = res.findIndexOf() { it.domainClass.name == afterClassName }
          if (newIndex >= 0) {
            //  Should be after, so place it there, unless we have a higher predecessor already
            index = Math.max(newIndex + 1, index)
          }
        }
      }

      // Then, see if this new entry should come before any in the list.
      if (entry.beforeClassNames) {
        // This entry has some predecessors
        for (beforeClassName in entry.beforeClassNames) {
          def newIndex = res.findIndexOf() { it.domainClass.name == beforeClassName }
          if (newIndex >= 0) {
            //  Should be before, so place it there, unless we have a higher predecessor already
            index = Math.min(newIndex - 1, index)
            index = Math.max(0, index)  // Not negative
          }
        }
      }

      // These two cases below handle the case when the precedence setting is in already in the list
      // as a precedence.
      def clazz = entry.domainClass

      // Next, see if this new entry is in any existing entries predecessors.
      for (int i = 0; i < res.size(); i++) {
        def resEntry = res[i]
        if (resEntry.afterClassNames?.contains(clazz.name)) {
          // Find the earliest existing entry that this new entry is a predecessor.
          index = Math.min(i, index)
        }
      }

      // Finally, see if this new entry is in any existing entries as a successor.
      for (int i = 0; i < res.size(); i++) {
        def resEntry = res[i]
        if (resEntry.beforeClassNames?.contains(clazz.name)) {
          // Find the latest existing entry that this new entry is a successor.
          index = Math.max(i + 1, index)
        }
      }

      res.add(index, entry)
    }
    return res
  }


  /**
   * The Initial Data load (IDL) spec for a single domain.
   */
  @ToString(includeNames = true, includePackage = false)
  protected class IDL {
    Class domainClass
    List<String> afterClassNames
    List<String> beforeClassNames

    void load() {
      def clazz = domainClass
      if (!DomainUtils.instance.isDomainEntity(clazz)) {
        // Fallback to a known domain for non-domain loaders.
        clazz = User
      }

      clazz.withTransaction {
        log.debug('Loading initial data for {}', domainClass)
        def res = domainClass.initialDataLoad()
        InitialDataRecords.instance.register((Map) res, (Class) domainClass)
      }
    }

  }
}

