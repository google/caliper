/*
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

package com.google.caliper.examples;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Tests various Character methods, intended for testing multiple
 * implementations against each other.
 */
public class CharacterBenchmark extends SimpleBenchmark {

    @Param CharacterSet characterSet;
    static Collection<CharacterSet> characterSetValues = EnumSet.allOf(CharacterSet.class);

    char[] chars;

    @Override protected void setUp() throws Exception {
        this.chars = characterSet.chars;
    }

    enum CharacterSet {
        ASCII(128),
        UNICODE(65536);
        char[] chars;
        CharacterSet(int size) {
            this.chars = new char[65536];
            for (int i = 0; i < 65536; ++i) {
                chars[i] = (char) (i % size);
            }
        }
    }

    public void timeDigit(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.digit(chars[ch], 10);
            }
        }
    }

    public void timeGetNumericValue(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.getNumericValue(chars[ch]);
            }
        }
    }

    public void timeIsDigit(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.isDigit(chars[ch]);
            }
        }
    }

    public void timeIsIdentifierIgnorable(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.isIdentifierIgnorable(chars[ch]);
            }
        }
    }

    public void timeIsJavaIdentifierPart(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.isJavaIdentifierPart(chars[ch]);
            }
        }
    }

    public void timeIsJavaIdentifierStart(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.isJavaIdentifierStart(chars[ch]);
            }
        }
    }

    public void timeIsLetter(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.isLetter(chars[ch]);
            }
        }
    }

    public void timeIsLetterOrDigit(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.isLetterOrDigit(chars[ch]);
            }
        }
    }

    public void timeIsLowerCase(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.isLowerCase(chars[ch]);
            }
        }
    }

    public void timeIsSpaceChar(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.isSpaceChar(chars[ch]);
            }
        }
    }

    public void timeIsUpperCase(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.isUpperCase(chars[ch]);
            }
        }
    }

    public void timeIsWhitespace(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.isWhitespace(chars[ch]);
            }
        }
    }

    public void timeIsNull(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                boolean b = (ch == ' ');
            }
        }
    }

    public void timeToLowerCase(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.toLowerCase(chars[ch]);
            }
        }
    }

    public void timeToUpperCase(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (int ch = 0; ch < 65536; ++ch) {
                Character.toUpperCase(chars[ch]);
            }
        }
    }
}
