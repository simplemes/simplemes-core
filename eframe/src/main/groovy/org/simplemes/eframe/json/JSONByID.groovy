/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Defines a field that will serialized to JSON using just the domain record ID.
 *
 */
// TODO: Rename to JSONByUUID?  With docs in json.adoc
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = JSONByIDSerializer)
@JsonDeserialize(using = JSONByIDDeserializer)
@interface JSONByID {

}