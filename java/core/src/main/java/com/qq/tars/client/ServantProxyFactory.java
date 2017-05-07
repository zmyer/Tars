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

import com.qq.tars.rpc.common.LoadBalance;
import com.qq.tars.rpc.common.ProtocolInvoker;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

// TODO: 17/4/15 by zmyer
class ServantProxyFactory {
    //重入锁对象
    private final ReentrantLock lock = new ReentrantLock();
    //缓存对象
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<String, Object>();
    //通信对象
    private final Communicator communicator;

    // TODO: 17/4/15 by zmyer
    public ServantProxyFactory(Communicator communicator) {
        this.communicator = communicator;
    }

    // TODO: 17/4/15 by zmyer
    public <T> Object getServantProxy(Class<T> clazz, String objName, ServantProxyConfig servantProxyConfig,
        LoadBalance loadBalance, ProtocolInvoker<T> protocolInvoker) {
        //从缓存中读取代理对象
        Object proxy = cache.get(objName);
        if (proxy == null) {
            lock.lock();
            try {
                //如果缓冲区中的代理为空,则再读取一次
                proxy = cache.get(objName);
                if (proxy == null) {
                    //如果依旧为空,则直接创建
                    ObjectProxy<T> objectProxy = communicator.getObjectProxyFactory().getObjectProxy(clazz,
                        objName, servantProxyConfig, loadBalance, protocolInvoker);
                    //将新创建的代理对象,插入到缓存列表中
                    cache.putIfAbsent(objName, createProxy(clazz, objectProxy));
                    //在读取一次
                    proxy = cache.get(objName);
                }
            } finally {
                lock.unlock();
            }
        }
        return proxy;
    }

    // TODO: 17/4/15 by zmyer
    private <T> Object createProxy(Class<T> clazz, ObjectProxy<T> objectProxy) {
        //创建代理对象
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class[] {clazz, ServantProxy.class}, objectProxy);
    }

    // TODO: 17/4/15 by zmyer
    public Iterator<Object> getProxyIterator() {
        return cache.values().iterator();
    }
}
