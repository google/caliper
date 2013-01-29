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

import com.google.caliper.Benchmark;
import com.google.caliper.Runner;
import java.util.Formatter;

/**
 * Compares Formatter against hand-written StringBuilder code.
 */
public class FormatterBenchmark extends Benchmark {
  public void timeFormatter_NoFormatting(int reps) {
    for (int i = 0; i < reps; i++) {
      Formatter f = new Formatter();
      f.format("this is a reasonably short string that doesn't actually need any formatting");
      f.close();
    }
  }

  public void timeStringBuilder_NoFormatting(int reps) {
    for (int i = 0; i < reps; i++) {
      StringBuilder sb = new StringBuilder();
      sb.append("this is a reasonably short string that doesn't actually need any formatting");
    }
  }

  public void timeFormatter_OneInt(int reps) {
    for (int i = 0; i < reps; i++) {
      Formatter f = new Formatter();
      f.format("this is a reasonably short string that has an int %d in it", i);
      f.close();
    }
  }

  public void timeStringBuilder_OneInt(int reps) {
    for (int i = 0; i < reps; i++) {
      StringBuilder sb = new StringBuilder();
      sb.append("this is a reasonably short string that has an int ");
      sb.append(i);
      sb.append(" in it");
    }
  }

  public void timeFormatter_OneString(int reps) {
    for (int i = 0; i < reps; i++) {
      Formatter f = new Formatter();
      f.format("this is a reasonably short string that has a string %s in it", "hello");
      f.close();
    }
  }

  public void timeStringBuilder_OneString(int reps) {
    for (int i = 0; i < reps; i++) {
      StringBuilder sb = new StringBuilder();
      sb.append("this is a reasonably short string that has a string ");
      sb.append("hello");
      sb.append(" in it");
    }
  }

  public static void main(String[] args) throws Exception {
    Runner.main(FormatterBenchmark.class, args);
  }
}
