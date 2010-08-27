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

package examples;

import com.google.caliper.LinearTranslation;
import com.google.caliper.Param;
import com.google.caliper.SimpleBenchmark;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;

/**
 * This is a hack, but shows a neat trick that can be done by overriding the nanosToUnits method.
 * Benchmarks compression ratios instead of times.
 */
public class CompressionSizeBenchmark extends SimpleBenchmark {

  @Param({
      "this string will compress badly",
      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      "asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf"})
  private String toCompress;
  @Param({"bestCompression", "bestSpeed", "noCompression", "huffmanOnly"})
  private String compressionLevel;

  private double compressionRatio;

  public static final Map<String, Integer> compressionLevelMap = new HashMap<String, Integer>();
  static {
      compressionLevelMap.put("bestCompression", Deflater.BEST_COMPRESSION);
      compressionLevelMap.put("bestSpeed", Deflater.BEST_SPEED);
      compressionLevelMap.put("noCompression", Deflater.NO_COMPRESSION);
      compressionLevelMap.put("huffmanOnly", Deflater.HUFFMAN_ONLY);
  }

  public void timeCompressionSize(int reps) {
    byte[] initialBytes = toCompress.getBytes();
    byte[] finalBytes = compress(initialBytes);
    compressionRatio = (double) initialBytes.length / (double) finalBytes.length;

    // simulate actually doing runs to make caliper happy
    LinearTranslation translation = new LinearTranslation(0, 0, 1000000, 1000);
    try {
      Thread.sleep((long) translation.translate(reps));
    } catch (InterruptedException e) {
    }
  }

  private byte[] compress(byte[] bytes) {
    Deflater compressor = new Deflater();
    compressor.setLevel(compressionLevelMap.get(compressionLevel));
    compressor.setInput(bytes);
    compressor.finish();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    while (!compressor.finished()) {
      int count = compressor.deflate(buf);
      bos.write(buf, 0, count);
    }
    try {
      bos.close();
    } catch (IOException e) {
    }
    return bos.toByteArray();
  }

  @Override public Map<String, Integer> timeUnitNames() {
    Map<String, Integer> unitNames = new HashMap<String, Integer>();
    unitNames.put(" compression ratio (higher is better)", 1);
    return unitNames;
  }

  @Override public double nanosToUnits(double nanos) {
    return compressionRatio;
  }
}
