package common.medias.record;

import android.media.MediaRecorder;

import java.io.File;

import common.medias.utils.L;


/**
 * ******************(^_^)***********************<br>
 * User: 11776610771@qq.com<br>
 * Date: 2018/4/16<br>
 * Time: 15:09<br>
 * <P>DESC:
 * 使用{@link MediaRecorder}进行录制(音、视频)的类
 * 参考：
 * MediaRecorder recorder = new MediaRecorder();
 recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
 recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
 recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
 recorder.setOutputFile(PATH_NAME);
 recorder.prepare();
 recorder.start();   // Recording is now started
 ...
 recorder.stop();
 recorder.reset();   // You can reuse the object by going back to setAudioSource() step
 recorder.release(); // Now
 * </p>
 * ******************(^_^)***********************
 */
public class MrRecorder {
    protected final String TAG = getClass().getSimpleName();
    protected MediaRecorder mediaRecorder;

    protected volatile int recordState = STATE_IDLE;

    /**
     * 状态：空闲
     */
    public static final int STATE_IDLE = MediaRecorder.MEDIA_ERROR_SERVER_DIED * 10;
    /**
     * 状态：正在准备
     */
    public static final int STATE_PREPARING = STATE_IDLE + 1;
    /**
     * 状态：准备好了
     */
    public static final int STATE_PREPARED = STATE_PREPARING + 1;
    /**
     * 状态：正在录制
     */
    public static final int STATE_RECORDING = STATE_PREPARED + 1;
    /**
     * 状态：错误
     */
    public static final int STATE_ERROR = STATE_RECORDING + 1;
    /**
     * 状态：错误之准备时
     */
    public static final int STATE_ERROR_PREPARE = STATE_ERROR + 1;

    /**
     * 状态：错误之开始时
     */
    public static final int STATE_ERROR_START = STATE_ERROR_PREPARE + 1;

    /**
     * 状态：已停止
     */
    public static final int STATE_STOPPED = STATE_ERROR_START + 1;
    /**
     * 录制的媒体保存路径
     * eg.: /xxx/xx/record/
     */
    private String mediaSavePath;
    /**
     * 媒体录制时，音频的源
     * def:来自系统的麦克风
     */
    private int audioSource = MediaRecorder.AudioSource.MIC;
    /**
     * 音频的声道数量
     * 1:mono 单通道
     * 2:STEREO 双通道(立体声?)
     */
    private int audioNumChannels = 2;

    /**
     * 音频的采样率
     */
    private int audioSamplingRate = AudioDefConfig.DEF_SAMPLE_RATE;
    /**
     * 音频编码率(码流)
     */
    private int audioEncodingBitRate = 0;

    /**
     * 音频编码格式
     * def:AAC
     */
    private int audioEncoder = MediaRecorder.AudioEncoder.AAC;
    /**
     * 录制输出的文件格式：
     * def: mp4文件模式
     */
    private int outputFormat = MediaRecorder.OutputFormat.MPEG_4;

    /**
     * 录音时是否需要回音消除
     * def：false
     */
    private boolean isNeedAec;

    /**
     * 是否需要降噪
     */
    private boolean isNeedNoiseSuppressor;

    private AECerAndNoiseSuppressor aecAndNs;
    /**
     * 要设置录制时的音频源
     * @param audioSource 要使用的音频源 <P>
     *                    参考：
     *                    <ul>
     *                      <li>{@link MediaRecorder.AudioSource#DEFAULT}</li>
     *                      <li>{@link MediaRecorder.AudioSource#MIC}</li>
     *                      <li>{@link MediaRecorder.AudioSource#VOICE_UPLINK}</li>
     *                      <li>{@link MediaRecorder.AudioSource#VOICE_DOWNLINK}</li>
     *                      <li>{@link MediaRecorder.AudioSource#VOICE_CALL}</li>
     *                      <li>{@link MediaRecorder.AudioSource#CAMCORDER}</li>
     *                      <li>{@link MediaRecorder.AudioSource#VOICE_RECOGNITION}</li>
     *                      <li>{@link MediaRecorder.AudioSource#VOICE_COMMUNICATION}</li>
     *                      <li>{@link MediaRecorder.AudioSource#REMOTE_SUBMIX}</li>
     *                      <li>{@link MediaRecorder.AudioSource#UNPROCESSED}</li>
     *                    </ul>
     * </P>
     * @return self
     */
    public MrRecorder withAudioSource(int audioSource) {
        this.audioSource = audioSource;
        return this;
    }

