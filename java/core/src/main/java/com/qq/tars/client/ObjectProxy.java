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

import com.qq.tars.client.support.ServantCacheManager;
import com.qq.tars.client.util.ClientLogger;
import com.qq.tars.common.support.ScheduledExecutorManager;
import com.qq.tars.common.util.StringUtils;
import com.qq.tars.rpc.common.InvokeContext;
import com.qq.tars.rpc.common.Invoker;
import com.qq.tars.rpc.common.LoadBalance;
import com.qq.tars.rpc.common.ProtocolInvoker;
import com.qq.tars.rpc.common.exc.NoInvokerException;
import com.qq.tars.rpc.exc.ClientException;
import com.qq.tars.rpc.exc.NoConnectionException;
import com.qq.tars.support.stat.InvokeStatHelper;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// TODO: 17/4/15 by zmyer
public final class ObjectProxy<T> implements ServantProxy, InvocationHandler {
    //类对象
    private final Class<T> api;
    //对象名称
    private final String objName;
    //通信对象
    private final Communicator communicator;
    //缓存管理器
    private final ServantCacheManager servantCacheManager = ServantCacheManager.getInstance();
    //代理配置对象
    private volatile ServantProxyConfig servantProxyConfig;
    //负载均衡对象
    private LoadBalance loadBalancer;
    //协议调用者对象
    private ProtocolInvoker<T> protocolInvoker;
    private ScheduledFuture<?> statReportFuture;
    private ScheduledFuture<?> queryRefreshFuture;

    private final Random random = new Random(System.currentTimeMillis() / 1000);

    // TODO: 17/4/18 by zmyer
    public ObjectProxy(Class<T> api, String objName, ServantProxyConfig servantProxyConfig,
        LoadBalance loadBalance,
        ProtocolInvoker<T> protocolInvoker, Communicator communicator) {
        this.api = api;
        this.objName = objName;
        this.communicator = communicator;
        this.servantProxyConfig = servantProxyConfig;
        this.loadBalancer = loadBalance;
        this.protocolInvoker = protocolInvoker;
        this.initialize();
    }

