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

import com.google.gwt.widgetideas.graphics.client.Color;

final class Colors {
  private static final Color[][] COLORS = new Color[][] {
      { new Color( 35,  41, 170), new Color(111, 115, 199), new Color(149, 152, 214) }, // blue
      { new Color(196,   0,  10), new Color(216,  89,  95), new Color(226, 133, 137) }, // red
      { new Color(  0, 169,   0), new Color( 89, 199,  89), new Color(133, 213, 133) }, // green
      { new Color(181,  93,  37), new Color(206, 149, 113), new Color(219, 177, 150) }, // orange
      { new Color(  0, 165, 167), new Color( 89, 196, 197), new Color(133, 211, 212) }, // teal
      { new Color(159,  29, 170), new Color(192, 107, 199), new Color(209, 146, 214) }, // fuschia
      { new Color(100,  57,  40), new Color(154, 126, 115), new Color(180, 160, 152) }, // brown
      { new Color(170, 148,  11), new Color(199, 185,  96), new Color(214, 203, 138) }, // yellow
      { new Color(108, 168,  57), new Color(159, 198, 126), new Color(184, 213, 160) }, // moss
      { new Color(181,  93, 144), new Color(206, 149, 182), new Color(219, 177, 201) }, // pink
      { new Color( 46,  76,  94), new Color(118, 138, 150), new Color(155, 169, 177) }, // denim
      { new Color(110, 110, 110), new Color(160, 160, 160), new Color(185, 185, 185) }, // grey
      { new Color(126, 158, 166), new Color(171, 191, 197), new Color(193, 208, 212) }, // blue-grey
      { new Color( 75,  94,   0), new Color(137, 150,  89), new Color(168, 177, 133) }, // olive
  };

  /**
   * Returns a three-element array of the color for the given style containing
   * the dark, medium, and light variants.
   */
  static Color[] forStyle(int style) {
    int index = style % COLORS.length;
    return COLORS[index];
  }
}
