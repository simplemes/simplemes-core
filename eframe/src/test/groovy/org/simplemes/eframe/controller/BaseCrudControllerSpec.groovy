/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.controller


import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.MockRenderer
import org.simplemes.eframe.test.MockSecurityUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.controller.RMAController
import sample.domain.AllFieldsDomain
import sample.domain.RMA
import sample.domain.SampleChild
import sample.domain.SampleParent

/**
 * Tests.
 */
class BaseCrudControllerSpec extends BaseAPISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [RMA, FlexType, SampleParent, User]

  Class<BaseCrudController> buildSampleParentController() {
    def src = """package sample
      import org.simplemes.eframe.controller.BaseCrudController
      import groovy.util.logging.Slf4j
      import io.micronaut.security.annotation.Secured
      import io.micronaut.http.annotation.Controller

      @Slf4j
      @Secured("isAnonymous()")
      @Controller("/sampleParent")
      class SampleParentController  extends BaseCrudController {
      }
    """
    return CompilerTestUtils.compileSource(src)
  }


  @Rollback
  def "verify show creates the correct model and view"() {
    given: 'a domain record'
    def sampleParent = new SampleParent(name: 'ABC').save()

    and: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    when: 'the index is called from the controller'
    def controller = buildSampleParentController().newInstance()
    def res = controller.show(sampleParent.uuid.toString(), new MockPrincipal())

    then: 'the response is correct'
    res.status == HttpStatus.OK
    def model = mock.model
    model[StandardModelAndView.LOGGED_IN] == true
    model[StandardModelAndView.USER_NAME] == SecurityUtils.TEST_USER
    model[StandardModelAndView.MARKER_CONTEXT].controller == controller
    model[StandardModelAndView.MARKER_CONTEXT].view == 'sampleParent/show'
    mock.view == 'sampleParent/show'

    and: 'the domain object is in the model'
    model.sampleParent == sampleParent
  }

  @Rollback
  def "verify show checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller for SampleParent'
    Class clazz = buildSampleParentController()
    def controller = clazz.newInstance()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the page method is called on the controller'
    def res = controller.show('1', null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  @Rollback
  def "verify create generates the correct model and view"() {
    given: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    when: 'the create is called from the controller'
    def controller = buildSampleParentController().newInstance()
    def res = controller.create(new MockPrincipal())

    then: 'the values are set correctly'
    res.status == HttpStatus.OK
    def model = mock.model
    model[StandardModelAndView.LOGGED_IN] == true
    model[StandardModelAndView.USER_NAME] == SecurityUtils.TEST_USER
    model[StandardModelAndView.MARKER_CONTEXT].controller == controller
    model[StandardModelAndView.MARKER_CONTEXT].view == 'sampleParent/create'
    mock.view == 'sampleParent/create'

    and: 'the domain object is in the model with the default values set from the domain class'
    model.sampleParent.moreNotes == new SampleParent().moreNotes
  }

  @Rollback
  def "verify create checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller for SampleParent'
    Class clazz = buildSampleParentController()
    def controller = clazz.newInstance()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the page method is called on the controller'
    def res = controller.create(null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  @Rollback
  def "verify createPost can save a record"() {
    when: 'the save is called from the controller'
    def controller = buildSampleParentController().newInstance()
    def res = controller.createPost(mockRequest(), [name: 'ABC', title: 'abc'], new MockPrincipal())

    then: 'the record is created in the DB'
    def sampleParent = SampleParent.findByName('ABC')
    sampleParent.title == 'abc'
    def id = sampleParent.uuid

    then: 'the HTTP response is correct'
    res.status == HttpStatus.FOUND
    res.headers.get(HttpHeaders.LOCATION) == "/sampleParent/show/${id}"
  }


  def "verify createPost returns the correct show page location for all uppercase controller name"() {
    given: 'a default flex type'
    def flexType = DataGenerator.buildFlexType()

    when: 'the save is called from the controller'
    def controller = new RMAController()
    def res = controller.createPost(mockRequest(),
                                    [rma: 'ABC', rmaType: flexType.id.toString()],
                                    new MockPrincipal('joe', 'MANAGER'))

    then: 'the HTTP response is correct'
    res.status == HttpStatus.FOUND

    and: 'the redirect location is correct'
    def rma = RMA.findByRma('ABC')
    def id = rma.uuid
    res.headers.get(HttpHeaders.LOCATION) == "/rma/show/${id}"
  }


  @Rollback
  def "verify createPost correctly handles a domain validation failure"() {
    given: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    when: 'the save is called from the controller with a missing field'
    def controller = buildSampleParentController().newInstance()
    def res = controller.createPost(mockRequest(uri: '/sampleParent/create'), [title: 'abc'], new MockPrincipal())

    then: 'the HTTP response is correct'
    res.status == HttpStatus.OK
    mock.view == 'sampleParent/create'

    and: 'the model has the errors'
    def model = mock.model
    List<ValidationError> errors = model[ControllerUtils.MODEL_KEY_DOMAIN_ERRORS] as List<ValidationError>
    errors.size() == 1
    errors[0].code == 1
    errors[0].fieldName == 'name'

    and: 'no record is written to the DB'
    SampleParent.count() == 0
  }

  @Rollback
  def "verify createPost correctly handles a child parsing exception"() {
    given: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    when: 'the save is called from the controller with a bad field value'
    def controller = buildSampleParentController().newInstance()
    def params = [name                        : "abc",
                  'sampleChildren[0].key'     : 'ABC',
                  'sampleChildren[0].title'   : 'abc',
                  'sampleChildren[0].sequence': 'gibberish']
    def res = controller.createPost(mockRequest(uri: '/sampleParent/create'), params, new MockPrincipal())

    then: 'the HTTP response is correct'
    res.status == HttpStatus.OK
    mock.view == 'sampleParent/create'

    and: 'the model has the errors'
    def model = mock.model
    List<ValidationError> errors = model[ControllerUtils.MODEL_KEY_DOMAIN_ERRORS] as List<ValidationError>
    errors.size() == 1
    errors[0].code == 206
    errors[0].fieldName == 'sampleChildren.sequence'

    and: 'no record is written to the DB'
    SampleParent.count() == 0
    SampleChild.count() == 0
  }

  @Rollback
  def "verify createPost calls bind extension method on controller"() {
    given: 'a controller with the bind method for extension'
    def src = '''package sample
      import org.simplemes.eframe.controller.BaseCrudController
      import io.micronaut.security.annotation.Secured

      @Secured("isAnonymous()")
      @groovy.util.logging.Slf4j
      class SampleParentController extends BaseCrudController {
        void bindEvent(Object record, params) {
          record.title = 'bind extension' 
        } 
      }
    '''
    Class clazz = CompilerTestUtils.compileSource(src)

    and: 'a mock renderer'
    new MockRenderer(this).install()

    when: 'the save is called from the controller'
    def controller = clazz.newInstance()
    controller.createPost(mockRequest(), [name: 'ABC', title: 'abc'], new MockPrincipal())

    then: 'the record is created in the DB'
    SampleParent.withTransaction {
      def sampleParent = SampleParent.findByName('ABC')
      assert sampleParent.title == 'bind extension'
      true
    }
  }

  @Rollback
  def "verify createPost checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller for SampleParent'
    Class clazz = buildSampleParentController()
    def controller = clazz.newInstance()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the page method is called on the controller'
    def res = controller.createPost(mockRequest(), null, null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  @Rollback
  def "verify edit generates the correct model and view"() {
    given: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    and: 'a domain record'
    def sampleParent = new SampleParent(name: 'ABC', moreNotes: 'more note value').save()

    when: 'the create is called from the controller'
    def controller = buildSampleParentController().newInstance()
    def res = controller.edit(sampleParent.uuid.toString(), new MockPrincipal())

    then: 'the values are set correctly'
    res.status == HttpStatus.OK
    def model = mock.model
    model[StandardModelAndView.LOGGED_IN] == true
    model[StandardModelAndView.USER_NAME] == SecurityUtils.TEST_USER
    model[StandardModelAndView.MARKER_CONTEXT].controller == controller
    model[StandardModelAndView.MARKER_CONTEXT].view == 'sampleParent/edit'
    mock.view == 'sampleParent/edit'

    and: 'the domain object is in the model with the values from the record'
    model.sampleParent.moreNotes == sampleParent.moreNotes
  }

  @Rollback
  def "verify edit checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller for SampleParent'
    Class clazz = buildSampleParentController()
    def controller = clazz.newInstance()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the page method is called on the controller'
    def res = controller.edit('1', null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  @Rollback
  def "verify editPost can save a record"() {
    given: 'a mock renderer'
    new MockRenderer(this).install()

    and: 'a domain record'
    def sampleParent = new SampleParent(name: 'ABC', moreNotes: 'more note value').save()

    when: 'the save is called from the controller'
    def controller = buildSampleParentController().newInstance()
    def res = controller.editPost(mockRequest(), [id: sampleParent.uuid.toString(), title: 'abc'], new MockPrincipal())

    then: 'the record is update in the DB'
    def sampleParent2 = SampleParent.findByName('ABC')
    sampleParent2.title == 'abc'
    def id = sampleParent2.uuid

    then: 'the HTTP response is correct'
    res.status == HttpStatus.FOUND
    res.headers.get(HttpHeaders.LOCATION) == "/sampleParent/show/${id}"
  }

  @Rollback
  def "verify editPost works with a controller with all uppercase domain name"() {
    given: 'a mock renderer'
    new MockRenderer(this).install()

    and: 'a default flex type'
    def flexType = DataGenerator.buildFlexType()

    and: 'a domain record'
    def rma = new RMA(rma: 'ABC', rmaType: flexType).save()


    when: 'the save is called from the controller'
    def controller = new RMAController()
    def res = controller.editPost(mockRequest(), [id: rma.uuid.toString(), status: 'abc'],
                                  new MockPrincipal('joe', 'MANAGER'))

    then: 'the HTTP response is correct'
    res.status == HttpStatus.FOUND

    and: 'the redirect location is correct'
    def rma2 = RMA.findByRma('ABC')
    def id = rma2.uuid
    res.headers.get(HttpHeaders.LOCATION) == "/rma/show/${id}"
  }

  @Rollback
  def "verify editPost calls bind extension method on controller"() {
    given: 'a domain record'
    def sampleParent = new SampleParent(name: 'ABC', moreNotes: 'more note value').save()

    and: 'a mock renderer'
    new MockRenderer(this).install()

    and: 'a controller with the bind method for extension'
    def src = '''package sample
      import org.simplemes.eframe.controller.BaseCrudController
      import io.micronaut.security.annotation.Secured

      @Secured("isAnonymous()")
      @groovy.util.logging.Slf4j
      class SampleParentController extends BaseCrudController {
        void bindEvent(Object record, params) {
          record.title = 'bind extension' 
        } 
      }
    '''
    Class clazz = CompilerTestUtils.compileSource(src)

    when: 'the save is called from the controller'
    def controller = clazz.newInstance()
    controller.editPost(mockRequest(), [id: sampleParent.uuid.toString(), title: 'abc'], new MockPrincipal())


    then: 'the record is created in the DB'
    def sampleParent2 = SampleParent.findByName('ABC')
    sampleParent2.title == 'bind extension'
  }

  @Rollback
  def "verify editPost correctly handles a domain validation failure"() {
    given: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    and: 'a domain record'
    def sampleParent = new SampleParent(name: 'ABC', title: 'original').save()

    when: 'the save is called from the controller with a field value too long'
    def controller = buildSampleParentController().newInstance()
    def res = controller.editPost(mockRequest(uri: '/sampleParent/create'),
                                  [id: sampleParent.uuid.toString(), title: 'A' * 21], new MockPrincipal())

    then: 'the HTTP response is correct'
    res.status == HttpStatus.OK
    mock.view == 'sampleParent/edit'
    mock.model.sampleParent.title == "${'A' * 21}"

    and: 'the record is unchanged in the DB'
    def sampleParent2 = SampleParent.findByName('ABC')
    sampleParent2.title == sampleParent.title
  }

  @Rollback
  def "verify editPost correctly handles a binder parse validation failure"() {
    given: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    and: 'a domain record'
    def sampleParent = new SampleParent(name: 'ABC').save()

    when: 'the save is called from the controller with a field value too long'
    def controller = buildSampleParentController().newInstance()
    def params = [id                          : sampleParent.uuid.toString(),
                  title                       : " 'new title",
                  'sampleChildren[0].key'     : 'ABC',
                  'sampleChildren[0].title'   : 'abc',
                  'sampleChildren[0].sequence': 'gibberish']
    def res = controller.editPost(mockRequest(uri: '/sampleParent/create'), params, new MockPrincipal())

    then: 'the HTTP response is correct'
    res.status == HttpStatus.OK
    mock.view == 'sampleParent/edit'

    and: 'the record is unchanged in the DB'
    def sampleParent2 = SampleParent.findByName('ABC')
    sampleParent2.title == sampleParent.title
  }

  @Rollback
  def "verify editPost checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller for SampleParent'
    Class clazz = buildSampleParentController()
    def controller = clazz.newInstance()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the page method is called on the controller'
    def res = controller.editPost(mockRequest(), null, null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  def "verify getView can be overridden"() {
    given: 'a controller with invalid domain'
    def src = '''package sample
      import org.simplemes.eframe.controller.BaseCrudController
      import groovy.util.logging.Slf4j
      import io.micronaut.security.annotation.Secured

      @Slf4j
      @Secured("isAnonymous()")
      class _TestController extends BaseCrudController {
        String getView(String methodName) {
          return "someOtherPath/forViews/$methodName"
        } 
      }
    '''
    Class clazz = CompilerTestUtils.compileSource(src)

    when: 'the list is called from the controller'
    def controller = clazz.newInstance()
    def view = controller.getView('method1')

    then: 'the override is Ok'
    view == "someOtherPath/forViews/method1"
  }

  @Rollback
  def "verify delete can delete a record"() {
    given: 'a domain record'
    def sampleParent = new SampleParent(name: 'ABC<script>').save()

    when: 'the save is called from the controller'
    def controller = buildSampleParentController().newInstance()
    def res = controller.delete(mockRequest(), [id: sampleParent.uuid.toString()], new MockPrincipal())

    then: 'the record is deleted from the DB'
    SampleParent.count() == 0

    and: 'the HTTP response is correct'
    def domainName = GlobalUtils.lookup("${NameUtils.lowercaseFirstLetter(SampleParent.simpleName)}.label")
    def s = TypeUtils.toShortString(sampleParent)
    def msg = URLEncoder.encode(GlobalUtils.lookup('deleted.message', null, domainName, s), "UTF-8")
    res.status == HttpStatus.FOUND
    res.headers.get(HttpHeaders.LOCATION) == "/sampleParent?_info=$msg"
  }

  @Rollback
  def "verify delete can work with a controller with an uppercase domain name"() {
    given: 'a default flex type'
    def flexType = DataGenerator.buildFlexType()

    and: 'a domain record'
    def rma = new RMA(rma: 'ABC', rmaType: flexType).save()

    when: 'the save is called from the controller'
    def controller = new RMAController()
    def res = controller.delete(mockRequest(), [id: rma.uuid.toString()],
                                new MockPrincipal('joe', 'MANAGER'))

    then: 'the redirect location is correct'
    res.status == HttpStatus.FOUND
    res.headers.get(HttpHeaders.LOCATION).startsWith('/rma')
  }

  @Rollback
  def "verify delete handles related records"() {
    given: 'a domain with some related files - an AllFieldsDomain with the same key field'
    new AllFieldsDomain(name: 'SAMPLE').save()
    def sampleParent = new SampleParent(name: 'SAMPLE', title: 'Sample').save()


    when: 'the save is called from the controller'
    def controller = buildSampleParentController().newInstance()
    def res = controller.delete(mockRequest(), [id: sampleParent.uuid.toString()], new MockPrincipal())

    then: 'the records are deleted from the DB'
    SampleParent.count() == 0
    AllFieldsDomain.count() == 0

    and: 'the response is good'
    res.status == HttpStatus.FOUND
  }

  @Rollback
  def "verify delete gracefully handles record not found"() {
    given: 'a bad id'
    def id = '94757876'

    when: 'the save is called from the controller'
    def controller = buildSampleParentController().newInstance()
    def res = controller.delete(mockRequest(), [id: id], new MockPrincipal())

    then: 'the HTTP response is correct'
    def msg = GlobalUtils.lookup('error.105.message', null, id)
    res.status == HttpStatus.FOUND
    res.headers.get(HttpHeaders.LOCATION) == "/sampleParent?_error=$msg"
  }

  @Rollback
  def "verify delete checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller for SampleParent'
    def controller = buildSampleParentController().newInstance()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the list is called from the controller'
    def res = controller.delete(mockRequest(), '1', null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  def "verify that delete works in a live server with related records"() {
    given: 'a record to delete'
    def record1 = null
    SampleParent.withTransaction {
      record1 = new SampleParent(name: 'SAMPLE', title: 'Sample').save()
    }

    when:
    login()
    sendRequest(uri: "/sample/delete", method: 'post',
                content: [id: record1.uuid.toString()], status: HttpStatus.FOUND)

    then: 'the record is removed from the DB'
    AllFieldsDomain.withTransaction {
      assert SampleParent.count() == 0
      assert AllFieldsDomain.count() == 0
      true
    }
  }


  // test show/edit with record not found behavior.

  /*
  def "verify handleList with filtering works"() {
  def "verify handleList works - with a post-processing closure"() {
   */
}
