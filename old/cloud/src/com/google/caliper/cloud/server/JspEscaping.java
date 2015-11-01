/**
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.caliper.cloud.server;

/**
 * A utility class to do string escaping so our JSPs aren't massive XSS
 * exploits waiting to happen.
 * 
 * <p>The reason for using something like this instead of using EL and
 * standard tags is twofold:
 * <ol><li>Use of EL means our JSPs put off important bits to runtime
 * interpretation instead of compiling everything possible.
 * <li>I'd need a custom tag anyway for javascript escaping, and a custom
 * taglib is a pain.</ol>
 */
public class JspEscaping {
  
  /** Escape the argument in a manner appropriate for a javascript string */
  public static String js(String in) {
    // Get both kinds of quotes, and escape < to avoid a </script> closing the block
    if (in == null) {
      return null;
    }
    return in.replace("\\", "\\\\").replace("\"", "\\x22")
        .replace("'", "\\x27").replace("<", "\\x3c");
  }

  /** Escape the argument in a manner appropriate for html */
  public static String html(String in) {
    if (in == null) {
      return null;
    }
    return in.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&#34;").replace("'", "&#39;");
  }  
}
