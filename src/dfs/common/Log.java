package dfs.common;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// timestamped console logging
public final class Log {

    // must include milliseconds as events in this system happen within the same second
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final String tag;

    public Log(String tag) {
        this.tag = tag;
    }

    public void info(String format, Object... args) {
        print(System.out, format, args);
    }

    public void warn(String format, Object... args) {
        print(System.err, format, args);
    }

    private void print(PrintStream out, String format, Object[] args) {
        out.println(LocalTime.now().format(TIME) + "  [" + tag + "] " + String.format(format, args));
    }
}