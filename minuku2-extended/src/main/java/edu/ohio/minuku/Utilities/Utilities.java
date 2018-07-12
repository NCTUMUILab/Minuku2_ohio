package edu.ohio.minuku.Utilities;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by Lawrence on 2018/7/1.
 */

public class Utilities {

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
