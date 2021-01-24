/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition
import com.fasterxml.jackson.databind.module.SimpleDeserializers
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.module.SimpleSerializers
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.databind.type.MapType
import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.FieldHolderMapInterface
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.DomainUtils

/**
 * Provides extra features for Micronaut Data's use of the Jackson object mapper.
 * This alters the property serializer for specific cases, such as parent references and foreign domain references.
 * See https://www.baeldung.com/jackson for some great tutorials.
 * <p>
 * <b>Note:</b> This module is loaded with the default moduleScan setting (true).  The file
 *   src/main/resources/META-INF/services/com.fasterxml.jackson.databind.Module contains a reference to this module.
 */
class EFrameJacksonModule extends SimpleModule {

/**
 * Method that returns a display that can be used by Jackson
 * for informational purposes, as well as in associating extensions with
 * module that provides them.
 */
  @Override
  String getModuleName() {
    return this.getClass().simpleName
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
    context.addBeanSerializerModifier(new EFrameBeanSerializerModifier())
    context.addBeanDeserializerModifier(new EFrameBeanDeserializerModifier())
    context.addDeserializationProblemHandler(new DeserializationProblemHandler())
    def serializers = new SimpleSerializers()
    def deserializers = new SimpleDeserializers()
    serializers.addSerializer(DateOnly, new DateOnlySerializer())
    deserializers.addDeserializer(DateOnly, new DateOnlyDeserializer())
    deserializers.addDeserializer(FieldHolderMapInterface, new FieldHolderMapDeserializer())

    context.addSerializers(serializers)
    context.addDeserializers(deserializers)
  }

  /**
   * Determines if the given field is a custom field holder.
   * @param domainClass The domain class the field is in.
   * @param fieldName The field to check.
   * @return True if the field is the custom field holder.
   */
  static boolean isCustomFieldHolder(Class domainClass, String fieldName) {
    def customFieldHolderName = ExtensibleFieldHelper.instance.getCustomHolderFieldName(domainClass)
    return (customFieldHolderName == fieldName)
  }

  /**
   * Determines if the given field is a custom field holder.
   * @param fieldName The field to check.
   * @return True if the field is the custom field holder.
   */
  static boolean isComplexCustomFieldHolder(String fieldName) {
    return (ExtensibleFieldHolder.COMPLEX_CUSTOM_FIELD_NAME == fieldName)
  }


}

@Slf4j
class EFrameBeanDeserializerModifier extends BeanDeserializerModifier {

  /**
   * @param config
   * @param type
   * @param beanDesc
   * @param deserializer
   */
  @Override
  JsonDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
    return super.modifyMapDeserializer(config, type, beanDesc, deserializer)
  }

  /**
   * This sub-class removes the un-wanted fields that we never want to de-serialize from JSON.
   * This mainly removes the custom field holders.  Those fields are handled by the
   * {@link DeserializationProblemHandler}.
   *
   * @param config
   * @param beanDesc
   * @param propDefs
   */
  @Override
  List<BeanPropertyDefinition> updateProperties(DeserializationConfig config, BeanDescription beanDesc, List<BeanPropertyDefinition> propDefs) {
    def clazz = beanDesc.type.rawClass
    List<String> fieldsToRemove = []
    for (BeanPropertyDefinition propDef : propDefs) {
      //println "propDef(${propDef.name}) = $propDef.properties"
      if (EFrameJacksonModule.isCustomFieldHolder(clazz, propDef.name)) {
        fieldsToRemove << propDef.name
      } else if (EFrameJacksonModule.isComplexCustomFieldHolder(propDef.name)) {
        fieldsToRemove << propDef.name
      }
    }

    // Remove any problem properties.
    if (fieldsToRemove) {
      log.debug("updateProperties(): Removing JSON properties {} for {}", fieldsToRemove, clazz)
    }
    for (fieldToRemove in fieldsToRemove) {
      propDefs.removeAll { it.name == fieldToRemove }
    }
    return super.updateProperties(config, beanDesc, propDefs)
  }
}

