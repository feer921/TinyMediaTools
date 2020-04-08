package common.medias.systemcodec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import common.medias.utils.L;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/8/27<br>
 * Time: 20:17<br>
 * <P>DESC:
 *
 * 关于视频合成的一些问题
 * 1. 前后2个视频的video fps不一样，有关系吗？
 * 没有关系。底层decoder不会因为fps有变化，就不解码。做render的时候，
 * 是根据pts做显示，不会用fps。(fps一般用于pts没有，或者出错时，decoder根据fps重新计算pts，送给render。)
 *
 * 2. 前后2个视频的video codec不一样，有关系吗？
 * 有关系。底层decoder会根据不同的codec建立真正的decoder实例，如AVCDecoder, HEVCDecoder，到解码完成前，这个无法实时切换。
 *
 * 3. 前后2个视频的video 分辨率不一样，有关系吗？
 * 有关系。底层decoder解码时，会根据分辨率等参数，创建解码后的Output Buffer, 假设分辨率突然变大了，那buffer大小就不够，无法存储，会崩溃。
 *
 * 4. Audio sample rate /channel number不一样有关系吗？
 * 有关系。因为这2个参数，在播放音频时，底层建立decoder，就需要设置这2个参数，如果解码到一半，突然变了，一声道变成2声道了，
 * 或者sample rate从44.1k变成32k了，那有些厂商如果不支持sample rate动态变化，就会出错，最终效果，很可能是播放时变成杂音。
 * 原文链接：https://blog.csdn.net/newchenxf/article/details/78524896
 * </p>
 * ******************(^_^)***********************
 */
public class VideoComposer {
    private final String TAG = "VideoComposer";
    private ArrayList<String> mVideoList;
    private String mOutFilename;

    private MediaMuxer mMuxer;
    private ByteBuffer mReadBuf;
    private int mOutAudioTrackIndex;
    private int mOutVideoTrackIndex;
    private MediaFormat mAudioFormat;
    private MediaFormat mVideoFormat;

    public VideoComposer(ArrayList<String> videoList, String outFilename) {
        mVideoList = videoList;
        this.mOutFilename = outFilename;
        mReadBuf = ByteBuffer.allocate(1048576);
    }

