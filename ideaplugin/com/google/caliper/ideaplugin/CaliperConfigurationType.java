/*
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

package com.google.caliper.ideaplugin;

import com.intellij.execution.LocatableConfigurationType;
import com.intellij.execution.Location;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA. User: kevinb Date: Jan 11, 2010 Time: 2:58:47 PM To change this
 * template use File | Settings | File Templates.
 */
public class CaliperConfigurationType implements LocatableConfigurationType {
  public static final CaliperConfigurationType INSTANCE = new CaliperConfigurationType();

  public RunnerAndConfigurationSettings createConfigurationByLocation(Location location) {
    return null;
  }

  public boolean isConfigurationByLocation(RunConfiguration runConfiguration, Location location) {
    return true;
  }

  public String getDisplayName() {
    return "Caliper benchmark";
  }

  public String getConfigurationTypeDescription() {
    return "Blah";
  }

  public Icon getIcon() {
    return new ApplicationConfigurationType().getIcon();
  }

  @NotNull public String getId() {
    return "Caliper";
  }

  public ConfigurationFactory[] getConfigurationFactories() {
    ConfigurationFactory factory = new ConfigurationFactory(this) {
      @Override public RunConfiguration createTemplateConfiguration(Project project) {
        return new CaliperRunConfiguration("what does this do?", project, INSTANCE);
      }
    };
    return new ConfigurationFactory[] {factory};
  }
}
