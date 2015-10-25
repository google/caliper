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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Iterates through all permutations created by selecting one element
 * from each of a list of collections.
 */
class PermutationsIterator<T> implements Iterator<List<T>> {

  private final List<Collection<T>> columns;
  private final List<Iterator<T>> counters = new ArrayList<Iterator<T>>();
  private List<T> next;

  PermutationsIterator(List<Collection<T>> columns) {
    this.next = new ArrayList<T>();
    this.columns = columns;

    // seed the columns and counters
    for (Collection<T> column : columns) {
      Iterator<T> iterator = column.iterator();
      next.add(iterator.next());
      counters.add(iterator);
    }
  }


  public boolean hasNext() {
    return next != null;
  }

  public List<T> next() {
    List<T> result = next;
    this.next = computeNext();
    return result;
  }

  /**
   * Finds the next permutation of values.
   */
  public List<T> computeNext() {
    if (columns.isEmpty()) {
      return null;
    }
    
    // start with the most recent permutation
    List<T> result = new ArrayList<T>(next);

    /*
     * Increment the entire permutation to the next permutation by attempting
     * to increment each column, from rightmost to leftmost. When a column
     * is out of values (in decimal, it's a "9") we roll over (to a "0") and
     * then try to increment the column to its left.
     */
    for (int c = columns.size() - 1; c >= 0; c--) {
      Iterator<T> valuesForColumn = counters.get(c);

      // if we found a column to increment, we're out of permutations
      if (valuesForColumn.hasNext()) {
        result.set(c, valuesForColumn.next());
        break;
      }

      // if we've hit the upper limit (ie. "999" on three columns), we're done
      if (c == 0) {
        return null;
      }

      // roll over this column and continue to the left
      Iterator<T> rolledOver = columns.get(c).iterator();
      result.set(c, rolledOver.next());
      counters.set(c, rolledOver);
    }

    return result;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
