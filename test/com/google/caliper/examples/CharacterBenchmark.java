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

    char[] values;

    @Override protected void setUp() throws Exception {
        values = characterSet.chars;
    }

    enum CharacterSet {
        ASCII(128),
        UNICODE(65536);
        char[] chars;
        CharacterSet(int size) {
            chars = new char[size];
            for (int i = 0; i < chars.length; ++i) {
                chars[i] = (char) i;
            }
        }
    }

    public void timeDigit(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.digit(ch, 10);
            }
        }
    }

    public void timeGetNumericValue(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.getNumericValue(ch);
            }
        }
    }

    public void timeIsDigit(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.isDigit(ch);
            }
        }
    }

    public void timeIsIdentifierIgnorable(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.isIdentifierIgnorable(ch);
            }
        }
    }

    public void timeIsJavaIdentifierPart(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.isJavaIdentifierPart(ch);
            }
        }
    }

    public void timeIsJavaIdentifierStart(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.isJavaIdentifierStart(ch);
            }
        }
    }

    public void timeIsLetter(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.isLetter(ch);
            }
        }
    }

    public void timeIsLetterOrDigit(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.isLetterOrDigit(ch);
            }
        }
    }

    public void timeIsLowerCase(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.isLowerCase(ch);
            }
        }
    }

    public void timeIsSpaceChar(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.isSpaceChar(ch);
            }
        }
    }

    public void timeIsUpperCase(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.isUpperCase(ch);
            }
        }
    }

    public void timeIsWhitespace(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.isWhitespace(ch);
            }
        }
    }

    public void timeIsNull(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                boolean b = (ch == ' ');
            }
        }
    }

    public void timeToLowerCase(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.toLowerCase(ch);
            }
        }
    }

    public void timeToUpperCase(int reps) {
        for (int i = 0; i < reps; ++i) {
            for (char ch = 0; ch < '}'; ++ch) {
                Character.toUpperCase(ch);
            }
        }
    }
}
