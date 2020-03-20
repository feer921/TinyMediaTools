package common.medias.utils;



import java.util.Locale;

import common.medias.BuildConfig;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2018/8/18<br>
 * Time: 15:33<br>
 * <P>DESC:
 * 本模块中的日志调试
 * </p>
 * ******************(^_^)***********************
 */
public class L {
    private static boolean IS_DEBUG = BuildConfig.DEBUG;

    public static void enableLog(boolean toEnable) {
        IS_DEBUG = toEnable;
    }

    public static void w(String tag, String content) {
        if (IS_DEBUG) {
            android.util.Log.w(tag, content);
        }
    }

    public static void w(final String tag, Object... objs) {
        if (IS_DEBUG) {
            android.util.Log.w(tag, getInfo(objs));
        }
    }

    public static void i(String tag, String content) {
        if (IS_DEBUG) {
            android.util.Log.i(tag, content);
        }
    }

    public static void i(final String tag, Object... objs) {
        if (IS_DEBUG) {
            android.util.Log.i(tag, getInfo(objs));
        }
    }

    public static void d(String tag, String content) {
        if (IS_DEBUG) {
            android.util.Log.d(tag, content);
        }
    }

    public static void d(final String tag, Object... objs) {
        if (IS_DEBUG) {
            android.util.Log.d(tag, getInfo(objs));
        }
    }

    public static void e(String tag, String content) {
        if (IS_DEBUG) {
            android.util.Log.e(tag, content);
        }
    }

    public static void e(String tag, String content, Throwable e) {
        if (IS_DEBUG) {
            android.util.Log.e(tag, content, e);
        }
    }

    public static void e(final String tag, Object... objs) {
        if (IS_DEBUG) {
            android.util.Log.e(tag, getInfo(objs));
        }
    }

    private static String getInfo(Object... objs) {
        if (objs == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Object object : objs) {
            sb.append(object);
        }
        return sb.toString();
    }

    public static void sysOut(Object msg) {
        if (IS_DEBUG) {
            System.out.println(msg);
        }
    }

    public static void sysErr(Object msg) {
        if (IS_DEBUG) {
            System.err.println(msg);
        }
    }

    public static String formatStr(String srcStr, Object... args) {
        return String.format(Locale.getDefault(), srcStr, args);
    }
}
