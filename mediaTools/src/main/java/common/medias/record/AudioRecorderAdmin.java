package common.medias.record;

import android.annotation.SuppressLint;
import android.media.AudioRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import common.medias.ffmpeg.MediaEditor;
import common.medias.systemcodec.AbsAudioEncoder;
import common.medias.systemcodec.PcmToWav;
import common.medias.systemcodec.RecordStreamListener;
import common.medias.utils.CheckUtil;
import common.medias.utils.FileUtils;
import common.medias.utils.IOUtil;
import common.medias.utils.L;
import common.medias.utils.ThreadPoolUitl;

import static common.medias.record.AudioDefConfig.DEF_AUDIO_FORMAT;
import static common.medias.record.AudioDefConfig.DEF_AUDIO_SOURCE;
import static common.medias.record.AudioDefConfig.DEF_CHANNEL_CONFIG;
import static common.medias.record.AudioDefConfig.DEF_SAMPLE_RATE;


/**
 * ******************(^_^)***********************<br>
 * User: 11776610771@qq.com<br>
 * Date: 2017/8/8<br>
 * Time: 18:18<br>
 * <P>DESC:
 * AudioRecord录音模块
 * </p>
 * ******************(^_^)***********************
 */
public class AudioRecorderAdmin {
    private static final String TAG = "AudioRecorderAdmin";

    private static AudioRecorderAdmin audioRecorder;

//    /**
//     * 采用频率
//     44100是目前的标准，但是某些设备仍然支持8000, 22050，16000，11025
//     采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
//     */
//    private static final int DEF_SAMPLE_RATE = 16000;//
    /**
     * 音频源
     */
//    private static final int DEF_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    /**
     * 通道配置
     */
//    private static final int DEF_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;//立体声
    /**
     *
     */
//    private static final int DEF_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;//每次采样的精度


    // 缓冲区字节大小
    private int bufferSizeInBytes = 0;

    /**
     * 准备去读取MIC所采集的音频数据的字节大小
     */
    private int readyReadBufferSize = 1024;

    private byte[] toReadBuffer;
    //录音对象
    private AudioRecord audioRecord;

    //录音状态
    private volatile int status = STATUS_NO_READY;

    //文件名
    private String fileName;

    /**
     * 保存当前录音文件的路径
     */
    private String theSaveRecordAudioFilePath;

    private String theEncodeToWavAudioFilePath;
    /**
     * 临时存储PCM文件的路径
     * 该文件为使用 AudioRecord 录制的原始 PCM格式音频
     */
    private String theTempSaveRecordPcmFilePath;
    //录音文件
    private List<String> filesName = new ArrayList<>();

    //未开始
    public static final int STATUS_NO_READY = 0;
    //预备
    public static final int STATUS_READY = 1;
    //录音
    public static final int STATUS_STARTED = 2;
    //暂停
    public static final int STATUS_PAUSE = 3;
    //停止
    public static final int STATUS_STOP = 4;

    /**
     * 启动失败
     */
    public static final int STATUS_START_FAILURE = 5;

    private AECerAndNoiseSuppressor mAECAndNS;

    private boolean isNeedEncodePcmToWavFile = false;

    private boolean isNeedEncodeAACFile = false;

    /**
     * 是否需要丢弃早前的录音数据
     * def: true;
     */
    private boolean isNeedDiscardForwardAudioBytes = true;
    private AbsAudioEncoder aacAudioEncoder;
    private AudioRecorderAdmin() {
    }

    //单例模式
    public static AudioRecorderAdmin getInstance() {
        if (audioRecorder == null) {
            audioRecorder = new AudioRecorderAdmin();
        }
        return audioRecorder;

    }

    /**
     * 提供需要录制音频到文件时的文件名字
     * @param fileName 文件名
     * @return self
     */
    public AudioRecorderAdmin withRecordFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public AudioRecorderAdmin withRecordDataListener(RecordStreamListener listener) {
        this.recordStreamListener = listener;
        return this;
    }

