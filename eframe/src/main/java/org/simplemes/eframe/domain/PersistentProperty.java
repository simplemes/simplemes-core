package org.simplemes.eframe.domain;/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
 */

import javax.annotation.Nullable;
import javax.persistence.Column;
import java.lang.reflect.Field;

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
   * The max length (String only fields).  Defaults to 255 if @Column is not defined.
   */
  int maxLength = 0;

  /**
   * The Field from the Class definition.
   */
  Field field;

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
    if (type == String.class) {
      maxLength = 255;
      Column column = field.getAnnotation(Column.class);
      if (column != null && column.length() > 0) {
        maxLength = column.length();
      }
    }
    this.field = field;
  }

  @Override
  public String toString() {
    String col = (type == String.class) ? ", maxLength=" + maxLength : "";
    return "PersistentProperty{" +
        "name='" + name + '\'' +
        ", type=" + type +
        ", nullable=" + nullable +
        col +
        '}';
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Class getType() {
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

  public int getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }

  public Field getField() {
    return field;
  }

  public void setField(Field field) {
    this.field = field;
  }
}
