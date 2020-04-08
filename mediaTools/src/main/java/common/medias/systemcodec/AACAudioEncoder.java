package common.medias.systemcodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import common.medias.utils.L;


/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/8/10<br>
 * Time: 15:00<br>
 * <P>DESC:
 * 使用AAC编码的音频编码者
 * </p>
 * ******************(^_^)***********************
 */
public class AACAudioEncoder extends AbsAudioEncoder {

    private final static String AUDIO_MIME = "audio/mp4a-latm";

    /**
     * 码率 = 采样频率 * 采样位数 * 声道个数
     */

    /**
     * 每次采样的 字节数 = 采样频率 * 16(采样位宽) * channelCount(声道数) /8(变成字节)
     */
    private final static long AUDIOBYTESPERSAMPLE_DEF = 44100 * 16 / 8;//除以8表示变成字节单位

    /**
     * 每次采样字节数
     */
    private long audioBytesPerSample = AUDIOBYTESPERSAMPLE_DEF ;

    private MediaCodec mediaCodec;
    private MediaCodec.BufferInfo encodeBufferInfo;
    private ByteBuffer[] encodeInputBuffers;
    private ByteBuffer[] encodeOutputBuffers;
    private byte[] chunkAudio = new byte[0];

    private BufferedOutputStream outputStream;

    /**
     * 编码结果保存的文件路径
     */
    private String encodeResultFilePath;

    /**s
     * 拼接AAC头信息时的 表明当前采样率的标志位值
     * def: 4 == 44.1K
     */
    private int theFreqIndex = 4;

    private boolean isNeedAddAdtsHeader = true;
    private int inputBufferSize = 2048;

    AACAudioEncoder(String rawAudioFile) {
        super(rawAudioFile);
    }

    /**
     *        0: 96000 Hz
     *      * 1: 88200 Hz
     *      * 2: 64000 Hz
     *      * 3: 48000 Hz
     *      * 4: 44100 Hz
     *      * 5: 32000 Hz
     *      * 6: 24000 Hz
     *      * 7: 22050 Hz
     *      * 8: 16000 Hz
     *      * 9: 12000 Hz
     *      * 10: 11025 Hz
     *      * 11: 8000 Hz
     *      * 12: 7350 Hz
     *      * 13: Reserved
     *      * 14: Reserved
     *      * 15: frequency is written explictly
     * @param theSampleRate
     * @return
     */
    private static int mapTheFreqIdxWhenAACEncode(int theSampleRate) {
        int freqIdx = 15;
        switch (theSampleRate) {
            case 96000:
                return 0;

            case 88200:
                return 1;

            case 64000:
                return 2;

            case 48000:
                return 3;

            case 44100:
                return 4;

            case 32000:
                return 5;

            case 24000:
                return 6;

            case 22050:
                return 7;

            case 16000:
                return 8;

            case 12000:
                return 9;

            case 11025:
                return 10;

            case 8000:
                return 11;

            case 7350:
                return 12;
        }
        return freqIdx;
    }

