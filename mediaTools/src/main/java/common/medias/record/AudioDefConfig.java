package common.medias.record;

import android.media.AudioFormat;
import android.media.MediaRecorder;

/**
 * ******************(^_^)***********************<br>
 * User: 11776610771@qq.com<br>
 * Date: 2017/7/26<br>
 * Time: 17:20<br>
 * <P>DESC:
 * </p>
 * ******************(^_^)***********************
 */

public class AudioDefConfig {
    /**
     * 采样频率
     44100是目前的标准，但是某些设备仍然支持8000, 22050，16000，11025
     采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
     */
    public static final int DEF_SAMPLE_RATE = 16000;//
    /**
     * 音频源
     */
    public static final int DEF_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    /**
     * 通道配置
     */
//    public static final int DEF_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;//立体声
    public static final int DEF_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;//单声道

//    public static final int DEF_CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_MONO;

    public static final int DEF_CHANNEL_IN_STEREO = AudioFormat.CHANNEL_IN_STEREO;
    /**
     *
     */
    public static final int DEF_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
}
