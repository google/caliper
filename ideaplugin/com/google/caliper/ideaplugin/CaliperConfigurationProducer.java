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

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.junit.JavaRuntimeConfigurationProducerBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * Created by IntelliJ IDEA. User: kevinb Date: Jan 11, 2010 Time: 3:52:22 PM To change this
 * template use File | Settings | File Templates.
 */
public class CaliperConfigurationProducer extends JavaRuntimeConfigurationProducerBase
    implements Cloneable {
  private PsiClass myElement;

  public CaliperConfigurationProducer() {
    super(CaliperConfigurationType.INSTANCE);
  }

  protected RunnerAndConfigurationSettingsImpl createConfigurationByElement(Location location,
                                                                          ConfigurationContext context) {
    location = JavaExecutionUtil.stepIntoSingleClass(location);
    PsiElement element = location.getPsiElement();
    element = PsiTreeUtil.getParentOfType(element, PsiClass.class);

    myElement = (PsiClass) element;
    if (myElement == null) {
      return null;
    }
    PsiReferenceList extendsList = myElement.getExtendsList();
    PsiElement[] elements = extendsList.getChildren();
    if (elements.length != 1) {
      return null;
    }

    if (elements[0].getText().equals("SimpleBenchmark")) {
      return createConfiguration(myElement, context);
    }
    return null;
 }

 private RunnerAndConfigurationSettingsImpl createConfiguration(PsiClass clazz, ConfigurationContext context) {
   Project project = clazz.getProject();
   RunnerAndConfigurationSettingsImpl settings = cloneTemplateConfiguration(project, context);
   CaliperRunConfiguration configuration = (CaliperRunConfiguration) settings.getConfiguration();
   configuration.setRunClass(clazz.getName());
   configuration.setName("benchmark");
   setupConfigurationModule(context, configuration);
   copyStepsBeforeRun(project, configuration);
   return settings;
 }

  @Override public PsiElement getSourceElement() {
    return myElement;
  }

  public int compareTo(Object o) {
    return PREFERED; // HA HA HA HA
  }
}
