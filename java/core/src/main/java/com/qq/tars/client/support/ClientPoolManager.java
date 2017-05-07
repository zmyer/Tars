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

package com.qq.tars.client.support;

import com.qq.tars.client.CommunicatorConfig;
import com.qq.tars.client.ServantProxyConfig;
import com.qq.tars.client.rpc.ServantClient;
import com.qq.tars.common.util.concurrent.TaskQueue;
import com.qq.tars.common.util.concurrent.TaskThreadFactory;
import com.qq.tars.common.util.concurrent.TaskThreadPoolExecutor;
import com.qq.tars.net.core.nio.SelectorManager;
import com.qq.tars.net.protocol.ProtocolFactory;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// TODO: 17/4/15 by zmyer
public class ClientPoolManager {
    //客户端执行线程池映射表
    private final static ConcurrentHashMap<CommunicatorConfig, ThreadPoolExecutor> clientThreadPoolMap = new ConcurrentHashMap<CommunicatorConfig, ThreadPoolExecutor>();
    //代理对象与选择器之间的映射表
    private final static ConcurrentHashMap<ServantProxyConfig, SelectorManager> selectorsMap = new ConcurrentHashMap<ServantProxyConfig, SelectorManager>();

    // TODO: 17/4/15 by zmyer
    public static ThreadPoolExecutor getClientThreadPoolExecutor(
        CommunicatorConfig communicatorConfig) {
        //根据通信对象配置,读取对应的线程池对象
        ThreadPoolExecutor clientPoolExecutor = clientThreadPoolMap.get(communicatorConfig);
        if (clientPoolExecutor == null) {
            synchronized (ServantClient.class) {
                //重新读取一遍
                clientPoolExecutor = clientThreadPoolMap.get(communicatorConfig);
                if (clientPoolExecutor == null) {
                    //如果不存在,则重新创建
                    clientThreadPoolMap.put(communicatorConfig, createThreadPool(communicatorConfig));
                    //读取客户端线程池对象
                    clientPoolExecutor = clientThreadPoolMap.get(communicatorConfig);
                }
            }
        }
        //读取客户端线程池对象
        return clientPoolExecutor;
    }

    // TODO: 17/4/15 by zmyer
    private static ThreadPoolExecutor createThreadPool(CommunicatorConfig communicatorConfig) {
        int corePoolSize = communicatorConfig.getCorePoolSize();
        int maxPoolSize = communicatorConfig.getMaxPoolSize();
        int keepAliveTime = communicatorConfig.getKeepAliveTime();
        int queueSize = communicatorConfig.getQueueSize();
        //创建任务队列对象
        TaskQueue taskqueue = new TaskQueue(queueSize);

        String namePrefix = "tars-client-executor-";
        //创建线程池对象
        TaskThreadPoolExecutor executor = new TaskThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, taskqueue, new TaskThreadFactory(namePrefix));
        taskqueue.setParent(executor);
        //返回线程池对象
        return executor;
    }

    // TODO: 17/4/15 by zmyer
    public static SelectorManager getSelectorManager(ProtocolFactory protocolFactory,
        ThreadPoolExecutor threadPoolExecutor, boolean keepAlive,
        boolean udpMode, ServantProxyConfig servantProxyConfig) throws IOException {
        //读取选择器管理器对象
        SelectorManager selector = selectorsMap.get(servantProxyConfig);
        if (selector == null) {
            synchronized (selectorsMap) {
                //从选择器映射表中读取对应的选择器
                selector = selectorsMap.get(servantProxyConfig);
                if (selector == null) {
                    //选择器的数量
                    int selectorPoolSize = convertInt(System.getProperty("com.qq.tars.net.client.selectorPoolSize"), 2);
                    //创建选择器管理器
                    selector = new SelectorManager(selectorPoolSize, protocolFactory, threadPoolExecutor, null, keepAlive, "servant-proxy-" + servantProxyConfig.getCommunicatorId(), udpMode);
                    //启动管理器
                    selector.start();
                    //将选择器插入到映射表
                    selectorsMap.put(servantProxyConfig, selector);
                }
            }
        }
        //返回选择器
        return selector;
    }

    // TODO: 17/4/15 by zmyer
    private static int convertInt(String value, int defaults) {
        if (value == null) {
            return defaults;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaults;
        }
    }
}
