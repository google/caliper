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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunJavaConfiguration;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import java.util.Collection;
import java.util.Collections;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA. User: kevinb Date: Jan 11, 2010 Time: 3:05:25 PM To change this
 * template use File | Settings | File Templates.
 */
public class CaliperRunConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule>
    implements RunJavaConfiguration {

  private String benchmarkClassName;
  private final CaliperConfigurationType type;

  public CaliperRunConfiguration(String name, Project project, CaliperConfigurationType type) {
    super(name, new JavaRunConfigurationModule(project, true), type.getConfigurationFactories()[0]);
    this.type = type;
  }

  public RunProfileState getState(@NotNull Executor executor,
      @NotNull ExecutionEnvironment env) throws ExecutionException {
    final JavaCommandLineState state = new MyJavaCommandLineState(env);
    state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
    return state;
  }

  public static final String CALIPER_MAIN = "com.google.caliper.Runner";

  private class MyJavaCommandLineState extends JavaCommandLineState {

    private MyJavaCommandLineState(@NotNull ExecutionEnvironment environment) {
      super(environment);
    }

    @Override protected JavaParameters createJavaParameters() throws ExecutionException {
      JavaParameters params = new JavaParameters();
      int classPathType = JavaParametersUtil.getClasspathType(
          getConfigurationModule(), CALIPER_MAIN, false);
      JavaParametersUtil.configureModule(getConfigurationModule(), params, classPathType, null);
      JavaParametersUtil.configureConfiguration(params, CaliperRunConfiguration.this);
      params.setMainClass(CALIPER_MAIN);
      return params;
    }
  }

  public void setProperty(int i, String s) {}

  public String getProperty(int i) {
    return null;
  }

  public boolean isAlternativeJrePathEnabled() {
    return false;
  }

  public void setAlternativeJrePathEnabled(boolean b) {}

  public String getAlternativeJrePath() {
    return null;
  }

  public void setAlternativeJrePath(String s) {}

  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new SettingsEditor<CaliperRunConfiguration>() {
      @Override protected void resetEditorFrom(CaliperRunConfiguration caliperRunConfiguration) {
      }
      @Override protected void applyEditorTo(CaliperRunConfiguration caliperRunConfiguration)
          throws ConfigurationException {
      }
      @NotNull @Override protected JComponent createEditor() {
        return new JPanel();
      }
      @Override protected void disposeEditor() {
      }
    };
  }

  public void setRunClass(String className) {
    this.benchmarkClassName = className;
  }

  public String getRunClass() {
    return benchmarkClassName; // ??
  }

  public String getPackage() {
    return null;
  }

  @Override public Collection<Module> getValidModules() {
    // why switch?
    return Collections.singleton(getConfigurationModule().getModule());
  }

  @Override protected ModuleBasedConfiguration createInstance() {
    return new CaliperRunConfiguration(getName(), getProject(), type);
  }
}
