package org.bedework.util.servlet;

import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

public class SessionSerializer implements Logged {
  /* Try to serialize requests from a single session
   * This is very imperfect.
   */
  static class Waiter {
    boolean active;
    int waiting;
  }

  private static final HashMap<String, Waiter> waiters =
          new HashMap<>();

  public synchronized void tryWait(
          final HttpServletRequest req,
          final boolean in) throws Throwable {
    Waiter wtr;
    //String key = req.getRequestedSessionId();
    final String key = req.getRemoteUser();
    if (key == null) {
      return;
    }

    wtr = waiters.get(key);
    if (wtr == null) {
      if (!in) {
        return;
      }

      wtr = new Waiter();
      wtr.active = true;
      waiters.put(key, wtr);
      return;
    }

      if (!in) {
        wtr.active = false;
        wtr.notify();
        return;
      }

      wtr.waiting++;
      while (wtr.active) {
        if (debug()) {
          debug("in: waiters=" + wtr.waiting);
        }

        wtr.wait();
      }
      wtr.waiting--;
      wtr.active = true;
  }

  public synchronized void removeSession(final String sessid) {
    waiters.remove(sessid);
  }

  /* ==============================================================
   *                   Logged methods
   * ============================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
