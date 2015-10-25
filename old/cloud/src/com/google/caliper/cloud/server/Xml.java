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

package com.google.caliper.cloud.server;

import com.google.caliper.XmlUtils;
import com.google.caliper.cloud.client.Benchmark;
import com.google.caliper.cloud.client.RunMeta;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class Xml {
  public static Benchmark benchmarkFromXml(InputStream in) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      return readBenchmarkElement(document.getDocumentElement());
    } catch (Exception e) {
      throw new IllegalStateException("Malformed XML document", e);
    }
  }

  private static Benchmark readBenchmarkElement(Element element) {
    String benchmarkName = element.getAttribute("name");
    String benchmarkOwner = element.getAttribute("owner");

    String cVariable = null;
    List<String> rVariables = null;
    List<RunMeta> runMetas = null;
    Map<String, Map<String, Boolean>> variableValuesShown =
        new HashMap<String, Map<String, Boolean>>();
    for (Node childNode : XmlUtils.childrenOf(element)) {
      String nodeName = childNode.getNodeName();
      if (nodeName.equals("cVariable")) {
        cVariable = childNode.getTextContent();
      } else if (nodeName.equals("rVariables")) {
        rVariables = new ArrayList<String>();
        for (Node rVariableNode : XmlUtils.childrenOf(childNode)) {
          rVariables.add(rVariableNode.getTextContent());
        }
      } else if (nodeName.equals("runMetas")) {
        List<Long> runMetaKeys = new ArrayList<Long>();
        for (Node runMetaNode : XmlUtils.childrenOf(childNode)) {

          runMetaKeys.add(Long.parseLong(XmlUtils.attributesOf((Element) runMetaNode).get("id")));
        }
        runMetas = new RunStore().getRunMetas(runMetaKeys);
      } else if (nodeName.equals("variableValuesShown")) {
        for (Node variableNode : XmlUtils.childrenOf(childNode)) {
          String variableName = XmlUtils.attributesOf((Element) variableNode).get("name");

          Map<String, Boolean> valuesShown = new HashMap<String, Boolean>();
          for (Node valueNode : XmlUtils.childrenOf(variableNode)) {
            Map<String, String> valueAttributes = XmlUtils.attributesOf((Element) valueNode);
            String valueName = valueAttributes.get("name");
            boolean valueShown = Boolean.valueOf(valueAttributes.get("shown"));
            valuesShown.put(valueName, valueShown);
          }

          variableValuesShown.put(variableName, valuesShown);
        }
      } else {
        throw new RuntimeException("unexpected XML node: " + nodeName);
      }
    }

    if (benchmarkName.isEmpty() || benchmarkOwner.isEmpty() || runMetas == null) {
      throw new RuntimeException("missing name or owner attributes of the root node, or cVariable, rVariables or runMetas tags");
    }

    return new Benchmark(benchmarkOwner, benchmarkName, runMetas, rVariables, cVariable,
        variableValuesShown);
  }

  public static void benchmarkToXml(Benchmark benchmark, OutputStream out) {
    // hack from RunStore
    System.setProperty("javax.xml.transform.TransformerFactory",
        "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");

    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      doc.appendChild(createBenchmarkElement(doc, benchmark));
      TransformerFactory.newInstance().newTransformer()
          .transform(new DOMSource(doc), new StreamResult(out));
    } catch (Exception e) {
      throw new IllegalStateException("Malformed XML document", e);
    }
  }

  private static Element createBenchmarkElement(Document doc, Benchmark benchmark) {
    Element benchmarkElement = doc.createElement("benchmarkSnapshot");
    benchmarkElement.setAttribute("name", benchmark.getName());
    benchmarkElement.setAttribute("owner", benchmark.getOwner());

    Element cVariableElement = doc.createElement("cVariable");
    cVariableElement.setTextContent(benchmark.getCVariable());
    benchmarkElement.appendChild(cVariableElement);

    Element rVariablesElement = doc.createElement("rVariables");
    for (String rVariable : benchmark.getRVariables()) {
      Element rVariableElement = doc.createElement("rVariable");
      rVariableElement.setTextContent(rVariable);
      rVariablesElement.appendChild(rVariableElement);
    }
    benchmarkElement.appendChild(rVariablesElement);

    Element runMetasElement = doc.createElement("runMetas");
    for (RunMeta runMeta : benchmark.getRuns()) {
      Element runMetaElement = doc.createElement("runMeta");
      runMetaElement.setAttribute("id", Long.toString(runMeta.getId()));
      runMetasElement.appendChild(runMetaElement);
    }
    benchmarkElement.appendChild(runMetasElement);

    Element variableValuesShownElement = doc.createElement("variableValuesShown");
    for (Map.Entry<String, Map<String, Boolean>> variableEntry :
        benchmark.getVariableValuesShown().entrySet()) {
      Element variableElement = doc.createElement("variable");
      variableElement.setAttribute("name", variableEntry.getKey());
      for (Map.Entry<String, Boolean> valueEntry : variableEntry.getValue().entrySet()) {
        Element valueElement = doc.createElement("value");
        valueElement.setAttribute("name", valueEntry.getKey());
        valueElement.setAttribute("shown", valueEntry.getValue().toString());
        variableElement.appendChild(valueElement);
      }
      variableValuesShownElement.appendChild(variableElement);
    }
    benchmarkElement.appendChild(variableValuesShownElement);

    return benchmarkElement;
  }
}
