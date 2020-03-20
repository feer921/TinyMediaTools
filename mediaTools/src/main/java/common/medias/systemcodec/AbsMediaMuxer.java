package common.medias.systemcodec;

import java.util.ArrayList;
import java.util.List;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/8/12<br>
 * Time: 20:11<br>
 * <P>DESC:
 * 抽象的媒体合成者
 * </p>
 * ******************(^_^)***********************
 */
public abstract class AbsMediaMuxer {

    protected final String TAG = getClass().getSimpleName();

    /**
     * 合成最后所保存的文件路径
     */
    protected String theMuxResultFilePath;

//    /**
//     * 媒体合成者
//     */
//    protected MediaMuxer theMediaMuxer;


    /**
     * 要合并的文件参数
     */
    protected List<MuxFileParams> toMuxFilesParams;


    public <I extends AbsMediaMuxer> I addMuxFileParams(MuxFileParams muxFileParams) {
        if (muxFileParams != null) {
            if (toMuxFilesParams == null) {
                toMuxFilesParams = new ArrayList<>();
            }
            toMuxFilesParams.add(muxFileParams);
        }
        return (I) this;
    }

    public abstract boolean startMux();


    public <I extends AbsMediaMuxer> I clearMuxFileParams() {
        if (toMuxFilesParams != null) {
            toMuxFilesParams.clear();
        }
        return (I) this;
    }

    public <I extends AbsMediaMuxer> I setMuxResultFilePath(String muxResultFilePath) {
        this.theMuxResultFilePath = muxResultFilePath;
        return (I) this;
    }

    /**
     * 直接合并，音频文件和一个视频文件
     * @param audioFilePath 音频文件路径
     * @param videoFilePath 视频文件路径
     * @return true:mux ok;
     */
    public abstract boolean muxAudioAndVideo(String audioFilePath, String videoFilePath, String unSupportAudioMimeType);
    public interface IMuxCallback{
        void onMuxState(boolean isStartOrEnd);

    }

}
