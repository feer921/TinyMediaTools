package common.medias.systemcodec;

/**
 * 
 * 获取录音的音频流,用于拓展的处理
 */
public interface RecordStreamListener {
    int STATE_BEGIN_RECORD = 10;
    int STATE_STOP_RECORD = 11;
    int STATE_WORK_OVER = 12;
    int STATE_WORK_FAIL = 13;
    void onRecordData(byte[] recordedDatas, int beginFlag, int end);
}
