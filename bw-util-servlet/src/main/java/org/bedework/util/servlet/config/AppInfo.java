package org.bedework.util.servlet.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The AppInfo class represents an application configuration that manages
 * default forward mappings and helper configurations.
 * <p>
 * This class provides methods to register and retrieve default forwards
 * and helpers. Default forwards are mappings to predefined paths identified
 * by a name, while helpers represent additional components or services related
 * to the application's behavior, which can include forward information.
 * <p>
 * Helpers added and retrived through this classes add and get helper
 * methods are for the "GET" http method.
 * <p>
 * Further http method helpers may be defined by adding method specific
 * configurations with the addMethodInfo method.
 */
public class AppInfo {
  private String method;

  private List<ForwardInfo> defaultForwards = new ArrayList<>();

  private List<HelperInfo> helpers = new ArrayList<>();

  private List<AppInfo> methodHelpers = new ArrayList<>();

  private Map<String, ForwardInfo> defaultForwardsMap =
          new HashMap<>();

  private final Map<String, HelperInfo> helpersMap =
          new HashMap<>();

  private final Map<String, AppInfo> methodHelpersMap =
          new HashMap<>();

  public AppInfo() {
    this("GET");
  }

  public AppInfo(final String method) {
    this.method = method;
  }

  public void setMethod(final String val) {
    method = val;
  }

  public String getMethod() {
    return method;
  }

  public void setDefaultForwards(final List<ForwardInfo> val) {
    defaultForwards = val;
  }

  public List<ForwardInfo> getDefaultForwards() {
    return defaultForwards;
  }

  public void setHelpers(final List<HelperInfo> helpers) {
    this.helpers = helpers;
  }

  public List<HelperInfo> getHelpers() {
    return helpers;
  }

  public void setMethodHelpers(final List<AppInfo> methodHelpers) {
    this.methodHelpers = methodHelpers;
  }

  public List<AppInfo> getMethodHelpers() {
    return methodHelpers;
  }

  public AppInfo addMethodInfo(final AppInfo appInfo) {
    methodHelpersMap.put(appInfo.getMethod(), appInfo);
    return this;
  }

  public AppInfo addDefaultForward(final String name,
                                  final String path) {
    defaultForwards.add(new ForwardInfo(name, path));

    return this;
  }

  public AppInfo addHelper(final HelperInfo helper) {
    helpers.add(helper);
    helpersMap.put(helper.getName(), helper);
    return this;
  }

  public ForwardInfo getDefaultForward(final String name) {
    return defaultForwardsMap.get(name);
  }

  public Map<String, HelperInfo> getHelpersMap() {
    return helpersMap;
  }

  public AppInfo getMethodInfo(final String val) {
    if ((val == null) || "GET".equalsIgnoreCase(val)) {
      return this;
    }
    return methodHelpersMap.get(val);
  }

  public HelperInfo getHelper(final String method,
                              final String name) {
    final AppInfo ai = getMethodInfo(method);
    return ai.helpersMap.get(name);
  }

  public void mapObjects() {
    defaultForwardsMap.clear();
    for (final ForwardInfo fi: defaultForwards) {
      defaultForwardsMap.put(fi.getName(), fi);
    }

    helpersMap.clear();
    for (final HelperInfo hi: helpers) {
      helpersMap.put(hi.getName(), hi);
    }

    methodHelpersMap.clear();
    for (final AppInfo ai: methodHelpers) {
      methodHelpersMap.put(ai.getMethod(), ai);
    }
  }
}
