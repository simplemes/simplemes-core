/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain

import edu.umd.cs.findbugs.annotations.NonNull
import edu.umd.cs.findbugs.annotations.Nullable
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.EachBean
import io.micronaut.context.annotation.Parameter
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.data.annotation.Query
import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.data.jdbc.operations.DefaultJdbcRepositoryOperations
import io.micronaut.data.model.runtime.InsertOperation
import io.micronaut.data.model.runtime.PreparedQuery
import io.micronaut.data.model.runtime.UpdateOperation
import io.micronaut.data.runtime.date.DateTimeProvider
import io.micronaut.http.codec.MediaTypeCodec
import io.micronaut.transaction.TransactionOperations
import io.micronaut.transaction.jdbc.DataSourceUtils
import io.micronaut.transaction.jdbc.exceptions.CannotGetJdbcConnectionException
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.application.issues.WorkArounds

import javax.annotation.Nonnull
import javax.inject.Named
import javax.sql.DataSource
import java.lang.annotation.Annotation
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.concurrent.ExecutorService

/**
 * A substitute for the micronaut JdbcRepositoryOperations implementation to add features for the enterprise
 * framework.  Provides: Check for transactions on updates, support for optimistic locking and possible
 * work-arounds for issues.
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>trace</b> - Logs the queries that are checked for transaction. </li>
 * </ul>
 */

@Slf4j
@CompileStatic
@EachBean(DataSource.class)
class EFrameJdbcRepositoryOperations extends DefaultJdbcRepositoryOperations {

  /**
   * Keep a local copy of the operations so we can use this to check for existing txn on the current thread.
   */
  TransactionOperations<Connection> localTransactionOperations

  /**
   * Keep a local copy of the operations so we can use this to check for existing txn on the current thread from Java
   * classes.  This is done to prevent a connection leak that happens when we directly call getConnection()
   * outside of a txn.
   */
  static TransactionOperations<Connection> localTransactionOperationsStatic

  EFrameJdbcRepositoryOperations(@Parameter String dataSourceName, DataSource dataSource,
                                 @Parameter TransactionOperations<Connection> transactionOperations,
                                 @Named("io") @Nullable ExecutorService executorService,
                                 BeanContext beanContext, List<MediaTypeCodec> codecs,
                                 @NonNull DateTimeProvider dateTimeProvider) {
    super(dataSourceName, dataSource, transactionOperations, executorService, beanContext, codecs, dateTimeProvider)
    localTransactionOperations = transactionOperations
    localTransactionOperationsStatic = transactionOperations
  }

  /**
   * Read an entity using the given prefix to be passes to result set lookups.
   * @param resultSet The result set
   * @param type The entity type
   * @throws DataAccessException if it is not possible read the result from the result set.
   * @return The entity result
   */
  @Override
  <T> T persist(@NonNull InsertOperation<T> operation) {
    //AnnotationMetadata annotationMetadata = operation.getAnnotationMetadata()
    //String[] params = annotationMetadata.stringValues(DataMethod.class, DataMethod.META_MEMBER_PARAMETER_BINDING_PATHS)
    //println "params = $params ${operation.entity.getProperties()}"
    //String query = annotationMetadata.stringValue(Query.class).orElse(null)
    //println "query = $query"
    checkForTransaction(operation)
    return super.persist(operation)
  }

  @Override
  @NonNull
  Optional<Number> executeDelete(@NonNull PreparedQuery<?, Number> preparedQuery) {
    checkForTransaction(preparedQuery)
    return executeUpdate(preparedQuery)
  }

  @Override
  <T> T update(@NonNull UpdateOperation<T> operation) {
    checkForTransaction(operation)
    if (WorkArounds.workAroundOptimistic) {
      //AnnotationMetadata annotationMetadata = operation.getAnnotationMetadata()
      //String[] params = annotationMetadata.stringValues(DataMethod.class, DataMethod.META_MEMBER_PARAMETER_BINDING_PATHS)
      //String query = annotationMetadata.stringValue(Query.class).orElse(null)
      //println "thread = ${Thread.currentThread()} ${Thread.currentThread().dump()}"
      //operation.entity.version = operation.entity.version + 1
      //println "update2() operation = $operation ${operation.entity} ${operation.method}"
      //println "operation = $operation"
      return super.update(new AlterableUpdateOperation(operation))
    } else {
      return super.update(operation)
    }
  }

  /**
   * Ensure that the current SQL is being executed in a transaction.
   */
  void checkForTransaction(Object context) {
    // This relies on an undocumented feature of the TransactionOperations that fail when no txn is active on the connection.
    try {
      if (log.traceEnabled) {
        def sql = 'Unknown'
        if (context instanceof InsertOperation || context instanceof UpdateOperation) {
          AnnotationMetadata annotationMetadata = context.getAnnotationMetadata()
          //String[] params = annotationMetadata.stringValues(DataMethod.class, DataMethod.META_MEMBER_PARAMETER_BINDING_PATHS)
          //println "params = $params ${operation.entity.getProperties()}"
          sql = annotationMetadata.stringValue(Query.class).orElse(null)
          //println "query = $query"
        } else if (context instanceof PreparedQuery) {
          sql = context.query
        }
        log.trace("checkForTransaction() Query: {}", sql)
      }
      localTransactionOperations.getConnection()
    } catch (CannotGetJdbcConnectionException e) {
      def s = "No active transaction for SQL insert/update/delete.  Use domain.withTransaction, @Transactional or @Rollback for Beans."
      throw new IllegalStateException(s, e)
    }
    //println "connection = $connection"
  }

