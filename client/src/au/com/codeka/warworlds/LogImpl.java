package au.com.codeka.warworlds;

import au.com.codeka.common.Log;

/**
 * This class works with the Log class to provide a concrete implementation we can use in Android.
 */
public class LogImpl {
    public static void setup() {
        Log.setImpl(new LogImplImpl());
    }

    private static final int[] LevelMap = {
        android.util.Log.ERROR,
        android.util.Log.WARN,
        android.util.Log.INFO,
        android.util.Log.DEBUG
    };

    private static class LogImplImpl implements Log.LogImpl {
        private static String fixTag(String tag) {
            if (tag.length() > 20) {
                return tag.substring(20);
            }
            return tag;
        }

        @Override
        public boolean isLoggable(String tag, int level) {
            return android.util.Log.isLoggable(fixTag(tag), LevelMap[level]);
        }

        @Override
        public void write(String tag, int level, String msg) {
            android.util.Log.println(LevelMap[level], fixTag(tag), msg);
        }
    }
}
