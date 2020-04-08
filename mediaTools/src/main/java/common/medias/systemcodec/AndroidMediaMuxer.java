package common.medias.systemcodec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.nio.ByteBuffer;

import common.medias.utils.L;


/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/8/14<br>
 * Time: 11:44<br>
 * <P>DESC:
 * 基于安卓原生的 MediaMuxer 进行音、视频的合并功能
 * </p>
 * ******************(^_^)***********************
 */
public class AndroidMediaMuxer extends AbsMediaMuxer {
    private final static int ALLOCATE_BUFFER = 256 * 1024;
    private static final String MIME_AUDIO_FLAG = "audio/";
    private static final String MIME_VIDEO_FLAG = "video/";
    private MediaMuxer mediaMuxer;
    private boolean isMuxerStarted = false;

    @Override
    public boolean startMux() {
        if (toMuxFilesParams != null && !toMuxFilesParams.isEmpty()) {
            try {
                if (mediaMuxer == null) {
                    mediaMuxer = new MediaMuxer(theMuxResultFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    isMuxerStarted = false;
                }
                for (MuxFileParams theMuxFileParams : toMuxFilesParams) {
                    handleMuxFileParam(theMuxFileParams);
                }
                isMuxerStarted = false;
                mediaMuxer.stop();
                mediaMuxer.release();
                mediaMuxer = null;
                return true;
            } catch (Exception e) {
                L.e(TAG, "-->startMux() occur: " + e);
                mediaMuxer = null;
            }
        }
        return false;
    }

    @Override
    public boolean muxAudioAndVideo(String audioFilePath, String videoFilePath, String unSupportAudioMimeType) {
        boolean muxSuc = false;
        MediaMuxer mediaMuxer = null;
        MediaExtractor audioExtractor = null;
        MediaExtractor videoExtractor = null;
        try {
            //提取 audio MediaFormat
            audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(audioFilePath);
            int audioTrackCount = audioExtractor.getTrackCount();
            int audioTrackIndex = -1;
            MediaFormat audioMediaFormat = null;
            String audioMime = "";
            for (int trackIndex = 0; trackIndex < audioTrackCount; trackIndex++) {
                MediaFormat mediaFormat = audioExtractor.getTrackFormat(trackIndex);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith(MIME_AUDIO_FLAG)) {
                    audioTrackIndex = trackIndex;
                    audioMediaFormat = mediaFormat;
                    audioMime = mimeType;
                    break;
                }
            }
            L.d(TAG, "-->muxAudioAndVideo() audioMediaFormat = " + audioMediaFormat);
            if (unSupportAudioMimeType != null && audioMime.startsWith(unSupportAudioMimeType)) {
                return false;
            }
            if (audioTrackIndex != -1) {
                audioExtractor.selectTrack(audioTrackIndex);
                audioMediaFormat = audioExtractor.getTrackFormat(audioTrackIndex);
            }
            else {
                return false;
            }

            //提取 video MediaFormat
            videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(videoFilePath);
            int videoTrackCount = videoExtractor.getTrackCount();
            MediaFormat videoMediaFormat = null;
            int videoTrackIndex = -1;
            for (int trackIndex = 0; trackIndex < videoTrackCount; trackIndex++) {
                MediaFormat mediaFormat = videoExtractor.getTrackFormat(trackIndex);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith(MIME_VIDEO_FLAG)) {
                    videoTrackIndex = trackIndex;
                    videoMediaFormat = mediaFormat;
                    break;
                }
            }
            if (videoTrackIndex != -1) {
                videoExtractor.selectTrack(videoTrackIndex);
                videoMediaFormat = videoExtractor.getTrackFormat(videoTrackIndex);
            }
            L.d(TAG, "-->muxAudioAndVideo() videoMediaFormat = " + videoMediaFormat);
            int audioTrackWriteIndex = -1;

            int videoTrackWriteIndex = -1;

            mediaMuxer = new MediaMuxer(theMuxResultFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            if (audioMediaFormat != null) {
                audioTrackWriteIndex = mediaMuxer.addTrack(audioMediaFormat);
            }
            if (videoMediaFormat != null) {
                videoTrackWriteIndex = mediaMuxer.addTrack(videoMediaFormat);
            }



            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            mediaMuxer.start();

//            boolean writeAudioOk =
//                    writeSampleData(audioExtractor, mediaMuxer, audioTrackWriteIndex, audioTrackIndex,false);
//
//            boolean writeVideoOk =
//            writeSampleData(videoExtractor, mediaMuxer, videoTrackWriteIndex, videoTrackIndex,false);


            int bufferSize = 256 * 1024;

            ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);

            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

            int frameCount = 0;

            int offset = 100;

            //计算出时间戳，以视频为准
            long sampletime = 0;
//            L.d(TAG, "-->muxAudioAndVideo() sampletime = " + sampletime);

            //write video track datas...
            boolean saweos = false;
            while (!saweos) {
                videoBufferInfo.offset = offset;
                videoBufferInfo.size = videoExtractor.readSampleData(byteBuffer, offset);
                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    saweos = true;
                    videoBufferInfo.size = 0;
                }
                else {
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                    //noinspection wrongconstant
                    videoBufferInfo.flags = videoExtractor.getSampleFlags();
                    mediaMuxer.writeSampleData(videoTrackWriteIndex, byteBuffer, videoBufferInfo);
                    videoExtractor.advance();
                    frameCount++;
                }
            }

