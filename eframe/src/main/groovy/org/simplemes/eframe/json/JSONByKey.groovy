package org.simplemes.eframe.json

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines a field that will serialized to JSON using just the domain record's primary key field (first string key field).
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = JSONByKeySerializer)
@JsonDeserialize(using = JSONByKeyDeserializer)
@interface JSONByKey {

}