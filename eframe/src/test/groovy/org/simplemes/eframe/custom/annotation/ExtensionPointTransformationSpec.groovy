/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.annotation


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.UnitTestUtils
import sample.SampleExtensionInterface
import sample.SampleExtensionPOGOInterface
import sample.SampleNoArgumentExtensionInterface
import sample.pogo.SampleAlternatePOGO
import sample.pogo.SamplePOGO

/**
 * Tests.
 */
class ExtensionPointTransformationSpec extends BaseSpecification {

  def "verify that the annotation calls the invokePre and invokePost helper methods - POGO arguments"() {
    given:
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.pogo.SampleAlternatePOGO
      import sample.pogo.SamplePOGO
      import sample.SampleExtensionPOGOInterface
      
      class TestClass {
        @ExtensionPoint(SampleExtensionPOGOInterface)
        SampleAlternatePOGO coreMethod(SamplePOGO coreArgument1) {
          def x = 'ABC'
          return new SampleAlternatePOGO(name: "Core argument1: "+coreArgument1.name)
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a mocked helper'
    def mock = Mock(ExtensionPointHelper)
    ExtensionPointHelper.instance = mock

    def argument1 = new SamplePOGO(name: 'name1')
    def coreReturnValue = new SampleAlternatePOGO(name: "Core argument1: name1")
    def alteredReturnValue = new SampleAlternatePOGO(name: "Return from post")

    when: 'the method is called'
    def o = clazz.newInstance()
    def res = o.coreMethod(argument1) as SampleAlternatePOGO

    then: 'the helper pre and post methods are called'
    1 * mock.invokePre(SampleExtensionPOGOInterface, 'preCoreMethod', argument1)
    1 * mock.invokePost(SampleExtensionPOGOInterface, 'postCoreMethod', coreReturnValue, argument1) >> alteredReturnValue

    then: 'the correct value is returned from the method call'
    res.name == "Return from post"
  }


  def "verify that the annotation calls the invokePre and invokePost helper methods - simple arguments"() {
    given:
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.SampleExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleExtensionInterface)
        Map coreMethod(String argument1, Integer argument2) {
          def x = 'XYZ'
          return [argument1: argument1,argument2: argument2,x: x]
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a mocked helper'
    def mock = Mock(ExtensionPointHelper)
    ExtensionPointHelper.instance = mock

    def coreReturnValue = [argument1: 'ABC', argument2: 237, x: 'XYZ']
    def alteredReturnValue = [argument1: 'ABC', argument2: 237, x: 'XYZ']

    when: 'the method is called'
    def o = clazz.newInstance()
    def res = o.coreMethod('ABC', 237) as Map

    then: 'the helper pre and post methods are called'
    1 * mock.invokePre(SampleExtensionInterface, 'preCoreMethod', 'ABC', 237)
    1 * mock.invokePost(SampleExtensionInterface, 'postCoreMethod', coreReturnValue, 'ABC', 237) >> alteredReturnValue

    then: 'the correct value is returned from the method call'
    res == alteredReturnValue
  }

  def "verify that the annotation calls the invokePre and invokePost helper methods - multiple return paths"() {
    given:
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.SampleExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleExtensionInterface)
        Map coreMethod(String argument1, Integer argument2) {
          def x = 'XYZ'
          if (argument2==237) {
            def y = x+argument2
            return [argument1: argument1,argument2: argument2,x: y]
          } else {
            return [argument1: argument1,argument2: argument2,x: x]
          }
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a mocked helper'
    def mock = Mock(ExtensionPointHelper)
    ExtensionPointHelper.instance = mock

    def coreReturnValue = [argument1: 'ABC', argument2: 237, x: 'XYZ237']
    def alteredReturnValue = [argument1: 'ABC', argument2: 237, x: 'XYZ']

    when: 'the method is called'
    def o = clazz.newInstance()
    def res = o.coreMethod('ABC', 237) as Map

    then: 'the helper pre and post methods are called'
    1 * mock.invokePre(SampleExtensionInterface, 'preCoreMethod', 'ABC', 237)
    1 * mock.invokePost(SampleExtensionInterface, 'postCoreMethod', coreReturnValue, 'ABC', 237) >> alteredReturnValue

    then: 'the correct value is returned from the method call'
    res == alteredReturnValue
  }

  def "verify that the annotation calls the invokePre and invokePost helper methods - multiple return paths with if else"() {
    given:
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.SampleExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleExtensionInterface)
        Map coreMethod(String argument1, Integer argument2) {
          def x = 'XYZ'
          if (argument2==137) {
            return [argument1: argument1,argument2: argument2,x: x]
          } else if (argument2==237) {
            def y = x+argument2
            return [argument1: argument1,argument2: argument2,x: y]
          } else {
            return [argument1: argument1,argument2: argument2,x: x]
          }
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a mocked helper'
    def mock = Mock(ExtensionPointHelper)
    ExtensionPointHelper.instance = mock

    def coreReturnValue = [argument1: 'ABC', argument2: 237, x: 'XYZ237']
    def alteredReturnValue = [argument1: 'ABC', argument2: 237, x: 'XYZ']

    when: 'the method is called'
    def o = clazz.newInstance()
    def res = o.coreMethod('ABC', 237) as Map

    then: 'the helper pre and post methods are called'
    1 * mock.invokePost(SampleExtensionInterface, 'postCoreMethod', coreReturnValue, 'ABC', 237) >> alteredReturnValue

    then: 'the correct value is returned from the method call'
    res == alteredReturnValue
  }

  def "verify that the annotation calls the invokePre and invokePost helper methods - zero arguments and void return"() {
    given:
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.SampleNoArgumentExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleNoArgumentExtensionInterface)
        Map coreMethod() {
          return
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a mocked helper'
    def mock = Mock(ExtensionPointHelper)
    ExtensionPointHelper.instance = mock

    when: 'the method is called'
    def o = clazz.newInstance()
    o.coreMethod()

    then: 'the helper pre and post methods are called'
    1 * mock.invokePre(SampleNoArgumentExtensionInterface, 'preCoreMethod')
    1 * mock.invokePost(SampleNoArgumentExtensionInterface, 'postCoreMethod', null)
  }

  def "verify that the annotation gracefully detects no return statement in the method"() {
    given: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the source with the error is compiled'
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.SampleNoArgumentExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleNoArgumentExtensionInterface)
        Map coreMethod() {
        }
      }
    """
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['return', 'statements', 'coreMethod'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the annotation gracefully detects no interface class in annotation"() {
    given: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the source with the error is compiled'
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.SampleNoArgumentExtensionInterface
      
      class TestClass {
        @ExtensionPoint
        void coreMethod() {
        }
      }
    """
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['no', 'interface', 'coreMethod'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the annotation gracefully detects no  pre method in interface"() {
    given: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the source with the error is compiled'
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.SampleNoArgumentExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleNoArgumentExtensionInterface)
        void coreXMethod(String s) {
          return
        }
      }
    """
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['preCoreXMethod', 'SampleNoArgumentExtensionInterface', '@ExtensionPoint'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the annotation gracefully detects no post method in interface"() {
    given: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the source with the error is compiled'
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.SampleMisMatchedMethodExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleMisMatchedMethodExtensionInterface)
        void coreMethod(String s) {
          return
        }
      }
    """
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['postCoreMethod', 'SampleMisMatchedMethodExtensionInterface', '@ExtensionPoint'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the annotation gracefully detects pre method in interface with different number of parameters"() {
    given: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the source with the error is compiled'
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.SampleMisMatchedMethodExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleMisMatchedMethodExtensionInterface)
        void coreMethod2(String s) {
          return
        }
      }
    """
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['preCoreMethod2', 'SampleMisMatchedMethodExtensionInterface',
                                              '@ExtensionPoint', 'different', 'parameters'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the annotation gracefully detects argument count mis-match between interface and post method"() {
    given: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the source with the error is compiled'
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.SampleMisMatchedMethodExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleMisMatchedMethodExtensionInterface)
        String coreMethod3() {
          return
        }
      }
    """
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['parameters', 'different', 'SampleMisMatchedMethodExtensionInterface',
                                              'postCoreMethod3', 'coreMethod3'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the annotation gracefully detects argument type mis-match between interface and pre method"() {
    given: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the source with the error is compiled'
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.pogo.SampleAlternatePOGO
      import sample.pogo.SamplePOGO
      import sample.SampleExtensionPOGOInterface
      
      class TestClass {
        @ExtensionPoint(SampleExtensionPOGOInterface)
        SampleAlternatePOGO coreMethod(String coreArgument1a) {
          return
        }
      }
    """
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['parameters', 'different', 'SampleExtensionPOGOInterface',
                                              'preCoreMethod', 'coreMethod', 'SamplePOGO', 'String', 'coreArgument1a'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the annotation gracefully detects argument type mis-match between interface and post method"() {
    given: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the source with the error is compiled'
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.pogo.SampleAlternatePOGO
      import sample.pogo.SamplePOGO
      import sample.SampleMisMatchedMethodExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleMisMatchedMethodExtensionInterface)
        String coreMethod4(Integer coreArgument4) {
          return
        }
      }
    """
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['parameters', 'different', 'SampleMisMatchedMethodExtensionInterface',
                                              'postCoreMethod4', 'coreMethod', 'Integer', 'Boolean', 'coreArgument4'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the annotation gracefully detects return type mis-match between interface and post method"() {
    given: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the source with the error is compiled'
    def src = """
      import org.simplemes.eframe.custom.annotation.ExtensionPoint
      import sample.pogo.SampleAlternatePOGO
      import sample.pogo.SamplePOGO
      import sample.SampleMisMatchedMethodExtensionInterface
      
      class TestClass {
        @ExtensionPoint(SampleMisMatchedMethodExtensionInterface)
        String coreMethod5() {
          return
        }
      }
    """
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['return', 'correct', 'type', 'SampleMisMatchedMethodExtensionInterface',
                                              'postCoreMethod5', 'coreMethod5', 'String', 'Integer'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }



}
