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

package com.qq.tars.common.support;

import java.util.HashMap;
import java.util.Map;

// TODO: 17/4/15 by zmyer
public class ClassLoaderManager {
    //单例对象
    private static final ClassLoaderManager instance = new ClassLoaderManager();
    //类加载映射表
    private Map<String, ClassLoader> classLoaders = new HashMap<String, ClassLoader>();

    // TODO: 17/4/15 by zmyer
    private ClassLoaderManager() {
    }

    // TODO: 17/4/15 by zmyer
    public static ClassLoaderManager getInstance() {
        return instance;
    }

    // TODO: 17/4/15 by zmyer
    public ClassLoader getClassLoader(String contextName) {
        return this.classLoaders.get(contextName);
    }

    // TODO: 17/4/15 by zmyer
    public void registerClassLoader(String contextName, ClassLoader classLoader) {
        this.classLoaders.put(contextName, classLoader);
    }
}
