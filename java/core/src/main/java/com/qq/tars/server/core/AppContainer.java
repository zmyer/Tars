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

package com.qq.tars.server.core;

import com.qq.tars.common.support.ClassLoaderManager;
import com.qq.tars.server.apps.AppContext;
import com.qq.tars.server.config.ConfigurationManager;
import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.ConcurrentHashMap;

// TODO: 17/4/15 by zmyer
@Deprecated
public class AppContainer implements Container {
    //默认执行上下文对象
    AppContext defaultApp = null;
    //上下文对象映射表
    private final ConcurrentHashMap<String, AppContext> contexts = new ConcurrentHashMap<String, AppContext>();

    // TODO: 17/4/15 by zmyer
    public void start() throws Exception {
        //首先加载app
        loadApps();
        //读取默认的执行上下文对象
        defaultApp = contexts.get("");
        System.out.println("[SERVER] The container started successfully.");
    }

    // TODO: 17/4/15 by zmyer
    public void stop() throws Exception {
        stopApps();
        System.out.println("[SERVER] The container stopped successfully.");
    }

    // TODO: 17/4/15 by zmyer
    public void loadApps() throws Exception {
        //读取根目录
        String root = ConfigurationManager.getInstance().getserverConfig().getBasePath();
        //读取保存app的目录
        File dirs = new File(root + "/apps");
        //读取类加载管理器
        final ClassLoaderManager protocolManager = ClassLoaderManager.getInstance();

        //开始进行过滤操作
        dirs.listFiles(new FileFilter() {

            // TODO: 17/4/15 by zmyer
            public boolean accept(File path) {
                String name = path.getName();
                if (name.equals("ROOT")) {
                    name = "";
                }
                if (path.isDirectory()) {
                    //创建执行上下文对象
                    AppContext context = new AppContext(name, path);
                    //将执行上下文对象插入到映射表中
                    contexts.put(name, context);
                    //将该上下文对象注册到协议管理器对象中
                    protocolManager.registerClassLoader(name, context.getAppContextClassLoader());
                }
                return false;
            }
        });
    }

    public AppContext getDefaultAppContext() {
        return defaultApp;
    }

    public AppContext getAppContext(String name) {
        return contexts.get(name);
    }

    public String getContextPath(String reqeustURI) {
        String contextPath = "/";
        int pos = -1;
        pos = reqeustURI.indexOf("/", 1);

        if (pos > 0) {
            contextPath = reqeustURI.substring(1, pos - 1);
            if (contexts.get(contextPath) == null) {
                contextPath = "/";
            }
        }

        return contextPath;
    }

    public void stopApps() throws Exception {

    }
}
