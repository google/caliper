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

package com.google.caliper.runner;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.caliper.runner.WorkerProcess.ShutdownHookRegistrar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests {@link WorkerProcess}.
 */

@RunWith(MockitoJUnitRunner.class)
public class WorkerProcessTest {
  @Mock ShutdownHookRegistrar registrar;
  @Mock Process delegate;
  @Captor ArgumentCaptor<Thread> hookCaptor;
  private WorkerProcess workerProcess;

  @Before public void createWorkerProcess() {
    this.workerProcess = new WorkerProcess(registrar, delegate);
  }

  @Test public void shutdownHook_waitFor() throws Exception {
    verify(registrar).addShutdownHook(hookCaptor.capture());
    when(delegate.waitFor()).thenReturn(0);
    workerProcess.waitFor();
    verify(registrar).removeShutdownHook(hookCaptor.getValue());
  }

  @Test public void shutdownHook_exitValueThrows() throws Exception {
    verify(registrar).addShutdownHook(hookCaptor.capture());
    when(delegate.exitValue()).thenThrow(new IllegalThreadStateException());
    try {
      workerProcess.exitValue();
      fail();
    } catch (IllegalThreadStateException expected) {}
    verify(registrar, never()).removeShutdownHook(hookCaptor.getValue());
  }

  @Test public void shutdownHook_exitValue() throws Exception {
    verify(registrar).addShutdownHook(hookCaptor.capture());
    when(delegate.exitValue()).thenReturn(0);
    workerProcess.exitValue();
    verify(registrar).removeShutdownHook(hookCaptor.getValue());
  }

  @Test public void shutdownHook_destroy() throws Exception {
    verify(registrar).addShutdownHook(hookCaptor.capture());
    workerProcess.destroy();
    verify(delegate).destroy();
    verify(registrar).removeShutdownHook(hookCaptor.getValue());
  }
}