    public AudioRecorderAdmin withSaveRecordAudioFilePath(String toSaveRecordAudioFilePath, boolean isAlsoAsTheTempPcmFilePath) {
        this.theSaveRecordAudioFilePath = toSaveRecordAudioFilePath;
        if (isAlsoAsTheTempPcmFilePath) {
            this.theTempSaveRecordPcmFilePath = toSaveRecordAudioFilePath;
        }
        else {
            this.theTempSaveRecordPcmFilePath = toSaveRecordAudioFilePath + ".pcm";
        }
        return this;
    }

    public AudioRecorderAdmin withEncodePcmToWavFile(boolean isNeedEncodePcmToWavFile) {
        this.isNeedEncodePcmToWavFile = isNeedEncodePcmToWavFile;
        return this;
    }

    public AudioRecorderAdmin withEncodeAACFile(boolean isNeedEncodeAACFile) {
        this.isNeedEncodeAACFile = isNeedEncodeAACFile;
        return this;
    }
    public AudioRecorderAdmin withAECAndNoiseSuppress(boolean isNeedAec, boolean isNeedNoiseSuppress) {
        if (this.mAECAndNS == null) {
            mAECAndNS = new AECerAndNoiseSuppressor();
        }
        mAECAndNS.needAec(isNeedAec)
                .needNoiseSuppress(isNeedNoiseSuppress)
                ;
        return this;
    }

    private volatile RecordStreamListener recordStreamListener;

    public AudioRecorderAdmin withNeedDiscardForwardAudioDatas(boolean isNeedDiscardForwardAudioBytes) {
        this.isNeedDiscardForwardAudioBytes = isNeedDiscardForwardAudioBytes;
        return this;
    }

    private int audioSource = DEF_AUDIO_SOURCE;

    private int audioSampleRateInHz = DEF_SAMPLE_RATE;

    private int channelConfig = DEF_CHANNEL_CONFIG;

    private int audioFormat = DEF_AUDIO_FORMAT;

