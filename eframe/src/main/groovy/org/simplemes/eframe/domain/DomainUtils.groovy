/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain

import io.micronaut.core.beans.BeanIntrospection
import io.micronaut.core.beans.BeanIntrospector
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.data.FieldDefinitionFactory
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntityHelper
import org.simplemes.eframe.domain.annotation.DomainEntityInterface
import org.simplemes.eframe.domain.validate.ValidationErrorInterface
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.misc.UUIDUtils

import javax.persistence.ManyToOne
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Domain Object manipulation utilities.
 * These class provide common domain class utilities that simplify
 * access to domain objects and their use.
 * This includes methods to return the primary keys for a domain object.
 */
class DomainUtils {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static DomainUtils instance = new DomainUtils()

  /**
   * Gets the persistent fields for the given Domain entity class.
   * @param domainClass The class to find the fields in.
   * @return A list of field, which includes a name and a type (class).
   */
  List<PersistentProperty> getPersistentFields(Class domainClass) {
    List<PersistentProperty> res = new ArrayList<>()
    for (field in domainClass.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
        // Weed out any fields the use @Transient too
        def transAnn = field.getAnnotation(Transient.class)
        if (!transAnn) {
          res << new PersistentProperty(field)
        }
      }
    }
    return res

  }

  /**
   * Gets a single persistent field for the given domain entity class.
   * <p>
   * <b>Note</b>: This should not be used in production.  See {@link #getFieldDefinitions(java.lang.Class)} for a more efficient method.
   * This method is mainly used for unit testing.
   * @param c The class to find the field in.
   * @return A Persistent Property (field), which includes a name and a type (class).
   */
  PersistentProperty getPersistentField(Class c, String name) {
    def fields = getPersistentFields(c)
    return fields.find { it.name == name }
  }

  /**
   * Gets the transient fields for the given domain entity class.
   * @param domainClass The class to find the fields in.
   * @return A list of transient fields.
   */
  List<Field> getTransientFields(Class domainClass) {
    List<Field> res = []
    for (field in domainClass.getDeclaredFields()) {
      def ann = field.getAnnotation(Transient)
      if (Modifier.isTransient(field.modifiers) || ann) {
        res << field
      }
    }
    return res
  }

  /**
   * Finds the class for the given field name.
   * @param clazz The class to find the field on.
   * @return A list of field.  The returned values each has a name and a type(a Class).
   */
  Class getFieldType(Class clazz, String fieldName) {
    //println "clazz = ${clazz.metaClass.getProperties()*.name}"
    def field = clazz.metaClass.getMetaProperty(fieldName)
    //def field = clazz.getDeclaredField(fieldName)
    return field?.type
  }


  /**
   * Returns true if this is a domain entity.
   * @param c The class to check.
   * @return True if this is a real domain entity.
   */
  boolean isDomainEntity(Class c) {
    if (!c) {
      return false
    }

    return DomainEntityInterface.isAssignableFrom(c)
  }

  /**
   * Find the FieldDefinitions for the given domain/POGO class.
   * @param c The class to find the field order in.
   * @return A list of field names, in the display field order.
   */
  FieldDefinitions getFieldDefinitions(Class c) {
    def res = new FieldDefinitions()
    if (isDomainEntity(c)) {
      def properties = getPersistentFields(c)
      for (property in properties) {
        if (!isPropertySpecial(c, property.name)) {
          res << FieldDefinitionFactory.buildFieldDefinition(property)
        }
      }
      // Add the transients like a simple field from any class.
      def transients = getTransientFields(c)
      for (t in transients) {
        if (!isPropertySpecial(c, t.name)) {
          res << FieldDefinitionFactory.buildFieldDefinition(t)
        }
      }
    } else {
      // Try as simple POGO
      def fields = c.declaredFields
      for (field in fields) {
        if (!isPropertySpecial(c, field.name)) {
          res << FieldDefinitionFactory.buildFieldDefinition(field)
        }
      }
    }

    return res
  }

  /**
   * Find static field order from the class (and super classes).
   * @param c The class to find the field order in.
   * @return A list of field names, in the display field order.
   */
  List<String> getStaticFieldOrder(Class c) {
    def lists = TypeUtils.getStaticPropertyInSuperClasses(c, 'fieldOrder')
    List<String> res = []
    for (list in lists) {
      res.addAll((Collection) list)
    }
    return res
  }

  /**
   * Finds the field name for the parent reference field name on the given domain class.
   * @param clazz The class to find the field on.
   */
  String getParentFieldName(Class clazz) {
    for (field in clazz.getDeclaredFields()) {
      ManyToOne manyToOne = field.getAnnotation(ManyToOne.class)
      if (manyToOne && manyToOne.targetEntity() == void) {
        return field.name
      }
    }
    return null
  }

  /**
   * The names for special properties that are not considered normal domain fields.
   */
  static specialProperties = ['uuid', 'version', 'dateCreated', 'dateUpdated',
                              ExtensibleFieldHolder.COMPLEX_CUSTOM_FIELD_NAME]

  /**
   * Determine if the given property is a special field that we want to ignore in GUIs and such.
   * This is used to filter out framework fields such as 'id', 'version', 'dateCreated', 'dateUpdated' or '_customField'.
   * @param c The class this property is in (Also checks the custom field definition for parent classes).
   * @param propertyName The name of the property to check.
   * @return True if property is a special property.
   */
  @SuppressWarnings("unused")
  boolean isPropertySpecial(Class c, String propertyName) {
    if (ExtensibleFieldHelper.instance.getCustomHolderFieldName(c) == propertyName) {
      return true
    }
    return (specialProperties.contains(propertyName))
  }

  /**
   * Determines the default sort field for the domain.  Uses the first field in the primary key list.
   * @param c The class to find the sort field for.
   * @return The primary sort field name.
   */
  String getPrimaryKeyField(Class c) {
    def keys = getKeyFields(c)
    if (keys) {
      return keys[0]
    }
    return null
  }

  /**
   * Finds the key field(s) for a given domain class.
   * If no static 'keys' list is defined in the domain class, then key field is assumed to be first field in the
   * fieldOrder list.
   * @param clazz The Domain class.
   * @return The list of key field name(s).  Empty if none found.
   */
  List<String> getKeyFields(Class clazz) {
    def keys = TypeUtils.getStaticPropertyInSuperClasses(clazz, 'keys') as List<String>
    List<String> flatList = []
    keys.each { flatList.addAll(it) }
    if (flatList) {
      return flatList
    }

    // Fallback to the first element in the field order.
    def fieldOrder = getStaticFieldOrder(clazz)
    if (fieldOrder) {
      return [fieldOrder[0]]
    }
    return []
  }

  /**
   * Returns all of the domain classes defined in the system.
   * <p>
   * @return
   */
  List<Class> getAllDomains() {
    //PerformanceUtils.elapsedPrint()
    Collection<BeanIntrospection<Object>> introspections = BeanIntrospector.SHARED.findIntrospections(MappedEntity.class)
    return introspections*.getBeanType()
  }

  /**
   * Returns a domain class, using the simple name for the class (case insensitive).
   * @return The class.
   */
  Class getDomain(String domainName) {
    domainName = domainName.toLowerCase()
    def allDomains = getAllDomains()
    return allDomains.find { it.simpleName.toLowerCase() == domainName }
  }

  /**
   * Determines the root URI for the controller associated with the given domain.
   * This just converts the name.  It does not guarantee that the controller exists.
   * @param domainClass The domain class to determine the root for the controller.
   * @return The URI root.  Does not include the leading '/'.
   */
  String getURIRoot(Class domainClass) {
    return NameUtils.lowercaseFirstWord(domainClass.simpleName)
  }

  /**
   * Forces the load of child records that might have a lazy loader.  This is recursive.
   * @param object The domain object to make sure the child records are loaded.
   */
  void loadChildRecords(Object object) {
    if (object == null) {
      return
    }

    for (property in getPersistentFields(object.class)) {
      def value = object[property.name] // Forces the load.

      if (Collection.isAssignableFrom(property.type) && value) {
        // Now, load any children.
        Collection list = (Collection) value
        for (item in list) {
          loadChildRecords(item)
        }
      }
    }
  }

  /**
   * Adds a single child to the given element using the dynamic addToXYZ method.
   * @param domainObject The object to add the child to.
   * @param child The child.
   * @param collectionName The name of the collection to add the child to.
   */
  void addChildToDomain(Object domainObject, Object child, String collectionName) {
    def methodName = "addTo${NameUtils.uppercaseFirstLetter(collectionName)}"
    //println "calling ${methodName}, child = $child"
    def method = domainObject.metaClass.getMetaMethod(methodName, [Object])
    //println "method = $method. child = $child, test = ${Holders.environmentTest}"
    if (method) {
      // Must be a domain object, so call the method.
      method.invoke(domainObject, child)
    } else {
      // No method found, so try adding using std List calls.
      domainObject."${collectionName}" << child
    }
    //println "after ${methodName}, child = $child, ${child.parent}"
  }


  /**
   * Validates the given domain object.
   * @param domainObject The domain object to validate.
   * @return The list of validation errors.  Never null.
   */
  List<ValidationErrorInterface> validate(Object domainObject) {
    return DomainEntityHelper.instance.validate(domainObject as DomainEntityInterface)
  }

  /**
   * Converts the domain object's errors into readable messages and stores them in a MessageHolder,
   * as errors.
   * @param domainObject The domain object with errors.  This assumes the object has validate() called previously.
   * @return
   */
  MessageHolder getValidationMessages(Object domainObject) {
    def res = new MessageHolder()
    def errors = DomainEntityHelper.instance.validate((DomainEntityInterface) domainObject)
    for (error in errors) {
      res.addError(text: "$error.fieldName: $error")
    }

    return res
  }

  /**
   * Returns the domain record that matches the given uuid or primary key.
   * @param domainClass The domain to find the given record for.
   * @param keyOrUuid The UUID or primary key.  Supports UUID as a string input.
   * @return The record.
   */
  Object findDomainRecord(Class domainClass, Object keyOrUuid) {
    if (!keyOrUuid) {
      return null
    }
    keyOrUuid = UUIDUtils.convertToUUIDIfPossible(keyOrUuid)
    if (keyOrUuid instanceof UUID) {
      return domainClass.findByUuid(keyOrUuid)
    } else {
      def keys = DomainUtils.instance.getKeyFields(domainClass)
      if (keys.size() < 0) {
        throw new IllegalArgumentException("Domain class ${domainClass} no supported.  Needs a key field.")
      }
      def keyName = keys[0]

      // Find the correct findBy method for the repository.
      def repo = domainClass.repository
      def method = repo.getClass().getDeclaredMethod("findBy${NameUtils.uppercaseFirstLetter(keyName)}", String)
      // Force access to the method.  For some reason, this method is not accessible, even though it is public.
      method.setAccessible(true)

      def res = method.invoke(repo, keyOrUuid)
      if (res instanceof Optional) {
        // Strip the Optional wrapper for the findBy() case.
        res = ((Optional<?>) res).orElse(null)
      }
      return res
    }
  }

  /**
   * Calls the domain object's findRelatedRecords() method for the given object.
   * If implemented, this will return the list of related records.   Commonly used
   * for archiving and deleting records.
   * @param record The domain object(record) to find related records for.
   * @return The list of related records.
   */
  List findRelatedRecords(Object record) {
    def method = record.class.declaredMethods.find { it.name == 'findRelatedRecords' }
    if (method) {
      return (List) method.invoke(record)
    }
    return null
  }
}
