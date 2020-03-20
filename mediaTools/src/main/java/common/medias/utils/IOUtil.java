package common.medias.utils;

import java.io.Closeable;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2020/3/19<br>
 * Time: 15:00<br>
 * <P>DESC:
 * </p>
 * ******************(^_^)***********************
 */
public class IOUtil {
    /**
     * 关闭输入输出流
     * @param io
     * @return
     */
    public static boolean safeCloseIO(Closeable io){
        if(io != null){
            try {
                io.close();
                return true;
            } catch (Exception e) {
                L.e("info",io +" --> close occur " + e);
            }
        }
        return false;
    }
}
