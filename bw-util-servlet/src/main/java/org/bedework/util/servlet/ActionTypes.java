package org.bedework.util.servlet;

public interface ActionTypes {
  /** */
  public final static int actionTypeUnknown = 0;
  /** */
  public final static int actionTypeRender = 1;
  /** */
  public final static int actionTypeAction = 2;
  /** */
  public final static int actionTypeResource = 3;

  /** */
  public final static String[] actionTypes = {"unknown",
                                              "render",
                                              "action",
                                              "resource"};
}
