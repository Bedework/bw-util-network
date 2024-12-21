package org.bedework.util.servlet.config;

public class ForwardInfo {
  private final String name;
  private final String path;
  private boolean redirect;

  // for jackson
  public ForwardInfo() {
    this(null, null, false);
  }

  public ForwardInfo(final String name,
                     final String path,
                     final boolean redirect) {
    this.name = name;
    this.path = path;
    this.redirect = redirect;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public boolean isRedirect() {
    return redirect;
  }
}
