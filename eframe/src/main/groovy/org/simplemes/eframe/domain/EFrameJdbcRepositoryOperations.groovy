package org.simplemes.eframe.domain

import edu.umd.cs.findbugs.annotations.NonNull
import edu.umd.cs.findbugs.annotations.Nullable
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.EachBean
import io.micronaut.context.annotation.Parameter
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.data.annotation.Query
import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.data.intercept.annotation.DataMethod
import io.micronaut.data.jdbc.operations.DefaultJdbcRepositoryOperations
import io.micronaut.data.model.runtime.InsertOperation
import io.micronaut.data.model.runtime.UpdateOperation
import io.micronaut.http.codec.MediaTypeCodec
import io.micronaut.transaction.TransactionOperations

import javax.annotation.Nonnull
import javax.inject.Named
import javax.sql.DataSource
import java.lang.annotation.Annotation
import java.sql.Connection
import java.util.concurrent.ExecutorService

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */

@CompileStatic
@EachBean(DataSource.class)
class EFrameJdbcRepositoryOperations extends DefaultJdbcRepositoryOperations {

  EFrameJdbcRepositoryOperations(@Parameter String dataSourceName, DataSource dataSource,
                                 @Parameter TransactionOperations<Connection> transactionOperations,
                                 @Named("io") @Nullable ExecutorService executorService,
                                 BeanContext beanContext, List<MediaTypeCodec> codecs) {
    super(dataSourceName, dataSource, transactionOperations, executorService, beanContext, codecs)
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
    //String query = annotationMetadata.stringValue(Query.class).orElse(null)
    //println "query = $query"
    return super.persist(operation)
  }

  @Override
  <T> T update(@NonNull UpdateOperation<T> operation) {
    //AnnotationMetadata annotationMetadata = operation.getAnnotationMetadata()
    //String[] params = annotationMetadata.stringValues(DataMethod.class, DataMethod.META_MEMBER_PARAMETER_BINDING_PATHS)
    //String query = annotationMetadata.stringValue(Query.class).orElse(null)
    //println "thread = ${Thread.currentThread()} ${Thread.currentThread().dump()}"
    //operation.entity.version = operation.entity.version + 1
    //println "query2 = $query $params $annotationMetadata"
    //println "update2() operation = $operation ${operation.entity} ${operation.method}"
    //println "operation = $operation"

    //return super.update(operation)
    return super.update(new AlterableUpdateOperation(operation))
  }
}

// TODO: Find out why this won't compile with @Delegate
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
    if (annotation == Query) {
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
        println "altered query for optimistic locking= $query"
        res = Optional.of(query)
      }
    }

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

    // TODO: Remove temporary fix for issue with using uuid in update.
    // See issue: https://github.com/micronaut-projects/micronaut-data/issues/323
    if (annotation == DataMethod && member == DataMethod.META_MEMBER_PARAMETER_BINDING_PATHS) {
      if (values.contains('id')) {
        // Convert 'id' 'uuid'.
        for (int i = 0; i < values.length; i++) {
          if (values[i] == 'id') {
            values[i] = 'uuid'
          }
        }
      }
    }

    return values
  }

  @CompileDynamic
  Integer incrementVersion(Object entity) {
    entity.version++
    return entity.version
  }

}

