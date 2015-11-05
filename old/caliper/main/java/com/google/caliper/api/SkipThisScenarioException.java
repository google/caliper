/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.api;

/**
 * Throw this exception from your benchmark class's setUp method, or benchmark method
 * to indicate that the combination of parameters supplied should not be benchmarked. For example,
 * while you might want to test <i>most</i> combinations of the parameters {@code size} and {@code
 * comparator}, you might not want to test the specific combination of {@code size=100000000} and
 * {@code comparator=reallyExpensiveComparator}.
 */
@SuppressWarnings("serial")
public final class SkipThisScenarioException extends RuntimeException {}
