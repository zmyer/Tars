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

import com.qq.tars.client.util.ClientLogger;
import com.qq.tars.common.util.Constants;
import com.qq.tars.common.util.Loader;
import com.qq.tars.common.util.StringUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

// TODO: 17/4/15 by zmyer
public final class ServantCacheManager {
    //单例对象
    private final static ServantCacheManager instance = new ServantCacheManager();
    //属性对象
    private final Properties props = new Properties();
    //
    private final AtomicBoolean propsInited = new AtomicBoolean();
    //重入锁对象
    private final ReentrantLock lock = new ReentrantLock();

    // TODO: 17/4/15 by zmyer
    private ServantCacheManager() {
    }

    // TODO: 17/4/15 by zmyer
    public static ServantCacheManager getInstance() {
        return instance;
    }

    // TODO: 17/4/15 by zmyer
    public String get(String CommunicatorId, String objName, String dataPath) {
        loadCacheData(dataPath);
        return props.getProperty(makeKey(CommunicatorId, objName));
    }

    // TODO: 17/4/15 by zmyer
    private String makeKey(String CommunicatorId, String objName) {
        return objName + "@" + CommunicatorId;
    }

    // TODO: 17/4/15 by zmyer
    public void save(String CommunicatorId, String objName, String endpointList, String dataPath) {
        try {
            //直接加载缓存数据
            loadCacheData(dataPath);
            //从属性对象中删除指定的键值
            props.remove(objName);
            //将新的信息插入到缓存中
            props.put(makeKey(CommunicatorId, objName), endpointList);
            //将缓存保存到本地
            saveToLocal(dataPath);
        } catch (Throwable e) {
            ClientLogger.getLogger().error("", e);
        }
    }

    // TODO: 17/4/15 by zmyer
    private File getCacheFile(String dataPath) throws Exception {
        String path = dataPath;
        if (StringUtils.isEmpty(path)) {
            URL url = Loader.getResource("", true);
            if (url != null) {
                path = url.getFile();
            }
            if (StringUtils.isEmpty(path)) {
                path = System.getProperty("user.dir");
            }
        }
        if (StringUtils.isEmpty(path)) {
            return null;
        }
        File f = new File(path, Constants.SERVER_NODE_CACHE_FILENAME);
        if (!f.exists()) {
            f.createNewFile();
        }
        return f;
    }

    // TODO: 17/4/15 by zmyer
    private void saveToLocal(String dataPath) {
        lock.lock();
        OutputStream out = null;
        try {
            //读取缓存文件
            File file = getCacheFile(dataPath);
            if (file == null) {
                return;
            }
            //创建输入缓冲区流对象
            out = new BufferedOutputStream(new FileOutputStream(file));
            //将缓存数据写入到输出流对象中
            props.store(out, (new Date()).toString());
            ClientLogger.getLogger().info("save " + file.getAbsolutePath());
        } catch (Exception e) {
            ClientLogger.getLogger().error("save " + Constants.SERVER_NODE_CACHE_FILENAME + " failed", e);
        } finally {
            if (null != out) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
            lock.unlock();
        }
    }

    // TODO: 17/4/15 by zmyer
    private void loadCacheData(String dataPath) {
        if (propsInited.get()) {
            return;
        }
        if (propsInited.compareAndSet(false, true)) {
            InputStream in = null;
            try {
                //读取缓存文件
                File file = getCacheFile(dataPath);
                if (file == null) {
                    return;
                }
                //创建文件输入流对象
                in = new BufferedInputStream(new FileInputStream(file));
                //开始从输入流对象中读取相关内容
                props.load(in);
                ArrayList<String> removeKey = new ArrayList<String>();
                for (Entry<Object, Object> entry : props.entrySet()) {
                    if (entry.getKey().toString().startsWith("<")) {
                        removeKey.add(entry.getKey().toString());
                    }
                }
                for (String key : removeKey) {
                    props.remove(key);
                }
                ClientLogger.getLogger().info("load " + Constants.SERVER_NODE_CACHE_FILENAME);
            } catch (Throwable e) {
                ClientLogger.getLogger().error("read file " + Constants.SERVER_NODE_CACHE_FILENAME + " error.", e);
            } finally {
                if (null != in) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }
}
