package org.simplemes.eframe.test

import ch.qos.logback.classic.Level
import io.micronaut.security.rules.SecurityRule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the tester.
 */
class ControllerTesterSpec extends BaseSpecification {

  def "verify that test with defaults works correctly with secured controller"() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      @Secured("ADMIN")
      class ATestController {
        String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
    }

    then: 'no error is thrown'
    notThrown(Throwable)
  }

  def "verify that test with totally unsecured controller detects the error"() {
    given: 'a controller'
    def src = """
      package sample
      
      import io.micronaut.security.annotation.Secured

      class ATestController {
        String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['ATestController', 'method1', 'not', 'secured'])
  }

  def "verify that test with specific top-level role works for secured controller"() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      @Secured("MANAGER")
      class ATestController {
        String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      role 'MANAGER'
    }

    then: 'no error is thrown'
    notThrown(Throwable)
  }

  def "verify that test with specific top-level role fails for secured controller with wrong role"() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      @Secured("CUSTOMIZER")
      class ATestController {
        String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      role 'MANAGER'
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['ATestController', 'method1', 'MANAGER', 'not', 'secured'])
  }


  def "verify that test with all method secured and no top-level works"() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      class ATestController {
        @Secured("MANAGER")
        String method1() {}   
        @Secured("MANAGER")
        String method2() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
    }

    then: 'no error is thrown'
    notThrown(Throwable)
  }

  def "verify that test with one method not secured and no top-level fails"() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      class ATestController {
        @Secured("MANAGER")
        String method1() {}   

        String method2() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['method2', 'not', 'secured'])
  }

  def "verify that unsecured private and protected methods are ignored"() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      class ATestController {
        @Secured("MANAGER")
        public String method1() {}   
        private String method2() {}   
        protected String method3() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
    }

    then: 'no error is thrown'
    notThrown(Throwable)
  }

  def "verify that specific role on specific method works "() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      class ATestController {
        @Secured("MANAGER")
        public String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      secured 'method1', 'MANAGER'
    }

    then: 'no error is thrown'
    notThrown(Throwable)
  }

  def "verify that specific role on specific method detects missing role"() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      class ATestController {
        @Secured("MANAGER")
        public String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      secured 'method1', 'ADMIN'
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['method1', 'not', 'secured', 'ADMIN'])
  }

  def "verify that if no specific method role is given that the method level mismatch is detected"() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      @Secured("DESIGNER")
      class ATestController {
        @Secured("MANAGER")
        public String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      role 'DESIGNER'
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['ATestController', 'method1', 'not', 'secured', 'MANAGER', 'DESIGNER'])
  }

  def "verify that special isAnonymous role is supported when the controller uses it"() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      @Secured("isAnonymous()")
      class ATestController {
        public String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      role 'isAnonymous()'
    }

    then: 'no exception is thrown'
    notThrown(Throwable)
  }

  def "verify that special isAnonymous role check fails when the controller does not use it"() {
    given: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      @Secured("MANAGER")
      class ATestController {
        public String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      role 'isAnonymous()'
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['method1', 'not', 'secured', 'isAnonymous'])
  }

  def "verify that tester logs warning when the security check is disabled"() {
    given: 'a mock appender for Warn level only'
    def mockAppender = MockAppender.mock(ControllerTester, Level.WARN)

    and: 'a controller'
    def src = """package sample
      import io.micronaut.security.annotation.Secured

      class ATestController {
        public String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      securityCheck false
    }

    then: 'the warn message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['WARN', 'check', 'disabled'])
  }

  def "verify that test with controller class secured by isAnonymous fails unless the tester explicitly calls for isAnonymous"() {
    given: 'a controller'
    def src = """
      package sample
      
      import io.micronaut.security.annotation.Secured

      @Secured("isAnonymous()")
      class ATestController {
        String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['ATestController', 'isAnonymous', 'only', 'secured'])
  }

  def "verify that test with controller method secured by isAnonymous fails unless the tester explicitly calls for isAnonymous"() {
    given: 'a controller'
    def src = """
      package sample
      
      import io.micronaut.security.annotation.Secured

      class ATestController {
        @Secured("isAnonymous()")
        String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['ATestController', 'method1', 'isAnonymous', 'only', 'secured'])
  }

  def "verify that test with controller class secured by isAnonymous and another role passes"() {
    given: 'a controller'
    def src = """
      package sample
      
      import io.micronaut.security.annotation.Secured

      @Secured(["isAnonymous()","MANAGER"])
      class ATestController {
        String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
    }

    then: 'no exception is thrown'
    notThrown(Throwable)
  }

  def "verify that test with controller method secured by isAnonymous and another role passes"() {
    given: 'a controller'
    def src = """
      package sample
      
      import io.micronaut.security.annotation.Secured

      class ATestController {
        @Secured(["isAnonymous()","MANAGER"])
        String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
    }

    then: 'no exception is thrown'
    notThrown(Throwable)
  }

  def "verify that test with controller class secured by isAnonymous and role is specified as isAnonymous"() {
    given: 'a controller'
    def src = """
      package sample
      
      import io.micronaut.security.annotation.Secured

      @Secured(["isAnonymous()"])
      class ATestController {
        String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      role 'isAnonymous()'
    }

    then: 'no exception is thrown'
    notThrown(Throwable)
  }

  def "verify that test with controller method secured by isAnonymous and role is specified as isAnonymous"() {
    given: 'a controller'
    def src = """
      package sample
      
      import io.micronaut.security.annotation.Secured

      class ATestController {
        @Secured("isAnonymous()")
        String method1() {}   
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      role SecurityRule.IS_ANONYMOUS
    }

    then: 'no exception is thrown'
    notThrown(Throwable)
  }

  def "verify that test with valid task menu works"() {
    given: 'a mock appender for Warn level only'
    MockAppender.mock(ControllerTester, Level.WARN)

    and: 'a controller'
    def src = """
    package sample
    import org.simplemes.eframe.web.task.TaskMenuItem
        
    class _ATestController {
      def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'user', uri: '/user', 
                                            displayOrder: 7050, clientRootActivity: true)]
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      securityCheck false
      taskMenu name: 'user', uri: '/user', clientRootActivity: true
    }

    then: 'no exception is thrown'
    notThrown(Throwable)
  }

  def "verify that test on controller with no task menu detects failure"() {
    given: 'a mock appender for Warn level only'
    MockAppender.mock(ControllerTester, Level.WARN)

    and: 'a controller'
    def src = """
    package sample
    import org.simplemes.eframe.web.task.TaskMenuItem
        
    class _ATestController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      securityCheck false
      taskMenu name: 'user'
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['taskMenuItems', '_ATestController', 'no'])
  }

  def "verify that test with missing task menu in controller detects failure"() {
    given: 'a mock appender for Warn level only'
    MockAppender.mock(ControllerTester, Level.WARN)

    and: 'a controller'
    def src = """
    package sample
    import org.simplemes.eframe.web.task.TaskMenuItem
        
    class _ATestController {
      def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'user', uri: '/user', 
                                            displayOrder: 7050, clientRootActivity: true)]
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      securityCheck false
      taskMenu name: 'userX'
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['userX', '_ATestController'])
  }

  def "verify that test with wrong task menu uri in controller detects failure"() {
    given: 'a mock appender for Warn level only'
    MockAppender.mock(ControllerTester, Level.WARN)

    and: 'a controller'
    def src = """
    package sample
    import org.simplemes.eframe.web.task.TaskMenuItem
        
    class _ATestController {
      def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'user', uri: '/good', 
                                            displayOrder: 7050, clientRootActivity: true)]
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      securityCheck false
      taskMenu name: 'user', uri: '/bad'
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['/bad', '/good', 'URI'])
  }

  def "verify that test with wrong task menu clientRootActivity setting in controller detects failure"() {
    given: 'a mock appender for Warn level only'
    MockAppender.mock(ControllerTester, Level.WARN)

    and: 'a controller'
    def src = """
    package sample
    import org.simplemes.eframe.web.task.TaskMenuItem
        
    class _ATestController {
      def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'user', uri: '/good', 
                                            displayOrder: 7050, clientRootActivity: false)]
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      securityCheck false
      taskMenu name: 'user', clientRootActivity: true
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['true', 'false', 'clientRootActivity'])
  }

  def "verify that test with option for the task menu checks detects failure"() {
    given: 'a mock appender for Warn level only'
    MockAppender.mock(ControllerTester, Level.WARN)

    and: 'a controller'
    def src = """
    package sample
    import org.simplemes.eframe.web.task.TaskMenuItem
        
    class _ATestController {
      def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'user', uri: '/good', 
                                            displayOrder: 7050, clientRootActivity: false)]
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the tester is run'
    ControllerTester.test {
      controller clazz
      securityCheck false
      taskMenu name: 'user', clientRootActivityX: true
    }

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['clientRootActivityX', 'taskMenu', 'invalid'])
  }
}