  /**
   * Ensure that the current SQL is being executed in a transaction - Static version for a single data source.
   */
  @SuppressWarnings("unused")
  static void checkForTransactionStatic() {
    // This relies on an undocumented feature of the TransactionOperations that fail when no txn is active on the connection.
    try {
      localTransactionOperationsStatic.getConnection()
    } catch (CannotGetJdbcConnectionException e) {
      def s = "No active transaction for SQL statement.  Use domain.withTransaction or @Transactional for Beans. (This avoids a connection leak)."
      throw new IllegalStateException(s, e)
    }
  }


  /**
   * Creates a prepared SQL statement.
   * @param sql The SQL.
   * @return The statement.
   */
  private PreparedStatement getPreparedStatement192(String sql) throws SQLException {
    DataSource dataSource = Holders.getApplicationContext().getBean(DataSource.class)
    Connection connection = DataSourceUtils.getConnection(dataSource)
    return connection.prepareStatement(sql)
  }

  /**
   * Gets the (first) generic type for the given field.  Works for fields like: List&lt;XYZ&gt;.
   *
   * @param field The field.
   * @return The underlying type (from the generic definition).  Can be null.
   */
  Class<?> getGenericType192(Field field) {
    Type childType = field.getGenericType()
    if (!(childType instanceof ParameterizedType)) {
      return null
    }
    ParameterizedType childParameterizedType = (ParameterizedType) childType
    return (Class<?>) childParameterizedType.getActualTypeArguments()[0]
  }


}

class AlterableUpdateOperation implements UpdateOperation {
  UpdateOperation originalOperation
  AnnotationMetadata annotationMetadata

  AlterableUpdateOperation(UpdateOperation operation) {
    originalOperation = operation
    annotationMetadata = new AlterableAnnotationMetadata(operation.annotationMetadata, this)
  }


  @Override
  AnnotationMetadata getAnnotationMetadata() {
    return annotationMetadata
  }

  /**
   * @return The entity to insert.
   */
  @Override
  Object getEntity() {
    return originalOperation.getEntity()
  }

  /**
   * The root entity type.
   *
   * @return The root entity type
   */
  @Override
  Class getRootEntity() {
    return originalOperation.getRootEntity()
  }

  /**
   * @return The repository type.
   */
  @Override
  Class<?> getRepositoryType() {
    return originalOperation.getRepositoryType()
  }

  /**
   * @return The name of the component
   */
  @Override
  String getName() {
    return originalOperation.getName()
  }


}

@Slf4j
class AlterableAnnotationMetadata implements AnnotationMetadata {
  /*@Delegate*/
  AnnotationMetadata originalAnnotationMetadata
  UpdateOperation operation

  AlterableAnnotationMetadata(AnnotationMetadata annotationMetadata, UpdateOperation operation) {
    originalAnnotationMetadata = annotationMetadata
    this.operation = operation
  }

  /**
   * The value as an optional string for the given annotation and member.
   * This altered annotation meta data will fix WHERE clause for optimistic checking.
   *
   * @param annotation The annotation
   * @return The string value if it is present
   */
  @Override
  Optional<String> stringValue(@Nonnull Class<? extends Annotation> annotation) {
    def res = originalAnnotationMetadata.stringValue(annotation)
    if (annotation == Query && operation.entity.hasProperty("version")) {
      def version = incrementVersion(operation.entity)
      def query = res.orElse(null)
      def quote = '`'
      if (query.contains('`uuid`')) {
        quote = '`'
      }
      if (query.contains('"uuid"')) {
        quote = '"'
      }
      def whereClause = """WHERE (${quote}uuid${quote} = ?)"""
      if (query.endsWith(whereClause)) {
        def loc = query.indexOf(whereClause) - 1
        query = query[0..loc] + "$whereClause and (${quote}version${quote} = ${version - 1})"
        log.debug('altered query for optimistic locking: {} ', query)
        res = Optional.of(query)
      }
    }
    //println "query = ${res.get()}"

    return res
  }

  /**
   * The values as string array for the given annotation and member.
   *
   * @param annotation The annotation
   * @param member The member
   * @return The string values if it is present
   */
  @Override
  String[] stringValues(@Nonnull Class<? extends Annotation> annotation, @Nonnull String member) {
    def values = originalAnnotationMetadata.stringValues(annotation, member)

    return values
  }

  @CompileDynamic
  Integer incrementVersion(Object entity) {
    entity.version++
    return entity.version
  }

}

