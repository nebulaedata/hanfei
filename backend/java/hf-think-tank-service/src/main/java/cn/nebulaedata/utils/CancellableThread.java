package cn.nebulaedata.utils;

import java.util.concurrent.atomic.AtomicBoolean;

public class CancellableThread<T> extends Thread {
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final Callback<T> callback;

    public CancellableThread(Callback<T> callback) {
        this.callback = callback;
    }

    public void cancel() {
        isCancelled.set(true);
    }

    @Override
    public void run() {

        Object result = null;
        try {
            if (isCancelled.get()) { // 失败1
                // 如果线程在开始执行之前被取消，则直接返回
                throw new InterruptedException();
            }
            // 成功
            result = executeTask();
        } catch (InterruptedException e) {
            // 在执行任务过程中被中断
            result = "ERR";
        } finally {
            // 线程执行完毕且未被取消，执行回调
            callback.onComplete(result);
        }

    }

    // 需要在子类中实现具体的任务逻辑
    protected Object executeTask() throws InterruptedException {
        // 执行具体的任务逻辑
        return callback.onExecute();
    }


//    // 需要在子类中实现具体的任务逻辑
//    protected Object executeTask(Object o,String str,Object ... objects) throws InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        // 执行具体的任务逻辑
//
//        Method method = o.getClass().getMethod(str);
//        Class [] classes =new Class[objects.length];
//        Object invoke = method.invoke(method ),;
//        return callback.onExecute();
//    }


    // 定义回调接口
    public interface Callback<Object> {
        void onComplete(java.lang.Object result);

        java.lang.Object onExecute();
    }


    public static void main(String[] args) {

        CancellableThread<String> thread = new CancellableThread<>(new Callback<String>() {
            @Override
            public void onComplete(Object result) {
                System.out.println(result);
            }

            @Override
            public Object onExecute() {
                return null;
            }

        });
        thread.setName("nihao");
        thread.start();

        // 模拟一段时间后取消线程
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        thread.cancel();
        thread.isAlive();

    }
}