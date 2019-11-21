package org.simplemes.mes.misc

/**
 * Defines many global constants that control basic definitions in the MES.
 * Primarily field widths and hard limits.
 *
 */

@SuppressWarnings("UnnecessaryQualifiedReference")
class FieldSizes {
  /**
   * Defines the size of most object's code. Uses eframe's MAX_CODE_LENGTH: <b>30</b> characters.
   */
  public static final int MAX_CODE_LENGTH = org.simplemes.eframe.misc.FieldSizes.MAX_CODE_LENGTH

  /**
   * Defines the size of most object's name (short description) field. Uses eframe's MAX_TITLE_LENGTH: <b>80</b> characters.
   */
  public static final int MAX_TITLE_LENGTH = org.simplemes.eframe.misc.FieldSizes.MAX_TITLE_LENGTH

  /**
   * Defines the size of the key field for a product definition object: <b>128</b> characters.
   * This is used by many key fields in the product package (e.g. product, router, etc).
   */
  public static final int MAX_PRODUCT_LENGTH = 128

  /**
   * Defines the size of an Lot/Serial Number (LSN) identifier: <b>50</b> characters.
   */
  public static final int MAX_LSN_LENGTH = 50

  /**
   * Defines the size of a generic long string, non=key field: <b>200</b> characters.
   */
  public static final int MAX_LONG_STRING_LENGTH = 200

  /**
   * Defines the size of a client-defined custom flex field value: <b>70</b> characters.
   */
  public static final int MAX_FLEX_VALUE_LENGTH = 100

  /**
   * The standard scale/precision for decimal values.  This is used for all quantity values.  Value: <b>4</b> decimal places.
   */
  public static final int STANDARD_DECIMAL_SCALE = 4

}
