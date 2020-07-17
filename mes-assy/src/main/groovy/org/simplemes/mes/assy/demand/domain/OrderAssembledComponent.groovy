package org.simplemes.mes.assy.demand.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.mes.assy.demand.AssembledComponentStateEnum
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.domain.Product

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.ManyToOne

/**
 * Defines a single component assembled into an order (and optionally an LSN).
 * This includes a 'location' mechanism to track where the component was loaded from on the work center setup.
 * This location can be a bin, shelf or automated feeder location. <p>
 * This object records the long-term component history, so there are no real primary keys for the
 * record.
 * <p>
 * This element informally belongs to the MES Core Order domain.  Since the Order is in another module,
 * the normal child relationship can't be used.
 * Instead, the {@link org.simplemes.mes.assy.application.AssemblyAddition} adds this as a custom child to the core domain.
 *
 */
@DomainEntity
@MappedEntity
@SuppressWarnings("unused")
@ToString(includeNames = true, includePackage = false, excludes = ['order', 'lsn'])
@EqualsAndHashCode(includes = ['uuid'])
class OrderAssembledComponent {

  /**
   * This is the order this component was assembled onto.
   */
  @ManyToOne
  @MappedProperty(type = DataType.UUID)
  Order order

  /**
   * This is the Lot/Serial (LSN) within the order that this component was assembled onto. (<b>Optional</b>)
   */
  @Nullable
  @ManyToOne(targetEntity = LSN)
  @MappedProperty(type = DataType.UUID)
  LSN lsn

  /**
   * A unique sequence for this record.  This is assigned automatically.
   */
  Integer sequence = 0

  /**
   * The sequence from the BOM component requirement (e.g. from ProductComponent).  A value of 0 means a non-BOM
   * component was assembled.  Non-BOM means the component is not in the orders BOM component list.
   */
  Integer bomSequence = 0

  /**
   * The location name.  This is the location the component was loaded from during assembly. <b>(Default: '@')</b>
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = true)
  String location = NameUtils.DEFAULT_KEY

  /**
   * This is the product for the component. <b>(Required)</b>
   */
  @ManyToOne(targetEntity = Product)
  @MappedProperty(type = DataType.UUID)
  Product component

  /**
   * The flexible data type used to define the assembly data for this component.
   * The actual data is stored in a field 'assemblyDataValues' in JSON format.
   */
  @Nullable
  @ManyToOne(targetEntity = FlexType)
  @MappedProperty(type = DataType.UUID)
  FlexType assemblyData

  /**
   * Holder for custom fields.
   */
  @Nullable
  @ExtensibleFieldHolder
  @MappedProperty(type = DataType.JSON)
  String fields

  /**
   * The number of pieces assembled (<b>Optional</b>).
   */
  @Nullable
  BigDecimal qty

  /**
   * The user who assembled this component. (User ID).
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String userName

  /**
   * The work center the component was assembled in <b>(Optional)</b>
   */
  @Nullable
  @ManyToOne(targetEntity = WorkCenter)
  @MappedProperty(type = DataType.UUID)
  WorkCenter workCenter

  /**
   * The current state of this component (can be removed).  (<b>Default:</b> ASSEMBLED)
   */
  @Column(length = AssembledComponentStateEnum.Sizes.ID_LENGTH)
  AssembledComponentStateEnum state = AssembledComponentStateEnum.ASSEMBLED

  /**
   * The user who removed this component. (User ID).
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = true)
  String removedByUserName

  /**
   * The date/time the user removed this component.
   */
  @Nullable @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date removedDate

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  /**
   * The primary key(s) for this child element.
   */
  static keys = ['order', 'sequence']

  /**
   * Internal definitions.
   */
  static fieldOrder = ['lsn', 'sequence', 'component', 'qty', 'userName', 'location', 'state']

}
