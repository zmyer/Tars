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

package com.qq.tars.support.om;

import com.qq.tars.common.support.ScheduledExecutorManager;
import com.qq.tars.server.config.ConfigurationManager;
import com.qq.tars.support.node.NodeHelper;
import com.qq.tars.support.property.PropertyReportHelper;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

// TODO: 17/4/15 by zmyer
public class ScheduledServiceMngr {
    //单例对象
    private final static ScheduledServiceMngr Instance = new ScheduledServiceMngr();
    //心跳时间间隔
    private static final int HEART_BEAT_INTERVAL = 10;
    //报告最小时间间隔
    private static final int PROPERTY_REPORT_MIN_INTERVAL = 30000;
    //报告时间间隔
    private static int REPORT_INTERVAL = ConfigurationManager.getInstance().getserverConfig().getCommunicatorConfig().getReportInterval();
    private final Random random = new Random(System.currentTimeMillis() / 1000);
    //调度管理器
    private ScheduledExecutorManager taskExecutorManager = ScheduledExecutorManager.getInstance();

    // TODO: 17/4/18 by zmyer
    private ScheduledServiceMngr() {
    }

    // TODO: 17/4/18 by zmyer
    public static ScheduledServiceMngr getInstance() {
        return Instance;
    }

    // TODO: 17/4/15 by zmyer
    public void start() {
        startHandleService();
    }

    // TODO: 17/4/18 by zmyer
    public void shutdown() {
        taskExecutorManager.shutdown();
    }

    // TODO: 17/4/18 by zmyer
    public List<Runnable> shutdownNow() {
        return taskExecutorManager.shutdownNow();
    }

    // TODO: 17/4/18 by zmyer
    public boolean isShutdown() {
        return taskExecutorManager.isShutdown();
    }

    // TODO: 17/4/18 by zmyer
    public boolean isTerminated() {
        return taskExecutorManager.isTerminated();
    }

    // TODO: 17/4/18 by zmyer
    class NodeHandleThread implements Runnable {

        // TODO: 17/4/18 by zmyer
        @Override
        public void run() {
            NodeHelper.getInstance().keepAlive();
        }

    }

    // TODO: 17/4/18 by zmyer
    class PropertyHandleThread implements Runnable {

        // TODO: 17/4/18 by zmyer
        @Override
        public void run() {
            PropertyReportHelper.getInstance().report();
        }

    }

    // TODO: 17/4/18 by zmyer
    class StatHandleThread implements Runnable {

        // TODO: 17/4/18 by zmyer
        @Override
        public void run() {
            ServerStatHelper.getInstance().report();
        }
    }

    // TODO: 17/4/15 by zmyer
    private void startHandleService() {
        //心跳
        Runnable nodeHandler = new Thread(new NodeHandleThread(), "HeartBeat");
        taskExecutorManager.scheduleAtFixedRate(nodeHandler, 0, HEART_BEAT_INTERVAL, TimeUnit.SECONDS);

        //stats统计
        int initialDelay = REPORT_INTERVAL + (random.nextInt(30) * 1000);
        Runnable statHandler = new Thread(new StatHandleThread(), "ServerStat");
        taskExecutorManager.scheduleAtFixedRate(statHandler, initialDelay, REPORT_INTERVAL, TimeUnit.MILLISECONDS);

        if (REPORT_INTERVAL < PROPERTY_REPORT_MIN_INTERVAL) {
            REPORT_INTERVAL = PROPERTY_REPORT_MIN_INTERVAL;
        }
        //property统计
        initialDelay = REPORT_INTERVAL + (random.nextInt(30) * 1000);
        Runnable propertyHandler = new Thread(new PropertyHandleThread(), "PropertyReport");
        taskExecutorManager.scheduleAtFixedRate(propertyHandler, initialDelay, REPORT_INTERVAL, TimeUnit.MILLISECONDS);
    }
}
