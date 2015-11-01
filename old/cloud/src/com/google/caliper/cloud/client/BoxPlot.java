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


package com.google.caliper.cloud.client;

import com.google.caliper.MeasurementSet;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
import java.util.Collections;
import java.util.List;

/**
 * A horizontal box plot chart like so: {@code |--[ | ]---|}.
 */
public final class BoxPlot {

  private final int maxWidth;
  private final int maxHeight;

  public BoxPlot(int maxWidth, int maxHeight) {
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
  }

  public Widget create(int style, double max, MeasurementSet measurementSet, boolean useNanos) {
    /*
     * Compute the five displayed values of the box plot from the measurements.
     *
     * a = minimum
     * b = lower quartile
     * c = median
     * d = upper quartile
     * e = maximum
     */
    List<Double> measurements = useNanos
        ? measurementSet.getMeasurementsRaw()
        : measurementSet.getMeasurementUnits();
    Collections.sort(measurements);
    int numMeasurements = measurements.size();
    double unitsPerPixel = max / maxWidth;
    int quartile = numMeasurements / 4;
    double a = measurements.get(0) / unitsPerPixel;
    double b = measurements.get(quartile) / unitsPerPixel;
    double c = measurements.get(numMeasurements / 2) / unitsPerPixel;
    double d = measurements.get(numMeasurements - (quartile > 0 ? quartile : 1)) / unitsPerPixel;
    double e = measurements.get(numMeasurements - 1) / unitsPerPixel;

    Color[] colors = Colors.forStyle(style);
    Color dark = colors[0];
    Color medium = colors[1];
    Color light = colors[2];

    GWTCanvas canvas = new GWTCanvas(maxWidth, maxHeight);
    if (numMeasurements > 3) {
      canvas.setFillStyle(dark);
      canvas.fillRect(0, 0, b, maxHeight);

      canvas.setFillStyle(medium);
      canvas.fillRect(b, 0, (c-b), maxHeight);

      canvas.setFillStyle(light);
      canvas.fillRect(c, 0, (d-c), maxHeight);

    } else {
      canvas.setFillStyle(dark);
      canvas.fillRect(0, 0, c, maxHeight);
    }

    if (numMeasurements > 1) {
      int quarterHeight = maxHeight / 4;
      int halfHeight = maxHeight / 2;

      canvas.setStrokeStyle(light);
      canvas.setLineWidth(1);

      // the |-- of the |--[ | ]---|
      if ((int) (a - b) != 0) {
        canvas.beginPath();
        canvas.moveTo(a, quarterHeight);
        canvas.lineTo(a, maxHeight - quarterHeight);
        canvas.stroke();
        canvas.beginPath();
        canvas.moveTo(a, halfHeight);
        canvas.lineTo(b, halfHeight);
        canvas.stroke();
      }

      // the ---| of the |--[ | ]---|
      if ((int) (e - d) != 0) {
        canvas.setStrokeStyle(dark);
        canvas.beginPath();
        canvas.moveTo(d, halfHeight);
        canvas.lineTo(e, halfHeight);
        canvas.stroke();
        canvas.beginPath();
        canvas.moveTo(e, quarterHeight);
        canvas.lineTo(e, maxHeight - quarterHeight);
        canvas.stroke();
      }
    }

    return canvas;
  }
}
