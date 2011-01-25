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

import com.google.common.collect.ImmutableMap;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This exists for backwards compatibility with old data, which is stored in XML format.
 * All new data is stored in JSON.
 */
public final class Xml {
  private static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssz";
  private static final String ENVIRONMENT_ELEMENT_NAME = "environment";
  private static final String RESULT_ELEMENT_NAME = "result";
  private static final String RUN_ELEMENT_NAME = "run";
  private static final String BENCHMARK_ATTRIBUTE = "benchmark";
  private static final String EXECUTED_TIMESTAMP_ATTRIBUTE = "executedTimestamp";
  private static final String OLD_SCENARIO_ELEMENT_NAME = "scenario";
  // for backwards compatibility, use a different name
  private static final String SCENARIO_ELEMENT_NAME = "newScenario";
  private static final String MEASUREMENTS_ELEMENT_NAME = "measurements";
  private static final String TIME_EVENT_LOG_ELEMENT_NAME = "eventLog";

  private static Result readResultElement(Element element) throws Exception {
    Environment environment = null;
    Run run = null;
    for (Node topLevelNode : XmlUtils.childrenOf(element)) {
      if (topLevelNode.getNodeName().equals(ENVIRONMENT_ELEMENT_NAME)) {
        Element environmentElement = (Element) topLevelNode;
        environment = readEnvironmentElement(environmentElement);
      } else if (topLevelNode.getNodeName().equals(RUN_ELEMENT_NAME)) {
        run = readRunElement((Element) topLevelNode);
      } else {
        throw new RuntimeException("illegal node name: " + topLevelNode.getNodeName());
      }
    }

    if (environment == null || run == null) {
      throw new RuntimeException("missing environment or run elements");
    }

    return new Result(run, environment);
  }

  private static Environment readEnvironmentElement(Element element) {
    return new Environment(XmlUtils.attributesOf(element));
  }

  private static Run readRunElement(Element element) throws Exception {
    String benchmarkName = element.getAttribute(BENCHMARK_ATTRIBUTE);
    String executedDateString = element.getAttribute(EXECUTED_TIMESTAMP_ATTRIBUTE);
    Date executedDate = new SimpleDateFormat(DATE_FORMAT_STRING).parse(executedDateString);

    ImmutableMap.Builder<Scenario, ScenarioResult> measurementsBuilder = ImmutableMap.builder();
    for (Node scenarioNode : XmlUtils.childrenOf(element)) {
      Element scenarioElement = (Element) scenarioNode;
      Scenario scenario = new Scenario(XmlUtils.attributesOf(scenarioElement));
      ScenarioResult scenarioResult;

      // for backwards compatibility with older runs
      if (scenarioNode.getNodeName().equals(OLD_SCENARIO_ELEMENT_NAME)) {
        MeasurementSet measurement =
            Json.measurementSetFromJson(scenarioElement.getTextContent());
        scenarioResult = new ScenarioResult(measurement, "",
            null, null, null, null);
      } else if (scenarioNode.getNodeName().equals(SCENARIO_ELEMENT_NAME)) {
        MeasurementSet timeMeasurementSet = null;
        String eventLog = null;
        for (Node node : XmlUtils.childrenOf(scenarioElement)) {
          if (node.getNodeName().equals(MEASUREMENTS_ELEMENT_NAME)) {
            timeMeasurementSet = Json.measurementSetFromJson(node.getTextContent());
          } else if (node.getNodeName().equals(TIME_EVENT_LOG_ELEMENT_NAME)) {
            eventLog = node.getTextContent();
          } else {
            throw new RuntimeException("illegal node name: " + node.getNodeName());
          }
        }
        if (timeMeasurementSet == null || eventLog == null) {
          throw new RuntimeException("missing node \"" + MEASUREMENTS_ELEMENT_NAME + "\" or \""
              + TIME_EVENT_LOG_ELEMENT_NAME + "\"");
        }
        // "new Measurement[0]" used instead of empty varargs argument since MeasurementSet has
        // an empty private constructor.
        scenarioResult = new ScenarioResult(timeMeasurementSet, eventLog,
            null, null, null, null);
      } else {
        throw new RuntimeException("illegal node name: " + scenarioNode.getNodeName());
      }

      measurementsBuilder.put(scenario, scenarioResult);
    }

    return new Run(measurementsBuilder.build(), benchmarkName, executedDate);
  }

  /**
   * Creates a result by decoding XML from the specified stream. The XML should
   * be consistent with the format emitted by the now deleted runToXml(Run, OutputStream).
   */
  public static Run runFromXml(InputStream in) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      return readRunElement(document.getDocumentElement());
    } catch (Exception e) {
      throw new IllegalStateException("Malformed XML document", e);
    }
  }

  /**
   * Creates an environment by decoding XML from the specified stream. The XML should
   * be consistent with the format emitted by the now deleted
   * environmentToXml(Environment, OutputStream).
   */
  public static Environment environmentFromXml(InputStream in) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      Element environmentElement = document.getDocumentElement();
      return readEnvironmentElement(environmentElement);
    } catch (Exception e) {
      throw new IllegalStateException("Malformed XML document", e);
    }
  }

  public static Result resultFromXml(InputStream in) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      return readResultElement(document.getDocumentElement());
    } catch (Exception e) {
      throw new IllegalStateException("Malformed XML document", e);
    }
  }

  private Xml() {}
}
