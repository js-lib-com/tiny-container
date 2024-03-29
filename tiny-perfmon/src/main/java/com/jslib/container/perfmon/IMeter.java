package com.jslib.container.perfmon;

/**
 * Generic performance meter.
 * <p>
 * Performance meters are design to work together with an observer class: meters gather numerical data and observer samples
 * meters periodically and creates time based statistics.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface IMeter
{
  /**
   * Get formatted string representation of meter instance ready to persist.
   * 
   * @return formatted string representation.
   */
  String toExternalForm();
}
