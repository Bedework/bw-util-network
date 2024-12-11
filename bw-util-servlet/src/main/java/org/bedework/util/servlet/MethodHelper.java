package org.bedework.util.servlet;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** These can be registered with a method and located by some
 * appropriate criteria, e.g. the first element in a path
 *
 */
public interface MethodHelper {
  void process(List<String> resourceUri,
               HttpServletRequest req,
               HttpServletResponse resp,
               MethodBase mb)
          throws ServletException;
}
