package dk.ilios.spanner.log;

/**
 * Log implementation for Android
 */
public class AndroidStdOut implements StdOut {

    private String LOG = "Gauge";

    @Override
    public void println(String line) {
        android.util.Log.i(LOG, line);
    }

    @Override
    public void format(String string, Object... args) {
        android.util.Log.i(LOG, String.format(string, args));
    }

    @Override
    public void flush() {
        // No-op
    }

    @Override
    public void println() {
        android.util.Log.i(LOG, "");
    }

    @Override
    public void printf(String s, Object... args) {
        format(s, args);
    }

    @Override
    public void print(String s) {
        println(s);
    }
}
