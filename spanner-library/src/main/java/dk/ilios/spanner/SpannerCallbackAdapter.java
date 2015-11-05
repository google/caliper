/*
 * Copyright (C) 2015 Christian Melchior.
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

package dk.ilios.spanner;

import dk.ilios.spanner.model.Trial;

/**
 * Default no-op implementation of {@link dk.ilios.spanner.Spanner.Callback}.
 * Override the methods you care about.
 */
public class SpannerCallbackAdapter implements Spanner.Callback {

    @Override
    public void onStart() {
    }

    @Override
    public void trialStarted(Trial trial) {
    }

    @Override
    public void trialSuccess(Trial trial, Trial.Result result) {
    }

    @Override
    public void trialFailure(Trial trial, Throwable error) {
    }

    @Override
    public void trialEnded(Trial trial) {
    }

    @Override
    public void onComplete() {
    }

    @Override
    public void onError(Exception error) {

    }
}
