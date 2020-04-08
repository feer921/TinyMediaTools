package common.medias.ffmpeg;


import android.os.AsyncTask;
import android.util.Log;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.util.AsyncSingleFFmpegExecuteTask;
import com.arthenica.mobileffmpeg.util.SingleExecuteCallback;

import java.util.Locale;

import common.medias.utils.L;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/9/4<br>
 * Time: 14:25<br>
 * <P>DESC:
 * FFMpeg命令、参数拼写者
 *
 * 常用命令:
 * <ul>
 *     <li>ffmpeg -i input.avi output.mp4 视频容器(封装格式)的转换</li>
 *     <li>ffmpeg -i xxx.mp4 -acodec copy -vn output.aac 提取音频(把视频 剔除)</li>
 *     <li>ffmpeg -i input.mp4 -vcodec copy -an output.mp4 提取视频(把音频/轨 剔除)</li>
 *     <li>ffmpeg -ss 00:00:15 -t 00:00:05 -i input.mp4 -vcodec copy -acodec copy output.mp4 视频剪切</li>
 *     <li>ffmpeg -i input.mp4 -b:v 2000k -bufsize 2000k output.mp4 码率控制</li>
 *     <li>ffmpeg -i input.mp4 -vcodec h264 output.mp4 视频编码格式转换</li>
 *     <li>ffmpeg -i input.mp4 -vf scale=960:540 output.mp4 绽放视频</li>
 *     <li>ffmpeg -i input.mp4 -i iQIYI_logo.png -filter_complex overlay output.mp4 给视频添加logo</li>
 *     <li>ffmpeg -i input.mp4 output.yuv 输入yuv原始数据</li>
 *     <li>...</li>
 * </ul>
 * </p>
 * ******************(^_^)***********************
 */
public class FFMpegCmdSpeller extends ACmdLineArgsSpeller<FFMpegCmdSpeller> {
    public static final String CMD_HEADER = "ffmpeg";
    private static final long serialVersionUID = 4137194703376146014L;

    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public FFMpegCmdSpeller() {
//        add(CMD_HEADER);//因为FFMpeg 的命令行都是以"ffmpeg"开头 removed by fee   ffmpeg 不需要拼接
    }

    /**
     * 拼接 ffmpeg命令的头部
     * @return self
     */
    public FFMpegCmdSpeller ffmpeg() {
        if (!contains(CMD_HEADER)) {
            add(CMD_HEADER);
        }
        return self();
    }
    /**
     * 追加设定输入流 参数 "-i"
     * @return self
     */
    public FFMpegCmdSpeller _i(){
        add("-i");
        return this;
    }

    /**
     * -i xxx.mp4
     * 配置输入媒体
     * @param inputStreamPath 要处理的媒体文件路径; eg.: xxx.mp4/ xx.aac
     * @return self
     */
    public FFMpegCmdSpeller _i(String inputStreamPath) {
        _i().append(inputStreamPath);
        return this;
    }
    /**
     * -f 设定输入/输出格式
     * @return self
     */
    public FFMpegCmdSpeller _f() {
        add("-f");
        return this;
    }

    /**
     * Force input or output file format.
     * The format is normally auto detected for input files and guessed from the file extension for output files,
     * so this option is not needed in most cases.
     * @param format 好象是指路 封装格式；如 mp3/mp4/avi
     * @return self
     */
    public FFMpegCmdSpeller _f(String format) {
        if (format != null && !format.isEmpty()) {
            _f().append(format);
        }
        return this;
    }
    /**
     * -ss 开始时间
     * @return self
     */
    public FFMpegCmdSpeller _ss() {
        add("-ss");
        return this;
    }

    /**
     * -ss 00:00:05
     * 配置开始时间
     * @param startTime 格式: xx:xx:xx |"0"(从0秒开始)
     * @return self
     */
    public FFMpegCmdSpeller _ss(String startTime) {
        _ss().append(startTime);
        return this;
    }

    /**
     * -t hh:mm:ss
     * 配置需要的时间
     * @param needTime 所需要的时间; 格式：hh:mm:ss |"3"(到第3秒)
     * @return self
     */
    public FFMpegCmdSpeller _t(String needTime) {
        append("-t").append(needTime);
        return this;
    }
    /**
     * -acodec 设定声音编解码器，未设定时则使用与输入流相同的编解码器
     */
    public FFMpegCmdSpeller _acodec() {
        add("-acodec");
        return this;
    }

    public FFMpegCmdSpeller _acodec(String codecFormat) {
        _acodec();
        if (codecFormat != null && !codecFormat.isEmpty()) {
            append(codecFormat);
        }
        return this;
    }

    /**
     * -vcodec 设定视频编解码器，未设定时则使用与输入流相同的编解码器
     * @return self
     */
    public FFMpegCmdSpeller _vcodec() {
        add("-vcodec");
        return this;
    }

    public FFMpegCmdSpeller _vcodec(String codecFormat) {
        _vcodec();
        if (codecFormat != null && !codecFormat.isEmpty()) {
            append(codecFormat);
        }
        return this;
    }
    public FFMpegCmdSpeller copy() {
        add("copy");
        return this;
    }

