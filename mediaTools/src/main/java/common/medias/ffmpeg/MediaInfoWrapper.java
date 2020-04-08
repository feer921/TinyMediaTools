package common.medias.ffmpeg;

import com.arthenica.mobileffmpeg.MediaInformation;
import com.arthenica.mobileffmpeg.StreamInformation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.medias.utils.L;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2020/3/31<br>
 * Time: 14:54<br>
 * <P>DESC:
 * 媒体信息包装
 * </p>
 * ******************(^_^)***********************
 */
public class MediaInfoWrapper{

    /**
     * Format
     */
    private String format;
    /**
     * Start time, in milliseconds
     */
    private long startTime;
    /**
     * Path
     */
    private String path;

    private long duration;

    /**
     * Bitrate, kb/s
     */
    private long bitrate;

    /**
     * Metadata map
     */
    private Map<String, String> metadata;

    /**
     * List of streams
     */
    private List<StreamInformation> streams;

    /**
     * Raw unparsed media information
     */
    private String rawInformation;

    private MediaInformation rawMediaInfo;
    public MediaInfoWrapper(MediaInformation mediaInfor) {
        if (mediaInfor != null) {
            rawInformation = mediaInfor.getRawInformation();
            streams = mediaInfor.getStreams();
            Long bitrateL = mediaInfor.getBitrate();
            if (bitrateL != null) {
                this.bitrate = bitrateL;
            }
            Long durationL = mediaInfor.getDuration();
            if (durationL != null) {
                this.duration = durationL;
            }
            this.format = mediaInfor.getFormat();
            this.path = mediaInfor.getPath();
            Long startTimeL = mediaInfor.getStartTime();
            if (startTimeL != null) {
                this.startTime = startTimeL;
            }
        }
        this.rawMediaInfo = mediaInfor;
    }

    public MediaInfoWrapper() {

    }
    /**
     * Returns all metadata entries.
     *
     * @return set of metadata entries
     */
    public Set<Map.Entry<String, String>> getMetadataEntries() {
        return this.metadata != null ? metadata.entrySet() : null;
    }

    /**
     * Returns all streams.
     *
     * @return list of streams
     */
    public List<StreamInformation> getStreams() {
        return streams;
    }

    public String peekMetaData(String metaKey) {
        if (this.metadata == null && this.rawMediaInfo != null) {
            Set<Map.Entry<String, String>> infos = this.rawMediaInfo.getMetadataEntries();
            this.metadata = new HashMap<String, String>();
            for (Map.Entry<String, String> info : infos) {
                this.metadata.put(info.getKey(), info.getValue());
            }
        }
        if (this.metadata != null) {
            return metadata.get(metaKey);
        }
        return "";
    }

    public String getFormat() {
        return format;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getPath() {
        return path;
    }

    public long getDuration() {
        return duration;
    }

    public long getBitrate() {
        return bitrate;
    }

    public String getRawInformation() {
        return rawInformation;
    }

    @Override
    public String toString() {
        return "MediaInfoWrapper{" +
                "format='" + format + '\'' +
                ", startTime=" + startTime +
                ", path='" + path + '\'' +
                ", duration=" + duration +
                ", bitrate=" + bitrate +
                ", rawInformation='" + rawInformation + '\'' +
                '}';
    }

    public void printMetaDatas() {
        peekMetaData("");
        Set<Map.Entry<String,String>> metaEntries = getMetadataEntries();
        if (metaEntries != null) {
            for (Map.Entry<String, String> metadataEntry : metaEntries) {
                L.d("MediaInfoWraper", " key: " + metadataEntry.getKey() + " value: " + metadataEntry.getValue());
            }
        }
    }

    public void printStreamInfos() {
        if (streams != null) {
            for (StreamInformation stream : streams) {
                L.d("MediaInfoWraper", "-->printStreamInfos() " + stream.getSampleRate() + "  type = " + stream.getType());
            }
        }
    }
}
