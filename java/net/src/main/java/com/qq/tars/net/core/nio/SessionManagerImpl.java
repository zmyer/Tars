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

package com.qq.tars.net.core.nio;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.qq.tars.net.core.Session;
import com.qq.tars.net.core.SessionEvent;
import com.qq.tars.net.core.SessionListener;
import com.qq.tars.net.core.SessionManager;

// TODO: 17/4/15 by zmyer
public class SessionManagerImpl extends SessionManager {
    //会话超时时间
    private long timeout = 1000 * 60 * 1;
    //会话检查时间间隔
    private long interval = 1000 * 30;
    //会话定时器列表
    private List<SessionListener> listeners = new LinkedList<SessionListener>();
    //会话列表
    private CopyOnWriteArrayList<Session> sessionList = new CopyOnWriteArrayList<Session>();
    //是否启动
    private volatile boolean started = false;

    // TODO: 17/4/15 by zmyer
    public void registerSession(Session session) {
        sessionList.add(session);
        notifySessionCreated(session);
    }

    // TODO: 17/4/15 by zmyer
    public void unregisterSession(Session session) {
        sessionList.remove(session);
        notifySessionDestory(session);
    }

    // TODO: 17/4/15 by zmyer
    @Override
    public void addSessionListener(SessionListener listener) {
        this.listeners.add(listener);
    }

    // TODO: 17/4/15 by zmyer
    private void notifySessionCreated(Session newSession) {
        for (SessionListener listener : listeners) {
            try {
                listener.onSessionCreated(new SessionEvent(newSession));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // TODO: 17/4/15 by zmyer
    private void notifySessionDestory(Session oldSession) {
        for (SessionListener listener : listeners) {
            try {
                listener.onSessionDestoryed(new SessionEvent(oldSession));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // TODO: 17/4/15 by zmyer
    public synchronized void start() {
        if (started) return;

        new Thread(new Runnable() {

            public void run() {
                System.out.println("The session manager service started...");

                long lastUpdateOperationTime = -1;

                while (true) {
                    try {
                        for (Session session : sessionList) {
                            //检查会话超时
                            lastUpdateOperationTime = session.getLastOperationTime();
                            if ((System.currentTimeMillis() - lastUpdateOperationTime) > timeout) {
                                String s = "The session has timed out. [from ip: " + session.getRemoteIp() + " port: " + session.getRemotePort() + "]";
                                System.out.println(s);
                                //关闭会话
                                session.asyncClose();
                                //注销会话
                                unregisterSession(session);
                            }
                        }

                        Thread.sleep(interval);
                    } catch (Exception ex) {
                    }
                }
            }

        }, "SessionManageImpl Thread").start();

        started = true;
    }

    // TODO: 17/4/15 by zmyer
    public void setCheckInterval(long interval) {
        this.interval = interval;
    }

    // TODO: 17/4/15 by zmyer
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
