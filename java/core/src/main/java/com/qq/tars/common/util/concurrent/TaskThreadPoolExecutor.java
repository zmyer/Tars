/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qq.tars.common.util.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Same as a java.util.concurrent.ThreadPoolExecutor but implements a much more efficient
 * {@link #getSubmittedCount()} method, to be used to properly handle the work queue.
 * If a RejectedExecutionHandler is not specified a default one will be configured
 * and that one will always throw a RejectedExecutionException
 *
 * @author fhanik
 */
// TODO: 17/4/15 by zmyer
public class TaskThreadPoolExecutor extends java.util.concurrent.ThreadPoolExecutor {

    /**
     * The number of tasks submitted but not yet finished. This includes tasks
     * in the queue and tasks that have been handed to a worker thread but the
     * latter did not start executing the task yet.
     * This number is always greater or equal to {@link #getActiveCount()}.
     */
    //提交任务的数量
    private final AtomicInteger submittedCount = new AtomicInteger(0);
    //最近上下文关闭时间戳
    private final AtomicLong lastContextStoppedTime = new AtomicLong(0L);

    /**
     * Most recent time in ms when a thread decided to kill itself to avoid
     * potential memory leaks. Useful to throttle the rate of renewals of
     * threads.
     */
    //
    private final AtomicLong lastTimeThreadKilledItself = new AtomicLong(0L);

    /**
     * Delay in ms between 2 threads being renewed. If negative, do not renew threads.
     */
    private long threadRenewalDelay = 1000L;

    // TODO: 17/4/15 by zmyer
    public TaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
        BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    // TODO: 17/4/15 by zmyer
    public TaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
        RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    // TODO: 17/4/15 by zmyer
    public TaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory,
            new RejectHandler());
    }

    // TODO: 17/4/15 by zmyer
    public TaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
        BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new RejectHandler());
    }

    // TODO: 17/4/15 by zmyer
    public long getThreadRenewalDelay() {
        return threadRenewalDelay;
    }

    // TODO: 17/4/15 by zmyer
    public void setThreadRenewalDelay(long threadRenewalDelay) {
        this.threadRenewalDelay = threadRenewalDelay;
    }

    @Override
    // TODO: 17/4/15 by zmyer
    protected void afterExecute(Runnable r, Throwable t) {
        //递减提交的任务数量
        submittedCount.decrementAndGet();
        if (t == null) {
            stopCurrentThreadIfNeeded();
        }
    }

    /**
     * If the current thread was started before the last time when a context was
     * stopped, an exception is thrown so that the current thread is stopped.
     */
    // TODO: 17/4/15 by zmyer
    protected void stopCurrentThreadIfNeeded() {
        if (currentThreadShouldBeStopped()) {
            //更新线程kill的时间戳
            long lastTime = lastTimeThreadKilledItself.longValue();
            if (lastTime + threadRenewalDelay < System.currentTimeMillis()) {
                if (lastTimeThreadKilledItself.compareAndSet(lastTime,
                    System.currentTimeMillis() + 1)) {
                    // OK, it's really time to dispose of this thread

                    final String msg = "Stopping thread to avoid potential memory leaks after a context was stopped.";

                    Thread.currentThread().setUncaughtExceptionHandler(
                        new UncaughtExceptionHandler() {

                            public void uncaughtException(Thread t,
                                Throwable e) {
                                // yes, swallow the exception
                                e.printStackTrace();
                            }
                        });
                    throw new RuntimeException(msg);
                }
            }
        }
    }

    // TODO: 17/4/15 by zmyer
    protected boolean currentThreadShouldBeStopped() {
        if (threadRenewalDelay >= 0 && Thread.currentThread() instanceof TaskThread) {
            //读取当前的执行线程对象
            TaskThread currentTaskThread = (TaskThread) Thread.currentThread();
            if (currentTaskThread.getCreationTime() < this.lastContextStoppedTime.longValue()) {
                return true;
            }
        }
        return false;
    }

    // TODO: 17/4/15 by zmyer
    public int getSubmittedCount() {
        return submittedCount.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // TODO: 17/4/15 by zmyer
    public void execute(Runnable command) {
        execute(command, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Executes the given command at some time in the future.  The command
     * may execute in a new thread, in a pooled thread, or in the calling
     * thread, at the discretion of the <tt>Executor</tt> implementation.
     * If no threads are available, it will be added to the work queue.
     * If the work queue is full, the system will wait for the specified
     * time and it throw a RejectedExecutionException if the queue is still
     * full after that.
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be accepted for execution - the queue is full
     * @throws NullPointerException if command or unit is null
     */
    // TODO: 17/4/15 by zmyer
    public void execute(Runnable command, long timeout, TimeUnit unit) {
        //递增提交的任务数量
        submittedCount.incrementAndGet();
        try {
            //开始执行指令
            super.execute(command);
        } catch (RejectedExecutionException rx) {
            if (super.getQueue() instanceof TaskQueue) {
                //读取任务队列
                final TaskQueue queue = (TaskQueue) super.getQueue();
                try {
                    //将任务队列插入到等待列表中
                    if (!queue.force(command, timeout, unit)) {
                        //如果插入失败,则递减提交的任务数量
                        submittedCount.decrementAndGet();
                        throw new RejectedExecutionException("Queue capacity is full.");
                    }
                } catch (InterruptedException x) {
                    submittedCount.decrementAndGet();
                    Thread.interrupted();
                    throw new RejectedExecutionException(x);
                }
            } else {
                submittedCount.decrementAndGet();
                throw rx;
            }

        }
    }

    // TODO: 17/4/15 by zmyer
    public void contextStopping() {
        this.lastContextStoppedTime.set(System.currentTimeMillis());

        // save the current pool parameters to restore them later
        int savedCorePoolSize = this.getCorePoolSize();
        TaskQueue taskQueue = getQueue() instanceof TaskQueue ? (TaskQueue) getQueue() : null;
        if (taskQueue != null) {
            // note by slaurent : quite oddly threadPoolExecutor.setCorePoolSize
            // checks that queue.remainingCapacity()==0. I did not understand
            // why, but to get the intended effect of waking up idle threads, I
            // temporarily fake this condition.
            taskQueue.setForcedRemainingCapacity(0);
        }

        // setCorePoolSize(0) wakes idle threads
        this.setCorePoolSize(0);

        // wait a little so that idle threads wake and poll the queue again,
        // this time always with a timeout (queue.poll() instead of
        // queue.take())
        // even if we did not wait enough, TaskQueue.take() takes care of timing
        // out, so that we are sure that all threads of the pool are renewed in
        // a limited time, something like 
        // (threadKeepAlive + longest request time)
        try {
            Thread.sleep(200L);
        } catch (InterruptedException e) {
            // yes, ignore
        }

        if (taskQueue != null) {
            // ok, restore the state of the queue and pool
            taskQueue.setForcedRemainingCapacity(null);
        }
        this.setCorePoolSize(savedCorePoolSize);
    }

    // TODO: 17/4/15 by zmyer
    private static class RejectHandler implements RejectedExecutionHandler {

        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor executor) {
            throw new RejectedExecutionException();
        }

    }

}
