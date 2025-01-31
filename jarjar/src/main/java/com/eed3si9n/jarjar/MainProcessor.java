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

import com.eed3si9n.jarjar.misplaced.MisplacedClassProcessorFactory;
import com.eed3si9n.jarjar.util.*;
import java.util.function.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

class MainProcessor implements JarProcessor
{
    private final boolean verbose;

    private final Function<PackageRemapper, JarProcessor> chain;

    private final KeepProcessor kp;
    private final Map<String, String> renames = new HashMap<String, String>();

    private final Supplier<PackageRemapper> packageRemapper;

    public MainProcessor(List<PatternElement> patterns, boolean verbose, boolean skipManifest) {
        this(patterns, verbose, skipManifest, null);
    }

    /**
     * Creates a new MainProcessor, which automatically generates the standard zap, keep, remap,
     * etc processors.
     *
     * @param patterns List of rules to parse.
     * @param verbose Whether to verbosely log information.
     * @param skipManifest If true, omits the manifest file from the processed jar.
     * @param misplacedClassStrategy The strategy to use when processing class files that are in the
     * wrong package (see MisplacedClassProcessorFactory.STRATEGY_* constants).
     */
    public MainProcessor(List<PatternElement> patterns, boolean verbose, boolean skipManifest,
                         String misplacedClassStrategy) {
        this.verbose = verbose;
        List<Zap> zapList = new ArrayList<Zap>();
        List<Rule> ruleList = new ArrayList<Rule>();
        List<Keep> keepList = new ArrayList<Keep>();
        for (PatternElement pattern : patterns) {
            if (pattern instanceof Zap) {
                zapList.add((Zap) pattern);
            } else if (pattern instanceof Rule) {
                ruleList.add((Rule) pattern);
            } else if (pattern instanceof Keep) {
                keepList.add((Keep) pattern);
            }
        }

        this.packageRemapper = () -> new PackageRemapper(ruleList, verbose);
        kp = keepList.isEmpty() ? null : new KeepProcessor(keepList);

        this.chain = (pr) -> {
            List<JarProcessor> processors = new ArrayList<JarProcessor>();
            if (skipManifest)
                processors.add(ManifestProcessor.getInstance());
            if (kp != null)
                processors.add(kp);

            JarProcessor misplacedClassProcessor = MisplacedClassProcessorFactory.getInstance()
                    .getProcessorForName(misplacedClassStrategy);

            processors.add(new ZapProcessor(zapList));
            processors.add(misplacedClassProcessor);
            processors.add(new JarTransformerChain(new RemappingClassTransformer[]{
                    new RemappingClassTransformer(pr)
            }));
            processors.add(new MethodSignatureProcessor(pr));
            processors.add(new ResourceProcessor(pr));
            return new JarProcessorChain(processors.toArray(new JarProcessor[processors.size()]));
        };
    }

    public void strip(File file) throws IOException {
        if (kp == null)
            return;
        Set<String> excludes = getExcludes();
        if (!excludes.isEmpty())
            StandaloneJarProcessor.run(file, file, new ExcludeProcessor(excludes, verbose));
    }

    /**
     * Returns the <code>.class</code> files to delete. As well the root-parameter as the rename ones
     * are taken in consideration, so that the concerned files are not listed in the result.
     *
     * @return the paths of the files in the jar-archive, including the <code>.class</code> suffix
     */
    private Set<String> getExcludes() {
        Set<String> result = new HashSet<String>();
        for (String exclude : kp.getExcludes()) {
            String name = exclude + ".class";
            String renamed = renames.get(name);
            result.add((renamed != null) ? renamed : name);
        }
        return result;
    }

    /**
     *
     * @param struct
     * @return <code>true</code> if the entry is to include in the output jar
     * @throws IOException
     */
    public boolean process(EntryStruct struct) throws IOException {
        EntryStruct origStruct = struct.copy();
        PackageRemapper pr = packageRemapper.get();
        boolean keepIt = chain.apply(pr).process(struct);

        if (!keepIt) {
            if (verbose)
                System.err.println("Removed " + origStruct.name);
            return keepIt;
        }

        if (!pr.modified) {
            // copy only data field.
            // Specifically, don't copy name for the sake of Misplaced class processors.
            struct.data = origStruct.data;
        }

        if (!origStruct.name.equals(struct.name)) {
            if (kp != null)
                renames.put(origStruct.name, struct.name);
            if (verbose)
                System.err.println("Renamed " + origStruct.name + " -> " + struct.name);
        }
        return keepIt;
    }
}