    public boolean joinVideo() {
        boolean getAudioFormat = false;
        boolean getVideoFormat = false;
        Iterator videoIterator = mVideoList.iterator();

        //--------step 1 MediaExtractor拿到多媒体信息，用于MediaMuxer创建文件
        while(videoIterator.hasNext()) {
            String videoPath = (String)videoIterator.next();
            MediaExtractor extractor = new MediaExtractor();

            try {
                extractor.setDataSource(videoPath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            int trackIndex;
            if(!getVideoFormat) {
                trackIndex = this.selectTrack(extractor, "video/");
                if(trackIndex < 0) {
                    L.e(TAG, "No video track found in " + videoPath);
                } else {
                    extractor.selectTrack(trackIndex);
                    mVideoFormat = extractor.getTrackFormat(trackIndex);
                    getVideoFormat = true;
                }
            }

            if(!getAudioFormat) {
                trackIndex = this.selectTrack(extractor, "audio/");
                if(trackIndex < 0) {
                    L.e(TAG, "No audio track found in " + videoPath);
                } else {
                    extractor.selectTrack(trackIndex);
                    mAudioFormat = extractor.getTrackFormat(trackIndex);
                    getAudioFormat = true;
                }
            }

            extractor.release();
            if(getVideoFormat && getAudioFormat) {
                break;
            }
        }

        try {
            mMuxer = new MediaMuxer(this.mOutFilename, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(getVideoFormat) {
            mOutVideoTrackIndex = mMuxer.addTrack(mVideoFormat);
        }
        if(getAudioFormat) {
            mOutAudioTrackIndex = mMuxer.addTrack(mAudioFormat);
        }
        mMuxer.start();
        //--------step 1 end---------------------------//


        //--------step 2 遍历文件，MediaExtractor读取帧数据，MediaMuxer写入帧数据，并记录帧信息
        long ptsOffset = 0L;
        Iterator trackIndex = mVideoList.iterator();
        while(trackIndex.hasNext()) {
            String videoPath = (String)trackIndex.next();
            boolean hasVideo = true;
            boolean hasAudio = true;
            MediaExtractor videoExtractor = new MediaExtractor();

            try {
                videoExtractor.setDataSource(videoPath);
            } catch (Exception var27) {
                var27.printStackTrace();
            }

            int inVideoTrackIndex = this.selectTrack(videoExtractor, "video/");
            if(inVideoTrackIndex < 0) {
                hasVideo = false;
            }

            videoExtractor.selectTrack(inVideoTrackIndex);
            MediaExtractor audioExtractor = new MediaExtractor();

            try {
                audioExtractor.setDataSource(videoPath);
            } catch (Exception e) {
                e.printStackTrace();
            }

            int inAudioTrackIndex = this.selectTrack(audioExtractor, "audio/");
            if(inAudioTrackIndex < 0) {
                hasAudio = false;
            }

            audioExtractor.selectTrack(inAudioTrackIndex);
            boolean bMediaDone = false;
            long presentationTimeUs = 0L;
            long audioPts = 0L;
            long videoPts = 0L;

            while(!bMediaDone) {
                if(!hasVideo && !hasAudio) {
                    break;
                }

                int outTrackIndex;
                MediaExtractor extractor;
                int currenttrackIndex;
                if((!hasVideo || audioPts - videoPts <= 50000L) && hasAudio) {
                    currenttrackIndex = inAudioTrackIndex;
                    outTrackIndex = mOutAudioTrackIndex;
                    extractor = audioExtractor;
                } else {
                    currenttrackIndex = inVideoTrackIndex;
                    outTrackIndex = mOutVideoTrackIndex;
                    extractor = videoExtractor;
                }

                mReadBuf.rewind();
                int chunkSize = extractor.readSampleData(mReadBuf, 0);//读取帧数据
                if(chunkSize < 0) {
                    if(currenttrackIndex == inVideoTrackIndex) {
                        hasVideo = false;
                    } else if(currenttrackIndex == inAudioTrackIndex) {
                        hasAudio = false;
                    }
                } else {
                    if(extractor.getSampleTrackIndex() != currenttrackIndex) {
                        L.e(TAG, "WEIRD: got sample from track " + extractor.getSampleTrackIndex() + ", expected " + currenttrackIndex);
                    }

                    presentationTimeUs = extractor.getSampleTime();//读取帧的pts
                    if(currenttrackIndex == inVideoTrackIndex) {
                        videoPts = presentationTimeUs;
                    } else {
                        audioPts = presentationTimeUs;
                    }

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    info.offset = 0;
                    info.size = chunkSize;
                    info.presentationTimeUs = ptsOffset + presentationTimeUs;//pts重新计算
                    if((extractor.getSampleFlags() & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                        info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                    }

                    mReadBuf.rewind();
//                    L.i(TAG, String.format("write sample track %d, size %d, pts %d flag %d", new Object[]{Integer.valueOf(outTrackIndex), Integer.valueOf(info.size), Long.valueOf(info.presentationTimeUs), Integer.valueOf(info.flags)}));
                    mMuxer.writeSampleData(outTrackIndex, mReadBuf, info);//写入文件
                    extractor.advance();
                }
            }

            //记录当前文件的最后一个pts，作为下一个文件的pts offset
            ptsOffset += videoPts > audioPts ? videoPts : audioPts;
//            ptsOffset += 10000L;//前一个文件的最后一帧与后一个文件的第一帧，差10ms，只是估计值，不准确，但能用 //removed by fee 2019-08-27:会造成停顿一下

            L.i(TAG, "finish one file, ptsOffset " + ptsOffset);

            videoExtractor.release();
            audioExtractor.release();
        }

        if(mMuxer != null) {
            try {
                mMuxer.stop();
                mMuxer.release();
            } catch (Exception e) {
                L.e(TAG, "Muxer close error. No data was written");
            }

            mMuxer = null;
        }

        L.i(TAG, "video join finished");
        return true;
    }

    private int selectTrack(MediaExtractor extractor, String mimePrefix) {
        int numTracks = extractor.getTrackCount();

        for(int i = 0; i < numTracks; ++i) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString("mime");
            if(mime.startsWith(mimePrefix)) {
                return i;
            }
        }

        return -1;
    }
}
