/*
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

package com.google.caliper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class XmlUtils {
  public static ImmutableList<Node> childrenOf(Node node) {
    NodeList children = node.getChildNodes();
    ImmutableList.Builder<Node> result = ImmutableList.builder();
    for (int i = 0, size = children.getLength(); i < size; i++) {
      result.add(children.item(i));
    }
    return result.build();
  }

  public static ImmutableMap<String, String> attributesOf(Element element) {
    NamedNodeMap map = element.getAttributes();
    ImmutableMap.Builder<String, String> result = ImmutableMap.builder();
    for (int i = 0, size = map.getLength(); i < size; i++) {
      Attr attr = (Attr) map.item(i);
      result.put(attr.getName(), attr.getValue());
    }
    return result.build();
  }

  private XmlUtils() {}
}
