package studio.kdb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.TimeZone;

public class Lm {
    private static final int majorVersion = 3;
    private static final int minorVersion = 35;
    public static Date buildDate;

    static {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
            f.setTimeZone(TimeZone.getTimeZone("GMT"));
            buildDate = f.parse("20190409");
        } catch (ParseException ignored) {
        }
    }

    public static int getMajorVersion() {
        return majorVersion;
    }

    public static int getMinorVersion() {
        return minorVersion;
    }

    public static String getVersionString() {
        NumberFormat numberFormatter = new DecimalFormat("##.00");
        double d = getMajorVersion() + getMinorVersion() / 100.0;
        return numberFormatter.format(d);
    }
}
