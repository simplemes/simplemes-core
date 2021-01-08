/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.domain;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Relation;
import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.data.intercept.annotation.DataMethod;
import io.micronaut.data.jdbc.operations.DefaultJdbcRepositoryOperations;
import io.micronaut.data.model.Association;
import io.micronaut.data.model.DataType;
import io.micronaut.data.model.runtime.RuntimePersistentEntity;
import io.micronaut.data.model.runtime.RuntimePersistentProperty;
import io.micronaut.data.model.runtime.UpdateOperation;
import io.micronaut.data.runtime.date.DateTimeProvider;
import io.micronaut.http.codec.MediaTypeCodec;
import io.micronaut.transaction.TransactionOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * A substitute for the micronaut JdbcRepositoryOperations implementation to add features for the enterprise
 * framework.  Provides: support for optimistic locking.
 * <p>
 * This class is a Java language class since it contains copies of the Micronaut core updateOnt() method.
 * This is done to allow us to access the update row count result from the PreparedStatement.executeUpdate() method
 * call.
 * <p>
 * The code in this file is mostly copied as-is from the core
 * <a href="https://github.com/micronaut-projects/micronaut-data/blob/master/data-jdbc/src/main/java/io/micronaut/data/jdbc/operations/DefaultJdbcRepositoryOperations.java">DefaultJdbcRepositoryOperations</a>
 * The changes to the updateOne() method is noted below.
 * <p>
 * When the core Micronaut logic is changed, this logic will have to be updated too.
 */
public class EFrameJdbcRepositoryOperationsJava extends DefaultJdbcRepositoryOperations {

  /**
   * The logger.
   */
  private static final Logger log = LoggerFactory.getLogger(EFrameJdbcRepositoryOperationsJava.class);

  /**
   * Keep a local copy of the operations so we can use this to check for existing txn on the current thread.
   */
  TransactionOperations<Connection> transactionOperations;

  public EFrameJdbcRepositoryOperationsJava(@Parameter String dataSourceName, DataSource dataSource,
                                            @Parameter TransactionOperations<Connection> transactionOperations,
                                            @Named("io") @Nullable ExecutorService executorService,
                                            BeanContext beanContext, List<MediaTypeCodec> codecs,
                                            @NonNull DateTimeProvider dateTimeProvider) {
    super(dataSourceName, dataSource, transactionOperations, executorService, beanContext, codecs, dateTimeProvider);
    this.transactionOperations = transactionOperations;
  }

  // ****************************************************************
  //  Start of original code from DefaultJdbcRepositoryOperations
  // ****************************************************************

  @NonNull
  @Override
  public <T> T update(@NonNull UpdateOperation<T> operation) {
    final AnnotationMetadata annotationMetadata = operation.getAnnotationMetadata();
    final String[] params = annotationMetadata.stringValues(DataMethod.class, DataMethod.META_MEMBER_PARAMETER_BINDING_PATHS);
    final String query = annotationMetadata.stringValue(Query.class).orElse(null);
    final T entity = operation.getEntity();
    final Set persisted = new HashSet(10);
    final Class<?> repositoryType = operation.getRepositoryType();
    return updateOne(repositoryType, annotationMetadata, query, params, entity, persisted);
  }

