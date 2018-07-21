package js.container;

import js.annotation.Private;
import js.annotation.Public;

/**
 * Access control for managed method when invoked remotely. This enumeration is internal representation for {@link Public} and
 * {@link Private} annotations.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public enum Access
{
  /** Method can be accessed remotely without authentication. */
  PUBLIC,

  /** Method can be invoked remotely only within an authenticated web session. */
  PRIVATE
}