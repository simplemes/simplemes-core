package org.simplemes.eframe.web.ui

import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.data.format.ConfigurableTypeDomainFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainRefListFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.web.ui.webix.widget.BooleanFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.ComboboxWidget
import org.simplemes.eframe.web.ui.webix.widget.ConfigurableTypeFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.DateFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.DateOnlyFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.GridWidget
import org.simplemes.eframe.web.ui.webix.widget.MultiComboboxWidget
import org.simplemes.eframe.web.ui.webix.widget.NumberFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.TextFieldWidget
import org.simplemes.eframe.web.ui.webix.widget.WidgetContext

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class WidgetFactorySpec extends BaseSpecification {


  def "verify that build handles the supported cases"() {
    given: 'a widgetContext for the field definition'
    def widgetContext = new WidgetContext()
    widgetContext.fieldDefinition = new SimpleFieldDefinition(name: 'aField', format: format.instance)

    when: 'the factory builds the widget'
    def widget = WidgetFactory.instance.build(widgetContext)

    then: 'it is the correct widget'
    widget.getClass() == widgetClass

    and: 'and has the widget context passed to it'
    widget.widgetContext == widgetContext

    where:
    format                       | widgetClass
    StringFieldFormat            | TextFieldWidget
    BigDecimalFieldFormat        | NumberFieldWidget
    IntegerFieldFormat           | NumberFieldWidget
    LongFieldFormat              | NumberFieldWidget
    BooleanFieldFormat           | BooleanFieldWidget
    DateFieldFormat              | DateFieldWidget
    DateOnlyFieldFormat          | DateOnlyFieldWidget
    EnumFieldFormat              | ComboboxWidget
    EncodedTypeFieldFormat       | ComboboxWidget
    DomainReferenceFieldFormat   | ComboboxWidget
    ChildListFieldFormat         | GridWidget
    DomainRefListFieldFormat     | MultiComboboxWidget
    ConfigurableTypeDomainFormat | ConfigurableTypeFieldWidget
  }

}