    /**
     * 指定不处理 视频 参数 "-vn"
     * @return self
     */
    public FFMpegCmdSpeller _vn() {
        add("-vn");
        return this;
    }

    /**
     * -an 不处理音频
     * @return self
     */
    public FFMpegCmdSpeller _an() {
        add("-an");
        return this;
    }

    /**
     * -b 设定视频流量(码率)，默认为200Kbit/s ?
     * @return
     */
    public FFMpegCmdSpeller _b() {
        add("-b");
        return this;
    }

    /**
     * -r 设定帧速率，默认为25
     * @return
     */
    public FFMpegCmdSpeller _r() {
        add("-r");
        return this;
    }

    /**
     * -s 设定画面的宽与高
     * @return
     */
    public FFMpegCmdSpeller _s() {
        add("-s");
        return this;
    }

    /**
     * 设定画面的比例
     * @return
     */
    public FFMpegCmdSpeller _aspect() {
        add("-aspect");
        return this;
    }


    /**
     * -ar 设定采样率
     * @param audioSampleRate 音频采样率；eg.: 44100,48000,16000
     * @return self
     */
    public FFMpegCmdSpeller _ar(int audioSampleRate) {
        add("-ar");
        if (audioSampleRate > 0) {
            append(audioSampleRate);
        }
        return this;
    }

    /**
     * -ac 设定声音的Channel数
     * @param audioChannelCount 音频通道数量; eg.: 1;2
     * @return self
     */
    public FFMpegCmdSpeller _ac(int audioChannelCount) {
        add("-ac");
        if (audioChannelCount > 0) {
            append(audioChannelCount);
        }
        return this;
    }

    /**
     * -ab 设定音频比特率
     * @param audioBitRate 指定音频比特率；eg.: 16K
     * @return self
     */
    public FFMpegCmdSpeller _ab(String audioBitRate) {
        add("-ab");
        if (audioBitRate != null && !audioBitRate.isEmpty()) {
            append(audioBitRate);
        }
        return this;
    }

    /**
     * -vol <百分比> 设定音量
     * @param audioVolPercent 指定音频音量的百分比；eg.: 50,指定音量为原来的50%
     * @return
     */
    public FFMpegCmdSpeller _vol(int audioVolPercent) {
        add("-vol");
        if (audioVolPercent >= 0) {
            append(audioVolPercent);
        }
        return this;
    }
    /**
     * 增加直接执行FFMpeg命令的操作，
     * 注：因为所依赖框架设计原因，不可以有多个命令并发执行，主要是因为监听者是一个导致的，如果都不需要监听结果的话，可能可以多个实例执行
     * @param mediaDuration 操作的媒体文件时长duration,主要用于计算处理进度
     * @param executeCallback 命令执行的回调如果不为 null则为异步执行; 为null则为同步执行
     */
    public boolean selfExecute(long mediaDuration, SingleExecuteCallback executeCallback) {
        String[] cmdLineArgs = toArray(new String[]{});
        StringBuilder sb = new StringBuilder();
        int argsCount = cmdLineArgs.length;
        for (int i = 0; i < argsCount; i++) {
            String oneArg = cmdLineArgs[i];
            sb.append(oneArg);
            if (i != argsCount - 1) {
                sb.append(" ");
            }
        }
        String wholeCmdStr = sb.toString();
        L.i("FFMpegCmdSpeller",wholeCmdStr);
        if (executeCallback != null) {
            AsyncSingleFFmpegExecuteTask aSyncTask = new AsyncSingleFFmpegExecuteTask(wholeCmdStr, executeCallback);
            aSyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else {
            int rec = FFmpeg.execute(cmdLineArgs);
            boolean isOk = rec == 0;
            if (!isOk) {
                L.i(Config.TAG, L.formatStr("Command execution failed with rc=%d and the output below.", rec));
                Config.printLastCommandOutput(Log.INFO);
            }
            return rec == 0;
        }
        return true;
    }

    public void outPut(String outputResultFilePath) {
        append(outputResultFilePath);
    }

    /**
     * -filter_complex ffmpeg滤镜功能，非常强大
     * 参考<a>http://ffmpeg.org/ffmpeg.html#filter_005fcomplex_005foption</a>
     * @return self
     */
    public FFMpegCmdSpeller _filterComplex() {
        append("-filter_complex");
        return this;
    }

    /**
     * amix=inputs=2:duration=first:dropout_transition=2
     * @param inputsCount 输入流数量
     * @param durationDependOnWhat 混音结果时音频时长依赖何种
     * @return self
     */
    public FFMpegCmdSpeller amixArgs(int inputsCount, String durationDependOnWhat) {
        String amixArgsFormat = "amix=inputs=%d:duration=%s:dropout_transition=%d";
        String amixArgsStr = String.format(Locale.getDefault(), amixArgsFormat, inputsCount, durationDependOnWhat, inputsCount);
        append(amixArgsStr);
        return this;
    }

    /**
     * -y 需要覆盖
     * @return self
     */
    public FFMpegCmdSpeller _y() {
        add("-y");
        return this;
    }

    public void cancelExecute() {
        FFmpeg.cancel();
    }
}
