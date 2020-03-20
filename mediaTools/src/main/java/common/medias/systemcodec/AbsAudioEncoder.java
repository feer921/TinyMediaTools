package common.medias.systemcodec;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/8/10<br>
 * Time: 14:56<br>
 * <P>DESC:
 * 抽象的音频编码器
 * </p>
 * ******************(^_^)***********************
 */
public abstract class AbsAudioEncoder {
    protected final String TAG = getClass().getSimpleName();
    String rawAudioFilePath;

    /**
     * 编码 比特率 (码流)
     * def: 128000 单位：字节 B
     */

//    protected int encodeBitRate = 128000;
    protected int encodeBitRate = 0;


    /**
     * 音频通道数
     * def: 1
     */
    protected int audioChannelCount = 2;

    /**
     * 音频重采样率
     * def: 44100
     */
    protected int audioSampleRate = 44100;


    AbsAudioEncoder(String theRawAudioFilePath) {
        this.rawAudioFilePath = theRawAudioFilePath;
    }

    public static AACAudioEncoder getDefAudioEncoder(String theRawAudioDataFilePath) {
        return new AACAudioEncoder(theRawAudioDataFilePath);
    }

    public abstract void encodeToFile(String encodeResultFilePath);


    public <I extends AbsAudioEncoder> I setRawAudioFilePath(String rawAudioFilePath) {
        this.rawAudioFilePath = rawAudioFilePath;
        return (I) this;
    }

    public <I extends AbsAudioEncoder> I setEncodeBitRate(int encodeBitRate) {
        this.encodeBitRate = encodeBitRate;
        return (I) this;
    }

    public <I extends AbsAudioEncoder> I setAudioChannelCount(int audioChannelCount) {
        this.audioChannelCount = audioChannelCount;
        return (I) this;
    }

    public <I extends AbsAudioEncoder> I setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
        return (I) this;
    }

    public <I extends AbsAudioEncoder> I setNeedAddExtraBytes(boolean isNeedAddExtraBytes) {
        return (I) this;
    }

}
