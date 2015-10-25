package dk.ilios.spanner.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import dk.ilios.spanner.Spanner;
import dk.ilios.spanner.internal.InvalidBenchmarkException;
import dk.ilios.spanner.model.Trial;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getName();
    private LinearLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootLayout = ((LinearLayout) findViewById(R.id.container));
        rootLayout.removeAllViews();
        try {
            startBenchmark();
        } catch (InvalidBenchmarkException e) {
            throw new RuntimeException(e);
        }
    }

    private void startBenchmark() throws InvalidBenchmarkException {
        Spanner.runAllBenchmarks(ActivityBenchmarks.class, new Spanner.Callback() {
            @Override
            public void trialStarted(Trial trial) {
                addStatus("Start: " + getDescription(trial));
            }

            @Override
            public void trialSuccess(Trial trial, Trial.Result result) {
                double baselineFailure = 15; //benchmarkConfiguration.getBaselineFailure()
                if (trial.hasBaseline()) {
                    double absChange = Math.abs(trial.getChangeFromBaseline());
                    if (absChange > baselineFailure) {
                        addStatus(String.format("Change from baseline was to big: %.2f%%. Limit is %.2f%%",
                                absChange, baselineFailure));
                    }
                } else {
                    String resultString = String.format(" [%.2f ns.]", trial.getMedian());
                    addStatus(getDescription(trial) + resultString);
                }
            }

            @Override
            public void trialFailure(Trial trial, Throwable error) {
                addStatus(error.getMessage());
            }

            @Override
            public void trialEnded(Trial trial) {
                // Ignore
            }
        });
    }

    private String getDescription(Trial trial) {
        return trial.experiment().instrumentation().benchmarkMethod().getName();
    }

    private void addStatus(String txt) {
        Log.i(TAG, txt);
        TextView tv = new TextView(this);
        tv.setText(txt);
        rootLayout.addView(tv);
    }
}
