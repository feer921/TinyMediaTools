package common.medias.utils;

import android.media.ThumbnailUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * *****************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2020/3/22<br>
 * Time: 18:08<br>
 * <P>DESC:
 * 线程池工具类
 * </p>
 * ******************(^_^)***********************
 */
public final class ThreadPoolUitl {
    private static final int MAX_POOL_SIZE = 5;          //最大线程池的数量
    private static final int KEEP_ALIVE_TIME = 1;        //存活的时间
    private static final TimeUnit UNIT = TimeUnit.HOURS; //时间单位
    private volatile static ThreadPoolUitl me;
    /**
     * 核心线程池的数量，同时能执行的线程数量，默认3个
     */
    private int corePoolSize = 3;
    private ThreadPoolExecutor executorService;
    private ThreadPoolUitl() {

    }

    public static ThreadPoolUitl getMe() {
        if (me == null) {
            synchronized (ThumbnailUtils.class) {
                if (me == null) {
                    me = new ThreadPoolUitl();
                }
            }
        }
        return me;
    }
    public ThreadPoolExecutor getExecutor() {
        if (executorService == null) {
            synchronized (ThreadPoolUitl.class) {
                if (executorService == null) {
                    executorService = new ThreadPoolExecutor(
                            corePoolSize,
                            MAX_POOL_SIZE,
                            KEEP_ALIVE_TIME,
                            UNIT,
                            new PriorityBlockingQueue<Runnable>(),//无限容量的缓冲队列
                            Executors.defaultThreadFactory(),//线程创建工厂
                            new ThreadPoolExecutor.AbortPolicy()//继续超出上限的策略，阻止
                    );
                }
            }
        }
        return executorService;
    }



    public void setCorePoolSize(int theCorePoolSize) {
        this.corePoolSize = theCorePoolSize;
    }


    public void excute(Runnable task) {
        if (task != null) {
            getExecutor().execute(task);
        }
    }

    public void removeTask(Runnable task) {
        if (task != null) {
            getExecutor().remove(task);
        }
    }

}
