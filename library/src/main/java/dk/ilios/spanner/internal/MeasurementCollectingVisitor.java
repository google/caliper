package dk.ilios.spanner.internal;

import java.util.List;

import dk.ilios.spanner.bridge.LogMessageVisitor;
import dk.ilios.spanner.model.Measurement;

public interface MeasurementCollectingVisitor extends LogMessageVisitor {

    boolean isDoneCollecting();
    boolean isWarmupComplete();
    List<Measurement> getMeasurements();

    /**
     * Returns all the messages created while collecting measurements.
     * <p>
     * <p>A message is some piece of user visible data that should be displayed to the user along
     * with the trial results.
     * <p>
     * <p>TODO(lukes): should we model these as anything more than strings.  These messages already
     * have a concept of 'level' based on the prefix.
     */
    List<String> getMessages();
}

