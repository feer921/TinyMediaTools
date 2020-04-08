package common.medias.ffmpeg;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.FFprobe;
import com.arthenica.mobileffmpeg.MediaInformation;
import com.arthenica.mobileffmpeg.util.AsyncSingleFFmpegExecuteTask;
import com.arthenica.mobileffmpeg.util.SingleExecuteCallback;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import common.medias.utils.L;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/9/4<br>
 * Time: 12:06<br>
 * <P>DESC:
 * 扩展 基于FFMpeg的功能
 * </p>
 * ******************(^_^)***********************
 */
public class MediaEditor {

    //ffmpeg -i 1v.mp4 -i 1temp.mp4.aac -acodec copy -ss 0 -t 5 new.mp4 //合并音频

    /**
     * 下面的命令是用audio音频替换video中的音频
     * ffmpeg -i video.mp4 -i audio.wav -c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0 output.mp4
     *
     * 使用FFMpeg(命令)来将一个音频文件嵌入到目标视频文件中
     *
     * 注：如果目标视频中已经有音频track了，则该音频track会被替换
     * @param audioFilePath 音频文件路径
     * @param videoFilePath 视频文件路径
//     * @param maxMediaDurationUs 两个媒体文件中时长最大的时长；该参数主要目的是用来计算操作进度的，可以为0；单位为：微秒
     * @param editorListener 执行监听者，如果为null，则为同步执行；如果不为null则为异步执行.
     */
    public static boolean muxAudioAndVideo(String audioFilePath, String videoFilePath,
                                        String muxResultFilePath, SingleExecuteCallback editorListener) {
        String cmdStr = "-i %s -i %s -c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0 %s";
        int rec = FFmpeg.execute(String.format(cmdStr, videoFilePath, audioFilePath, muxResultFilePath));
//        FFMpegCmdSpeller cmdBuilder = new FFMpegCmdSpeller();
//        cmdBuilder._i().append(audioFilePath)
//                ._i().append(videoFilePath)
//                ._vcodec().copy().append(muxResultFilePath);
//
//       return cmdBuilder.selfExecute(0, editorListener);
        return 0 == rec;
    }


    /**
     * 无损拼接媒体文件，要求相对严格
     * 对视频格式严格，需要分辨率，帧率，码率都相同，不支持对要合并的视频进行其他处理操作，该方法合并速度很快，
     * 另：两段同格式的音频拼接也可使用该方法
     * ffmpeg -f concat -i list.txt -c copy "all.mp3" 拼接音频 ???
     * @param toAppendVideos
     * @param muxResultVideoFilePath
     * @throws InterruptedException
     */
    public static boolean appendAVMedias(List<String> toAppendVideos, String muxResultVideoFilePath) {
        String cmdStr = "-f concat -safe 0 -i %s -c copy %s";
        if (toAppendVideos == null || toAppendVideos.isEmpty()) {
            return false;
        }
        String aMediaFilePath = toAppendVideos.get(0);
        File parentPathFile = new File(aMediaFilePath).getParentFile();
        File listFile = new File(parentPathFile, "list.txt");
        listFile.delete();
        FileWriter fw = null;
        try {
            fw = new FileWriter(listFile, true);
            for (String toAppendVideo : toAppendVideos) {
                fw.write(String.format("file \'%s\'\n", toAppendVideo));
            }
            fw.flush();
        } catch (Exception e) {

        }finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        int rec = FFmpeg.execute(String.format(cmdStr, listFile.getAbsolutePath(), muxResultVideoFilePath));
        return 0 == rec;
    }


    /**
     * 解复用视频(意思就是将一个目标视频分离出它的纯视频或者纯音频成一个新文件)
     * @param toDeMuxVideoFilePath 要分离的视频文件路径
     * @param isDemuxOutVideo true:分离出纯视频；false: 分离出纯音频
     * @param codecFormat 编码解码器：默认(传null或者"")为使用原视频中的相应的编解码器
     * @param outputFilePath 输出的文件路径
     * @param editorListener 命令执行监听者，不为null时为异步执行
     */
    public static boolean deMuxVideo(String toDeMuxVideoFilePath, boolean isDemuxOutVideo, String codecFormat,
                                  String outputFilePath, SingleExecuteCallback editorListener) {
        File resultFile = new File(outputFilePath);
        if (resultFile.exists()) {//FFMpeg 好象如果生成的文件存在时会操作失败(其实不是操作失败，是命令行会提示是否覆盖,然APP内无法输入),所以先删除
            resultFile.delete();
        }
        boolean isSyncOpt = editorListener == null;
        FFMpegCmdSpeller cmdSpeller = new FFMpegCmdSpeller();
        //ffmpeg -i xxx.mp4 -acodec copy -vn output.aac
        cmdSpeller._i(toDeMuxVideoFilePath);
        if (isDemuxOutVideo) {//分离出视频
            cmdSpeller._vcodec(codecFormat)
                    .copy()._an();//不处理音频
        }
        else {//分离出音频  ffmpeg -i xxx.mp4 -acodec aac -vn output.aac
            cmdSpeller._acodec(codecFormat);
            if (codecFormat == null || codecFormat.isEmpty()) {
                cmdSpeller.copy();
            }
            cmdSpeller._vn();//不处理视频
        }
        cmdSpeller.append(outputFilePath);
       return cmdSpeller.selfExecute(0, editorListener);
    }

