/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.assy.product.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository
import org.simplemes.mes.product.domain.Product

/**
 * The repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProductComponentRepository extends BaseRepository, CrudRepository<ProductComponent, UUID> {

  Optional<ProductComponent> findByUuid(UUID uuid)

  List<ProductComponent> findAllByProduct(Product product)

  List<ProductComponent> list()

}
