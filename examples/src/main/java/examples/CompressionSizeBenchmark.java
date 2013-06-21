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

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.model.ArbitraryMeasurement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;

/**
 * Example "arbitrary measurement" benchmark.
 */
public class CompressionSizeBenchmark {

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

  @Benchmark long simpleCompression(int reps) {
    long dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += compress(toCompress.getBytes()).length;
    }
    return dummy;
  }

  @ArbitraryMeasurement(units = ":1", description = "ratio of uncompressed to compressed")
  public double compressionSize() {
    byte[] initialBytes = toCompress.getBytes();
    byte[] finalBytes = compress(initialBytes);
    compressionRatio = (double) initialBytes.length / (double) finalBytes.length;
    return compressionRatio;
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
}
