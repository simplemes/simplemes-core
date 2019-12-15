package sample.domain

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity

import javax.persistence.Column

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
//@Singleton
@MappedEntity('ordr')
@DomainEntity
@ToString(includeNames = true)
@EqualsAndHashCode(includes = ['uuid'])
@CompileStatic
class Order2 extends BaseWIPEntity {

  @Column(name = 'ordr')
  String order
  BigDecimal qtyToBuild = 1.0

  // TODO: Add product foreign reference

  //@OneToMany(targetEntity=OrderLine, cascade= CascadeType.ALL, mappedBy="order")
  //List<OrderLine> orderLines = [] 


  Integer version = 0
  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated
  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  @Id @AutoPopulated UUID uuid;

  Order2(String order) {
    this.order = order
  }

  Order2(String order, UUID uuid) {
    this.order = order
    this.uuid = uuid
  }

  /*
  @Nullable
  Product getAlternateProduct() {
    if (alternateProductUuid) {
      println "alternateProductUuid = $alternateProductUuid"
      def productRepository = Application.applicationContext.getBean(ProductRepository)
      alternateProduct = productRepository.findById(alternateProductUuid).orElse(null)
    }
    return alternateProduct
  }

  void setAlternateProduct(@Nullable Product alternateProduct) {
    this.alternateProductUuid = alternateProduct?.uuid
  }

  // TODO: Add via transform?
  static Object findById(UUID uuid) {
    findById(this, uuid)
  }
*/
}