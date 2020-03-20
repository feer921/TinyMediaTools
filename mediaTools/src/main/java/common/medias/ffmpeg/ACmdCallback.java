package common.medias.ffmpeg;

import com.arthenica.mobileffmpeg.util.SingleExecuteCallback;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2020/3/18<br>
 * Time: 15:08<br>
 * <P>DESC:
 * 一个命令执行的回调
 * </p>
 * ******************(^_^)***********************
 */
public abstract class ACmdCallback implements SingleExecuteCallback,ACmdLineArgsSpeller.ICmdExecuteCallBack{
    private String theCmdStr;

    public ACmdCallback(String theCmdStr) {
        this.theCmdStr = theCmdStr;
    }
    @Override
    public void apply(int returnCode, String executeOutput) {
        if (returnCode == 0) {
            onSuccess();
        }
        else {
            onFailure(executeOutput);
        }
    }

    /**
     * 命令行阻塞回调给用户 要输入信息继续
     *
     * @param cmdLineAsk 命令行的询问
     * @return 用户要输入的信息；eg.: 用户输入"Y"
     */
    @Override
    public String onBlockAskAndSubmit(String cmdLineAsk) {
        return null;
    }
}