    /**
     * ffmpeg -i 1.mp4 -acodec copy -ss 0 -t 3 output.mp4
     * 从一个原媒体文件中裁切出从开始时间到指定时间的一段新媒体文件
     * @param toClipAVMediaFilePath 要裁切的媒体文件，可以是完整视频、单独音频、单独视频
     * @param fromTimeSecond 从第几秒开始
     * @param toTimeSecond 到第几秒结束
     * @param ignoreAudio 是否不要音频了
     * @param ignoreVideo 是否不要视频了
     * @param outputResultFilePath 输出到新文件路径
     * @param editorListener 监听者,为null时为同步执行
     */
    public static boolean clipAVMedia(String toClipAVMediaFilePath, double fromTimeSecond, double toTimeSecond,
                                      boolean ignoreAudio, boolean ignoreVideo,
                                      String outputResultFilePath, SingleExecuteCallback editorListener) {
        if (ignoreAudio && ignoreVideo) {//音频视频都不要了，则啥也不做
            return false;
        }


        L.i("MediaEditor", "-->clipAVMedia() fromTimeSecond = " + fromTimeSecond + " toTimeSecond = " + toTimeSecond);
        File resultFile = new File(outputResultFilePath);
        if (resultFile.exists()) {
            resultFile.delete();
        }

        FFMpegCmdSpeller cmdSpeller = new FFMpegCmdSpeller();
        cmdSpeller._i(toClipAVMediaFilePath)
                ;

        if (!ignoreAudio && !ignoreVideo) {//音频和视频都要
            cmdSpeller._acodec().copy();
        }
        else {
            if (ignoreAudio) {
                cmdSpeller._an()._vcodec().copy();
            }

            if (ignoreVideo) {
                cmdSpeller._vn()._acodec().copy();
            }
        }
        cmdSpeller._ss(fromTimeSecond + "")
                ._t(toTimeSecond + "")
                .outPut(outputResultFilePath);
        return cmdSpeller.selfExecute(0, editorListener);
    }

    /**
     * 裁切掉一个媒体文件的末尾几秒的时间
     * @param toClipAVMediaFilePath 要裁切的原文件
     * @param removeEndTimeMS 裁切掉末尾的 时间：秒
     * @param ignoreAudio 是否不要音频
     * @param ignoreVideo 是否不要视频
     * @param outputResultFilePath 输出新的文件路径
     * @param editorListener 监听者；如果为null 则为异常执行
     * @return true:同步执行结果成功；false:同步执行失败，或者为异常执行，此时不具有参考意义
     */
    public static boolean clipAVMediaByRemoveEndTime(String toClipAVMediaFilePath, long removeEndTimeMS,
                                                     boolean ignoreAudio, boolean ignoreVideo,
                                                     String outputResultFilePath, SingleExecuteCallback editorListener
                                                     ){

        if (ignoreAudio && ignoreVideo) {
            return false;
        }
        long curMediaDuration = extractMediaDuration(toClipAVMediaFilePath) / 1000;
        L.i("ExtraEpEditor", "-->clipAVMediaByRemoveEndTime() the media duration in ms is " + curMediaDuration);
        if (curMediaDuration == 0 || curMediaDuration <= removeEndTimeMS) {
            L.e("ExtraEpEditor", "-->clipAVMediaByRemoveEndTime() media duration exception...");
            return false;
        }
        long mediaRetainDuration = curMediaDuration - removeEndTimeMS;
        double mediaRetainSecond = mediaRetainDuration *1.0 / 1000;

        return clipAVMedia(toClipAVMediaFilePath, 0, mediaRetainSecond, ignoreAudio, ignoreVideo, outputResultFilePath, editorListener);
    }


