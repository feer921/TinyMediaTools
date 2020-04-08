package common.medias.systemcodec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/8/9<br>
 * Time: 17:01<br>
 * <P>DESC:
 * 音频混合——混音
 * </p>
 * ******************(^_^)***********************
 */
public class AudioMixer {

    public static boolean mixAudios(String mixResultFilePath, String... rawAudioFiles) {
        if (rawAudioFiles == null || mixResultFilePath == null) {
            return false;
        }
        int fileSize = rawAudioFiles.length;
        if (fileSize < 2) {
            return false;
        }
        FileInputStream[] audioFileStreams = new FileInputStream[fileSize];
        File audioFile = null;
        FileInputStream inputStream;
        byte[][] allAudioBytes = new byte[fileSize][];
        boolean[] streamDoneArray = new boolean[fileSize];
        byte[] buffer = new byte[512];
        int offset;
        FileOutputStream fos = null;
        boolean mixSuc = false;
        try {
            for (int fileIndex = 0; fileIndex < fileSize; ++fileIndex) {
                audioFile = new File(rawAudioFiles[fileIndex]);
                audioFileStreams[fileIndex] = new FileInputStream(audioFile);
            }
            while (true) {
                for (int streamIndex = 0; streamIndex < fileSize; ++streamIndex) {
                    inputStream = audioFileStreams[streamIndex];
                    if (!streamDoneArray[streamIndex] && (offset = inputStream.read(buffer)) != -1) {//表示当前该文件没有读取完成
                        allAudioBytes[streamIndex] = Arrays.copyOf(buffer, buffer.length);
                    }
                    else {
                        streamDoneArray[streamIndex] = true;//标记当前文件流读取完成
                        allAudioBytes[streamIndex] = new byte[512];
                    }
                }
                byte[] mixBytes = mixRawAudioBytes(allAudioBytes);//mixBytes 就是混合后的数据
                if (mixBytes != null) {
                    if (fos == null) {
                        fos = new FileOutputStream(mixResultFilePath, true);
                    }
                    fos.write(mixBytes);
                }
                boolean done = true;
                for (boolean doneState : streamDoneArray) {
                    if (!doneState) {
                        done = false;
                    }
                }
                if (done) {
                    break;
                }
            }
            if (fos != null) {
                fos.flush();
            }
            mixSuc = true;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                for (FileInputStream audioFileStream : audioFileStreams) {
                    if (audioFileStream != null) {
                        audioFileStream.close();
                    }
                }
                if (fos != null) {
                    fos.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mixSuc;
    }

    private static byte[] mixRawAudioBytes(byte[][] allRawAudioBytes) {
        if (allRawAudioBytes == null || allRawAudioBytes.length == 0) {
            return null;
        }
        byte[] oneMixAudioBytes = allRawAudioBytes[0];
        if (allRawAudioBytes.length == 1) {
            return oneMixAudioBytes;
        }
        int allRawAudioBytesLen = allRawAudioBytes.length;
        for (int i = 0; i < allRawAudioBytesLen; ++i) {
            if (allRawAudioBytes[i].length != oneMixAudioBytes.length) {
                return null;
            }
        }
        int row = allRawAudioBytesLen;
        int coloum = oneMixAudioBytes.length / 2;
        short[][] sMulRoadAudios = new short[row][coloum];
        for (int r = 0; r < row; ++r) {
            for (int c = 0; c < coloum; ++c) {
                sMulRoadAudios[r][c] = (short) ((allRawAudioBytes[r][c*2] &0xff) |(allRawAudioBytes[r][c*2+1]&0xff)<<8);
            }
        }
        short[] sMixAudio = new short[coloum];
        int mixVal;
        int sr = 0;
        for (int sc = 0; sc < coloum; ++sc) {
            mixVal = 0;
            sr = 0;
            for (; sr < row; ++sr) {
                mixVal+= sMulRoadAudios[sr][sc];
            }
            sMixAudio[sc] = (short) (mixVal / row);
        }
        for (sr = 0; sr < coloum; ++sr) {
            oneMixAudioBytes[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
            oneMixAudioBytes[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
        }
        return oneMixAudioBytes;
    }

    public boolean decodeAudioFile(String theAudioFile) {
        return false;
    }


}
