/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import javax.annotation.Nullable

/**
 * Defines the fields used in a SampleParent object.  Used to simulate a child class.
 */
interface SampleParentInterface {
  String getName()

  void setName(String name)

  @Nullable
  String getTitle()

  void setTitle(@Nullable String title)

  @Nullable
  String getNotes()

  void setNotes(@Nullable String notes)

  @Nullable
  String getNotDisplayed()

  void setNotDisplayed(@Nullable String notDisplayed)

  @Nullable
  String getMoreNotes()

  void setMoreNotes(@Nullable String moreNotes)

  Date getDateCreated()

  void setDateCreated(Date dateCreated)

  Date getDateUpdated()

  void setDateUpdated(Date dateUpdated)

  @Nullable
  AllFieldsDomain getAllFieldsDomain()

  void setAllFieldsDomain(@Nullable AllFieldsDomain allFieldsDomain)

  List<AllFieldsDomain> getAllFieldsDomains()

  void setAllFieldsDomains(List<AllFieldsDomain> allFieldsDomains)

  List<SampleChild> getSampleChildren()

  void setSampleChildren(List<SampleChild> sampleChildren)

  UUID getUuid()

  void setUuid(UUID uuid)

}