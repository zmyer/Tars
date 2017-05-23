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

package com.qq.tars.client;

import com.qq.tars.client.support.ClientPoolManager;
import com.qq.tars.client.util.ClientLogger;
import com.qq.tars.common.util.StringUtils;
import com.qq.tars.rpc.common.LoadBalance;
import com.qq.tars.rpc.common.ProtocolInvoker;
import com.qq.tars.rpc.exc.CommunicatorConfigException;
import com.qq.tars.support.query.QueryHelper;
import com.qq.tars.support.query.prx.EndpointF;
import com.qq.tars.support.stat.StatHelper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

// TODO: 17/4/15 by zmyer
public final class Communicator {
    //通信对象id
    private volatile String id;
    //通信对象配置对象
    private volatile CommunicatorConfig communicatorConfig;
    //线程池对象
    private volatile ThreadPoolExecutor threadPoolExecutor;
    //服务器代理工厂对象
    private final ServantProxyFactory servantProxyFactory = new ServantProxyFactory(this);
    //对象代理对象
    private final ObjectProxyFactory objectProxyFactory = new ObjectProxyFactory(this);
    //查询对象
    private final QueryHelper queryHelper = new QueryHelper(this);
    //统计信息对象
    private final StatHelper statHelper = new StatHelper(this);

    //重入锁对象
    private final ReentrantLock lock = new ReentrantLock();
    //是否初始化
    private final AtomicBoolean inited = new AtomicBoolean(false);

    // TODO: 17/4/15 by zmyer
    Communicator(CommunicatorConfig config) {
        if (config != null) {
            //初始化通信对象
            this.initCommunicator(config);
        }
    }

    // TODO: 17/4/15 by zmyer
    public <T> T stringToProxy(Class<T> clazz, String objName) throws CommunicatorConfigException {
        return stringToProxy(clazz, objName, null, null, null);
    }

    // TODO: 17/4/15 by zmyer
    public <T> T stringToProxy(Class<T> clazz,
        ServantProxyConfig servantProxyConfig) throws CommunicatorConfigException {
        return stringToProxy(clazz, servantProxyConfig, null);
    }

    // TODO: 17/4/15 by zmyer
    public <T> T stringToProxy(Class<T> clazz, ServantProxyConfig servantProxyConfig,
        LoadBalance loadBalance) throws CommunicatorConfigException {
        return stringToProxy(clazz, servantProxyConfig.getObjectName(), servantProxyConfig, loadBalance, null);
    }

    // TODO: 17/4/15 by zmyer
    @SuppressWarnings("unchecked")
    private <T> T stringToProxy(Class<T> clazz, String objName,
        ServantProxyConfig servantProxyConfig,
        LoadBalance loadBalance, ProtocolInvoker<T> protocolInvoker)
        throws CommunicatorConfigException {
        if (!inited.get()) {
            throw new CommunicatorConfigException("communicator uninitialized!");
        }
        return (T) getServantProxyFactory().getServantProxy(clazz, objName,
            servantProxyConfig, loadBalance, protocolInvoker);
    }

    // TODO: 17/4/15 by zmyer
    @Deprecated
    public void initialize(CommunicatorConfig config) throws CommunicatorConfigException {
        this.initCommunicator(config);
    }

    // TODO: 17/4/15 by zmyer
    private void initCommunicator(CommunicatorConfig config) throws CommunicatorConfigException {
        if (inited.get()) {
            return;
        }
        //上锁
        lock.lock();
        try {
            if (!inited.get()) {
                try {
                    //初始化日志
                    ClientLogger.init(config.getLogPath(), config.getLogLevel());
                    if (StringUtils.isEmpty(config.getLocator())) {
                        //创建通信对象id
                        this.id = UUID.randomUUID().toString().replaceAll("-", "");
                    } else {
                        this.id = UUID.nameUUIDFromBytes(config.getLocator().getBytes()).toString().replaceAll("-", "");
                    }
                    this.communicatorConfig = config;
                    //创建线程池对象
                    this.threadPoolExecutor = ClientPoolManager.getClientThreadPoolExecutor(config);
                    inited.set(true);
                } catch (Throwable e) {
                    inited.set(false);
                    throw new CommunicatorConfigException(e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // TODO: 17/4/15 by zmyer
    String getId() {
        return this.id;
    }

    // TODO: 17/4/15 by zmyer
    protected ServantProxyFactory getServantProxyFactory() {
        return servantProxyFactory;
    }

    // TODO: 17/4/15 by zmyer
    protected ObjectProxyFactory getObjectProxyFactory() {
        return objectProxyFactory;
    }

    // TODO: 17/4/15 by zmyer
    public CommunicatorConfig getCommunicatorConfig() {
        return communicatorConfig;
    }

    // TODO: 17/4/15 by zmyer
    protected ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    // TODO: 17/4/15 by zmyer
    protected QueryHelper getQueryHelper() {
        return queryHelper;
    }

    // TODO: 17/4/15 by zmyer
    public StatHelper getStatHelper() {
        return statHelper;
    }
    
    // TODO: 17/4/15 by zmyer
    public List<EndpointF> getEndpoint4All(String objectName) {
        return getQueryHelper().findObjectById(objectName);
    }
}
