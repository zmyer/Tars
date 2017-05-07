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

import com.qq.tars.client.cluster.DefaultLoadBalance;
import com.qq.tars.client.rpc.tars.TarsProtocolInvoker;
import com.qq.tars.client.support.ServantCacheManager;
import com.qq.tars.client.util.ClientLogger;
import com.qq.tars.common.util.StringUtils;
import com.qq.tars.protocol.annotation.Servant;
import com.qq.tars.protocol.annotation.ServantCodec;
import com.qq.tars.rpc.common.LoadBalance;
import com.qq.tars.rpc.common.ProtocolInvoker;
import com.qq.tars.rpc.exc.ClientException;
import com.qq.tars.rpc.exc.CommunicatorConfigException;
import com.qq.tars.rpc.protocol.Codec;
import com.qq.tars.rpc.protocol.ServantProtocolFactory;
import com.qq.tars.rpc.protocol.tars.TarsCodec;
import java.lang.reflect.Constructor;

// TODO: 17/4/15 by zmyer
class ObjectProxyFactory {
    //通信对象
    private final Communicator communicator;

    // TODO: 17/4/15 by zmyer
    public ObjectProxyFactory(Communicator communicator) {
        this.communicator = communicator;
    }

    // TODO: 17/4/15 by zmyer
    public <T> ObjectProxy<T> getObjectProxy(Class<T> api, String objName, ServantProxyConfig servantProxyConfig,
        LoadBalance loadBalance, ProtocolInvoker<T> protocolInvoker) throws ClientException {
        if (servantProxyConfig == null) {
            //创建代理配置对象
            servantProxyConfig = createServantProxyConfig(objName);
        } else {
            //设置通信id
            servantProxyConfig.setCommunicatorId(communicator.getId());
            //设置本地加载器
            servantProxyConfig.setLocator(communicator.getCommunicatorConfig().getLocator());
        }
        //更新服务终端点的配置
        updateServantEndpoints(servantProxyConfig);

        if (loadBalance == null) {
            //如果负载均衡对象为空,则直接创建默认
            loadBalance = createLoadBalance(servantProxyConfig);
        }

        if (protocolInvoker == null) {
            //如果协议调用者为空,则重新创建
            protocolInvoker = createProtocolInvoker(api, objName, servantProxyConfig);
        }
        //返回代理对象
        return new ObjectProxy<T>(api, objName, servantProxyConfig, loadBalance, protocolInvoker, communicator);
    }

    // TODO: 17/4/15 by zmyer
    private <T> ProtocolInvoker<T> createProtocolInvoker(Class<T> api, String objName,
        ServantProxyConfig servantProxyConfig) throws ClientException {
        ProtocolInvoker<T> protocolInvoker;
        //创建编码对象
        Codec codec = createCodec(api, servantProxyConfig);
        if (api.isAnnotationPresent(Servant.class)) {
            if (codec == null) {
                codec = new TarsCodec(servantProxyConfig.getCharsetName());
            }
            //设置协议类型
            servantProxyConfig.setProtocol(codec.getProtocol());
            //创建协议调用对象
            protocolInvoker = new TarsProtocolInvoker<T>(api, servantProxyConfig, new ServantProtocolFactory(codec), communicator.getThreadPoolExecutor());
        } else {
            throw new ClientException(servantProxyConfig.getSimpleObjectName(), "unkonw protocol servant invoker", null);
        }
        return protocolInvoker;
    }

    // TODO: 17/4/15 by zmyer
    private LoadBalance createLoadBalance(ServantProxyConfig servantProxyConfig) {
        //创建默认的负载均衡对象
        return new DefaultLoadBalance(servantProxyConfig);
    }

    // TODO: 17/4/15 by zmyer
    private <T> Codec createCodec(Class<T> api, ServantProxyConfig servantProxyConfig) throws ClientException {
        Codec codec = null;
        ServantCodec servantCodec = api.getAnnotation(ServantCodec.class);
        if (servantCodec != null) {
            //读取编码器类对象
            Class<? extends Codec> codecClass = servantCodec.codec();
            if (codecClass != null) {
                Constructor<? extends Codec> constructor;
                try {
                    //读取类对象中对应的构造函数
                    constructor = codecClass.getConstructor(new Class[] {String.class});
                    //使用构造函数创建对应的编码器
                    codec = constructor.newInstance(servantProxyConfig.getCharsetName());
                } catch (Exception e) {
                    throw new ClientException(servantProxyConfig.getSimpleObjectName(), "error occurred on create codec, codec=" + codecClass.getName(), e);
                }
            }
        }
        //返回编码器
        return codec;
    }

    // TODO: 17/4/15 by zmyer
    private ServantProxyConfig createServantProxyConfig(String objName) throws CommunicatorConfigException {
        CommunicatorConfig communicatorConfig = communicator.getCommunicatorConfig();
        ServantProxyConfig cfg = new ServantProxyConfig(communicator.getId(), communicatorConfig.getLocator(), objName);
        cfg.setAsyncTimeout(communicatorConfig.getAsyncInvokeTimeout());
        cfg.setSyncTimeout(communicatorConfig.getSyncInvokeTimeout());
        cfg.setEnableSet(communicatorConfig.isEnableSet());
        cfg.setSetDivision(communicatorConfig.getSetDivision());
        cfg.setModuleName(communicatorConfig.getModuleName());
        cfg.setStat(communicatorConfig.getStat());
        cfg.setCharsetName(communicatorConfig.getCharsetName());
        return cfg;
    }

    // TODO: 17/4/15 by zmyer
    private void updateServantEndpoints(ServantProxyConfig cfg) {
        //读取通信对象配置
        CommunicatorConfig communicatorConfig = communicator.getCommunicatorConfig();

        String endpoints;
        if (StringUtils.isNotEmpty(communicatorConfig.getLocator()) && !cfg.isDirectConnection()
            && !communicatorConfig.getLocator().startsWith(cfg.getSimpleObjectName())) {
            try {
                /** 从主控拉取server node */
                //获取终端点对象
                endpoints = communicator.getQueryHelper().getServerNodes(cfg);
                if (StringUtils.isEmpty(endpoints)) {
                    throw new CommunicatorConfigException(cfg.getSimpleObjectName(), "servant node is empty on get by registry! communicator id=" + communicator.getId());
                }
                //将该服务终端点信息插入到缓存管理器中
                ServantCacheManager.getInstance().save(communicator.getId(), cfg.getSimpleObjectName(), endpoints,
                    communicatorConfig.getDataPath());
            } catch (CommunicatorConfigException e) {
                /** 如果失败，从本地绶存文件中拉取 */
                endpoints = ServantCacheManager.getInstance().get(communicator.getId(), cfg.getSimpleObjectName(), communicatorConfig.getDataPath());
                ClientLogger.getLogger().error(cfg.getSimpleObjectName() + " error occurred on get by registry, use by local cache=" + endpoints + "|" + e.getLocalizedMessage(), e);
            }

            if (StringUtils.isEmpty(endpoints)) {
                throw new CommunicatorConfigException(cfg.getSimpleObjectName(), "error occurred on create proxy, servant endpoint is empty! locator =" + communicatorConfig.getLocator() + "|communicator id=" + communicator.getId());
            }
            //在配置中设置终端点信息
            cfg.setObjectName(endpoints);
        }

        if (StringUtils.isEmpty(cfg.getObjectName())) {
            throw new CommunicatorConfigException(cfg.getSimpleObjectName(), "error occurred on create proxy, servant endpoint is empty!");
        }
    }
}
