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

package test;

import com.google.caliper.Result;
import com.google.caliper.Run;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import junit.framework.TestCase;

public class ResultXmlTest extends TestCase {

  public void testXmlRoundtrip() {
    Run a15dalvik = new Run(ImmutableMap.of(
        "foo", "A", "bar", "15", "vm", "dalvikvm"));
    Run b15dalvik = new Run(ImmutableMap.of(
        "foo", "B", "bar", "15", "vm", "dalvikvm"));

    Result toEncode = new Result(ImmutableMap.of(a15dalvik, 1200.1, b15dalvik, 1100.2));
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    toEncode.toXml(bytesOut);

    // we don't validate the XML directly because it's a hassle to cope with arbitrary orderings of
    // an element's attributes

    ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
    Result decoded = Result.fromXml(bytesIn);

    assertEquals(toEncode, decoded);
  }
}
