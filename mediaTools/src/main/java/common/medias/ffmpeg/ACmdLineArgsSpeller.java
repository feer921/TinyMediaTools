package common.medias.ffmpeg;

import java.util.ArrayList;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/9/4<br>
 * Time: 14:49<br>
 * <P>DESC:
 * 一个命令行 命令参数拼写者
 * </p>
 * ******************(^_^)***********************
 */
public class ACmdLineArgsSpeller<I extends ACmdLineArgsSpeller> extends ArrayList<String> {


    private static final long serialVersionUID = 5262442616094181842L;

    public I append(String arg) {
        add(arg);
        return self();
    }



    public I append(int i) {
        this.add(i + "");
        return self();
    }

    public I append(float f) {
        this.add(f + "");
        return self();
    }

    public I append(StringBuilder sb) {
        this.add(sb.toString());
        return self();
    }

    public I append(String[] ss) {
        for (String s:ss) {
            if(!s.replace(" ","").equals("")) {
                this.add(s);
            }
        }
        return self();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int argsSize = size();
        for (int i = 0; i < argsSize; i++) {
            String oneArg = get(i);
            sb.append(oneArg);
            if (i != argsSize - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public I clearCmdStr() {
        clear();
        return self();
    }

    protected I self() {
        return (I) this;
    }


    /**
     * 将所拼写完的各个参数 转成 命令行一行的字符串
     * @return toString() def implement
     */
    public String toCmdLineString() {
        return toString();
    }


    public void selfExecute() {
        //here do nothing,due to don't know how to execute
    }

    /**
     * 退出命令行
     */
    public void exit() {
        //here do nothing,due to don't know how to exit
    }

    public interface ICmdExecuteCallBack{
        /**
         * 命令执行失败
         * @param failureInfo 失败信息
         */
        void onFailure(String failureInfo);

        /**
         * 命令执行成功
         */
        void onSuccess();

        /**
         * 命令行阻塞回调给用户 要输入信息继续
         * @param cmdLineAsk 命令行的询问
         * @return 用户要输入的信息；eg.: 用户输入"Y"
         */
        String onBlockAskAndSubmit(String cmdLineAsk);

    }
}
