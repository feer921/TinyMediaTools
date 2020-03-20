package common.medias.systemcodec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import common.medias.utils.L;


/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/8/10<br>
 * Time: 15:25<br>
 * <P>DESC:
 * Android支持的音频解码器
 * </p>
 * ******************(^_^)***********************
 */
public class AndroidAudioDecoder extends AbsAudioDecoder {


    AndroidAudioDecoder(String theEncodeAudioFilePath) {
        super(theEncodeAudioFilePath);
    }

    @Override
    public RawAudioInfo decodeToFile(String outPutDecodedFilePath) throws IOException {

        long beginTime = System.currentTimeMillis();

        final String encodeFile = theEncodedWithAudioTrackFilePath;
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(encodeFile);

        //先从源文件中提取audio track 媒体信息
        MediaFormat mediaFormat = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i);
                mediaFormat = format;
                break;
            }
        }

        if (mediaFormat == null) {
            Log.e(TAG, "not a valid file with audio track..");
            extractor.release();
            return null;
        }

        RawAudioInfo rawAudioInfo = new RawAudioInfo();
        rawAudioInfo.channel = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        rawAudioInfo.sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        rawAudioInfo.tempRawFile = outPutDecodedFilePath;
        //解码时被解码出来的数据写入文件的输出流
        FileOutputStream fosDecoder = new FileOutputStream(outPutDecodedFilePath);

        String mediaMime = mediaFormat.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = MediaCodec.createDecoderByType(mediaMime);
        codec.configure(mediaFormat, null, null, 0);
        codec.start();

        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        final double audioDurationUs = mediaFormat.getLong(MediaFormat.KEY_DURATION);
        final long kTimeOutUs = 5000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int totalRawSize = 0;
        try {
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                        int sampleSize = extractor.readSampleData(dstBuf, 0);
                        if (sampleSize < 0) {
                            L.e(TAG, "saw input EOS.");
                            sawInputEOS = true;
                            codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            long presentationTimeUs = extractor.getSampleTime();
                            codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0);
                            extractor.advance();
                        }
                    }
                }
                int res = codec.dequeueOutputBuffer(info, kTimeOutUs);
                if (res >= 0) {
                    int outputBufIndex = res;
                    // Simply ignore codec config buffers.
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        L.e(TAG, "audio encoder: codec config buffer");
                        codec.releaseOutputBuffer(outputBufIndex, false);
                        continue;
                    }

                    if (info.size != 0) {
                        ByteBuffer outBuf = codecOutputBuffers[outputBufIndex];

                        outBuf.position(info.offset);
                        outBuf.limit(info.offset + info.size);
                        byte[] data = new byte[info.size];
                        outBuf.get(data);
                        totalRawSize += data.length;
                        fosDecoder.write(data);
                        onDecodeCallback(data, info.presentationTimeUs / audioDurationUs);
                        L.d(TAG, theEncodedWithAudioTrackFilePath + " presentationTimeUs : " + info.presentationTimeUs);
                    }

                    codec.releaseOutputBuffer(outputBufIndex, false);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        L.e(TAG, "saw output EOS.");
                        sawOutputEOS = true;
                    }

                } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    codecOutputBuffers = codec.getOutputBuffers();
                    L.e(TAG, "output buffers have changed.");
                } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat oformat = codec.getOutputFormat();
                    L.e(TAG, "output format has changed to " + oformat);
                }
            }
            fosDecoder.flush();
            rawAudioInfo.size = totalRawSize;
            onDecodeCallback(null, 1);
            L.i(TAG, "decode " + outPutDecodedFilePath + " cost " + (System.currentTimeMillis() - beginTime) + " milliseconds !");
            return rawAudioInfo;
        }
        finally {
            fosDecoder.close();
            codec.stop();
            codec.release();
            extractor.release();
        }

    }
}