    @Override
    public void encodeToFile(String encodeResultFilePath) {
        FileInputStream fisRawAudio = null;
        FileOutputStream fosAccAudio = null;
        theFreqIndex = mapTheFreqIdxWhenAACEncode(this.audioSampleRate);
//        audioBytesPerSample = audioSampleRate * 16 / 8;
        if (encodeBitRate == 0) {
            encodeBitRate = audioSampleRate * 16 * audioChannelCount/ 8;
        }

        audioBytesPerSample = encodeBitRate;

        L.d(TAG, "--> encodeToFile() encodeBitRate = " + encodeBitRate + "  audioBytesPerSample = " + audioBytesPerSample
                + " audioSampleRate = " + audioSampleRate
                + "  audioChannelCount = " + audioChannelCount
        );
        try {
            fisRawAudio = new FileInputStream(rawAudioFilePath);
            fosAccAudio = new FileOutputStream(encodeResultFilePath);

            final MediaCodec audioEncoder = createACCAudioEncoder();
            audioEncoder.start();

            ByteBuffer[] audioInputBuffers = audioEncoder.getInputBuffers();
            ByteBuffer[] audioOutputBuffers = audioEncoder.getOutputBuffers();
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            long audioTimeUs = 0;

            MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();

            boolean readRawAudioEOS = false;
            byte[] rawInputBytes = new byte[4096];
            int readRawAudioCount = 0;
            int rawAudioSize = 0;
            long lastAudioPresentationTimeUs = 0;

            int inputBufIndex, outputBufIndex;
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    inputBufIndex = audioEncoder.dequeueInputBuffer(10000);//10毫秒
                    if (inputBufIndex >= 0) {
                        ByteBuffer inputBuffer = audioInputBuffers[inputBufIndex];
                        inputBuffer.clear();

                        int bufferSize = inputBuffer.remaining();
                        if (bufferSize != rawInputBytes.length) {
                            rawInputBytes = new byte[bufferSize];
                        }

                        if (!readRawAudioEOS) {
                            readRawAudioCount = fisRawAudio.read(rawInputBytes);
                            if (readRawAudioCount == -1) {
                                readRawAudioEOS = true;
                            }
                        }

                        if (readRawAudioEOS) {
                            audioEncoder.queueInputBuffer(inputBufIndex, 0, 0,0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            sawInputEOS = true;
                        } else {
                            inputBuffer.put(rawInputBytes, 0, readRawAudioCount);
                            rawAudioSize += readRawAudioCount;
                            audioEncoder.queueInputBuffer(inputBufIndex, 0,readRawAudioCount, audioTimeUs, 0);
                            audioTimeUs = (long) (1000000 * (rawAudioSize / 2.0) / audioBytesPerSample);
                        }
                    }
                }

                outputBufIndex = audioEncoder.dequeueOutputBuffer(outBufferInfo, 10000);
                if (outputBufIndex >= 0) {

                    // Simply ignore codec config buffers.
                    if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        audioEncoder.releaseOutputBuffer(outputBufIndex, false);
                        continue;
                    }

                    if (outBufferInfo.size != 0) {
                        ByteBuffer outBuffer = audioOutputBuffers[outputBufIndex];
                        outBuffer.position(outBufferInfo.offset);
                        outBuffer.limit(outBufferInfo.offset+ outBufferInfo.size);
                        if (lastAudioPresentationTimeUs < outBufferInfo.presentationTimeUs) {
                            lastAudioPresentationTimeUs = outBufferInfo.presentationTimeUs;
                            int adtsByteLen = isNeedAddAdtsHeader ? 7 : 0;
                            int outBufSize = outBufferInfo.size;
                            int outPacketSize = outBufSize + adtsByteLen;

                            outBuffer.position(outBufferInfo.offset);
                            outBuffer.limit(outBufferInfo.offset + outBufSize);

                            byte[] outData = new byte[outBufSize + adtsByteLen];
                            if (isNeedAddAdtsHeader) {
                                addADTStoPacket(outData, outPacketSize);
                            }
                            outBuffer.get(outData, adtsByteLen, outBufSize);

                            fosAccAudio.write(outData, 0, outData.length);
                        }
                    }

                    audioEncoder.releaseOutputBuffer(outputBufIndex, false);

                    if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    }
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    audioOutputBuffers = audioEncoder.getOutputBuffers();
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat audioFormat = audioEncoder.getOutputFormat();
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            try {
                if (fisRawAudio != null)
                    fisRawAudio.close();
                if (fosAccAudio != null)
                    fosAccAudio.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private MediaCodec createACCAudioEncoder() throws IOException {
        MediaCodec codec = MediaCodec.createEncoderByType(AUDIO_MIME);
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, AUDIO_MIME);
        format.setInteger(MediaFormat.KEY_BIT_RATE, encodeBitRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, audioChannelCount);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, audioSampleRate);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, inputBufferSize);//作用于inputBuffer的大小
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        return codec;
    }

    /**
     * 初始化AAC编码器
     */
    private MediaCodec buildAACMediaEncode() {
        MediaCodec mediaCodec = null;
        try {
            //参数对应-> mime type、采样率、声道数
            MediaFormat encodeFormat = new MediaFormat();
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, encodeBitRate);//比特率
            encodeFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, audioChannelCount);
