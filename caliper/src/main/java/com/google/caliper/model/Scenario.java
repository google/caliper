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

package com.google.caliper.model;

import static com.google.caliper.model.PersistentHashing.getPersistentHashFunction;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static javax.persistence.AccessType.FIELD;

import com.google.caliper.model.BenchmarkSpec.BenchmarkSpecFunnel;
import com.google.caliper.model.Host.HostFunnel;
import com.google.caliper.model.VmSpec.VmSpecFunnel;
import com.google.common.base.Objects;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Index;

import javax.persistence.Access;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.QueryHint;

/**
 * The combination of properties whose combination, when measured with a particular instrument,
 * should produce a repeatable result
 *
 * @author gak@google.com (Gregory Kick)
 */
@Entity
@Access(FIELD)
@Immutable
@Cacheable
@NamedQuery(
    name = "listScenariosForHash",
    query = "SELECT s FROM Scenario s WHERE hash = :hash",
    hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.readOnly", value = "true")})
public final class Scenario {
  static final Scenario DEFAULT = new Scenario();

  @Id @GeneratedValue @ExcludeFromJson private int id;
  @ManyToOne(optional = false) private Host host;
  @ManyToOne(optional = false) private VmSpec vmSpec;
  @ManyToOne(optional = false) private BenchmarkSpec benchmarkSpec;
  // TODO(gak): include data about caliper itself and the code being benchmarked
  @ExcludeFromJson @Index(name = "hash_index") private int hash;

  private Scenario() {
    this.host = Host.DEFAULT;
    this.vmSpec = VmSpec.DEFAULT;
    this.benchmarkSpec = BenchmarkSpec.DEFAULT;
  }

  private Scenario(Builder builder) {
    this.host = builder.host;
    this.vmSpec = builder.vmSpec;
    this.benchmarkSpec = builder.benchmarkSpec;
  }

  public Host host() {
    return host;
  }

  public VmSpec vmSpec() {
    return vmSpec;
  }

  public BenchmarkSpec benchmarkSpec() {
    return benchmarkSpec;
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof Scenario) {
      Scenario that = (Scenario) obj;
      return this.host.equals(that.host)
          && this.vmSpec.equals(that.vmSpec)
          && this.benchmarkSpec.equals(that.benchmarkSpec);
    } else {
      return false;
    }
  }

  @PrePersist
  @PreUpdate
  private void initHash() {
    if (hash == 0) {
      this.hash = getPersistentHashFunction()
          .newHasher()
          .putObject(host, HostFunnel.INSTANCE)
          .putObject(vmSpec, VmSpecFunnel.INSTANCE)
          .putObject(benchmarkSpec, BenchmarkSpecFunnel.INSTANCE)
          .hash().asInt();
    }
  }

  @Override public int hashCode() {
    initHash();
    return hash;
  }

  @Override public String toString() {
    return Objects.toStringHelper(this)
        .add("environment", host)
        .add("vmSpec", vmSpec)
        .add("benchmarkSpec", benchmarkSpec)
        .toString();
  }

  public static final class Builder {
    private Host host;
    private VmSpec vmSpec;
    private BenchmarkSpec benchmarkSpec;

    public Builder host(Host.Builder hostBuilder) {
      return host(hostBuilder.build());
    }

    public Builder host(Host host) {
      this.host = checkNotNull(host);
      return this;
    }

    public Builder vmSpec(VmSpec.Builder vmSpecBuilder) {
      return vmSpec(vmSpecBuilder.build());
    }

    public Builder vmSpec(VmSpec vmSpec) {
      this.vmSpec = checkNotNull(vmSpec);
      return this;
    }

    public Builder benchmarkSpec(BenchmarkSpec.Builder benchmarkSpecBuilder) {
      return benchmarkSpec(benchmarkSpecBuilder.build());
    }

    public Builder benchmarkSpec(BenchmarkSpec benchmarkSpec) {
      this.benchmarkSpec = checkNotNull(benchmarkSpec);
      return this;
    }

    public Scenario build() {
      checkState(host != null);
      checkState(vmSpec != null);
      checkState(benchmarkSpec != null);
      return new Scenario(this);
    }
  }
}
