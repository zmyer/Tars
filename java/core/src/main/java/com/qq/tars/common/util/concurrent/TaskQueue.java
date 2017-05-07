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

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * As task queue specifically designed to run with a thread pool executor.
 * The task queue is optimised to properly utilize threads within
 * a thread pool executor. If you use a normal queue, the executor will spawn threads
 * when there are idle threads and you wont be able to force items unto the queue itself
 *
 * @author fhanik
 */
// TODO: 17/4/15 by zmyer
public class TaskQueue extends LinkedBlockingQueue<Runnable> {

    private static final long serialVersionUID = 1L;
    //任务队列关联的执行对象
    private TaskThreadPoolExecutor parent = null;

    // no need to be volatile, the one times when we change and read it occur in
    // a single thread (the one that did stop a context and fired listeners)
    //保留的队列空间
    private Integer forcedRemainingCapacity = null;

    // TODO: 17/4/25 by zmyer
    public TaskQueue() {
        super();
    }

    // TODO: 17/4/25 by zmyer
    public TaskQueue(int capacity) {
        super(capacity);
    }

    // TODO: 17/4/25 by zmyer
    public TaskQueue(Collection<? extends Runnable> c) {
        super(c);
    }

    // TODO: 17/4/25 by zmyer
    public void setParent(TaskThreadPoolExecutor tp) {
        parent = tp;
    }

    // TODO: 17/4/25 by zmyer
    public boolean force(Runnable o) {
        if (parent.isShutdown())
            throw new RejectedExecutionException("Executor not running, can't force a command into the queue");
        return super.offer(o); //forces the item onto the queue, to be used if the task is rejected
    }

    // TODO: 17/4/25 by zmyer
    public boolean force(Runnable o, long timeout, TimeUnit unit) throws InterruptedException {
        if (parent.isShutdown())
            throw new RejectedExecutionException("Executor not running, can't force a command into the queue");
        return super.offer(o, timeout, unit); //forces the item onto the queue, to be used if the task is rejected
    }

    // TODO: 17/4/25 by zmyer
    @Override
    public boolean offer(Runnable o) {
        //we can't do any checks
        if (parent == null)
            return super.offer(o);
        //we are maxed out on threads, simply queue the object
        if (parent.getPoolSize() == parent.getMaximumPoolSize())
            return super.offer(o);
        //we have idle threads, just add it to the queue
        if (parent.getSubmittedCount() < (parent.getPoolSize()))
            return super.offer(o);
        //if we have less threads than maximum force creation of a new thread
        if (parent.getPoolSize() < parent.getMaximumPoolSize())
            return false;
        //if we reached here, we need to add it to the queue
        return super.offer(o);
    }

    // TODO: 17/4/25 by zmyer
    @Override
    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        Runnable runnable = super.poll(timeout, unit);
        if (runnable == null && parent != null) {
            // the poll timed out, it gives an opportunity to stop the current
            // thread if needed to avoid memory leaks.
            //暂停当前的线程
            parent.stopCurrentThreadIfNeeded();
        }
        //返回从任务队列中读取到的任务对象
        return runnable;
    }

    // TODO: 17/4/25 by zmyer
    @Override
    public Runnable take() throws InterruptedException {
        if (parent != null && parent.currentThreadShouldBeStopped()) {
            //直接从任务队列中拉取任务
            return poll(parent.getKeepAliveTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
            // yes, this may return null (in case of timeout) which normally
            // does not occur with take()
            // but the ThreadPoolExecutor implementation allows this
        }
        return super.take();
    }

    // TODO: 17/4/25 by zmyer
    @Override
    public int remainingCapacity() {
        if (forcedRemainingCapacity != null) {
            // ThreadPoolExecutor.setCorePoolSize checks that
            // remainingCapacity==0 to allow to interrupt idle threads
            // I don't see why, but this hack allows to conform to this
            // "requirement"
            return forcedRemainingCapacity.intValue();
        }
        return super.remainingCapacity();
    }

    // TODO: 17/4/25 by zmyer
    public void setForcedRemainingCapacity(Integer forcedRemainingCapacity) {
        this.forcedRemainingCapacity = forcedRemainingCapacity;
    }

}