@Slf4j
class EFrameBeanSerializerModifier extends BeanSerializerModifier {
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
   */
  @Override
  BeanSerializerBuilder updateBuilder(SerializationConfig config, BeanDescription beanDesc, BeanSerializerBuilder builder) {
    def clazz = beanDesc.type.rawClass

    // Need to work around an issue with the Micronaut BeanIntrospectionModule.  It adds a new builder that
    // does not copy the _filterId (@JsonFilter) to the updated builder.
    def annotation = clazz.getAnnotation(JsonFilter)
    if (annotation?.value() == 'searchableFilter' && builder.filterId == null) {
      //noinspection GroovyAccessibility
      builder._filterId = 'searchableFilter'
    }
    return super.updateBuilder(config, beanDesc, builder)
  }


  /**
   * Method called by BeanSerializerFactory with tentative set
   * of discovered properties.
   */
  @Override
  List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
    def clazz = beanDesc.type.rawClass
    List<String> fieldsToRemove = []

    def fieldDefinitions = DomainUtils.instance.getFieldDefinitions(clazz)
    for (int i = 0; i < beanProperties.size(); i++) {
      BeanPropertyWriter w = beanProperties.get(i)
      //println "beanPropertyWriter = ${w?.dump()}"
      def fieldName = w.getName()
      if (alreadyHasSerializer(clazz, fieldName)) {
        continue
      }
      def fieldDef = fieldDefinitions?.get(fieldName)
      //println "  fieldDef = $fieldName $fieldDef ${beanDesc.classInfo}"
      if (fieldDef?.isReference()) {
        if (fieldDef.parentReference) {
          // Never serialize a parent reference
          fieldsToRemove << fieldName
        } else {
          if ((!fieldDef.child) && (!Collection.isAssignableFrom(fieldDef.type))) {
            def keys = DomainUtils.instance.getKeyFields(fieldDef.type)
            w.assignSerializer(new ForeignReferenceSerializer(keys))
            //w.assignDeserializer(new ForeignReferenceDeserializer())
          }
        }
      } else if (fieldDef?.type?.isEnum()) {
        w.assignSerializer(new EnumSerializer(fieldName))
      } else if (fieldDef?.format == EncodedTypeFieldFormat.instance) {
        w.assignSerializer(new EncodedTypeSerializer(fieldName))
        //} else if (EFrameJacksonModule.isCustomFieldHolder(clazz, fieldName)) {
        //  fieldsToRemove << fieldName
      } else if (EFrameJacksonModule.isComplexCustomFieldHolder(fieldName)) {
        w.assignSerializer(new ComplexCustomFieldSerializer(clazz))
      } else if (w.type.rawClass == FieldHolderMapInterface) {
        w.assignSerializer(new FieldHolderMapSerializer(FieldHolderMapInterface, fieldName))
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
    if (fieldsToRemove) {
      log.debug("changeProperties(): Removing JSON properties {} for {}", fieldsToRemove, clazz)
    }
    for (fieldToRemove in fieldsToRemove) {
      beanProperties.removeAll { it.name == fieldToRemove }
    }

    return super.changeProperties(config, beanDesc, beanProperties)
  }

  /**
   * Determines if the field already has a framework annotation (e.g. @JSONByID or similar) serializer.
   * @param clazz The clazz.
   * @param fieldName The field name to check.
   * @return True if already covered by a framework serializer annotation.
   */
  boolean alreadyHasSerializer(Class clazz, String fieldName) {
    try {
      def field = clazz.getDeclaredField(fieldName)
      return field.getAnnotation(JSONByID) != null || field.getAnnotation(JSONByKey) != null
    } catch (NoSuchFieldException ignored) {
      // If not in the clazz, we can assume it does not have the annotation.
    }
    return false
  }

}