            saweos = false;
            frameCount = 0;

            //write audio track data...
            ByteBuffer audioByteBuffer = ByteBuffer.allocate(bufferSize);
            while (!saweos) {
                frameCount++;
                audioBufferInfo.offset = offset;
                audioBufferInfo.size = audioExtractor.readSampleData(audioByteBuffer, offset);
                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    saweos = true;
                    audioBufferInfo.size = 0;
                }
                else {
                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
                    //noinspection wrongconstant
                    audioBufferInfo.flags = audioExtractor.getSampleFlags();
                    mediaMuxer.writeSampleData(audioTrackWriteIndex, audioByteBuffer, audioBufferInfo);
                    audioExtractor.advance();
                }
            }
            muxSuc = true;
//            muxSuc = writeAudioOk && writeVideoOk;
        }catch (Exception e){
            L.e(TAG, "-->muxAudioAndVideo() occur: " + e);
        }finally {
            try {
                if (mediaMuxer != null) {
                    mediaMuxer.stop();
                    mediaMuxer.release();
                }
                if (audioExtractor != null) {
                    audioExtractor.release();
                }
                if (videoExtractor != null) {
                    videoExtractor.release();
                }
            } catch (Exception e) {
                L.e(TAG, "-->muxAudioAndVideo() stop and release occur: " + e);
            }
        }
        isMuxerStarted = false;
        mediaMuxer = null;
        return muxSuc;
    }


    private void handleMuxFileParam(MuxFileParams theMuxFileParam) throws Exception {
        //1、使用MediaExtractor提取出 音、视频信息
        MediaExtractor mediaExtractor = new MediaExtractor();
        String theFilePath = theMuxFileParam.theMediaFilePath;
        mediaExtractor.setDataSource(theFilePath);

        int mediaTrackCount = mediaExtractor.getTrackCount();
        boolean isMuxAudioTrack = theMuxFileParam.isMuxAudio;
        boolean isMuxVideoTrack = theMuxFileParam.isMuxVideo;

        int audioTrackIndex = -1;
        MediaFormat audioMediaFormat = null;

        int videoTrackIndex = -1;
        MediaFormat videoMediaFormat = null;
        for (int trackIndex = 0; trackIndex < mediaTrackCount; trackIndex++) {
            MediaFormat mediaFormat = mediaExtractor.getTrackFormat(trackIndex);
            String mineType = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (isMuxAudioTrack) {
                if (mineType.startsWith(MIME_AUDIO_FLAG)) {
                    audioMediaFormat = mediaFormat;
                    audioTrackIndex = trackIndex;
                }
            }
            if (isMuxVideoTrack) {
                if (mineType.startsWith(MIME_VIDEO_FLAG)) {
                    videoMediaFormat = mediaFormat;
                    videoTrackIndex = trackIndex;
                }
            }
        }
        int audioMediaFormatWriteIndex = -1;
        if (audioMediaFormat != null) {
            audioMediaFormatWriteIndex = mediaMuxer.addTrack(audioMediaFormat);
        }

        int videoMediaFormatWriteIndex = -1;
        if (videoMediaFormat != null) {
            videoMediaFormatWriteIndex = mediaMuxer.addTrack(videoMediaFormat);
        }

        if (audioMediaFormatWriteIndex != -1 || videoMediaFormatWriteIndex != -1) {//表示有track要写入了
            if (!isMuxerStarted) {
                mediaMuxer.start();
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            MediaCodec.BufferInfo audiobufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo videobufferInfo = new MediaCodec.BufferInfo();
            long sampletime = 0;
            long first_sampletime = 0;
            long second_sampletime = 0;
            int curSelectedTrackIndex = -1;
            if(videoTrackIndex !=-1){
                mediaExtractor.selectTrack(videoTrackIndex);
                curSelectedTrackIndex = videoTrackIndex;
            }
            else if(audioTrackIndex != -1){
                mediaExtractor.selectTrack(audioTrackIndex);
                curSelectedTrackIndex = audioTrackIndex;
            }
            //获取时间戳,优先以视频track为准
            {
                mediaExtractor.readSampleData(byteBuffer, 0);
                first_sampletime = mediaExtractor.getSampleTime();
                mediaExtractor.advance();
                second_sampletime = mediaExtractor.getSampleTime();
                sampletime = Math.abs(second_sampletime - first_sampletime);//时间戳
                L.d(TAG, "-->handleMuxFileParam() sampletime" + sampletime);

            }
            mediaExtractor.unselectTrack(curSelectedTrackIndex);

            if (videoMediaFormatWriteIndex != -1) {//切到视频track
                mediaExtractor.selectTrack(videoTrackIndex);
                while (true) {
                    int readSampleCount = mediaExtractor.readSampleData(byteBuffer, 0);
                    L.d(TAG, "-->handleMuxFileParam() video:readSampleCount:" + readSampleCount);
                    if (readSampleCount < 0) {
                        break;
                    }
                    videobufferInfo.size = readSampleCount;
                    videobufferInfo.offset = 0;
                    videobufferInfo.flags = mediaExtractor.getSampleFlags();
                    videobufferInfo.presentationTimeUs += sampletime;
                    mediaMuxer.writeSampleData(videoMediaFormatWriteIndex, byteBuffer, videobufferInfo);
                    byteBuffer.clear();
                    mediaExtractor.advance();
                }
            }

            if (audioMediaFormatWriteIndex != -1) {//切到音频track
                mediaExtractor.selectTrack(audioTrackIndex);
                while (true) {
                    int readSampleCount = mediaExtractor.readSampleData(byteBuffer, 0);
                    L.d(TAG, "-->muxAudioAndVideo() audio:readSampleCount:" + readSampleCount);
                    if (readSampleCount < 0) {
                        break;
                    }
                    audiobufferInfo.size = readSampleCount;
                    audiobufferInfo.offset = 0;
                    audiobufferInfo.flags = mediaExtractor.getSampleFlags();
                    audiobufferInfo.presentationTimeUs += sampletime;
                    mediaMuxer.writeSampleData(audioMediaFormatWriteIndex, byteBuffer, audiobufferInfo);
                    byteBuffer.clear();
                    mediaExtractor.advance();
                }
            }

        }
        try {
            mediaExtractor.release();
        } catch (Exception ignore) {
        }
    }


    /**
     * write sample data to mediaMuxer
     *
     * @param mediaExtractor
     * @param mediaMuxer
     * @param writeTrackIndex
     * @param theTrackIndexOfMediaFormat
     * @return
     */
    private boolean writeSampleData(MediaExtractor mediaExtractor, MediaMuxer mediaMuxer,
                                    int writeTrackIndex, int theTrackIndexOfMediaFormat, boolean isNeedSampleTime) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(ALLOCATE_BUFFER);
            // 读取写入帧数据
            long sampleTime = 0;
            if (isNeedSampleTime) {
                sampleTime = getSampleTime(mediaExtractor, byteBuffer, theTrackIndexOfMediaFormat);
            }
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int offset = 100;
            while (true) {
                //读取帧之间的数据
                int readSampleSize = mediaExtractor.readSampleData(byteBuffer, offset);
                if (readSampleSize < 0) {
                    break;
                }

                bufferInfo.size = readSampleSize;
                bufferInfo.offset = offset;
                bufferInfo.flags = mediaExtractor.getSampleFlags();
//                bufferInfo.flags = MediaExtractor.SAMPLE_FLAG_SYNC;
                if (isNeedSampleTime) {
                    bufferInfo.presentationTimeUs += sampleTime;
                }
                else {
                    bufferInfo.presentationTimeUs = mediaExtractor.getSampleTime();
                }
                //写入帧的数据
                mediaMuxer.writeSampleData(writeTrackIndex, byteBuffer, bufferInfo);
                mediaExtractor.advance();
            }
            return true;
        } catch (Exception e) {
            L.w(TAG, "writeSampleData ex", e);
        }

        return false;
    }

    /**
     * 获取每帧的之间的时间
     *
     * @return
     */
    private long getSampleTime(MediaExtractor mediaExtractor, ByteBuffer byteBuffer, int videoTrack) {
        if (mediaExtractor == null) {
            L.w(TAG, "getSampleTime mediaExtractor is null");
            return 0;
        }
        mediaExtractor.readSampleData(byteBuffer, 0);
        //skip first I frame
        if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
            mediaExtractor.advance();
        }
        mediaExtractor.readSampleData(byteBuffer, 0);

        // get first and second and count sample time
        long firstVideoPTS = mediaExtractor.getSampleTime();
        mediaExtractor.advance();
        mediaExtractor.readSampleData(byteBuffer, 0);
        long SecondVideoPTS = mediaExtractor.getSampleTime();
        long sampleTime = Math.abs(SecondVideoPTS - firstVideoPTS);
        L.d(TAG, "getSampleTime is " + sampleTime);

        // 重新切换此信道，不然上面跳过了3帧,造成前面的帧数模糊
        mediaExtractor.unselectTrack(videoTrack);
        mediaExtractor.selectTrack(videoTrack);

        return sampleTime;
    }
}
