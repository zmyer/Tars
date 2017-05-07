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

package com.qq.tars.net.client.ticket;

import com.qq.tars.net.client.Callback;
import com.qq.tars.net.core.Request;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// TODO: 17/4/15 by zmyer
public class Ticket<T> {
    //默认的ticket的数量
    public static final int DEFAULT_TICKET_NUMBER = -1;
    //同步屏障对象
    private CountDownLatch latch = new CountDownLatch(1);

    //应答消息对象
    private T response = null;
    //请求对象
    private Request request = null;
    //是否过期
    private volatile boolean expired = false;
    //超时时间
    protected long timeout = 1000;
    //ticket开启时间戳
    public long startTime = System.currentTimeMillis();
    //回调对象
    private Callback<T> callback = null;
    private int ticketNumber = -1;
    //ticket监听器
    private static TicketListener ticketListener = null;

    // TODO: 17/4/15 by zmyer
    public Ticket(Request request, long timeout) {
        this.request = request;
        this.ticketNumber = request.getTicketNumber();
        this.timeout = timeout;
    }

    // TODO: 17/4/15 by zmyer
    public Request request() {
        return this.request;
    }

    // TODO: 17/4/15 by zmyer
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        //在同步屏障上等待事件发送
        boolean status = this.latch.await(timeout, unit);
        //检查是否过期
        checkExpired();
        return status;
    }

    // TODO: 17/4/15 by zmyer
    public void await() throws InterruptedException {
        this.latch.await();
        checkExpired();
    }

    // TODO: 17/4/15 by zmyer
    public void expired() {
        this.expired = true;
        if (callback != null)
            //如果过期了,则直接调用过期接口
            callback.onExpired();
        //通知ticket已经过期了
        this.countDown();
        if (ticketListener != null)
            //开始调用监听器接口
            ticketListener.onResponseExpired(this);
    }

    // TODO: 17/4/15 by zmyer
    public void countDown() {
        this.latch.countDown();
    }

    // TODO: 17/4/15 by zmyer
    public boolean isDone() {
        return this.latch.getCount() == 0;
    }

    // TODO: 17/4/15 by zmyer
    public void notifyResponse(T response) {
        //设置应答消息
        this.response = response;
        if (this.callback != null)
            //开始回调完成接口
            this.callback.onCompleted(response);
        if (ticketListener != null)
            //开始调用监听器接口
            ticketListener.onResponseReceived(this);
    }

    // TODO: 17/4/15 by zmyer
    public T response() {
        return this.response;
    }

    // TODO: 17/4/15 by zmyer
    public Callback<T> getCallback() {
        return callback;
    }

    // TODO: 17/4/15 by zmyer
    public void setCallback(Callback<T> callback) {
        this.callback = callback;
    }

    // TODO: 17/4/15 by zmyer
    public int getTicketNumber() {
        return this.ticketNumber;
    }

    // TODO: 17/4/15 by zmyer
    protected void checkExpired() {
        if (this.expired)
            throw new RuntimeException("", new IOException("The operation has timed out."));
    }

    // TODO: 17/4/15 by zmyer
    public static void setTicketListener(TicketListener listener) {
        ticketListener = listener;
    }
}
