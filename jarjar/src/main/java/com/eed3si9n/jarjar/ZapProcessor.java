/**
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eed3si9n.jarjar;

import com.eed3si9n.jarjar.util.*;
import java.io.IOException;
import java.util.*;

class ZapProcessor implements JarProcessor
{
    private List<Wildcard> wildcards;

    public ZapProcessor(List<Zap> zapList) {
        wildcards = PatternElement.createWildcards(zapList);
    }

    public boolean process(EntryStruct struct) throws IOException {
        String name = struct.name;
        String matchName = name.endsWith(".class") ?
                name.substring(0, name.length() - 6) :
                replaceResourceName(name);
        return !zap(matchName);
    }

    private static final String RESOURCE_SUFFIX = "RESOURCE";

    private static String replaceResourceName(String name) {
        int slash = name.lastIndexOf('/');

        String s = (slash < 0) ? RESOURCE_SUFFIX : name.substring(0, slash + 1) + RESOURCE_SUFFIX;
        boolean absolute = s.startsWith("/");
        return absolute ? s.substring(1) : s;
    }
    
    private boolean zap(String desc) {
        // TODO: optimize
        for (Wildcard wildcard : wildcards) {
            if (wildcard.matches(desc))
                return true;
        }
        return false;
    }
}
    
