/**
 * Copyright (C) 2010 Google Inc.
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

package com.google.caliper.cloud.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;
import java.util.Map;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("benchmark")
public interface BenchmarkService extends RemoteService {
  Long createSnapshot(Benchmark benchmark);
  List<String> fetchBenchmarkNames(String benchmarkOwner);
  BenchmarkMeta fetchBenchmark(String benchmarkOwner, String benchmarkName, Long snapshotId);
  void nameRun(long id, String name);
  void nameEnvironment(long id, String name);
  void setRunDeleted(long id, boolean deleted);
  void setSnapshotDeleted(long id, boolean deleted);
  void reorderVariables(String benchmarkOwner, String benchmarkName,
      List<String> rVariables, String cVariable);
  void setVariableValuesShown(String benchmarkOwner, String benchmarkName,
      Map<String, Map<String, Boolean>> variableValuesShown);
}
