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

package com.eed3si9n.jarjar.util;

import java.io.*;

import static com.eed3si9n.jarjar.misplaced.MisplacedClassProcessor.VERSIONED_CLASS_FOLDER;

import com.eed3si9n.jarjar.TracingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;

abstract public class JarTransformer extends RemappingJarProcessor {

    public JarTransformer(TracingRemapper remapper) {
        super(remapper);
    }

    public boolean processImpl(EntryStruct struct, Remapper remapper) throws IOException {
        if (struct.name.endsWith(".class") && !struct.skipTransform) {
            ClassReader reader;
            try {
                reader = new ClassReader(struct.data);
            } catch (RuntimeException e) {
                System.err.println("Unable to read bytecode from " + struct.name);
                e.printStackTrace();
                return true;
            }

            GetNameClassWriter w = new GetNameClassWriter(ClassWriter.COMPUTE_MAXS);
            try {
                reader.accept(transform(w, remapper), ClassReader.EXPAND_FRAMES);
            } catch (RuntimeException e) {
                throw new IOException("Unable to transform " + struct.name, e);
            }
            struct.data = w.toByteArray();
            String prefix = struct.name.startsWith(VERSIONED_CLASS_FOLDER) ?
                    struct.name.substring(0, struct.name.indexOf("/", VERSIONED_CLASS_FOLDER.length()) + 1) :
                    "";
            struct.name = prefix + pathFromName(w.getClassName());
        }
        return true;
    }

    abstract protected ClassVisitor transform(ClassVisitor v, Remapper remapper);

    private static String pathFromName(String className) {
        return className.replace('.', '/') + ".class";
    }
}
