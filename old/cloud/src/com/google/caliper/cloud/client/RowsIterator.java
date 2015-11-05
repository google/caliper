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

import java.util.*;

/**
 * Iterate the keys that make up the rows of the table.
 */
class RowsIterator {

  private final List<Variable> rVariables;
  private final Iterator<List<Value>> rValuesIterator;
  private List<Value> currentPermutation;

  RowsIterator(List<Variable> rVariables) {
    this.rVariables = rVariables;

    List<Collection<Value>> columns = new ArrayList<Collection<Value>>();
    for (Variable variable : rVariables) {
      List<Value> shownValues = new ArrayList<Value>();

      for (Value value : variable.getValues()) {
        if (value.isShown()) {
          shownValues.add(value);
        }
      }

      /*
       * If we have any variables with no values, we can't display a table.
       */
      if (shownValues.isEmpty()) {
        rValuesIterator = Collections.<List<Value>>emptySet().iterator();
        return;
      }

      columns.add(shownValues);
    }

    this.rValuesIterator = new PermutationsIterator<Value>(columns);
  }

  public boolean nextRow() {
    if (rValuesIterator.hasNext()) {
      currentPermutation = rValuesIterator.next();
      return true;
    } else {
      return false;
    }
  }

  public Value getRValue(int column) {
    if (currentPermutation == null) {
      throw new IllegalStateException();
    }

    return currentPermutation.get(column);
  }

  /**
   * Populates the given key with the current row values.
   */
  public void updateKey(Key key) {
    if (currentPermutation == null) {
      throw new IllegalStateException();
    }

    int rIndex = 0;
    for (Variable variable : rVariables) {
      key.set(variable, currentPermutation.get(rIndex++));
    }
  }
}
