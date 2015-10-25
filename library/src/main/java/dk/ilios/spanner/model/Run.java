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
 *
 * Original author gak@google.com (Gregory Kick)
 */

package dk.ilios.spanner.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Objects;

import org.threeten.bp.Instant;

import java.util.UUID;

import dk.ilios.spanner.config.SpannerConfiguration;
import dk.ilios.spanner.json.ExcludeFromJson;
import dk.ilios.spanner.options.SpannerOptions;

/**
 * A single invocation of Gauge.
 */
public final class Run {

    private UUID id;
    private String label;
    private Instant startTime;

    @ExcludeFromJson
    private SpannerConfiguration configuration;
    @ExcludeFromJson
    private SpannerOptions options;

    private Run(Builder builder) {
        this.id = builder.id;
        this.label = builder.label;
        this.startTime = builder.startTime;
        this.configuration = builder.config;
        this.options = builder.options;
    }

    public UUID id() {
        return id;
    }

    public String label() {
        return label;
    }

    public Instant startTime() {
        return startTime;
    }

    public SpannerConfiguration getConfiguration() {
        return configuration;
    }

    public SpannerOptions getOptions() {
        return options;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Run) {
            Run that = (Run) obj;
            return this.id.equals(that.id)
                    && this.label.equals(that.label)
                    && this.startTime.equals(that.startTime);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, label, startTime);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("label", label)
                .add("startTime", startTime)
                .toString();
    }

    /**
     * Builder for easily creating the Run
     */
    public static final class Builder {
        private UUID id;
        private String label = "";
        private Instant startTime;
        private SpannerConfiguration config;
        private SpannerOptions options;

        public Builder(UUID id) {
            this.id = checkNotNull(id);
        }

        public Builder label(String label) {
            this.label = checkNotNull(label);
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = checkNotNull(startTime);
            return this;
        }

        public Builder configuration(SpannerConfiguration config) {
            this.config = config;
            return this;
        }

        public Builder options(SpannerOptions options) {
            this.options = options;
            return this;
        }

        public Run build() {
            checkState(id != null);
            checkState(startTime != null);
            checkState(config != null);
            checkState(options != null);
            return new Run(this);
        }
    }
}
