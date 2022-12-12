package com.jslib.container.http.form;


/**
 * Named part item of a multipart form. A part from a multipart form has a name and a content type. Content specific methods are
 * provided by this interface specializations, see {@link FormField} and {@link UploadStream}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface Part
{
  /**
   * Get normalized name for this form part. Returned part name is Java member like, that is, camel case starting with lower
   * case. This is indeed true even if original form part name was dashed name.
   * 
   * @return this part item name.
   */
  String getName();

  /**
   * Test if this part has requested name. This predicate supports both Java member like and dashed names, that is,
   * <code>email-addresses</code> and <code>emailAddresses</code> are considered the same. Anyway, is still case sensitive.
   * <p>
   * This predicate is designated to be used with form iterators replacing a construct like
   * 
   * <pre>
   * if(formField.getName().equals("email-addresses") || formField..getName().equals("emailAddresses")) {
   *    . . .
   * }
   * </pre>
   * 
   * with
   * 
   * <pre>
   * if(formField.is("email-addresses")) {
   *    . . .
   * }
   * </pre>
   * 
   * @param name requested part name.
   * @return true if this part has requested name.
   * @throws IllegalArgumentException if requested name is null.
   */
  boolean is(String name) throws IllegalArgumentException;

  /**
   * Return this form part content type.
   * 
   * @return this part content type.
   */
  String getContentType();
}
