package org.simplemes.eframe.misc

/**
 * Defines many field size limits used in the framework.
 * These fields can be used by your application, but should be referenced inside of your own FieldSizes \
 * class.  For example:
 * <p/>
 *
 * <pre>
 * package org.example.misc
 * class FieldSizes &#123;
 *   public static final int MAX_CODE_LENGTH = org.simplemes.eframe.misc.FieldSizes.MAX_CODE_LENGTH
 * &#125;
 * </pre>
 *
 * <b>Note:</b> The package name <code>org.simplemes.eframe.misc</code> should be used if your class is named
 * <code>FieldSizes</code>.
 */

class FieldSizes {
  /**
   * Defines the size of most object's code (ID) within the eframe classes: <b>30</b> characters.
   */
  public static final int MAX_CODE_LENGTH = 30

  /**
   * Defines the size of a label (or label lookup key): <b>80</b> characters.
   */
  public static final int MAX_LABEL_LENGTH = 80

  /**
   * Defines the size of most object's short title field: <b>80</b> characters.
   */
  public static final int MAX_TITLE_LENGTH = 80

  /**
   * Defines the size of the largest single-line text input field: <b>256</b> characters.
   */
  public static final int MAX_SINGLE_LINE_LENGTH = 255

  /**
   * Defines the size a typical class name: <b>255</b> characters.
   */
  public static final int MAX_CLASS_NAME_LENGTH = 255

  /**
   * Defines the maximum expected size of an application key field for domain objects: <b>100</b> characters.
   */
  public static final int MAX_KEY_LENGTH = 100

  /**
   * Defines the maximum expected size of a generic file name and path: <b>100</b> characters.
   */
  public static final int MAX_PATH_LENGTH = 100

  /**
   * Defines the maximum expected size of a generic URL/URI element: <b>255</b> characters.
   */
  public static final int MAX_URL_LENGTH = 255

  /**
   * Defines the maximum expected size of a generic XML-encoded element: <b>1024</b> characters.
   * Some XML elements may be unlimited.  This max length is used when an unlimited size is not typically needed.
   */
  public static final int MAX_XML_LENGTH = 1024

  /**
   * Defines the maximum expected size of a generic notes element: <b>1024</b> characters.
   */
  public static final int MAX_NOTES_LENGTH = 1024

  /**
   * The standard scale/precision for decimal values.  This is used for all quantity values.  Value: <b>4</b> decimal places.
   */
  public static final int STANDARD_DECIMAL_SCALE = 4


}
