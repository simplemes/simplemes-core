package org.simplemes.eframe.web.javascript


import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.test.BaseJSSpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.AllFieldsDomain
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of the eframe.js methods related to Ajax request to the server.
 */
@IgnoreIf({ !sys['geb.env'] })
class EframeJSAjaxGUISpec extends BaseJSSpecification {

  static dirtyDomains = [AllFieldsDomain]

  def "verify that get works"() {
    given: 'some JS to make a get call'
    def src = """
      ef.get('/allFieldsDomain/crud/ABC',{},function(s){
        displayResult(s)
      });
    """

    and: 'a test record'
    AllFieldsDomain.withTransaction {
      new AllFieldsDomain(name: 'ABC', title: 'abc').save()
    }

    when: 'the JS is executed'
    login()
    execute(src)

    then: 'the response is displayed'
    waitFor {
      result.text() != ''
    }
    result.text().contains('ABC')
    result.text().contains('abc')
  }

  def "verify that get fails gracefully and displays a message"() {
    given: 'some JS to make a bad get call'
    def src = """
      ef.get('/gibberish',{});
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the response is displayed'
    waitFor {
      messages.text() != ''
    }
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['/gibberish', 'get', 'failed', 'forbidden', '403'])
  }

  def "verify that get fails gracefully with a bad url and displays a message"() {
    given: 'some JS to make a bad call'
    def src = """
      ef.get('httpX://localhost:80/gibberish');
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the response is displayed'
    waitFor {
      messages.text() != ''
    }
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['/gibberish', 'get', 'failed'])
  }

  def "verify that post works"() {
    given: 'some JS to make a post call'
    def src = """
      var postData = {};
      postData.event = "ColumnResized";
      postData.pageURI = '/some/page';
      postData.element = 'htmlID';
      postData.column = 'aColumn';
      postData.newSize = 23.7;

      ef.post("/userPreference/guiStateChanged", postData,
        function (responseText) {  
          displayResult('POST Response is: '+responseText);
        }
      );
    """

    and: 'a test record'
    AllFieldsDomain.withTransaction {
      new AllFieldsDomain(name: 'ABC', title: 'abc').save()
    }

    when: 'the JS is executed'
    login()
    execute(src)

    then: 'the response is displayed'
    waitFor {
      result.text() != ''
    }
    result.text().contains('POST Response')

    and: 'the server-side has processed this into a record with the specified data'
    waitFor {
      def count = 0
      UserPreference.withTransaction {
        count = UserPreference.count()
      }
      count != 0
    }
    PreferenceHolder preference = PreferenceHolder.find {
      page '/some/page'
      user 'admin'
      element 'htmlID'
    }
    def columnPref = preference['aColumn']
    columnPref.width == 23.7
  }

  def "verify that post fails gracefully and displays a message"() {
    given: 'some JS to make a bad call'
    def src = """
      ef.post('/gibberish',{});
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the response is displayed'
    waitFor {
      messages.text() != ''
    }
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['/gibberish', 'post', 'failed', 'forbidden', '403'])
  }

  def "verify that post fails gracefully with a bad url and displays a message"() {
    given: 'some JS to make a bad call'
    def src = """
      ef.post('httpX://localhost:80/gibberish',{});
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the response is displayed'
    waitFor {
      messages.text() != ''
    }
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['/gibberish', 'post', 'failed'])
  }

}
