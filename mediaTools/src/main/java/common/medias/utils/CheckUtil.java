package common.medias.utils;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2020/3/19<br>
 * Time: 15:04<br>
 * <P>DESC:
 * </p>
 * ******************(^_^)***********************
 */
public class CheckUtil {
    public static boolean isEmpty(CharSequence str) {
        if (str == null || str.toString() == null || str.toString().trim()
                .length() == 0 || str.length() == 0) {
            return true;
        }
        return false;
    }
}
