package org.simplemes.eframe.json

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.data.annotation.ExtensibleFields
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.domain.DomainUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides extra features for Hibernate's use of the Jackson object mapper.
 * This alters the property serializer for specific cases, such as parent references and foreign domain references.
 */
class HibernateAwareJacksonModule extends Module {

  /**
   * Method that returns a display that can be used by Jackson
   * for informational purposes, as well as in associating extensions with
   * module that provides them.
   */
  @Override
  String getModuleName() {
    return 'HibernateAwareJacksonModule'
  }

  /**
   * Method that returns version of this module. Can be used by Jackson for
   * informational purposes.
   */
  @Override
  Version version() {
    return new Version(1, 0, 0, 'SNAPSHOT')
  }

  /**
   * Method called by ObjectMapper when module is registered.
   * It is called to let module register functionality it provides,
   * using callback methods passed-in context object exposes.
   */
  @Override
  void setupModule(SetupContext context) {
    //context.setClassIntrospector(new HibernateAwareClassIntrospector())
    context.addBeanSerializerModifier(new HibernateBeanSerializerModifier())
    //context.addBeanDeserializerModifier(new HibernateBeanDeserializerModifier())
    context.addDeserializationProblemHandler(new DeserializationProblemHandler())
  }
}

@Slf4j
class HibernateBeanSerializerModifier extends BeanSerializerModifier {
  /**
   * Method called by BeanSerializerFactory after constructing default
   * bean serializer instance with properties collected and ordered earlier.
   * Implementations can modify or replace given serializer and return serializer
   * to use. Note that although initial serializer being passed is of type
   * BeanSerializer, modifiers may return serializers of other types;
   * and this is why implementations must check for type before casting.
   * <p>
   * NOTE: since 2.2, gets called for serializer of those non-POJO types that
   * do not go through any of more specific <code>modifyXxxSerializer</code>
   * methods; mostly for JDK types like {@link java.util.Iterator} and such.
   */
  @Override
  JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
    //println "config = $config, serializer = ${serializer} ${serializer?.dump()}"
    return super.modifySerializer(config, beanDesc, serializer)

  }

  /**
   * Method called by BeanSerializerFactory with tentative set
   * of discovered properties.
   */
  @Override
  List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
    def clazz = beanDesc.type.rawClass
    List<String> fieldsToRemove = []

    //println "clazz = $clazz"
    def fieldDefinitions = DomainUtils.instance.getFieldDefinitions(clazz)
    for (int i = 0; i < beanProperties.size(); i++) {
      BeanPropertyWriter w = beanProperties.get(i)
      //println "beanPropertyWriter = ${w?.dump()}"
      def fieldName = w.getName()
      def fieldDef = fieldDefinitions?.get(fieldName)
      if (fieldDef?.isReference()) {
        if ((!fieldDef.isChild()) && (!Collection.isAssignableFrom(fieldDef.type))) {
          def keys = DomainUtils.instance.getKeyFields(fieldDef.type)
          w.assignSerializer(new ForeignReferenceSerializer(keys))
          //w.assignDeserializer(new ForeignReferenceDeserializer())
        }
      } else if (fieldDef?.type?.isEnum()) {
        w.assignSerializer(new EnumSerializer(fieldName))
      } else if (fieldDef?.format == EncodedTypeFieldFormat.instance) {
        w.assignSerializer(new EncodedTypeSerializer(fieldName))
      } else if (isCustomFieldHolder(clazz, fieldName)) {
        w.assignSerializer(new CustomFieldSerializer(clazz))
      } else if (isComplexCustomFieldHolder(fieldName)) {
        w.assignSerializer(new ComplexCustomFieldSerializer(clazz))
      } else {
        // Check for reference ID field (e.g. sampleParentId) to be removed.
        if (fieldName.endsWith('Id')) {
          def refFieldName = fieldName[0..-3]
          def refFieldDef = fieldDefinitions?.get(refFieldName)
          if (refFieldDef?.isReference()) {
            fieldsToRemove << fieldName
          }
        }
      }
    }

    // Remove any problem properties.
    log.debug("changeProperties(): Removing JSON properties {}", fieldsToRemove)
    for (fieldToRemove in fieldsToRemove) {
      beanProperties.removeAll { it.name == fieldToRemove }
    }

    return super.changeProperties(config, beanDesc, beanProperties)
  }

  /**
   * Determines if the given field is a custom field holder.
   * @param domainClass The domain class the field is in.
   * @param fieldName The field to check.
   * @return True if the field is the custom field holder.
   */
  boolean isCustomFieldHolder(Class domainClass, String fieldName) {
    def customFieldHolderName = '_' + ExtensibleFieldHelper.instance.getCustomHolderFieldName(domainClass)
    return (customFieldHolderName == fieldName)
  }

  /**
   * Determines if the given field is a custom field holder.
   * @param fieldName The field to check.
   * @return True if the field is the custom field holder.
   */
  boolean isComplexCustomFieldHolder(String fieldName) {
    return (ExtensibleFields.COMPLEX_CUSTOM_FIELD_NAME == fieldName)
  }

}
