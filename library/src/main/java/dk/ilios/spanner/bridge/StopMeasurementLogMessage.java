/*
 * Copyright (C) 2012 Google Inc.
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

package dk.ilios.spanner.bridge;

import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import dk.ilios.spanner.model.Measurement;

/**
 * A message signaling that the timing interval has ended in the worker.
 */
public class StopMeasurementLogMessage extends LogMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Collection<Measurement> measurements = new ArrayList<Measurement>();

    public StopMeasurementLogMessage(Collection<Measurement> measurements) {
        this.measurements.addAll(measurements);
    }

    public void setMeasurements(Iterable<Measurement> measurements) {
        this.measurements.clear();
        for (Measurement measurement : measurements) {
            this.measurements.add(measurement);
        }
    }

    public Collection<Measurement> measurements() {
        return measurements;
    }

    @Override
    public void accept(LogMessageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(measurements);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof StopMeasurementLogMessage) {
            StopMeasurementLogMessage that = (StopMeasurementLogMessage) obj;
            return this.measurements.equals(that.measurements);
        } else {
            return false;
        }
    }
}