    /**
     * 设置录制时的音源声道数量
     * @param audioNumChannels 1：单声道；2：双声道；other???
     * @return self
     */
    public MrRecorder withAudioNumChannels(int audioNumChannels) {
        if (audioNumChannels > 0) {
            this.audioNumChannels = audioNumChannels;
        }
        return this;
    }

    public MrRecorder withAudioSamplingRate(int audioSamplingRate) {
        if (audioSamplingRate > 0) {
            this.audioSamplingRate = audioSamplingRate;
        }
        return this;
    }

    public MrRecorder withAudioEncodingBitRate(int audioEncodingBitRate) {
        if (audioEncodingBitRate > 0) {
            this.audioEncodingBitRate = audioEncodingBitRate;
        }
        return this;
    }

    /**
     *
     * @param audioEncoder 音频编码器(格式) 参见：
     *                     <ul>
     *                          <li>{@link MediaRecorder.AudioEncoder#DEFAULT}</li>
     *                          <li>{@link MediaRecorder.AudioEncoder#AMR_NB}</li>
     *                          <li>{@link MediaRecorder.AudioEncoder#AMR_WB}</li>
     *                          <li>{@link MediaRecorder.AudioEncoder#AAC}</li>
     *                          <li>{@link MediaRecorder.AudioEncoder#HE_AAC}</li>
     *                          <li>{@link MediaRecorder.AudioEncoder#AAC_ELD}</li>
     *                          <li>{@link MediaRecorder.AudioEncoder#VORBIS}</li>
     *                     </ul>
     * @return
     */
    public MrRecorder withAudioEncoder(int audioEncoder) {
        this.audioEncoder = audioEncoder;
        return this;
    }

    /**
     * 录制媒体文件输出的格式
     * @param outputFormat 参考：
     *                     <ul>
     *                      <li>{@link MediaRecorder.OutputFormat#DEFAULT}</li>
     *                      <li>{@link MediaRecorder.OutputFormat#THREE_GPP}</li>
     *                      <li>{@link MediaRecorder.OutputFormat#MPEG_4}</li>
     *                      <li>{@link MediaRecorder.OutputFormat#RAW_AMR}</li>
     *                      <li>{@link MediaRecorder.OutputFormat#AMR_NB}</li>
     *                      <li>{@link MediaRecorder.OutputFormat#AMR_WB}</li>
     *                      <li>{@link MediaRecorder.OutputFormat#AAC_ADTS}</li>
     *                      <li>{@link MediaRecorder.OutputFormat#WEBM}</li>
     *                     </ul>
     * @return
     */
    public MrRecorder witOutputFormat(int outputFormat) {
        this.outputFormat = outputFormat;
        return this;
    }

