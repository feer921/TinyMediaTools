package common.medias.systemcodec;

import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;

import common.medias.utils.L;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/8/21<br>
 * Time: 17:36<br>
 * <P>DESC:
 * AEC(回声消除)
 * 和 NS(噪声抑制)
 * </p>
 * ******************(^_^)***********************
 */
public class AECerAndNoiseSuppressor {

    private AcousticEchoCanceler mAEC;

    /**
     * 噪音抑制(降噪)
     */
    private NoiseSuppressor mNoiseSuppressor;

    /**
     * 是否需要使用AEC
     */
    private boolean isNeedAEC;

    /**
     * 是否需要使用噪音抑制
     */
    private boolean isNeedNoiseSuppress;

    /**
     * 当前的音频session id
     */
    private int curAudioSessionId;

    public static boolean isSupportNoiseSuppress() {
        return NoiseSuppressor.isAvailable();
    }

    public static boolean isSupportAEC() {
        return AcousticEchoCanceler.isAvailable();
    }

    private boolean initNoiseSuppressor(int sessionId) {
        if (mNoiseSuppressor == null) {
            mNoiseSuppressor = NoiseSuppressor.create(sessionId);
        }
        return mNoiseSuppressor != null;
    }

    private boolean enableNoiseSuppressor(boolean isToEnable) {
        if (mNoiseSuppressor == null) {
            return false;
        }
        return mNoiseSuppressor.setEnabled(isToEnable) == NoiseSuppressor.SUCCESS;
    }

    private boolean releaseNoiseSuppressor() {
        if (mNoiseSuppressor != null) {
            mNoiseSuppressor.setEnabled(false);
            mNoiseSuppressor.release();
            mNoiseSuppressor = null;
            return true;
        }
        return false;
    }

    private boolean initAec(int audioSession) {
//        if (mAEC != null) {
//            return false;
//        }
//        if (audioSession == -1) {
//            //
//        }
        mAEC = AcousticEchoCanceler.create(audioSession);
        return mAEC != null;
    }

    private boolean setAecEnable(boolean isToEnable) {
        if (mAEC == null) {
            return false;
        }
        mAEC.setEnabled(isToEnable);
        return mAEC.getEnabled();
    }

    private boolean releaseAec() {
        if (mAEC == null) {
            return false;
        }
        mAEC.setEnabled(false);
        mAEC.release();
        mAEC = null;//直接赋值为null
        return true;
    }

    public AECerAndNoiseSuppressor needAec(boolean isNeedAEC) {
        this.isNeedAEC = isNeedAEC;
        return this;
    }

    public AECerAndNoiseSuppressor needNoiseSuppress(boolean isNeedNoiseSuppress) {
        this.isNeedNoiseSuppress = isNeedNoiseSuppress;
        return this;
    }

    public AECerAndNoiseSuppressor effectTheAudioSessionId(int theAudioSessionId) {
        this.curAudioSessionId = theAudioSessionId;
        return this;
    }

    public boolean startTheHardWork() {
        boolean isStartOk = false;
        boolean isSupportAEC = isSupportAEC();
        boolean isSupportNS = isSupportNoiseSuppress();
        try {//该功能为辅助功能，为了不影响外部的功能流程，内部自己catch掉可能的异常
            if (isNeedAEC && isSupportAEC) {
                initAec(curAudioSessionId);
                isStartOk = setAecEnable(true);
            }

            if (isNeedNoiseSuppress && isSupportNS) {
                initNoiseSuppressor(curAudioSessionId);
                boolean isNsOk = enableNoiseSuppressor(true);
                if (!isStartOk) {
                    isStartOk = isNsOk;
                }
            }
        } catch (Exception ignore) {
        }
        L.i("AECerAndNoiseSuppressor", "-->startTheHardWork() isStartOk = " + isStartOk
                + " curAudioSessionId = " + curAudioSessionId + " isSupportAEC = " + isSupportAEC
                + "  isSupportNS = " + isSupportNS
        );
        return isStartOk;
    }


    public void stopTheHardWork(/*boolean isRelease*/) {
        try {
            releaseAec();
            releaseNoiseSuppressor();
        } catch (Exception ignore) {
        }
    }

}
