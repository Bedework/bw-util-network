package org.bedework.util.servlet.config;

import java.util.ArrayList;
import java.util.List;

public class HelperInfo {
  private AppInfo parent;
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

  public void setParent(final AppInfo val) {
    parent = val;
  }

  public AppInfo getParent() {
    return parent;
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

  public ForwardInfo getForward(final String name) {
    for (final ForwardInfo fi: forwards) {
      if (fi.getName().equals(name)) {
        return fi;
      }
    }

    var ai = parent;
    while (ai != null) {
      final ForwardInfo fi = ai.getDefaultForward(name);
      if (fi != null) {
        return fi;
      }
      ai = ai.getParent();
    }

    return null;
  }
}
