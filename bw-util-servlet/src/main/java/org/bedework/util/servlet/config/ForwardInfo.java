package org.bedework.util.servlet.config;

public class ForwardInfo {
  private final String name;
  private final String path;

  // for jackson
  public ForwardInfo() {
    this(null, null);
  }

  public ForwardInfo(final String name, final String path) {
    this.name = name;
    this.path = path;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }
}