    public MrRecorder withOutputFilePath(String outputFilePath) {
        this.mediaSavePath = outputFilePath;
        return this;
    }
    /**
     * MediaRecorder recorder = new MediaRecorder();
     recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
     recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
     recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
     recorder.setOutputFile(PATH_NAME);
     recorder.prepare();
     * @return true:准备成功；false:准备失败.
     */
    public boolean prepare(boolean isNeedPrepare) {
        recordState = STATE_PREPARING;
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            //add listeners for self...
            RecordListener recordListener = new RecordListener();
            mediaRecorder.setOnErrorListener(recordListener);
            mediaRecorder.setOnInfoListener(recordListener);
        }
        //以下，reset()后是否需要再设置那些个参数？？？
        else{
            mediaRecorder.reset();
        }
        try {
            mediaRecorder.setAudioSource(audioSource);//Call before setOutputFormat()
            mediaRecorder.setOutputFormat(outputFormat);//after setAudioSource()/setVideoSource() but before prepare()
            mediaRecorder.setAudioChannels(audioNumChannels);//Call this method before prepare().
            mediaRecorder.setAudioEncoder(audioEncoder);//要在设置setOutputFormat()后设置
            if (audioSamplingRate > 0) {
                mediaRecorder.setAudioSamplingRate(audioSamplingRate);//Call before prepare().
            }
            if (audioEncodingBitRate > 0) {
                mediaRecorder.setAudioEncodingBitRate(audioEncodingBitRate);//Call before prepare().
            }
            File theFile = new File(mediaSavePath);
            File parentFile = theFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            theFile.delete();
            theFile.createNewFile();
            mediaRecorder.setOutputFile(mediaSavePath);//after setOutputFormat() but before prepare().
            if (isNeedPrepare) {
                mediaRecorder.prepare();
                recordState = STATE_PREPARED;
            }
        }
        catch (Exception e) {
            L.e(TAG, "-->prepare() occur : " + e);
            recordState = STATE_ERROR_PREPARE;
            onMrError(mediaRecorder,recordState,0);
            return false;
        }
        return true;
    }

    public void reset() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
        }
    }
    public boolean prepare() {
        return prepare(true);
    }

    public boolean start() {
        if (mediaRecorder != null) {
            try {
//                if (recordState == STATE_STOPPED) {
//                    mediaRecorder.reset();
//                }
                mediaRecorder.start();
                recordState = STATE_RECORDING;
                return true;
            } catch (Exception e) {
                L.e(TAG, "-->start() occur: ");
                recordState = STATE_ERROR_START;
                onMrError(mediaRecorder,recordState,0);
            }
        }
        return false;
    }

    public void stop() {
//        recordState = STATE_IDLE;//?????????????????? 还要根据其他的状态来判断？？？
        if (recordState == STATE_RECORDING) {
            if (mediaRecorder != null) {
                recordState = STATE_STOPPED;
                try {
                    mediaRecorder.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void release() {
        recordState = STATE_IDLE;
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaRecorder = null;
        }
    }

    /**
     * 是否正在录制
     * @return
     */
    public boolean isRecording() {
        return recordState == STATE_RECORDING;
    }
    private class RecordListener implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener{

        /**
         * Called when an error occurs while recording.
         *
         * @param mr    the MediaRecorder that encountered the error
         * @param what  the type of error that has occurred:
         *              <ul>
         *              <li>{@link MediaRecorder#MEDIA_RECORDER_ERROR_UNKNOWN}
         *              <li>{@link MediaRecorder#MEDIA_ERROR_SERVER_DIED}
         *              </ul>
         * @param extra an extra code, specific to the error type
         */
        @Override
        public void onError(MediaRecorder mr, int what, int extra) {
            onMrError(mr,what,extra);
        }

        /**
         * Called when an error occurs while recording.
         *
         * @param mr    the MediaRecorder that encountered the error
         * @param what  the type of error that has occurred:
         *              <ul>
         *              <li>{@link MediaRecorder#MEDIA_RECORDER_INFO_UNKNOWN}
         *              <li>{@link MediaRecorder#MEDIA_RECORDER_INFO_MAX_DURATION_REACHED}
         *              <li>{@link MediaRecorder#MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED}
         *              </ul>
         * @param extra an extra code, specific to the error type
         */
        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
                onMrInfo(mr,what,extra);
        }
    }
    private MediaRecorder.OnErrorListener outSideErrorListener;
    public MrRecorder withOnErrorListener(MediaRecorder.OnErrorListener listener) {
        this.outSideErrorListener = listener;
        return this;
    }
    private MediaRecorder.OnInfoListener outSideInfoListener;
    public MrRecorder withOnInfoListener(MediaRecorder.OnInfoListener listener) {
        this.outSideInfoListener = listener;
        return this;
    }


    protected void onMrError(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED) {
            //媒体服务挂了/

        }
        if (outSideErrorListener != null) {
            outSideErrorListener.onError(mr,what,extra);
        }
    }

    protected void onMrInfo(MediaRecorder mr, int what, int extra) {
        if (outSideInfoListener != null) {
            outSideInfoListener.onInfo(mr,what,extra);
        }
    }

}
