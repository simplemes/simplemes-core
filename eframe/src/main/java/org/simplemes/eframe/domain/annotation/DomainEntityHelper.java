package org.simplemes.eframe.domain.annotation;

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
 */

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import groovy.lang.Closure;
import io.micronaut.context.ApplicationContext;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.repository.GenericRepository;
import io.micronaut.transaction.SynchronousTransactionManager;
import io.micronaut.transaction.TransactionCallback;
import io.micronaut.transaction.TransactionStatus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Support methods for the @DomainEntity annotation.  Provides much of the logic injected into the domain
 * class.  This includes support for methods like list(), findById(), save(), etc.
 */
public class DomainEntityHelper {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  protected static DomainEntityHelper instance = new DomainEntityHelper();

  /**
   * Determine the repository associated with the given domain class. This is not for public access.
   * This is used only in the code inserted into the domain by the @DomainEntity annotation.
   *
   * @param clazz The domain class to check for a repository.
   * @return The repository bean.
   */
  @SuppressWarnings("unchecked")
  GenericRepository determineRepository(Class clazz) throws ClassNotFoundException {
    Class repoClazz = getRepositoryFromAnnotation(clazz);
    if (repoClazz == Object.class) {
      String className = clazz.getName() + "Repository";
      repoClazz = Class.forName(className);
    }

    return (GenericRepository) getApplicationContext().getBean(repoClazz);
  }

  /**
   * Determine the repository from the @DomainEntity annotation.
   *
   * @param clazz The domain class to get the repository for.
   * @return The repository class.
   */
  protected Class getRepositoryFromAnnotation(Class clazz) {
    DomainEntity annotation = (DomainEntity) clazz.getAnnotation(DomainEntity.class);
    if (annotation != null) {
      return annotation.repository();
    } else {
      return null;
    }
  }

  /**
   * Saves the given record.  If the record is new, this does an insert.  If not new, then it updates the record).
   * If the object has a uuid then assumes an update() is needed.
   * The repository must be a CrudRepository.
   *
   * @param object The domain object to save.
   * @return The object after saving.
   */
  Object save(DomainEntityInterface object) {
    CrudRepository repo = (CrudRepository) getRepository(object.getClass());
    if (repo == null) {
      throw new IllegalArgumentException("Missing repository for " + object.getClass());
    }
    if (object.getUuid() == null) {
      repo.save(object);
    } else {
      repo.update(object);
    }

    return object;
  }

  /**
   * Deletes the given record.
   *
   * @param object The domain object to delete.
   * @return The object that was deleted.
   */
  Object delete(DomainEntityInterface object) {
    CrudRepository repo = (CrudRepository) getRepository(object.getClass());
    if (repo == null) {
      throw new IllegalArgumentException("Missing repository for " + object.getClass());
    }
    //noinspection unchecked
    repo.delete(object);

    return object;
  }

  /**
   * Gets the repository from the domain class (static) field.
   *
   * @param clazz The domain class to get the repo for.
   * @return The repository.
   */
  @SuppressWarnings("unchecked")
  GenericRepository getRepository(Class clazz) {
    if (clazz != null) {
      try {
        //Class[] args = {};
        Method method = clazz.getMethod("getRepository");
        if (method != null) {
          return (GenericRepository) method.invoke(null);
        }
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        // Intentionally ignored.
        //ignored.printStackTrace();
      }
    }

    return null;
  }


  /**
   * Executes the list() method on the domain object's repository.
   *
   * @param domainClazz The domain class to read from.
   * @return The list.
   */
  @SuppressWarnings("unchecked")
  public List list(Class domainClazz) {
    CrudRepository repo = (CrudRepository) getRepository(domainClazz);
    Iterable iter = repo.findAll();
    List list = new ArrayList();
    for (Object record : iter) {
      list.add(record);
    }
    return list;
  }


