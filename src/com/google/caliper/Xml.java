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

public final class Xml {
  private static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssz";

  /**
   * Encodes this result as XML to the specified stream. This XML can be parsed
   * with {@link #runFromXml(InputStream)}. Sample output:
   * <pre>{@code
   * <result benchmark="examples.FooBenchmark"
   *     apiKey="56b35ad1-2985-4541-8f40-170471a46693"
   *     executedTimestamp="2010-01-05T11:08:15PST">
   *   <scenario bar="15" foo="A" vm="dalvikvm">1200.1</scenario>
   *   <scenario bar="15" foo="B" vm="dalvikvm">1100.2</scenario>
   * </result>
   * }</pre>
   */
  public static void runToXml(Run run, OutputStream out) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element result = doc.createElement("result");
      doc.appendChild(result);

      result.setAttribute("benchmark", run.getBenchmarkName());
      result.setAttribute("apiKey", run.getApiKey());
      String executedTimestampString = new SimpleDateFormat(DATE_FORMAT_STRING)
          .format(run.getExecutedTimestamp());
      result.setAttribute("executedTimestamp", executedTimestampString);

      for (Map.Entry<Scenario, MeasurementSet> entry : run.getMeasurements().entrySet()) {
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
   * be consistent with the format emitted by {@link #runToXml(Run, OutputStream)}.
   */
  public static Run runFromXml(InputStream in) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      Element result = document.getDocumentElement();

      String benchmarkName = result.getAttribute("benchmark");
      String apiKey = result.getAttribute("apiKey");
      String executedDateString = result.getAttribute("executedTimestamp");
      Date executedDate = new SimpleDateFormat(DATE_FORMAT_STRING).parse(executedDateString);

      ImmutableMap.Builder<Scenario, MeasurementSet> measurementsBuilder = ImmutableMap.builder();
      for (Node node : childrenOf(result)) {
        Element scenarioElement = (Element) node;
        Scenario scenario = new Scenario(attributesOf(scenarioElement));
        MeasurementSet measurement = MeasurementSet.valueOf(scenarioElement.getTextContent());
        measurementsBuilder.put(scenario, measurement);
      }

      return new Run(measurementsBuilder.build(), benchmarkName, apiKey, executedDate);
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

  private Xml() {}
}