  private <T> T updateOne(Class<?> repositoryType, AnnotationMetadata annotationMetadata, String query, String[] params, T entity, Set persisted) {
    Objects.requireNonNull(entity, "Passed entity cannot be null");
    if (StringUtils.isNotEmpty(query) && ArrayUtils.isNotEmpty(params)) {
      final RuntimePersistentEntity<T> persistentEntity =
          (RuntimePersistentEntity<T>) getEntity(entity.getClass());
      return transactionOperations.executeWrite(status -> {
        try {
          Connection connection = status.getConnection();
          if (QUERY_LOG.isDebugEnabled()) {
            QUERY_LOG.debug("Executing SQL UPDATE: {}", query);
          }
          try (PreparedStatement ps = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
              String propertyName = params[i];
              RuntimePersistentProperty<T> pp =
                  persistentEntity.getPropertyByName(propertyName);
              if (pp == null) {
                int j = propertyName.indexOf('.');
                if (j > -1) {
                  RuntimePersistentProperty embeddedProp = (RuntimePersistentProperty)
                      persistentEntity.getPropertyByPath(propertyName).orElse(null);
                  if (embeddedProp != null) {

                    // embedded case
                    pp = persistentEntity.getPropertyByName(propertyName.substring(0, j));
                    if (pp instanceof Association) {
                      Association assoc = (Association) pp;
                      if (assoc.getKind() == Relation.Kind.EMBEDDED) {
                        Object embeddedInstance = pp.getProperty().get(entity);

                        Object embeddedValue = embeddedInstance != null ? embeddedProp.getProperty().get(embeddedInstance) : null;
                        int index = i + 1;
                        preparedStatementWriter.setDynamic(
                            ps,
                            index,
                            embeddedProp.getDataType(),
                            embeddedValue
                        );
                      }
                    }
                  } else {
                    throw new IllegalStateException("Cannot perform update for non-existent property: " + persistentEntity.getSimpleName() + "." + propertyName);
                  }
                } else {
                  throw new IllegalStateException("Cannot perform update for non-existent property: " + persistentEntity.getSimpleName() + "." + propertyName);
                }
              } else {

                final Object newValue;
                final BeanProperty<T, ?> beanProperty = pp.getProperty();
                if (beanProperty.hasAnnotation(DateUpdated.class)) {
                  newValue = dateTimeProvider.getNow();
                  beanProperty.convertAndSet(entity, newValue);
                } else {
                  newValue = beanProperty.get(entity);
                }
                final DataType dataType = pp.getDataType();
                if (dataType == DataType.ENTITY && newValue != null && pp instanceof Association) {
                  final RuntimePersistentProperty<Object> idReader = getIdReader(newValue);
                  final Association association = (Association) pp;
                  final BeanProperty<Object, ?> idReaderProperty = idReader.getProperty();
                  final Object id = idReaderProperty.get(newValue);
                  if (QUERY_LOG.isTraceEnabled()) {
                    QUERY_LOG.trace("Binding parameter at position {} to value {}", i + 1, id);
                  }
                  if (id != null) {

                    preparedStatementWriter.setDynamic(
                        ps,
                        i + 1,
                        idReader.getDataType(),
                        id
                    );
                    if (association.doesCascade(Relation.Cascade.PERSIST) && !persisted.contains(newValue)) {
                      final Relation.Kind kind = association.getKind();
                      final RuntimePersistentEntity associatedEntity = (RuntimePersistentEntity) association.getAssociatedEntity();
                      switch (kind) {
                        case ONE_TO_ONE:
                        case MANY_TO_ONE:
                          persisted.add(newValue);
                          final StoredInsert<Object> updateStatement = resolveEntityUpdate(
                              annotationMetadata,
                              repositoryType,
                              associatedEntity.getIntrospection().getBeanType(),
                              associatedEntity
                          );
                          updateOne(
                              repositoryType,
                              annotationMetadata,
                              updateStatement.getSql(),
                              updateStatement.getParameterBinding(),
                              newValue,
                              persisted
                          );
                          break;
                        case MANY_TO_MANY:
                        case ONE_TO_MANY:
                          // handle cascading updates to collections?

                        case EMBEDDED:
                        default:
                          // embedded type updates
                      }
                    }
                  } else {
                    if (association.doesCascade(Relation.Cascade.PERSIST) && !persisted.contains(newValue)) {
                      final RuntimePersistentEntity associatedEntity = (RuntimePersistentEntity) association.getAssociatedEntity();

                      StoredInsert associatedInsert = resolveEntityInsert(
                          annotationMetadata,
                          repositoryType,
                          associatedEntity.getIntrospection().getBeanType(),
                          associatedEntity
                      );
/*  Disabled due to private method call.  Will throw UnsupportedOperationException.
                      persistOne(
                          annotationMetadata,
                          repositoryType,
                          associatedInsert,
                          newValue,
                          persisted
                      );
*/
                      final Object assignedId = idReaderProperty.get(newValue);
                      if (assignedId != null) {
                        preparedStatementWriter.setDynamic(
                            ps,
                            i + 1,
                            idReader.getDataType(),
                            assignedId
                        );
                      }
                      throw new UnsupportedOperationException("persistOne() not implemented yet");
                    }
                  }
                } else if (dataType == DataType.JSON && jsonCodec != null && newValue != null) {
                  String value = new String(jsonCodec.encode(newValue), StandardCharsets.UTF_8);
                  if (QUERY_LOG.isTraceEnabled()) {
                    QUERY_LOG.trace("Binding parameter at position {} to value {}", i + 1, value);
                  }
                  preparedStatementWriter.setDynamic(
                      ps,
                      i + 1,
                      dataType,
                      value
                  );
                } else {
                  if (QUERY_LOG.isTraceEnabled()) {
                    QUERY_LOG.trace("Binding parameter at position {} to value {}", i + 1, newValue);
                  }
                  preparedStatementWriter.setDynamic(
                      ps,
                      i + 1,
                      dataType,
                      newValue
                  );
                }
              }
            }
            checkUpdateCount(ps.executeUpdate(), entity);
            return entity;
          }
        } catch (SQLException e) {
          throw new DataAccessException("Error executing SQL UPDATE: " + e.getMessage(), e);
        }
      });
    }
    return entity;
  }

  // ****************************************************************
  // End of original code from DefaultJdbcRepositoryOperations
  // ****************************************************************

  public void checkUpdateCount(int count, Object entity) {
    if (count != 1) {
      log.trace("Update count is {}. Expected 1.  Entity: {}", count, entity);
      if (isWorkAround299Enabled()) {
        throw new UpdateFailedException(entity);
      }
    }
  }

  /**
   * Checks on whether workAround299 (optimistic locking) is enabled.
   *
   * @return True if enabled.
   */
  boolean isWorkAround299Enabled() {
    // use reflection to find the setting from the Groovy world.
    try {
      Class clazz = Class.forName("org.simplemes.eframe.application.issues.WorkArounds");
      Field field = clazz.getDeclaredField("workAround299");
      Object o = field.get(null);
      if (o instanceof Boolean) {
        Boolean b = (Boolean) o;
        return b;
      }
    } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
      // Ignore.  Assume no class/field means workAround is false.
    }
    return false;

  }

}
