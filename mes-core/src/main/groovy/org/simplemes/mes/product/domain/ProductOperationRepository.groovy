/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.product.domain


import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The ProductOperation repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface ProductOperationRepository extends BaseRepository, CrudRepository<ProductOperation, UUID> {

  Optional<ProductOperation> findByUuid(UUID uuid)

  List<ProductOperation> findAllByProduct(Product product)

  List<ProductOperation> list()

}
