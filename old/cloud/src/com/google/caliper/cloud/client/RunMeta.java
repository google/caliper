/**
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


package com.google.caliper.cloud.client;

import com.google.caliper.Run;
import java.io.Serializable;
import java.util.Comparator;

public final class RunMeta
    implements Deletable, Nameable, Serializable /* for GWT Serialization */ {

  public static final Comparator<RunMeta> ORDER_BY_DATE = new Comparator<RunMeta>() {
    public int compare(RunMeta a, RunMeta b) {
      return a.run.getExecutedTimestamp().compareTo(b.run.getExecutedTimestamp());
    }
  };

  private /*final*/ long id;
  private /*final*/ Run run;
  private /*final*/ String environmentKey;

  private String name;
  private boolean deleted;
  private int style;
  private EnvironmentMeta environmentMeta;

  public RunMeta(long id, Run run, String name, String environmentKey) {
    this.id = id;
    this.run = run;
    this.name = name;
    this.environmentKey = environmentKey;
    this.environmentMeta = null;
  }

  public long getId() {
    return id;
  }

  public Run getRun() {
    return run;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  /**
   * Returns a nearly-unique style integer, used to assign a color to this run.
   * We reuse style integers when we run out of colors.
   */
  public int getStyle() {
    return style;
  }

  public void setStyle(int style) {
    this.style = style;
  }

  public void setEnvironmentMeta(EnvironmentMeta environmentMeta) {
    this.environmentMeta = environmentMeta;
  }

  public EnvironmentMeta getEnvironmentMeta() {
    return environmentMeta;
  }

  public String getEnvironmentKey() {
    return environmentKey;
  }

  private RunMeta() {} // for GWT Serialization
}
