/**
 * Copyright (C) 2009 Google Inc.
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
import com.google.common.base.Joiner;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The complete result of a benchmark suite run.
 */
public final class Result {

  private final ImmutableMap<Run, Double> measurements;

  public Result(Map<Run, Double> measurements) {
    this.measurements = ImmutableMap.copyOf(measurements);
  }

  public ImmutableMap<Run, Double> getMeasurements() {
    return measurements;
  }

  @Override public boolean equals(Object o) {
    return o instanceof Result
        && ((Result) o).measurements.equals(measurements);
  }

  @Override public int hashCode() {
    return measurements.hashCode();
  }

  @Override public String toString() {
    return Joiner.on("\n").withKeyValueSeparator(": ").join(measurements);
  }

  /**
   * Encodes this result as XML to the specified stream. This XML can be parsed
   * with {@link #fromXml(InputStream)}. Sample output:
   * <pre>{@code
   * <result>
   *   <run bar="15" foo="A" vm="dalvikvm">1200.1</run>
   *   <run bar="15" foo="B" vm="dalvikvm">1100.2</run>
   * </result>
   * }</pre>
   */
  public void toXml(OutputStream out) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element result = doc.createElement("result");
      doc.appendChild(result);

      for (Map.Entry<Run, Double> entry : measurements.entrySet()) {
        Element runElement = doc.createElement("run");
        result.appendChild(runElement);

        Run run = entry.getKey();
        for (Map.Entry<String, String> parameter : run.getVariables().entrySet()) {
          runElement.setAttribute(parameter.getKey(), parameter.getValue());
        }
        runElement.setTextContent(String.valueOf(entry.getValue()));
      }

      TransformerFactory.newInstance().newTransformer()
          .transform(new DOMSource(doc), new StreamResult(out));
    } catch (Exception e) {
      throw new IllegalStateException("Malformed XML document", e);
    }
  }

  /**
   * Creates a result by decoding XML from the specified stream. The XML should
   * be consistent with the format emitted by {@link #toXml(OutputStream)}.
   */
  public static Result fromXml(InputStream in) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      Element result = document.getDocumentElement();

      ImmutableMap.Builder<Run, Double> measurementsBuilder = ImmutableMap.builder();
      for (Node node : childrenOf(result)) {
        Element runElement = (Element) node;
        Run run = new Run(attributesOf(runElement));
        double measurement = Double.parseDouble(runElement.getTextContent());
        measurementsBuilder.put(run, measurement);
      }

      return new Result(measurementsBuilder.build());
    } catch (Exception e) {
      throw new IllegalStateException("Malformed XML document", e);
    }
  }

  private static ImmutableList<Node> childrenOf(Node node) {
    NodeList children = node.getChildNodes();
    ImmutableList.Builder<Node> result = ImmutableList.builder();
    for (int i = 0, size = children.getLength(); i < size; i++) {
      result.add(children.item(i));
    }
    return result.build();
  }

  private static ImmutableMap<String, String> attributesOf(Element element) {
    NamedNodeMap map = element.getAttributes();
    ImmutableMap.Builder<String, String> result = ImmutableMap.builder();
    for (int i = 0, size = map.getLength(); i < size; i++) {
      Attr attr = (Attr) map.item(i);
      result.put(attr.getName(), attr.getValue());
    }
    return result.build();
  }
}
