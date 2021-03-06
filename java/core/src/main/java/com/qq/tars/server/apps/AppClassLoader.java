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

package com.qq.tars.server.apps;

import java.net.URL;
import java.net.URLClassLoader;

// TODO: 17/4/15 by zmyer
public class AppClassLoader extends URLClassLoader {
    //app名称
    private String appName = null;

    // TODO: 17/4/15 by zmyer
    public AppClassLoader(String appName, URL repositories[]) {
        super(repositories);
        this.appName = appName;
    }

    // TODO: 17/4/15 by zmyer
    public AppClassLoader(URL repositories[], ClassLoader parent) {
        super(repositories, parent);
    }

    // TODO: 17/4/15 by zmyer
    public String getAppName() {
        return this.appName;
    }
}
