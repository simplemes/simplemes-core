/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import sample.domain.SampleParent

/**
 * Tests.
 */
class ForeignReferenceSerializerSpec extends BaseSpecification {

  def "verify that serializer handles simple key field"() {
    given: 'a serializer'
    def serializer = new ForeignReferenceSerializer(['name'])

    and: 'an object to serialize'
    def object = new SampleParent(name: 'ABC', uuid: UUID.randomUUID())

    and: 'mocked inputs'
    def mockGen = Mock(JsonGenerator)
    def mockProvider = Mock(SerializerProvider)

    when: 'an object is serialized'
    serializer.serialize(object, mockGen, mockProvider)

    then: 'the serializer called the right methods'
    1 * mockGen.writeStartObject()
    1 * mockGen.writeStringField('name', 'ABC')
    1 * mockGen.writeStringField('uuid', object.uuid.toString())
    1 * mockGen.writeEndObject()
  }

  def "verify that serializer handles simple foreign reference as part of the key fields"() {
    given: 'a class with a foreign domain reference key field'
    def src = """
    package sample
    
    import sample.domain.SampleParent
    
    class TestClass{
      SampleParent sampleParent
      int sequence
      UUID uuid
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a serializer'
    def serializer = new ForeignReferenceSerializer(['sampleParent', 'sequence'])

    and: 'an object to serialize'
    def object = clazz.newInstance()
    object.sampleParent = new SampleParent(name: 'ABC', uuid: UUID.randomUUID())
    object.sequence = 237
    object.uuid = UUID.randomUUID()

    and: 'mocked inputs'
    def mockGen = Mock(JsonGenerator)
    def mockProvider = Mock(SerializerProvider)

    when: 'an object is serialized'
    serializer.serialize(object, mockGen, mockProvider)

    then: 'the serializer called the right methods'
    1 * mockGen.writeStartObject()
    1 * mockGen.writeStringField('sampleParent', object.sampleParent.uuid.toString())
    1 * mockGen.writeStringField('sequence', '237')
    1 * mockGen.writeStringField('uuid', object.uuid.toString())
    1 * mockGen.writeEndObject()
  }
}