    // TODO: 17/4/18 by zmyer
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //读取调动方法名
        String methodName = method.getName();
        //读取方法参数类型
        Class<?>[] parameterTypes = method.getParameterTypes();
        //创建调用上下文对象
        InvokeContext context = protocolInvoker.createContext(proxy, method, args);
        try {
            if ("toString".equals(methodName) && parameterTypes.length == 0) {
                return this.toString();
            } else if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
                return this.hashCode();
            } else if ("equals".equals(methodName) && parameterTypes.length == 1) {
                return this.equals(args[0]);
            } else if ("getObjectName".equals(methodName) && parameterTypes.length == 0) {
                return this.getObjectName();
            } else if ("getApi".equals(methodName) && parameterTypes.length == 0) {
                return this.getApi();
            } else if ("getConfig".equals(methodName) && parameterTypes.length == 0) {
                return this.getConfig();
            } else if ("destroy".equals(methodName) && parameterTypes.length == 0) {
                this.destroy();
                return null;
            } else if ("refresh".equals(methodName) && parameterTypes.length == 0) {
                this.refresh();
                return null;
            }
            //根据负载均衡算法,选择具体的调用对象
            Invoker<T> invoker = loadBalancer.select(protocolInvoker.getInvokers(), context);
            //开始调用rpc
            return invoker.invoke(context);
        } catch (Throwable e) {
            if (ClientLogger.getLogger().isDebugEnabled()) {
                ClientLogger.getLogger().debug(servantProxyConfig.getSimpleObjectName() + " error occurred on invoke|" + e.getLocalizedMessage(), e);
            }
            if (e instanceof NoInvokerException) {
                throw new NoConnectionException(servantProxyConfig.getSimpleObjectName(), e.getLocalizedMessage(), e);
            }
            throw new ClientException(servantProxyConfig.getSimpleObjectName(), e.getLocalizedMessage(), e);
        }
    }

    // TODO: 17/4/18 by zmyer
    public Class<T> getApi() {
        return api;
    }

    // TODO: 17/4/18 by zmyer
    public String getObjectName() {
        return servantProxyConfig.getSimpleObjectName();
    }

    // TODO: 17/4/18 by zmyer
    public void refresh() {
        //注册统计报表对象
        registryStatReproter();
        //注册节点更新对象
        registryServantNodeRefresher();
        //开始进行刷新操作
        protocolInvoker.refresh();
    }

    // TODO: 17/4/18 by zmyer
    public void destroy() {
        //取消统计
        statReportFuture.cancel(false);
        //取消查询刷新
        queryRefreshFuture.cancel(false);
        //关闭rpc
        protocolInvoker.destroy();
    }

    // TODO: 17/4/18 by zmyer
    public ServantProxyConfig getConfig() {
        return servantProxyConfig;
    }

    // TODO: 17/4/18 by zmyer
    private void initialize() {
        if (StringUtils.isNotEmpty(this.servantProxyConfig.getLocator())
            && !StringUtils.isEmpty(this.servantProxyConfig.getStat())) {
            //注册统计报表对象
            this.registryStatReproter();
        }
        if (!servantProxyConfig.isDirectConnection()) {
            //注册节点刷新
            this.registryServantNodeRefresher();
        }
    }

    // TODO: 17/4/18 by zmyer
    private void registryStatReproter() {
        if (this.statReportFuture != null && !this.statReportFuture.isCancelled()) {
            this.statReportFuture.cancel(false);
        }
        if (!StringUtils.isEmpty(communicator.getCommunicatorConfig().getStat())) {
            int interval = servantProxyConfig.getReportInterval();
            int initialDelay = interval + (random.nextInt(30) * 1000);
            //定时刷新
            this.statReportFuture = ScheduledExecutorManager.getInstance()
                .scheduleAtFixedRate(new ServantStatReproter(), initialDelay, interval, TimeUnit.MILLISECONDS);
        }
    }

    // TODO: 17/4/18 by zmyer
    private void registryServantNodeRefresher() {
        if (this.queryRefreshFuture != null && !this.queryRefreshFuture.isCancelled()) {
            this.queryRefreshFuture.cancel(false);
        }
        if (!servantProxyConfig.isDirectConnection()) {
            int interval = servantProxyConfig.getRefreshInterval();
            int initialDelay = interval + (random.nextInt(30) * 1000);
            this.queryRefreshFuture = ScheduledExecutorManager.getInstance()
                .scheduleAtFixedRate(new ServantNodeRefresher(), initialDelay, interval, TimeUnit.MILLISECONDS);
        }
    }

    // TODO: 17/4/18 by zmyer
    private class ServantNodeRefresher implements Runnable {

        public void run() {
            long begin = System.currentTimeMillis();
            try {
                //首先读取节点信息
                String nodes = communicator.getQueryHelper().getServerNodes(servantProxyConfig);
                if (nodes != null && !nodes.equals(servantProxyConfig.getObjectName())) {
                    //开始将节点信息插入到缓存管理器中
                    servantCacheManager.save(communicator.getId(), servantProxyConfig.getSimpleObjectName(),
                        nodes, communicator.getCommunicatorConfig().getDataPath());
                    servantProxyConfig.setObjectName(nodes);
                    //执行刷新操作
                    refresh();
                }
                ClientLogger.getLogger().debug(objName + " sync server|" + nodes);
            } catch (Throwable e) {
                ClientLogger.getLogger().error(objName + " error sync server", e);
            } finally {
                ClientLogger.getLogger().info("ServantNodeRefresher run(" + objName + "), use: " + (System.currentTimeMillis() - begin));
            }
        }
    }

    // TODO: 17/4/18 by zmyer
    private class ServantStatReproter implements Runnable {

        public void run() {
            long begin = System.currentTimeMillis();
            try {
                //报告统计信息
                communicator.getStatHelper().report(InvokeStatHelper.getInstance().getProxyStat(servantProxyConfig.getSimpleObjectName()));
            } catch (Exception e) {
                ClientLogger.getLogger().error("report stat worker error|" + servantProxyConfig.getSimpleObjectName(), e);
            } finally {
                ClientLogger.getLogger().info("ServantStatReproter run(), use: " + (System.currentTimeMillis() - begin));
            }
        }
    }
}
