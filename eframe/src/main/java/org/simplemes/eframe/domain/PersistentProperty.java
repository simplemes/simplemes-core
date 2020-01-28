/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain;/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
 */

import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Defines a single persistent property for a given domain object.
 */
public class PersistentProperty {

  /**
   * The property/field Name.
   */
  String name;

  /**
   * The field type.
   */
  Class type;

  /**
   * If true, then null is allowed in the field.
   */
  boolean nullable = false;

  /**
   * The max length (String only fields).  Defaults to 255 if @Column is not defined.  0 Means no max length.
   */
  Integer maxLength;

  /**
   * The Field from the Class definition.
   */
  Field field;

  /**
   * Defines the ultimate object this property references.  This is always a DomainEntity.
   * In some cases, the type is Collection and this referencedType indicates the type the list
   * contains.
   * Requires a Field to work.
   */
  public Class referenceType;

  /**
   * Defines if this property is parent reference in a child for a parent/child relationship.
   * Requires a Field to work.
   */
  public boolean isParentReference = false;

  /**
   * Defines if this property is child reference in a parent for a parent/child relationship.
   * Defines if this is the child side of parent/child relationship.
   * Requires a Field to work.
   */
  public boolean isChild = false;

  /**
   * Empty constructor.
   */
  public PersistentProperty() {
  }

  /**
   * The Field constructor.
   *
   * @param field The field to create the property from.
   */
  public PersistentProperty(Field field) {
    this.name = field.getName();
    this.type = field.getType();
    nullable = (field.getAnnotation(Nullable.class) != null);
    Column column = field.getAnnotation(Column.class);
    MappedProperty mappedProperty = field.getAnnotation(MappedProperty.class);
    if (type == String.class) {
      if (column != null) {
        maxLength = 255;
        if (column.length() > 0) {
          maxLength = column.length();
        }
      } else {
        // A simple string, so treat it as a limited length string.
        maxLength = 255;
      }
      if (mappedProperty != null) {
        if (mappedProperty.definition().equals("TEXT")) {
          // A TEXT/CLOB will be treated as no max length.
          maxLength = 0;
        }
      }
    }
    if (column != null) {
      nullable = column.nullable();
    }
    this.field = field;

    //  Figure out if this is a domain reference of any type or parent/child.
    // First, find the referenced type (if a domain).
    Class reference = type;
    if (Collection.class.isAssignableFrom(type)) {
      // Use the ultimate type for a collection.
      Type fieldType = field.getGenericType();
      if (fieldType instanceof ParameterizedType) {
        reference = (Class) ((ParameterizedType) fieldType).getActualTypeArguments()[0];
      }
    }
    if (reference != null && reference.getAnnotation(MappedEntity.class) != null) {
      referenceType = reference;
    }

    // Now, figure out the parent/child cases.
    OneToMany oneToMany = field.getAnnotation(OneToMany.class);
    ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
    if (manyToOne != null) {
      Class target = manyToOne.targetEntity();
      isParentReference = !(target.equals(type));
    }
    if (!isParentReference) {
      isChild = (oneToMany != null);
    }
  }

  @Override
  public String toString() {
    String col = (type == String.class) ? ", maxLength=" + maxLength : "";
    String owner = (field != null) ? ", owner=" + field.getDeclaringClass() : "";
    return "PersistentProperty{" +
        "name='" + name + '\'' +
        ", type=" + type +
        ", nullable=" + nullable +
        ", referenceType=" + referenceType +
        ", parentReference=" + isParentReference +
        ", child=" + isChild +
        col + owner +
        '}';
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Class<?> getType() {
    return type;
  }

  public void setType(Class type) {
    this.type = type;
  }

  public boolean isNullable() {
    return nullable;
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  public Integer getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(Integer maxLength) {
    this.maxLength = maxLength;
  }

  public Field getField() {
    return field;
  }

  public void setField(Field field) {
    this.field = field;
  }

  public Class getReferenceType() {
    return referenceType;
  }

  public void setReferenceType(Class referenceType) {
    this.referenceType = referenceType;
  }

  public boolean isParentReference() {
    return isParentReference;
  }

  public void setParentReference(boolean parentReference) {
    isParentReference = parentReference;
  }

  public boolean isChild() {
    return isChild;
  }

  public void setChild(boolean child) {
    isChild = child;
  }
}
