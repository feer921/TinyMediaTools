package common.medias.ffmpeg;


import androidx.annotation.WorkerThread;
import java.util.concurrent.CountDownLatch;
import common.medias.utils.L;


/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2019/9/4<br>
 * Time: 15:28<br>
 * <P>DESC:
 * 使用场景为：当执行的任务是异步的，而调用处又希望同步获取执行的结果时，该类则充当阻塞在调用处并且等待执行结果
 * </p>
 * ******************(^_^)***********************
 */
public class SyncWaitFFmpegCmdTask implements ACmdLineArgsSpeller.ICmdExecuteCallBack {
    private final String TAG = "SyncWaitFFmpegCmdTask";
    private CountDownLatch countDownLatch;

    private volatile boolean isResultOk;

    public SyncWaitFFmpegCmdTask() {
        countDownLatch = new CountDownLatch(1);
    }

    /**
     * 在哪调用就在哪阻塞
     * @throws InterruptedException
     */
    public void aWait() throws InterruptedException {
        countDownLatch.await();
    }

    /**
     * 命令执行失败
     *
     * @param failureInfo 失败信息
     */
    @Override
    public void onFailure(String failureInfo) {
        L.e(TAG, "-->onFailure()");
        isResultOk = false;
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    @WorkerThread
    @Override
    public void onSuccess() {
        L.i(TAG, "-->onSuccess()");
        isResultOk = true;
        if (countDownLatch != null) {
            countDownLatch.countDown();
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



//    @WorkerThread
//    @Override
//    public void onProgress(float progress) {
//        L.d(TAG, "-->onProgress() progress = " + progress);
//    }

    public boolean isExecuteOk() {
        return isResultOk;
    }
}
