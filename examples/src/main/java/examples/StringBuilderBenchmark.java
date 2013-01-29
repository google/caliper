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
import com.google.caliper.Param;
import com.google.caliper.runner.CaliperMain;

/**
 * Tests the performance of various StringBuilder methods.
 */
public class StringBuilderBenchmark extends Benchmark {

    @Param({"1", "10", "100"}) private int length;

    public void timeAppendBoolean(int reps) {
        for (int i = 0; i < reps; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; ++j) {
                sb.append(true);
            }
        }
    }

    public void timeAppendChar(int reps) {
        for (int i = 0; i < reps; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; ++j) {
                sb.append('c');
            }
        }
    }

    public void timeAppendCharArray(int reps) {
        char[] chars = "chars".toCharArray();
        for (int i = 0; i < reps; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; ++j) {
                sb.append(chars);
            }
        }
    }

    public void timeAppendCharSequence(int reps) {
        CharSequence cs = "chars";
        for (int i = 0; i < reps; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; ++j) {
                sb.append(cs);
            }
        }
    }

    public void timeAppendDouble(int reps) {
        double d = 1.2;
        for (int i = 0; i < reps; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; ++j) {
                sb.append(d);
            }
        }
    }

    public void timeAppendFloat(int reps) {
        float f = 1.2f;
        for (int i = 0; i < reps; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; ++j) {
                sb.append(f);
            }
        }
    }

    public void timeAppendInt(int reps) {
        int n = 123;
        for (int i = 0; i < reps; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; ++j) {
                sb.append(n);
            }
        }
    }

    public void timeAppendLong(int reps) {
        long l = 123;
        for (int i = 0; i < reps; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; ++j) {
                sb.append(l);
            }
        }
    }

    public void timeAppendObject(int reps) {
        Object o = new Object();
        for (int i = 0; i < reps; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; ++j) {
                sb.append(o);
            }
        }
    }

    public void timeAppendString(int reps) {
        String s = "chars";
        for (int i = 0; i < reps; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; ++j) {
                sb.append(s);
            }
        }
    }

    // TODO: remove this from all examples when IDE plugins are ready
    public static void main(String[] args) throws Exception {
        CaliperMain.main(StringBuilderBenchmark.class, args);
    }
}
