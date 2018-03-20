/*
 * Copyright (C) 2018 Google Inc.
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

package com.google.caliper.runner.target;

import com.google.caliper.runner.config.CaliperConfig;
import com.google.caliper.runner.config.DeviceConfig;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.common.util.concurrent.Service;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import javax.inject.Singleton;

/** Module for providing the {@link DeviceService}. */
@Module
public abstract class DeviceModule {

  @Provides
  static DeviceConfig provideDeviceConfig(CaliperOptions options, CaliperConfig config) {
    return config.getDeviceConfig(options.deviceName());
  }

  @Binds
  abstract ShutdownHookRegistrar bindShutdownHookRegistrar(RuntimeShutdownHookRegistrar registrar);

  // only local device supported at the moment
  @Binds
  @Singleton
  abstract DeviceService bindLocalDeviceService(LocalDeviceService localDeviceService);

  @Binds
  @IntoSet
  abstract Service bindDeviceService(DeviceService service);
}
