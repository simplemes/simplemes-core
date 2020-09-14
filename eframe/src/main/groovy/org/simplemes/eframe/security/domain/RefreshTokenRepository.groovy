/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.domain


import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The RefreshToken repository base interface.  Provides the methods for the repo.
 */
@SuppressWarnings("unused")
@JdbcRepository(dialect = Dialect.POSTGRES)
interface RefreshTokenRepository extends BaseRepository, CrudRepository<RefreshToken, UUID> {

  List<RefreshToken> findAllByUserName(String userName)

  Optional<RefreshToken> findByRefreshToken(String refreshToken)

  Optional<RefreshToken> findByUuid(UUID uuid)

  List<RefreshToken> list()

}
