package org.bedework.util.servlet.config;

import java.util.ArrayList;
import java.util.List;

public class HelperInfo {
  private final String name;
  private final String className;
  private final List<ForwardInfo> forwards = new ArrayList<>();

  // For jackson
  public HelperInfo() {
    this(null, null);
  }

  public HelperInfo(final String name,
                    final String className) {
    this.name = name;
    this.className = className;
  }

  public HelperInfo addForward(final String name,
                               final String path) {
    forwards.add(new ForwardInfo(name, path));

    return this;
  }

  public String getName() {
    return name;
  }

  public String getClassName() {
    return className;
  }

  public List<ForwardInfo> getForwards() {
    return forwards;
  }
}