    /**
     * 多个音频混音
     * ffmpeg -i 124.mp3 -i 123.mp3 -filter_complex amix=inputs=2:duration=first:dropout_transition=2 -f mp3 remix.mp3
     * -i a.aac -i b.aac -filter_complex "[0:a][1:a]amerge=inputs=2[a]" -map "[a]" -ac 2 output.aac 这个快
     *       解释：-i代表输入参数
     *       -filter_complex ffmpeg滤镜功能，非常强大，详细请查看文档
     *       amix是混合多个音频到单个音频输出
     *       inputs=2代表是2个音频文件，如果更多则代表对应数字
     *       duration 确定最终输出文件的长度
     *       longest(最长)|shortest（最短）|first（第一个文件）
     *       dropout_transition
     *       The transition time, in seconds, for volume renormalization when an input stream ends. The default value is 2 seconds.
     *       -f mp3  输出文件格式
     * @param mixResultFilePath 混音结果输出文件路径
     * @param resultPackageFormat 以什么格式输出；eg.: mp3等，可以不传
     * @param editorListener 命令执行监听者，为null时不同步执行
     * @param audioFilePaths 要混合的音频文件路径
     * @return true:如果是同步执行；false:同步执行case下为执行失败、非同步case下无意义
     */
    public static boolean mixAudios(String mixResultFilePath, String resultPackageFormat, SingleExecuteCallback editorListener, String... audioFilePaths) {
        if (isEmpty(mixResultFilePath) || audioFilePaths == null) {
            return false;
        }
        int audioFilesCount = audioFilePaths.length;
        if (audioFilesCount < 2) {// >= 2条音频才音频
            return false;
        }
        ArrayList<String> willToMixAudioFilePaths = null;
        for (String oneAudioFilePath : audioFilePaths) {
            if (!isEmpty(oneAudioFilePath)) {
                File audioFile = new File(oneAudioFilePath);
                if (audioFile.exists() && audioFile.length() > 0) {//要混音的音频文件也有效
                    if (willToMixAudioFilePaths == null) {
                        willToMixAudioFilePaths = new ArrayList<>(audioFilesCount);
                    }
                    willToMixAudioFilePaths.add(oneAudioFilePath);
                }
            }
        }
        audioFilesCount = willToMixAudioFilePaths != null ? willToMixAudioFilePaths.size() : 0;
        if (audioFilesCount < 2) {//也不需要混音
            return false;
        }

        FFMpegCmdSpeller cmdSpeller = new FFMpegCmdSpeller();
        for (String willToMixAudioFilePath : willToMixAudioFilePaths) {
            cmdSpeller._i(willToMixAudioFilePath);
        }
        cmdSpeller._filterComplex()
                .amixArgs(audioFilesCount, "first")

        ;
        if (!isEmpty(resultPackageFormat)) {
            cmdSpeller._f(resultPackageFormat);
        }
        cmdSpeller._y().outPut(mixResultFilePath);
        return cmdSpeller.selfExecute(0,editorListener);
    }

    /**
     * ffmpeg -f s16le -ar 48000 -ac 2 -i 1temp.pcm -y [-f mp3/mp4/avi ] out.aac //这样ok
     * PCM音频数据 使用相应的编码格式转换成对应的输出文件
     * PCM -->AAC
     * PCM -->Mp3
     * PCM -->WAV
     * 其实还可以重采样
     * @param pcmAudioDataFilePath PCM原音频数据
     * @param sampleRate PCM音频文件所使用的采样率 最好和原PCM音频文件所知的采样率一致
     * @param channelCount PCM音频文件所使用的音频通道数 最好和原PCM音频文件所知的音频通道数一致
     * @param outputFormat 音频输出(编码)格式，eg.: mp4、mp3、avi...
     * @param outputFilePath 转换输出的新文件路径 输出格式指定后，最好文件的后缀和输出格式操作一致
     * @param editorListener 监听者，为null时为同步执行
     * @return true:同步执行成功；false:同步执行失败或者异步执行时无参考意义
     */
    public static boolean pcmConvertTo(String pcmAudioDataFilePath, int sampleRate, int channelCount,
                                       String outputFormat, String outputFilePath,
                                       SingleExecuteCallback editorListener) {
        if (isEmpty(pcmAudioDataFilePath) || isEmpty(outputFilePath)) {
            return false;
        }
        File pcmAudioDataFile = new File(pcmAudioDataFilePath);
        if (!pcmAudioDataFile.exists() || pcmAudioDataFile.length() < 1) {
            return false;
        }
        FFMpegCmdSpeller cmdSpeller = new FFMpegCmdSpeller();
        cmdSpeller._f("s16le")//表示采样精度为16位,这里直接默认为16位，需要外部传入？
                ._ar(sampleRate)
                ._ac(channelCount)//前面为对"-i"输入流的描述
                ._i(pcmAudioDataFilePath)
                ._f(outputFormat)
                ;
        cmdSpeller._y().outPut(outputFilePath);
        return cmdSpeller.selfExecute(0, editorListener);
    }

