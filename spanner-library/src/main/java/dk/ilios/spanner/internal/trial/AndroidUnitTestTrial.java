package dk.ilios.spanner.internal.trial;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

import java.util.Collections;
import java.util.concurrent.Callable;

import dk.ilios.spanner.Spanner;
import dk.ilios.spanner.bridge.ShouldContinueMessage;
import dk.ilios.spanner.bridge.StartMeasurementLogMessage;
import dk.ilios.spanner.bridge.StopMeasurementLogMessage;
import dk.ilios.spanner.internal.MeasurementCollectingVisitor;
import dk.ilios.spanner.internal.benchmark.BenchmarkClass;
import dk.ilios.spanner.model.Trial;
import dk.ilios.spanner.options.SpannerOptions;
import dk.ilios.spanner.util.ShortDuration;
import dk.ilios.spanner.worker.Worker;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * A Trial that is running on the thread it was started on.
 * This can be useful when e.g. running tests on the Android UI thread.
 * <p>
 * Warning: This will block the executing thread until the trial is complete.
 */
public class AndroidUnitTestTrial implements Callable<Trial.Result> {

    private final Trial trial;
    private final SpannerOptions options;
    private final MeasurementCollectingVisitor measurementCollectingVisitor;
    private final TrialOutputLogger trialOutput;
    private final Stopwatch trialStopwatch = Stopwatch.createUnstarted();
    private final BenchmarkClass benchmark;
    private final Spanner.Callback callback;

    public AndroidUnitTestTrial(
            Trial trial,
            BenchmarkClass benchmarkClass,
            MeasurementCollectingVisitor measurementCollectingVisitor,
            SpannerOptions options,
            TrialOutputLogger trialOutput,
            Spanner.Callback callback
    ) {
        this.trial = trial;
        this.options = options;
        this.measurementCollectingVisitor = measurementCollectingVisitor;
        this.trialOutput = trialOutput;
        this.benchmark = benchmarkClass;
        this.callback = callback;
    }

    // TODO Timeout not possible when running on the same thread
    // TODO Error checking has been removed. Any crash in benchmark code will crash everything.
    @Override
    public Trial.Result call() throws Exception {
        callback.trialStarted(trial);
        Trial.Result result = null;
        try {
            result = getResult();
            callback.trialSuccess(trial, result);
        } catch (Throwable e) {
            callback.trialFailure(trial, e);
            throw e;
        } finally {
            callback.trialEnded(trial);
        }
        return result;
    }

    private Trial.Result getResult() throws Exception {
        Worker worker = (Worker) trial.experiment().instrumentation().workerClass().getDeclaredConstructors()[0].newInstance(
                benchmark,
                trial.experiment().instrumentation().benchmarkMethod(),
                Ticker.systemTicker(),
                trial.experiment().instrumentation().workerOptions(),
                trial.experiment().userParameters()
        );

        worker.setUpBenchmark();
        worker.bootstrap();
        boolean keepMeasuring = true;
        boolean isInWarmup = true;
        boolean doneCollecting = false;
        StopMeasurementLogMessage stopMessage = new StopMeasurementLogMessage(Collections.EMPTY_LIST);
        ShouldContinueMessage continueMessage = new ShouldContinueMessage();
        while (keepMeasuring) {
            new StartMeasurementLogMessage().accept(measurementCollectingVisitor);
            worker.preMeasure(isInWarmup);
            stopMessage.setMeasurements(worker.measure());
            stopMessage.accept(measurementCollectingVisitor);
            if (!doneCollecting && measurementCollectingVisitor.isDoneCollecting()) {
                doneCollecting = true;
            }
            continueMessage.update(!doneCollecting, measurementCollectingVisitor.isWarmupComplete());
            keepMeasuring = continueMessage.shouldContinue();
            isInWarmup = !continueMessage.isWarmupComplete();
            worker.postMeasure();
        }
        worker.tearDownBenchmark();
        trial.addAllMeasurements(measurementCollectingVisitor.getMeasurements());
        trial.addAllMessages(measurementCollectingVisitor.getMessages());
        return trial.getResult();
    }


    private long getTrialTimeLimitTrialNanos() {
        ShortDuration timeLimit = options.timeLimit();
        if (ShortDuration.zero().equals(timeLimit)) {
            return Long.MAX_VALUE;
        }
        return timeLimit.to(NANOSECONDS);
    }

}
