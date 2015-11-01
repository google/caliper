package dk.ilios.spanner.log;

/**
 * Interface for loggers.
 */
public interface StdOut {
    void println(String line);
    void format(String s, Object... args);
    void flush();
    void println();
    void printf(String s, Object... args);
    void print(String s);
}