    private int audioChannelsCount = 1;
    /**
     * 根据配置信息，创建AudioRecord
     * @param audioSource 音频源 @see:{@linkplain # MediaRecorder.AudioSource.MIC}
     * @param sampleRateInHz 采样率
     * @param channelConfig  通道：单通道、双通道等
     * @param audioFormat 采集音频数据格式
     * @return self
     */
    public AudioRecorderAdmin buildRecorder(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {
        //获取缓冲区字节大小 ,该大小还于当前Android系统自身有关，不同的设备值大小不一样
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        this.audioSource = audioSource;
        this.audioSampleRateInHz = sampleRateInHz;
        this.channelConfig = channelConfig;
        this.audioFormat = audioFormat;

        if (channelConfig == DEF_CHANNEL_CONFIG) {
            audioChannelsCount = 1;
        }
        else {
            audioChannelsCount = 2;
        }
//        mPrimeReadBufferSize = bufferSizeInBytes * 2;
        L.e(TAG,"---> buildRecorder()  bufferSizeInBytes = " + bufferSizeInBytes);
        readyReadBufferSize = bufferSizeInBytes / 2;//https://bL.csdn.net/jbgtwang/article/details/20642351
        try {
            audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
            status = STATUS_READY;
        }
        catch (Exception e) {
            L.e(TAG,"-->buildRecorder() occur " + e );
            status = STATUS_NO_READY;
            audioRecord = null;
        }
        return this;
    }

    private AudioRecorderAdmin initAudioRecord() {
        try {
            audioRecord = new AudioRecord(audioSource, audioSampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
            status = STATUS_READY;
        }
        catch (Exception e) {
            L.e(TAG,"-->buildRecorder() occur " + e );
            status = STATUS_NO_READY;
            audioRecord = null;
        }
        return this;
    }

    public int getCurMicMinBufferSize() {
        return bufferSizeInBytes;
    }

    /**
     * 提供动态修改 读取缓冲区的size
     * @param toReadBufferSize 读取缓冲区字节大小
     */
    public void configReadBufferSize(int toReadBufferSize) {
        if (toReadBufferSize > 0) {
//            if (toReadBufferSize > bufferSizeInBytes) {
//                toReadBufferSize = bufferSizeInBytes;
//            }
            readyReadBufferSize = toReadBufferSize;
            toReadBuffer = new byte[readyReadBufferSize];
        }
    }
    /**
     * 创建默认的音频录音器
     * @return self
     */
    public AudioRecorderAdmin buildDefRecorder() {
        return buildRecorder(DEF_AUDIO_SOURCE,DEF_SAMPLE_RATE,DEF_CHANNEL_CONFIG,DEF_AUDIO_FORMAT);
    }

    /**
     * 只要调用了就会赋值
     * 正确调用应该在{{@link #withSaveRecordAudioFilePath(String, boolean)}}后
     * @param toEncodeToWavAudioFilePath 将pcm保存的音频文件再编码与WAV格式文件的路径
     * @return self
     */
    public AudioRecorderAdmin withEncodeToWavAudioFilePath(String toEncodeToWavAudioFilePath) {
        this.theEncodeToWavAudioFilePath = toEncodeToWavAudioFilePath;
        if (toEncodeToWavAudioFilePath == null) {
            this.theEncodeToWavAudioFilePath = theSaveRecordAudioFilePath + ".wav";
        }
        return this;
    }
    private WriteDataToFileTask writeDataToFileTask = new WriteDataToFileTask();

    private class WriteDataToFileTask implements Runnable {
        @SuppressLint("NewApi")
        @Override
        public void run() {
            L.w(TAG, "---> 去读取录音数据...");
            //先清空一下，原来的缓存？？？
            if (audioRecord != null) {
//                int bufferSize = audioRecord.getBufferSizeInFrames();//960
//                L.e(TAG, "--> bufferSize = " + bufferSize);
                if (isNeedDiscardForwardAudioBytes) {
                    int firstReadBytes = audioRecord.read(new byte[readyReadBufferSize], 0, readyReadBufferSize);
                    int firstReadBytes2 = audioRecord.read(new byte[readyReadBufferSize], 0, readyReadBufferSize);
                    L.i(TAG, "--> before real read ,first read : " + firstReadBytes);
                }
            }
            writeDataToFile();
            boolean willDeleteThePcmFile = false;
            //PCM --> WAV
            if (isNeedEncodePcmToWavFile && !CheckUtil.isEmpty(theSaveRecordAudioFilePath)) {
                willDeleteThePcmFile = PcmToWav.pcmToWave(theTempSaveRecordPcmFilePath, audioChannelsCount,audioSampleRateInHz, theSaveRecordAudioFilePath);
            }

            //PCM --> AAC
            if (isNeedEncodeAACFile && !CheckUtil.isEmpty(theTempSaveRecordPcmFilePath)) {
                boolean convertAacOk = false;
                convertAacOk = MediaEditor.pcmConvertTo(
                    theTempSaveRecordPcmFilePath,
                        audioSampleRateInHz,
                        audioChannelsCount,
                        "aac",
                        theSaveRecordAudioFilePath,
                        null
                );
                if (!convertAacOk) {
                    if (aacAudioEncoder == null) {
                        aacAudioEncoder = AbsAudioEncoder.getDefAudioEncoder(theTempSaveRecordPcmFilePath);
                        aacAudioEncoder.setAudioSampleRate(audioSampleRateInHz)
                                .setAudioChannelCount(1)
                                .setEncodeBitRate(16000);
                    }
                }
                aacAudioEncoder.encodeToFile(theSaveRecordAudioFilePath);
                willDeleteThePcmFile = true;
            }
            //回调 通知 录音停止
            if (recordStreamListener != null) {
                recordStreamListener.onRecordData(null, RecordStreamListener.STATE_STOP_RECORD, 0);//用来表示结束了录制
            }

//            //一般pcm临时存储文件没有意义
//            if (willDeleteThePcmFile) {
//                new File(theTempSaveRecordPcmFilePath).delete();
//            }
            //上面不编码成WAV文件情况下还能再 编码成wav文件
            if (!isNeedEncodePcmToWavFile && !CheckUtil.isEmpty(theEncodeToWavAudioFilePath)) {
                PcmToWav.pcmToWave(theTempSaveRecordPcmFilePath, audioChannelsCount,audioSampleRateInHz, theEncodeToWavAudioFilePath);
            }
            //一般pcm临时存储文件没有意义
            if (willDeleteThePcmFile) {
                new File(theTempSaveRecordPcmFilePath).delete();
            }
            L.w(TAG, "---> 结束读取录音数据...willDeleteThePcmFile = " + willDeleteThePcmFile);
            if (recordStreamListener != null) {
                recordStreamListener.onRecordData(null,RecordStreamListener.STATE_WORK_OVER,0);
            }
        }
    }

    /**
     * 启动录音
     */
    public int startRecord() {
        if (audioRecord == null || status == STATUS_NO_READY) {
            initAudioRecord();
        }

        if (status != STATUS_NO_READY && audioRecord != null) {
            //AudioRecord 已经准备好
            try {
                audioRecord.startRecording();
                status = STATUS_STARTED;
                //added by fee 2019-08-21: 开启AEC NS
                if (mAECAndNS != null) {
                    mAECAndNS.effectTheAudioSessionId(audioRecord.getAudioSessionId())
                    .startTheHardWork()
                    ;
                }
                ThreadPoolUitl.getMe().excute(writeDataToFileTask);
            } catch (Exception e) {
                L.e(TAG, "-->startRecord() occur " + e);
                status = STATUS_START_FAILURE;
            }
        }
        L.e(TAG, "===startRecord===" + audioRecord.getState() +" / status=" + status);
        return status;
    }

    /**
     * 暂停录音
     */
    public void pauseRecord() {
        L.d(TAG, "===pauseRecord===");
        if (status == STATUS_STARTED) {
            if (audioRecord != null) {
                audioRecord.stop();
                status = STATUS_PAUSE;
            }
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        stopRecord(false);
    }

    public void stopRecord(boolean isNeedRelease) {
        L.d(TAG, "===stopRecord===  isNeedRelease = " + isNeedRelease);
        if (status == STATUS_STARTED) {
            if (audioRecord != null) {
                audioRecord.stop();
                status = STATUS_STOP;
                if (isNeedRelease) {
                    release();
                }
            }
        }
        if (mAECAndNS != null) {
            mAECAndNS.stopTheHardWork();
        }
    }
    /**
     * 释放资源
     */
    public void release() {
        L.d("AudioRecorderAdmin","===release===");
        if (audioRecord != null) {
            audioRecord.release();
        }
        status = STATUS_NO_READY;
        //假如有暂停录音
        try {
            if (filesName.size() > 0) {
                List<String> filePaths = new ArrayList<>();
                for (String fileName : filesName) {
                    filePaths.add(FileUtils.getPcmFileAbsolutePath(fileName));
                }
                //清除
                filesName.clear();
                //将多个pcm文件转化为wav文件
                mergePCMFilesToWAVFile(filePaths);

            }
            else {
                //这里由于只要录音过filesName.size都会大于0,没录音时fileName为null
                //会报空指针 NullPointerException
                // 将单个pcm文件转化为wav文件
                //L.d("AudioRecorderAdmin", "=====makePCMFileToWAVFile======");
                //makePCMFileToWAVFile();
            }
        } catch (IllegalStateException e) {
        }

    }

    /**
     * 取消录音
     */
    private void cancel() {
        filesName.clear();
        if (audioRecord != null) {
            audioRecord.release();
        }
        status = STATUS_NO_READY;
    }


    /**
     * 将音频信息写入文件
     *
     */
    @SuppressLint("NewApi")
    private void writeDataToFile() {
//        int bufferSize = 4 * 1024;
        FileOutputStream fos = null;
        File toSaveAudioFile = null;
        if (null != fileName) {
            String curFileName = fileName;
            if (status == STATUS_PAUSE) {
                //假如是暂停录音 将文件名后面加个数字,防止重名文件内容被覆盖
                curFileName += filesName.size();
            }
            filesName.add(curFileName);
            toSaveAudioFile = new File(FileUtils.getPcmFileAbsolutePath(curFileName));

        }
        else {
            if (!CheckUtil.isEmpty(theTempSaveRecordPcmFilePath)) {//单文件模式
                toSaveAudioFile = new File(theTempSaveRecordPcmFilePath);
            }
        }
        if (toSaveAudioFile != null) {
            if (toSaveAudioFile.exists()) {
                toSaveAudioFile.delete();
            }
            try {
                fos = new FileOutputStream(toSaveAudioFile);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        status = STATUS_STARTED;
        //added by fee 2018-06-09:
        if (recordStreamListener != null) {
            recordStreamListener.onRecordData(null, RecordStreamListener.STATE_BEGIN_RECORD, 0);//用来表示开始了录制
        }
//        int readSize = readyReadBufferSize/2;
//        L.e(TAG, "--->    **************************  readSize = " + readSize);
        //已经读取的字节数
        byte[] toReadBuffer = new byte[readyReadBufferSize];//这样每次都new出来，好像可以避免读取出的字节数为-2
//        if (toReadBuffer == null) {
//            toReadBuffer = new byte[readyReadBufferSize];
//        }
        int hasReadSize;
        while (status == STATUS_STARTED) {
            long startRecord = System.currentTimeMillis();
//            if (Util.isCompateApi(23)) {
//                isUseNonBlocking = true;
//                //android 6.0以上可以非阻塞的读取 (没什么用？？？)
//                readedDataSize = audioRecord.read(toReadBuffer, 0, readyReadBufferSize, AudioRecord.READ_NON_BLOCKING);
//            }
//            else{
//                readedDataSize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
//            }
            int offsetInBytes = 0;
            hasReadSize = audioRecord.read(toReadBuffer, 0, readyReadBufferSize);//阻塞
//            hasReadSize = audioRecord.read(toReadBuffer, 0, readSize);
            L.sysOut("writeDataTOFile() ---> hasReadSize= " + hasReadSize);
            if (AudioRecord.ERROR_INVALID_OPERATION != hasReadSize) {
                if (recordStreamListener != null) {
                    recordStreamListener.onRecordData(toReadBuffer, 0, 0);
                }
                long aSendCircleTime = System.currentTimeMillis() - startRecord;
//                System.out.println("info--> a read mic record time interval : " + aSendCircleTime);

                //考虑是否录制下来
                if (/*null != fileName &&*/ fos != null) {
                    try {
                        fos.write(toReadBuffer);
                        fos.flush();
                    } catch (Exception e) {
                        L.e(TAG, "--> save the record datas occur: " + e);
                    }
                }
                else {
                    //added by fee 2019-08-02: 添加AAC编码
//                    aacEncoder.encode(toReadBuffer);
                }
            }
            else{
                String desc = "有无效数据";
                if (hasReadSize == AudioRecord.ERROR_BAD_VALUE) {
                    desc = "Bad --value ";
                }
                L.e(TAG, desc);
            }
        }
        IOUtil.safeCloseIO(fos);
        toReadBuffer = null;
//        if (recordStreamListener != null) {
//            recordStreamListener.onRecordData(null, RecordStreamListener.STATE_STOP_RECORD, 0);//用来表示结束了录制
//        }
    }

    /**
     * 将pcm合并成wav
     *
     * @param filePaths
     */
    private void mergePCMFilesToWAVFile(final List<String> filePaths) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (PcmToWav.mergePCMFilesToWAVFile(filePaths, FileUtils.getWavFileAbsolutePath(fileName))) {
                    //操作成功
                } else {
                    //操作失败
                    L.e("AudioRecorderAdmin", "mergePCMFilesToWAVFile fail");
                }
            }
        }).start();
    }

    /**
     * 将单个pcm文件转化为wav文件
     */
    private void makePCMFileToWAVFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (PcmToWav.makePCMFileToWAVFile(FileUtils.getPcmFileAbsolutePath(fileName), FileUtils.getWavFileAbsolutePath(fileName), true)) {
                    //操作成功
                } else {
                    //操作失败
                    L.e("AudioRecorderAdmin", "makePCMFileToWAVFile fail");
                }
            }
        }).start();
    }

    /**
     * 获取录音对象的状态
     *
     * @return
     */
    public int getStatus() {
        return status;
    }

    /**
     * 获取本次录音文件的个数
     *
     * @return
     */
    public int getPcmFilesCount() {
        return filesName.size();
    }

}
