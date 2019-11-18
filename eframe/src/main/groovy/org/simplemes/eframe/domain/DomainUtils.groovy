package org.simplemes.eframe.domain

import grails.gorm.annotation.Entity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.validation.ValidationErrors
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.hibernate.proxy.HibernateProxy
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.data.FieldDefinitionFactory
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.data.annotation.ExtensibleFields
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TypeUtils
import org.springframework.validation.FieldError

import java.lang.reflect.Field

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

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
   * Gets the persistent fields for the given GORM entity class.
   * @param c The class to find the fields in.
   * @return A list of field, which includes a name and a type (class).
   */
  List<PersistentProperty> getPersistentFields(Class c) {
    return c.gormPersistentEntity.persistentProperties
  }

  /**
   * Gets a single persistent field for the given GORM entity class.
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
   * Gets the transient fields for the given GORM entity class.
   * Uses the transients static value.
   * @param c The class to find the fields in.
   * @return A list of transient fields.
   */
  List<Field> getTransientGORMFields(Class c) {
    List<List<String>> propList = TypeUtils.getStaticPropertyInSuperClasses(c, 'transients')
    List<String> transientNames = []
    // Flatten into a single list of names
    for (l in propList) {
      transientNames.addAll(l)
    }

    List<Field> res = []
    for (fieldName in transientNames) {
      def field = TypeUtils.safeGetField(c, fieldName)
      if (field) {
        res << c.getDeclaredField(fieldName)
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
   * Returns true if this is a GORM/Hibernate entity.
   * @param c The class to check for GORM injection.
   * @return True if this is a real GORM entity.
   */
  boolean isGormEntity(Class c) {
    if (!c) {
      return false
    }

    // Check for a proxy class
    if (HibernateProxy.isAssignableFrom(c)) {
      return true
    }

    def isGormEntity = c.isAnnotationPresent(Entity)
    if (!isGormEntity) {
      return false
    }

    // For unit tests without GORM active, we will check to make sure the entity is defined in GORM
    if (Holders.environmentDev || Holders.environmentTest) {
      isGormEntity = getAllDomains()?.contains(c)
    }
    return isGormEntity
  }

  /**
   * Find the FieldDefinitions for the given domain/POGO class.
   * @param c The class to find the field order in.
   * @return A list of field names, in the display field order.
   */
  FieldDefinitions getFieldDefinitions(Class c) {
    def res = new FieldDefinitions()
    if (isGormEntity(c)) {
      def properties = getPersistentFields(c)
      for (property in properties) {
        if (!isPropertySpecial(c, property.name)) {
          res << FieldDefinitionFactory.buildFieldDefinition(property)
        }
      }
      // Add the transients like a simple field from any class.
      def transients = getTransientGORMFields(c)
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
   * The names for special properties that are not considered normal domain fields.
   */
  static specialProperties = ['id', 'version', 'dateCreated', 'lastUpdated',
                              ExtensibleFields.DEFAULT_FIELD_NAME, ExtensibleFields.COMPLEX_CUSTOM_FIELD_NAME]

  /**
   * Determine if the given property is a special field that we want to ignore in GUIs and such.
   * This is used to filter out framework fields such as 'id', 'version', 'dateCreated', 'lastUpdated' or '_customField'.
   * @param c The class this property is in (Also checks the custom field definition for parent classes).
   * @param propertyName The name of the property to check.
   * @return True if property is a special property.
   */
  @SuppressWarnings("unused")
  boolean isPropertySpecial(Class c, String propertyName) {

    return (specialProperties.contains(propertyName))

/*
    // Now check for custom fields.
    def res = FieldExtensionHelper.isFieldCustomSupportField(c, propertyName)
    if (res) {
      return res
    }
    // Not in this class, check any parent classes to be safe.
    if (c.superclass != Object) {
      return isPropertySpecial(c.superclass, propertyName)
    }

    return false
*/
  }

  /**
   * Finds the real owning side setting for the given property.
   * This is needed since children of sub-classes are not flagged by GORM with the correct owningSide.
   * @param property The property to check.
   * @return True if property is a special property.
   */
  boolean isOwningSide(PersistentProperty property) {
    def owningSide = property.owningSide
    if (!owningSide) {
      // Not all child cases are flagged correctly by GORM.
      // We need to check the other side to see if it has a belongsTo for the property.
      // This mainly happens with sub-classes of a domain that has the belongsTo element.
      def domainClass = property.associatedEntity?.javaClass
      if (domainClass) {
        def belongsToList = TypeUtils.getStaticPropertyInSuperClasses(domainClass, 'belongsTo')
        // Check each belongsTo for the property
        def owner = property.owner?.javaClass
        def possibleOwnerClasses = TypeUtils.getSuperClasses(owner)
        possibleOwnerClasses << owner
        for (list in belongsToList) {
          for (key in list.keySet()) {
            if (possibleOwnerClasses.contains(list[key])) {
              // Force the child to true for this case.
              owningSide = true
            }
          }
        }
      }
    }


    return owningSide
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
   * <b>CAUTION:</b> This method uses the internal values of the GORM HibernateDatastore.  This may change someday.
   * @return
   */
  List<Class> getAllDomains() {
    def pe = Holders.hibernateDatastore?.mappingContext?.persistentEntities
    return pe*.javaClass
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
   * Resolves all proxies for the given domain class.  This allows the domain to be used outside of an hibernate session
   * if needed.  This will check all fields and sub-objects for proxies.
   * <p>
   * <b>Note:</b> This will usually force a read of all referenced objects.  This only works with a List of objects in most scenarios.
   *              Set/Collection does not support all scenarios.
   * @param object The object.
   */
  def resolveProxies(Object object) {
    if (object == null) {
      return
    }

    for (property in getPersistentFields(object.class)) {
      def value = object[property.name]
      //println "$property.name ($property.type) = $value $property"
      if (List.isAssignableFrom(property.type) && value) {
        // A List, so we can update the entry in the list with the un-proxied value.
        def list = (List) value
        for (int i = 0; i < list?.size(); i++) {
          def item = list[i]
          if (item != null) {
            if (item instanceof HibernateProxy) {
              // Replace the whole element if it is a proxy.
              list[i] = GrailsHibernateUtil.unwrapIfProxy(item)
            }
            // Resolved any sub-objects too.
            resolveProxies(item)
          }
        }
      } else if (Collection.isAssignableFrom(property.type) && value) {
        // Not a List, so we can't update the entry in the list with the un-proxied value.
        // All we can do is resolve any child proxies.
        for (item in value) {
          if (item != null) {
            // Resolved any sub-objects too.
            resolveProxies(item)
          }
        }
      } else {
        if (value instanceof HibernateProxy) {
          // A simple reference, so un-proxy it the GORM way.
          object[property.name] = GrailsHibernateUtil.unwrapIfProxy(value)
        }
        value = object[property.name]
        if (isGormEntity(value?.getClass()) && !property.referencedPropertyName) {
          // Finally resolve any sub-domain elements that are not parent references.
          // This uses a pattern in the property that sets the referencedPropertyName when the
          // belongsTo is used for a parent reference.
          resolveProxies(object[property.name])
        }
      }

    }
  }

  /**
   * Fixes the child's parent reference when a record is created by Jackson.  Jackson simply adds the child to the parent's
   * collection.  Instead, we must call the addXYZToChildren() method on the domain.  This method recursively finds all
   * new child records and removes then re-adds them with the addTo method.
   * @param object The domain object to check.
   */
  void fixChildParentReferences(Object object) {
    if (object == null) {
      return
    }

    for (property in getPersistentFields(object.class)) {
      def value = object[property.name]
      if (Collection.isAssignableFrom(property.type) && value) {
        Collection list = (Collection) value
        def recordsToFix = []
        for (item in list) {
          if (item.id == null) {
            // This is a new, unsaved item, so remember it to be fixed
            recordsToFix << item
          }
        }

        // Now, remove all new records before fixing them.
        list.removeAll { it.id == null }

        // An re-add with the special GORM method (will make sure the parent references are correct upon save).
        for (child in recordsToFix) {
          addChildToDomain(object, child, property.name)
        }

        // Now, check any sub-children.
        for (item in list) {
          fixChildParentReferences(item)
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
/*
    } else {
      // No method found, so try adding using std List calls.
      domainObject."${collectionName}" << child
*/
    }
    //println "after ${methodName}, child = $child, ${child.parent}"
  }


  /**
   * Converts the domain object's errors into readable messages and stores them in a MessageHolder,
   * as errors.
   * @param domainObject The domain object with errors.  This assumes the object has validate() called previously.
   * @return
   */
  MessageHolder getValidationMessages(Object domainObject) {
    def res = new MessageHolder()
    def messages = GlobalUtils.lookupValidationErrors(domainObject)
    messages.each { fieldName, errors ->
      for (s in errors) {
        res.addError(text: "$fieldName: $s")
      }
    }

    return res
  }

  /**
   * Adds a validation error to the given domain object.
   * @param domainObject The domain object to add the error to.
   */
  void addValidationError(Object domainObject, String fieldName, Object value, String messageKey) {
    def errors = domainObject.errors
    if (errors == null) {
      domainObject.errors = new ValidationErrors(domainObject)
    }
    def clazz = domainObject.getClass()
    errors.addError(new FieldError(clazz.simpleName, fieldName, value, true, [messageKey] as String[],
                                   [fieldName, clazz, value] as Object[], '{0} failed validation on {1}.'))

  }

  /**
   * Returns the domain record that matches the given ID or primary key.
   * @param domainClass The domain to find the given record for.
   * @param keyOrID The record ID or primary key.  The value is checked as key first.
   * @return The record.
   */
  Object findDomainRecord(Class domainClass, String keyOrID) {
    def keys = DomainUtils.instance.getKeyFields(domainClass)
    if (keys.size() == 1) {
      // Single primary key, try it first.
      def criteria = domainClass.createCriteria()
      List results = criteria {
        eq(keys[0], keyOrID)
      }
      if (results) {
        return results[0]
      }
    }
    // Find by record ID
    Long expectedID
    if (NumberUtils.isNumber(keyOrID)) {
      try {
        expectedID = Long.valueOf(keyOrID)
      } catch (Exception ignored) {
        // Not a valid Long, so treat as missing.
      }
    }

    def criteria = domainClass.createCriteria()
    List results = criteria {
      eq('id', expectedID)
    }
    if (results) {
      return results[0]
    }

    return null
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
