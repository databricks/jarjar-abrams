package com.eed3si9n.jarjar;

import com.eed3si9n.jarjar.util.EntryStruct;
import com.eed3si9n.jarjar.util.RemappingClassTransformer;
import junit.framework.TestCase;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import java.util.Arrays;
import java.util.List;

import static com.eed3si9n.jarjar.MethodRewriterTest.readInputStream;

public class UnmodifiedTest
        extends TestCase
{
    @Test
    public void testNotModified() throws Exception {
        Rule rule = new Rule();
        rule.setPattern("com.abc");
        rule.setResult("com.def");

        MainProcessor mp = new MainProcessor(Arrays.asList(rule), false, false, "move");

        EntryStruct entryStruct = new EntryStruct();
        entryStruct.name = "BigtableIO$Write.class";
        entryStruct.skipTransform = false;
        entryStruct.time = 0;
        entryStruct.data =
                readInputStream(
                        getClass().getResourceAsStream("/com/eed3si9n/jarjar/BigtableIO$Write.class"));

        EntryStruct orig = entryStruct.copy();

        mp.process(entryStruct);

        assertEquals(entryStruct.data, orig.data);
    }
}
