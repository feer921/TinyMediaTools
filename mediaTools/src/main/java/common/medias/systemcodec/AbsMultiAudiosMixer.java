package common.medias.systemcodec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/8/10<br>
 * Time: 17:36<br>
 * <P>DESC:
 * 多个音频文件mixer
 * </p>
 * ******************(^_^)***********************
 */
public abstract class AbsMultiAudiosMixer {
    private OnAudioMixListener mOnAudioMixListener;

    public static AbsMultiAudiosMixer createAudioMixer() {
        return new AverageAudioMixer();
    }

    public void setOnAudioMixListener(OnAudioMixListener l) {
        this.mOnAudioMixListener = l;
    }

    /**
     * <p>start to mix , you can call {@link #setOnAudioMixListener(OnAudioMixListener)} before this method to get mixed data.
     */
    public boolean mixAudios(String mixResultFilePath, String... rawAudioFiles) {
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
        boolean isMixSuc = false;
        try {
            for (int fileIndex = 0; fileIndex < fileSize; ++fileIndex) {
                audioFile = new File(rawAudioFiles[fileIndex]);
                audioFileStreams[fileIndex] = new FileInputStream(audioFile);
            }

            while (true) {
                for (int streamIndex = 0; streamIndex < fileSize; ++streamIndex) {
                    inputStream = audioFileStreams[streamIndex];
                    if (!streamDoneArray[streamIndex] && (offset = inputStream.read(buffer)) != -1) {
                        allAudioBytes[streamIndex] = Arrays.copyOf(buffer, buffer.length);
                    } else {
                        streamDoneArray[streamIndex] = true;
                        allAudioBytes[streamIndex] = new byte[512];
                    }
                }

                byte[] mixBytes = mixRawAudioBytes(allAudioBytes);
                if (mixBytes != null) {
                    boolean isHandled = false;
                    if (mOnAudioMixListener != null) {
                        isHandled = mOnAudioMixListener.onMixing(mixBytes);
                    }
                    if (!isHandled) {
                        if (fos == null) {
                            fos = new FileOutputStream(mixResultFilePath, true);
                        }
                        fos.write(mixBytes);
                    }
                }
                boolean done = true;
                for (boolean streamEnd : streamDoneArray) {
                    if (!streamEnd) {
                        done = false;
                    }
                }

                if (done) {
                    if (mOnAudioMixListener != null)
                        mOnAudioMixListener.onMixComplete();
                    break;
                }
            }
            if (fos != null) {
                fos.flush();
            }
            isMixSuc = true;
        } catch (IOException e) {
            e.printStackTrace();
            if (mOnAudioMixListener != null)
                mOnAudioMixListener.onMixError(1);
        } finally {
            try {
                for (FileInputStream in : audioFileStreams) {
                    if (in != null)
                        in.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isMixSuc;
    }

    abstract byte[] mixRawAudioBytes(byte[][] data);

    public interface OnAudioMixListener {
        /**
         * invoke when mixing, if you want to stop the mixing process, you can throw an AudioMixException
         *
         * @param mixBytes
         * @throws AudioMixException
         */
        boolean onMixing(byte[] mixBytes) throws IOException;

        void onMixError(int errorCode);

        /**
         * invoke when mix success
         */
        void onMixComplete();
    }

    public static class AudioMixException extends IOException {
        private static final long serialVersionUID = -1344782236320621800L;

        public AudioMixException(String msg) {
            super(msg);
        }
    }

}
