/*
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

package examples;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;

import java.util.EnumSet;
import java.util.Set;

/**
 * Measures EnumSet#contains().
 */
public class EnumSetContainsBenchmark {

  @Param private SetMaker setMaker;

  public enum SetMaker {
    ENUM_SET {
      @Override Set<?> newSet() {
        return EnumSet.allOf(RegularSize.class);
      }
      @Override Object[] testValues() {
        return new Object[] { RegularSize.E1, RegularSize.E2, RegularSize.E20,
            RegularSize.E39, RegularSize.E40, "A", LargeSize.E40, null };
      }
    },
    LARGE_ENUM_SET {
      @Override Set<?> newSet() {
        return EnumSet.allOf(LargeSize.class);
      }
      @Override Object[] testValues() {
        return new Object[] { LargeSize.E1, LargeSize.E63, LargeSize.E64,
            LargeSize.E65, LargeSize.E140, "A", RegularSize.E40, null };
      }
    };

    abstract Set<?> newSet();
    abstract Object[] testValues();
  }

  private enum RegularSize {
    E1, E2, E3, E4, E5, E6, E7, E8, E9, E10, E11, E12, E13, E14, E15, E16, E17,
    E18, E19, E20, E21, E22, E23, E24, E25, E26, E27, E28, E29, E30, E31, E32,
    E33, E34, E35, E36, E37, E38, E39, E40,
  }

  private enum LargeSize {
    E1, E2, E3, E4, E5, E6, E7, E8, E9, E10, E11, E12, E13, E14, E15, E16, E17,
    E18, E19, E20, E21, E22, E23, E24, E25, E26, E27, E28, E29, E30, E31, E32,
    E33, E34, E35, E36, E37, E38, E39, E40, E41, E42, E43, E44, E45, E46, E47,
    E48, E49, E50, E51, E52, E53, E54, E55, E56, E57, E58, E59, E60, E61, E62,
    E63, E64, E65, E66, E67, E68, E69, E70, E71, E72, E73, E74, E75, E76, E77,
    E78, E79, E80, E81, E82, E83, E84, E85, E86, E87, E88, E89, E90, E91, E92,
    E93, E94, E95, E96, E97, E98, E99, E100, E101, E102, E103, E104, E105, E106,
    E107, E108, E109, E110, E111, E112, E113, E114, E115, E116, E117, E118,
    E119, E120, E121, E122, E123, E124, E125, E126, E127, E128, E129, E130,
    E131, E132, E133, E134, E135, E136, E137, E138, E139, E140,
  }

  private Set<?> set;
  private Object[] testValues;

  @BeforeExperiment void setUp() {
    this.set = setMaker.newSet();
    this.testValues = setMaker.testValues();
  }

  @Benchmark void contains(int reps) {
    for (int i = 0; i < reps; i++) {
      set.contains(testValues[i % testValues.length]);
    }
  }
}
