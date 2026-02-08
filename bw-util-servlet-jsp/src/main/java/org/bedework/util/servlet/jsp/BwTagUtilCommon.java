/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.util.servlet.jsp;

import org.bedework.base.exc.BedeworkException;

import jakarta.servlet.jsp.JspWriter;

/**
 * User: mike Date: 4/8/22 Time: 14:39
 */
public class BwTagUtilCommon {
  public record Attribute(String name, String val) {}

  public static void outTagged(final JspWriter out,
                               final String indent,
                               final String tagName,
                               final boolean value) {
    outTagged(out, indent, tagName, String.valueOf(value),
              false, false, (Attribute[])null);
  }

  public static void outTagged(final JspWriter out,
                               final String indent,
                               final String tagName,
                               final int value) {
    outTagged(out, indent, tagName, String.valueOf(value),
              false, false, (Attribute[])null);
  }

  public static void outTagged(final JspWriter out,
                               final String indent,
                               final String tagName,
                               final String value) {
    outTagged(out, indent, tagName, value, false, false,
              (Attribute[])null);
  }

  public static void outTaggedIfPresent(final JspWriter out,
                                        final String indent,
                                        final String tagName,
                                        final String value) {
    outTagged(out, indent, tagName, value, false, false,
              true, (Attribute[])null);
  }

  public static void outTagged(final JspWriter out,
                               final String indent,
                               final String tagName,
                               final String value,
                               final boolean filtered,
                               final boolean alwaysCdata) {
    outTagged(out, indent, tagName, value, filtered, alwaysCdata,
              (Attribute[])null);
  }

  public static void outTagged(final JspWriter out,
                               final String indent,
                               final String tagName,
                               final String value,
                               final boolean filtered,
                               final boolean alwaysCdata,
                               final Attribute... attr) {
    outTagged(out, indent, tagName, value, filtered, alwaysCdata,
              false, attr);
  }

  public static void outTagged(final JspWriter out,
                               final String indent,
                               final String tagName,
                               final String value,
                               final boolean filtered,
                               final boolean alwaysCdata,
                               final boolean skipIfNull,
                               final Attribute... attr) {
    if (skipIfNull && (value == null)) {
      return;
    }

    openTag(out, indent, tagName, false, attr);

    if (value != null) {
      boolean cdata = alwaysCdata;
      if (!cdata && !filtered) {
        cdata = value.contains("&") ||
                value.contains("<") ||
                value.contains("<![CDATA[");
      }

      if (!cdata) {
        print(out, formatted(value, filtered));
      } else {
        // We have to watch for text that includes "]]"

        int start = 0;

        while (start < value.length()) {
          final int end = value.indexOf("]]", start);
          final boolean lastSeg = end < 0;
          final String seg;

          if (lastSeg) {
            seg = value.substring(start);
          } else {
            seg = value.substring(start, end);
          }

          cdata = alwaysCdata ||
                  (seg.indexOf('&') >= 0) ||
                  (seg.indexOf('<') >= 0) ||
                  (seg.contains("<![CDATA[")) ||
                  ((start > 0) && seg.startsWith(">")); // Don't rebuild "]]>"

          if (!cdata) {
            print(out, seg);
          } else {
            print(out, "<![CDATA[");
            print(out, seg);
            print(out, "]]>");
          }

          if (lastSeg) {
            break;
          }

          print(out, "]]");
          start = end + 2;
        }
      }
    }

    closeTag(out, null, tagName);
  }

  public static String openTag(final JspWriter out,
                               final String indent,
                               final String tagName,
                               final boolean newline) {
    return openTag(out, indent, tagName, newline, (Attribute[])null);
  }

  public static String openTag(final JspWriter out,
                               final String indent,
                               final String tagName,
                               final boolean newline,
                               final Attribute... attrs) {
    if (indent != null) {
      print(out, indent);
    }
    print(out, "<");
    print(out, tagName);

    if (attrs != null) {
      for (final var attr: attrs) {
        print(out, " ");
        print(out, attr.name());
        print(out, "=\"");
        print(out, attr.val());
        print(out, "\"");
      }
    }

    print(out, ">");
    if (newline) {
      newline(out);
    }

    return pushIndent(indent);
  }

  public static String startTag(final JspWriter out,
                                final String indent,
                                final String tagName,
                                final boolean newline) {
    if (indent != null) {
      print(out, indent);
    }
    print(out, "<");
    print(out, tagName);
    print(out, " ");

    if (newline) {
      newline(out);
    }

    return pushIndent(indent);
  }

  public static void outAttribute(final JspWriter out,
                                  final String indent,
                                  final boolean newline,
                                  final Attribute attr) {
    if (indent != null) {
      print(out, indent);
    }

    print(out, attr.name());
    print(out, "=\"");
    if (attr.val() != null) {
      print(out, attr.val());
    }
    print(out, "\"");
    print(out, " ");

    if (newline) {
      newline(out);
    }
  }

  public static String endTag(final JspWriter out,
                              final String indent,
                              final boolean newline) {
    if (indent != null) {
      print(out, indent);
    }
    print(out, "/>");

    if (newline) {
      newline(out);
    }

    return popIndent(indent);
  }

  public static String closeTag(final JspWriter out,
                                final String indent,
                                final String tagName) {
    return closeTag(out, indent, tagName, true);
  }

  public static String closeTag(final JspWriter out,
                                final String indent,
                                final String tagName,
                                final boolean newline) {
    final var curIndent = popIndent(indent);
    if (curIndent != null) {
      print(out, curIndent);
    }
    print(out, "</");
    print(out, tagName);
    print(out, ">");

    if (newline) {
      newline(out);
    }

    return curIndent;
  }

  public static String filter(final String value) {
    if (value != null && !value.isEmpty()) {
      StringBuilder result = null;
      String filtered;

      for (int i = 0; i < value.length(); ++i) {
        filtered = null;
        switch(value.charAt(i)) {
          case '"':
            filtered = "&quot;";
            break;
          case '&':
            filtered = "&amp;";
            break;
          case '\'':
            filtered = "&#39;";
            break;
          case '<':
            filtered = "&lt;";
            break;
          case '>':
            filtered = "&gt;";
        }

        if (result == null) {
          if (filtered != null) {
            result = new StringBuilder(value.length() + 50);
            if (i > 0) {
              result.append(value, 0, i);
            }

            result.append(filtered);
          }
        } else if (filtered == null) {
          result.append(value.charAt(i));
        } else {
          result.append(filtered);
        }
      }

      return result == null ? value : result.toString();
    } else {
      return value;
    }
  }

  private static String formatted(final String value,
                                  final boolean filtered) {
    if (filtered) {
      return filter(value);
    }

    return value;
  }

  public static String pushIndent(final String val) {
    if (val == null) {
      return null;
    }

    return val + "  ";
  }

  public static String popIndent(final String val) {
    if (val == null) {
      return null;
    }

    if (val.length() < 2) {
      return val;
    }

    return val.substring(2);
  }

  public static void print(final JspWriter out, final String val) {
    try {
      out.print(val);
    } catch (final Throwable t) {
      throw new BedeworkException(t);
    }
  }

  public static void newline(final JspWriter out) {
    try {
      out.newLine();
    } catch (final Throwable t) {
      throw new BedeworkException(t);
    }
  }
}
