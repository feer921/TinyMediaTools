package common.medias.systemcodec;

import java.io.IOException;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/8/10<br>
 * Time: 15:21<br>
 * <P>DESC:
 * 抽象的音频解码器
 * </p>
 * ******************(^_^)***********************
 */
public abstract class AbsAudioDecoder {
    protected String TAG = getClass().getSimpleName();
    protected IAudioDecodeCallback mCallback;
    /**
     * 包含了音频track的音、视频文件路径
     */
    String theEncodedWithAudioTrackFilePath;

    AbsAudioDecoder(String theEncodeAudioFilePath) {
        this.theEncodedWithAudioTrackFilePath = theEncodeAudioFilePath;
    }

    public static AbsAudioDecoder getDefAudioDecoder(String theFilePathWithAudioTrack) {
        return new AndroidAudioDecoder(theFilePathWithAudioTrack);
    }

    /**
     * 解码
     * @param outPutDecodedFilePath 解码完成，输出的文件路径
     * @return RawAudioInfo
     * @throws IOException
     */
    public abstract RawAudioInfo decodeToFile(String outPutDecodedFilePath)throws IOException;

    protected void onDecodeCallback(byte[] decodedBytes, double decodeProgress) {
        if (mCallback != null) {
            mCallback.onDecode(decodedBytes, decodeProgress);
        }
    }

    public void setAudioDecodeCallback(IAudioDecodeCallback l) {
        this.mCallback = l;
    }

    public interface IAudioDecodeCallback{
        void onDecode(byte[] decodedBytes, double progress);
    }

    public static class RawAudioInfo {
        public String tempRawFile;
        public int size;
        public long sampleRate;
        public int channel;
    }
}