//            encodeFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, inputBufferSize);//作用于inputBuffer的大小
            mediaCodec = MediaCodec.createEncoderByType(AUDIO_MIME);
            mediaCodec.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        if (mediaEncode == null) {
//            Log.e(TAG, "create mediaEncode failed");
//            return;
//        }
//        mediaEncode.start();
//        encodeInputBuffers = mediaEncode.getInputBuffers();
//        encodeOutputBuffers = mediaEncode.getOutputBuffers();
//        encodeBufferInfo = new MediaCodec.BufferInfo();

        return mediaCodec;
    }

    /**
     * Add ADTS header at the beginning of each and every AAC packet. This is
     * needed as MediaCodec encoder generates a packet of raw AAC data.
     *
     * Note the packetLen must count in the ADTS header itself.
     * freqIdx取值范围
     * 0: 96000 Hz
     * 1: 88200 Hz
     * 2: 64000 Hz
     * 3: 48000 Hz
     * 4: 44100 Hz
     * 5: 32000 Hz
     * 6: 24000 Hz
     * 7: 22050 Hz
     * 8: 16000 Hz
     * 9: 12000 Hz
     * 10: 11025 Hz
     * 11: 8000 Hz
     * 12: 7350 Hz
     * 13: Reserved
     * 14: Reserved
     * 15: frequency is written explictly
     **/
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = theFreqIndex; // 44.1KHz
        int chanCfg = audioChannelCount; // Channel Configurations

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    /**
     * 停止编码
     */
    public void stopEncode() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
        mediaCodec = null;
        if (outputStream != null) {
            try {
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        outputStream = null;
    }

    /**
     * 时时将PCM的原生 音频数据编码进要保存的文件中
     * @param audioPcmDatas pcm数据
     */
    public void encodeAudioData(byte[] audioPcmDatas) {
        if (audioPcmDatas == null || audioPcmDatas.length < 1) {
            return;
        }
        if (mediaCodec == null) {
            mediaCodec = buildAACMediaEncode();
            mediaCodec.start();
        }
        try {
            if (outputStream == null) {
                outputStream = new BufferedOutputStream(new FileOutputStream(encodeResultFilePath));
            }
        } catch (Exception e) {
        }

        int inputIndex;
        ByteBuffer inputBuffer;
        int outputIndex;
        ByteBuffer outputBuffer;

        int outBitSize;
        int outPacketSize;
        byte[] pcmAudioDatas = audioPcmDatas;

        encodeInputBuffers = mediaCodec.getInputBuffers();
        encodeOutputBuffers = mediaCodec.getOutputBuffers();
        encodeBufferInfo = new MediaCodec.BufferInfo();


        inputIndex = mediaCodec.dequeueInputBuffer(0);
        inputBuffer = encodeInputBuffers[inputIndex];
        inputBuffer.clear();
        inputBuffer.limit(pcmAudioDatas.length);
        inputBuffer.put(pcmAudioDatas);//PCM数据填充给inputBuffer
        mediaCodec.queueInputBuffer(inputIndex, 0, pcmAudioDatas.length, 0, 0);//通知编码器 编码


        outputIndex = mediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);
        while (outputIndex > 0) {
            outBitSize = encodeBufferInfo.size;
            outPacketSize = outBitSize + 7;//7为ADT头部的大小
            outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出Buffer
            outputBuffer.position(encodeBufferInfo.offset);
            outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
            chunkAudio = new byte[outPacketSize];
            addADTStoPacket(chunkAudio, outPacketSize);//添加ADTS
            outputBuffer.get(chunkAudio, 7, outBitSize);//将编码得到的AAC数据 取出到byte[]中

            try {
                //录制aac音频文件，保存在手机内存中
                outputStream.write(chunkAudio, 0, chunkAudio.length);
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            outputBuffer.position(encodeBufferInfo.offset);
            mediaCodec.releaseOutputBuffer(outputIndex, false);
            outputIndex = mediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);
        }
    }

    @Override
    public <I extends AbsAudioEncoder> I setNeedAddExtraBytes(boolean isNeedAddExtraBytes) {
        isNeedAddAdtsHeader = isNeedAddExtraBytes;
        return super.setNeedAddExtraBytes(isNeedAddExtraBytes);
    }

}
