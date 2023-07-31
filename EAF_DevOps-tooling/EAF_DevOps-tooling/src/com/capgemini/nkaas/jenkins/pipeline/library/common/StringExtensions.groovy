package com.capgemini.nkaas.jenkins.pipeline.library.common

import org.apache.commons.lang3.StringUtils

/**
 * String extension methods
 */
class StringExtensions {

  /**
   * Method to indent the lines in a string by a given number of whitepace
   * characters
   *
   * @param s the string to indent
   * @param indent the number of characters to indent by
   * @return the indented string
   */
  static String indent(String s, Integer indent) {
    if (s?.trim()) {
    return s.replaceAll(/(?m)^/, StringUtils.repeat(' ', indent))
    }
    return ''
   }

}