    static boolean isEmpty(String str) {
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return false;
    }

    /**
     * 直接执行FFMpeg 命令
     * @param cmdFormatStr 符合命令格式的字符串
     * @param executeCallback 如果为null,则为同步执行；如果不为null则为异步执行
     * @return true: 执行成功/或者在异步执行；false:执行失败.
     */
    public static boolean exeFFmpegCmd(String cmdFormatStr, SingleExecuteCallback executeCallback) {
        if (isEmpty(cmdFormatStr)) {
            return false;
        }
        if (executeCallback == null) {
            int rec = FFmpeg.execute(cmdFormatStr);
            L.d("MediaEditor", "-->exeFFmpegCmd() rec = " + rec);
            return rec == 0;
        }
        else {
            AsyncSingleFFmpegExecuteTask executeTask = new AsyncSingleFFmpegExecuteTask(cmdFormatStr, executeCallback);
            executeTask.executeOnExecutor(AsyncSingleFFmpegExecuteTask.THREAD_POOL_EXECUTOR);
        }
        return true;
    }

    /**
     * 转换音频文件格式
     * eg.: aac --> wav/mp3/... 转换的格式由 文件名后缀指定(eg.: out.mp3/out.wav)
     * @param theAudioFilePath 要转换的音频文件路径
     * @param resampleRate 重 采样率
     * @param audioChannelCount 重采样的 声道数
     * @param outputFilePath 输出文件路径
     * @param editorListener 为null时为同步执行；
     * @return true: 异步执行无参考意义。同步执行时：成功；否则为失败
     */
    public static boolean convertAudioFileFormat(String theAudioFilePath,String resampleRate,int audioChannelCount,String outputFilePath,
                                                 SingleExecuteCallback editorListener){
        if (isEmpty(theAudioFilePath) || isEmpty(outputFilePath)) {
            return false;
        }
        File theAudioFile = new File(theAudioFilePath);
        if (!theAudioFile.exists() || theAudioFile.length() < 1) {
            return false;
        }
        FFMpegCmdSpeller cmdSpeller = new FFMpegCmdSpeller();
        cmdSpeller._i(theAudioFilePath)
        ;
        if (audioChannelCount < 1) {
            audioChannelCount = 1;
        }
        if (!isEmpty(resampleRate)) {
            cmdSpeller.append("-ar").append(resampleRate)
                    ._ac(audioChannelCount)
            ;
        }
        cmdSpeller._y().outPut(outputFilePath);
        return cmdSpeller.selfExecute(0, editorListener);
    }

    public static long extractMediaDuration(String theMediaFilePath) {
        long duration = 0;
        if (!isEmpty(theMediaFilePath)) {
            MediaInformation mediaInfo = FFprobe.getMediaInformation(theMediaFilePath);
            if (mediaInfo != null) {
                Long durationL = mediaInfo.getDuration();
                if (durationL != null) {
                    duration = durationL;
                }
            }
        }
        return duration;
    }

    public static MediaInfoWrapper extractMediaInfos(String theMediaFilePath) {
        if (!isEmpty(theMediaFilePath)) {
            return new MediaInfoWrapper(FFprobe.getMediaInformation(theMediaFilePath));
        }
        return null;
    }

    /**
     * 调整输入的 媒体文件的音量 然后输出为一个新媒体文件
     *  ffmpeg -i a.aac -af volume=10dB -y out.aac
     * @param inputMediaFilePath 输入的媒体文件
     * @param targetVolumeInfo 要调整到的目标 音量; eg.: 0dB; 10dB
     * @param outPutFilePath 调整后的输出文件
     * @param editorListener 为null是为同步执行；否则为异步执行
     * @return true:异步执行或者同步执行ok; false: 执行失败
     */
    public static boolean adjustMediaVolume(String inputMediaFilePath, String targetVolumeInfo, String outPutFilePath, SingleExecuteCallback editorListener) {
        if (isEmpty(inputMediaFilePath) || isEmpty(outPutFilePath)) {
            return false;
        }
        FFMpegCmdSpeller cmdSpeller = new FFMpegCmdSpeller();
        cmdSpeller._i(inputMediaFilePath)
                .append("-af")
                .append(String.format("volume=%s",targetVolumeInfo))
                ._y()
                .outPut(outPutFilePath);
        return cmdSpeller.selfExecute(0, editorListener);
    }
}