  /**
   * Handles missing static methods in a @DomainEntity.  Provides a link to the repository's findByXYZ() methods.
   *
   * @param domainClazz The domain class the static method is called from.
   * @param methodName  The method name.
   * @param args        The arguments for the method.
   * @return The results of the method call, or throws an exception if missing.
   */
  @SuppressWarnings("unused")
  public Object staticMethodMissingHandler(Class domainClazz, String methodName, Object[] args)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    //System.out.println("domainClazz:" + domainClazz+" method: "+methodName+" args:"+args.length);
    //System.out.println("  args:"+args.getClass());
    Class<?>[] paramTypes = null;
    if (args.length > 0) {
      paramTypes = new Class<?>[args.length];
      for (int i = 0; i < args.length; i++) {
        paramTypes[i] = args[i].getClass();
      }
      //System.out.println("paramTypes:" + paramTypes[0]);
    }
    CrudRepository repo = (CrudRepository) getRepository(domainClazz);
    Method method = repo.getClass().getDeclaredMethod(methodName, paramTypes);
    // For some reason, the class generated by Micronaut-data creates the class with protected packaging.
    // We need to make this method accessible for this invocation.
    method.setAccessible(true);

    Object res = method.invoke(repo, args);
    if (methodName.startsWith("findBy") && res instanceof Optional) {
      // Strip the Optional wrapper for the findBy() case.
      //noinspection unchecked
      res = ((Optional) res).orElse(null);
    }
    return res;
  }

  /**
   * A cached context.
   */
  ApplicationContext applicationContext;

  /**
   * Get the application context from the holders.
   *
   * @return The context.
   */
  @SuppressWarnings("unchecked")
  public ApplicationContext getApplicationContext() {
    if (applicationContext == null) {
      // Use reflection to access the Holders.getApplicationContext() at run time since the Groovy
      // classes are not visible when this .java class is compiled.
      try {
        Class holdersClass = Class.forName("org.simplemes.eframe.application.Holders");
        Method method = holdersClass.getMethod("getApplicationContext");
        if (method != null) {
          applicationContext = (ApplicationContext) method.invoke(null);
        }
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException ignored) {
        // Intentionally ignored.
        //ignored.printStackTrace();
      }

    }
    return applicationContext;
  }

  /**
   * Get the current transaction manager.
   *
   * @return The transaction manager.
   */
  public SynchronousTransactionManager getTransactionManager() {
    return getApplicationContext().getBean(SynchronousTransactionManager.class);
  }

  /**
   * Start a transaction and rollback when finished.
   */
  @SuppressWarnings({"unchecked", "unused"})
  public void executeWrite(TransactionCallback closure) {
    SynchronousTransactionManager manager = getTransactionManager();
    manager.executeWrite(closure);
  }

  /**
   * Start a transaction and rollback when finished.
   */
  @SuppressWarnings({"unchecked", "unused"})
  public void executeWriteClosure(Class delegate, Closure closure) {
    SynchronousTransactionManager manager = getTransactionManager();
    manager.executeWrite(new TransactionCallbackWrapper(closure));
  }


  public static DomainEntityHelper getInstance() {
    return instance;
  }

  public static void setInstance(DomainEntityHelper instance) {
    DomainEntityHelper.instance = instance;
  }

  /**
   * Local class used to call the closure from the as a TransactionCallback.
   */
  protected static class TransactionCallbackWrapper implements TransactionCallback {

    Closure closure;

    public TransactionCallbackWrapper(Closure closure) {
      this.closure = closure;
    }

    /**
     * Code that runs within the context of a transaction will implement this method.
     *
     * @param status The transaction status.
     * @return The return value
     */
    @Nullable
    @Override
    public Object call(@NonNull TransactionStatus status) {
      return closure.call(status);
    }

    /**
     * Applies this function to the given argument.
     *
     * @param o the function argument
     * @return the function result
     */
    @Override
    public Object apply(Object o) {
      return null;
    }
  }
}
