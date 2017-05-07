/**
 * Tencent is pleased to support the open source community by making Tars available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.qq.tars.common.support;

import com.qq.tars.common.util.Constants;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// TODO: 17/4/15 by zmyer
public final class ScheduledExecutorManager implements ScheduledExecutorService {
    //单例对象
    private final static ScheduledExecutorManager instance = new ScheduledExecutorManager();
    //线程池对象
    private ScheduledThreadPoolExecutor taskExecutor = new ScheduledThreadPoolExecutor(4);
    //重入锁对象
    private final Lock lock = new ReentrantLock();

    // TODO: 17/4/18 by zmyer
    private ScheduledExecutorManager() {
    }

    // TODO: 17/4/18 by zmyer
    public static ScheduledExecutorManager getInstance() {
        return instance;
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public void shutdown() {
        taskExecutor.shutdown();
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public List<Runnable> shutdownNow() {
        return taskExecutor.shutdownNow();
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public boolean isShutdown() {
        return taskExecutor.isShutdown();
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public boolean isTerminated() {
        return taskExecutor.isTerminated();
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return taskExecutor.awaitTermination(timeout, unit);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        checkMaxSize();
        return taskExecutor.submit(task);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        checkMaxSize();
        return taskExecutor.submit(task, result);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public Future<?> submit(Runnable task) {
        checkMaxSize();
        return taskExecutor.submit(task);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return taskExecutor.invokeAll(tasks);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
        TimeUnit unit) throws InterruptedException {
        return taskExecutor.invokeAll(tasks, timeout, unit);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public <T> T invokeAny(
        Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return taskExecutor.invokeAny(tasks);
    }

    // TODO: 17/4/18 by zmyer

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
        TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return taskExecutor.invokeAny(tasks, timeout, unit);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public void execute(Runnable command) {
        checkMaxSize();
        taskExecutor.execute(command);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        checkMaxSize();
        return taskExecutor.schedule(command, delay, unit);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        checkMaxSize();
        return taskExecutor.schedule(callable, delay, unit);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
        TimeUnit unit) {
        checkMaxSize();
        return taskExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
        long delay, TimeUnit unit) {
        checkMaxSize();
        return taskExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    // TODO: 17/4/18 by zmyer
    private void checkMaxSize() throws RejectedExecutionException {
        int count = taskExecutor.getQueue().size();
        lock.lock();
        try {
            int maxQueueSize = Constants.default_background_queuesize;
            if (count >= maxQueueSize) {
                throw new RejectedExecutionException("the queue is full, the max queue size is " + maxQueueSize);
            }
        } finally {
            lock.unlock();
        }
    }
}
