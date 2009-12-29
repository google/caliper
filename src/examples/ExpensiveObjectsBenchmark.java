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

import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Benchmarks creation and cloning various expensive objects.
 */
@SuppressWarnings({"ResultOfObjectAllocationIgnored"}) // TODO: should fix!
public class ExpensiveObjectsBenchmark extends SimpleBenchmark {
    public void timeNewDecimalFormatSymbols(int reps) {
        for (int i = 0; i < reps; ++i) {
            new DecimalFormatSymbols(Locale.US);
        }
    }
    
    public void timeClonedDecimalFormatSymbols(int reps) {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        for (int i = 0; i < reps; ++i) {
            dfs.clone();
        }
    }
    
    public void timeNewNumberFormat(int reps) {
        for (int i = 0; i < reps; ++i) {
            NumberFormat.getInstance(Locale.US);
        }
    }
    
    public void timeClonedNumberFormat(int reps) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        for (int i = 0; i < reps; ++i) {
            nf.clone();
        }
    }
    
    public void timeNewSimpleDateFormat(int reps) {
        for (int i = 0; i < reps; ++i) {
            new SimpleDateFormat();
        }
    }
    
    public void timeClonedSimpleDateFormat(int reps) {
        SimpleDateFormat sdf = new SimpleDateFormat();
        for (int i = 0; i < reps; ++i) {
            sdf.clone();
        }
    }

    // TODO: remove this from all examples when IDE plugins are ready
    public static void main(String[] args) throws Exception {
        Runner.main(ExpensiveObjectsBenchmark.class, args);
    }
}
