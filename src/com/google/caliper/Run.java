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

import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
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
public final class Run {

  private static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssz";

  private final ImmutableMap<Scenario, Double> measurements;
  private final String benchmarkName;
  private final String executedByUuid;
  private final Date executedDate;

  // TODO: add more run properites such as checksums of the executed code

  public Run(Map<Scenario, Double> measurements,
      String benchmarkName, String executedByUuid, Date executedDate) {
    this.measurements = ImmutableMap.copyOf(measurements);
    this.benchmarkName = checkNotNull(benchmarkName);
    this.executedByUuid = checkNotNull(executedByUuid);
    this.executedDate = checkNotNull(executedDate);
  }

  public ImmutableMap<Scenario, Double> getMeasurements() {
    return measurements;
  }

  public String getBenchmarkName() {
    return benchmarkName;
  }

  public String getExecutedByUuid() {
    return executedByUuid;
  }

  public Date getExecutedDate() {
    return executedDate;
  }

  @Override public boolean equals(Object o) {
    if (o instanceof Run) {
      Run that = (Run) o;
      return measurements.equals(that.measurements)
          && benchmarkName.equals(that.benchmarkName)
          && executedByUuid.equals(that.executedByUuid)
          && executedDate.equals(that.executedDate);
    }

    return false;
  }

  @Override public int hashCode() {
    int result = measurements.hashCode();
    result = result * 37 + benchmarkName.hashCode();
    result = result * 37 + executedByUuid.hashCode();
    result = result * 37 + executedDate.hashCode();
    return result;
  }

  @Override public String toString() {
    return Joiner.on("\n").withKeyValueSeparator(": ").join(measurements);
  }

  /**
   * Encodes this result as XML to the specified stream. This XML can be parsed
   * with {@link #fromXml(InputStream)}. Sample output:
   * <pre>{@code
   * <result benchmark="examples.FooBenchmark"
   *     executedBy="A0:1F:CAFE:BABE"
   *     executedDate="2010-01-05T11:08:15PST">
   *   <scenario bar="15" foo="A" vm="dalvikvm">1200.1</scenario>
   *   <scenario bar="15" foo="B" vm="dalvikvm">1100.2</scenario>
   * </result>
   * }</pre>
   */
  public void toXml(OutputStream out) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element result = doc.createElement("result");
      doc.appendChild(result);

      result.setAttribute("benchmark", benchmarkName);
      result.setAttribute("executedBy", executedByUuid);
      String executedDateString = new SimpleDateFormat(DATE_FORMAT_STRING).format(executedDate);
      result.setAttribute("executedDate", executedDateString);

      for (Map.Entry<Scenario, Double> entry : measurements.entrySet()) {
        Element runElement = doc.createElement("scenario");
        result.appendChild(runElement);

        Scenario scenario = entry.getKey();
        for (Map.Entry<String, String> parameter : scenario.getVariables().entrySet()) {
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
  public static Run fromXml(InputStream in) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      Element result = document.getDocumentElement();

      String benchmarkName = result.getAttribute("benchmark");
      String executedByUuid = result.getAttribute("executedBy");
      String executedDateString = result.getAttribute("executedDate");
      Date executedDate = new SimpleDateFormat(DATE_FORMAT_STRING).parse(executedDateString);

      ImmutableMap.Builder<Scenario, Double> measurementsBuilder = ImmutableMap.builder();
      for (Node node : childrenOf(result)) {
        Element scenarioElement = (Element) node;
        Scenario scenario = new Scenario(attributesOf(scenarioElement));
        double measurement = Double.parseDouble(scenarioElement.getTextContent());
        measurementsBuilder.put(scenario, measurement);
      }

      return new Run(measurementsBuilder.build(), benchmarkName, executedByUuid, executedDate);
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
