package com.eed3si9n.jarjar;

import java.io.IOException;
import java.util.Collections;
import org.junit.Test;
import com.eed3si9n.jarjar.util.EntryStruct;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZapProcessorTest
{
  @Test
  public void testZap() throws IOException {
    Zap zap = new Zap();
    zap.setPattern("org.**");
    ZapProcessor zapProcessor = new ZapProcessor(Collections.singletonList(zap));

    EntryStruct entryStruct = new EntryStruct();
    entryStruct.name = "org/example/Object.class";
    assertFalse(zapProcessor.process(entryStruct));

    entryStruct.name = "com/example/Object.class";
    assertTrue(zapProcessor.process(entryStruct));

    entryStruct.name = "org/file.txt";
    assertFalse(zapProcessor.process(entryStruct));

    entryStruct.name = "com/file.txt";
    assertTrue(zapProcessor.process(entryStruct));
  }

  @Test
  public void testMetaInfZap() throws IOException {
    Zap zap = new Zap();
    zap.setPattern("META-INF.versions.9.**");
    ZapProcessor zapProcessor = new ZapProcessor(Collections.singletonList(zap));

    EntryStruct entryStruct = new EntryStruct();
    entryStruct.name = "META-INF/versions/9/org/example/Object.class";
    assertFalse(zapProcessor.process(entryStruct));

    entryStruct.name = "META-INF/versions/8/com/example/Object.class";
    assertTrue(zapProcessor.process(entryStruct));
  }
}
