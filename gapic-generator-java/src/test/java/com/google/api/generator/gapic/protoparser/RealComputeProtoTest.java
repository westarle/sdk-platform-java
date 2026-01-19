package com.google.api.generator.gapic.protoparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.generator.gapic.model.Method;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.model.Transport;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import com.google.protobuf.ExtensionRegistry;

class RealComputeProtoTest {

  @Test
  void testRealComputeProto() throws Exception {
    // ...
  }

  private FileDescriptor loadFileDescriptor(String path, String targetFile) throws Exception {
    try (FileInputStream fis = new FileInputStream(path)) {
      ExtensionRegistry registry = ExtensionRegistry.newInstance();
      com.google.api.AnnotationsProto.registerAllExtensions(registry);
      com.google.api.ClientProto.registerAllExtensions(registry);
      com.google.api.ResourceProto.registerAllExtensions(registry);
      com.google.api.FieldBehaviorProto.registerAllExtensions(registry);
      com.google.longrunning.OperationsProto.registerAllExtensions(registry);
      
      FileDescriptorSet set = FileDescriptorSet.parseFrom(fis, registry);
      
      Map<String, FileDescriptor> seen = new HashMap<>();
      seen.put(com.google.api.AnnotationsProto.getDescriptor().getName(), com.google.api.AnnotationsProto.getDescriptor());
      seen.put(com.google.api.ClientProto.getDescriptor().getName(), com.google.api.ClientProto.getDescriptor());
      seen.put(com.google.api.ResourceProto.getDescriptor().getName(), com.google.api.ResourceProto.getDescriptor());
      seen.put(com.google.api.FieldBehaviorProto.getDescriptor().getName(), com.google.api.FieldBehaviorProto.getDescriptor());
      seen.put(com.google.api.HttpProto.getDescriptor().getName(), com.google.api.HttpProto.getDescriptor());
      seen.put(com.google.longrunning.OperationsProto.getDescriptor().getName(), com.google.longrunning.OperationsProto.getDescriptor());
      seen.put(com.google.protobuf.DescriptorProtos.getDescriptor().getName(), com.google.protobuf.DescriptorProtos.getDescriptor());
      seen.put(com.google.protobuf.WrappersProto.getDescriptor().getName(), com.google.protobuf.WrappersProto.getDescriptor());
      seen.put(com.google.protobuf.AnyProto.getDescriptor().getName(), com.google.protobuf.AnyProto.getDescriptor());
      seen.put(com.google.protobuf.EmptyProto.getDescriptor().getName(), com.google.protobuf.EmptyProto.getDescriptor());
      seen.put(com.google.protobuf.TimestampProto.getDescriptor().getName(), com.google.protobuf.TimestampProto.getDescriptor());
      seen.put(com.google.protobuf.DurationProto.getDescriptor().getName(), com.google.protobuf.DurationProto.getDescriptor());
      seen.put(com.google.protobuf.FieldMaskProto.getDescriptor().getName(), com.google.protobuf.FieldMaskProto.getDescriptor());
      
      FileDescriptor target = null;
      
      // We need to build dependencies first. 
      // A simple topological sort or just repeated passes might work if not sorted.
      // But FileDescriptorSet usually has deps first? No guarantee.
      // Let's try to build what we can until we are done.
      
      List<FileDescriptorProto> remaining = new ArrayList<>(set.getFileList());
      int lastSize = -1;
      
      while (!remaining.isEmpty()) {
        if (remaining.size() == lastSize) {
           throw new RuntimeException("Stuck resolving dependencies for " + remaining.get(0).getName());
        }
        lastSize = remaining.size();
        
        List<FileDescriptorProto> nextRemaining = new ArrayList<>();
        for (FileDescriptorProto proto : remaining) {
           List<FileDescriptor> deps = new ArrayList<>();
           boolean allDepsFound = true;
           for (String depName : proto.getDependencyList()) {
             if (seen.containsKey(depName)) {
               deps.add(seen.get(depName));
             } else {
               allDepsFound = false;
               break;
             }
           }
           
           if (allDepsFound) {
             FileDescriptor fd = FileDescriptor.buildFrom(proto, deps.toArray(new FileDescriptor[0]));
             seen.put(proto.getName(), fd);
             if (proto.getName().equals(targetFile)) {
               target = fd;
             }
           } else {
             nextRemaining.add(proto);
           }
        }
        remaining = nextRemaining;
      }
      
      if (target == null) {
          throw new RuntimeException("Target file " + targetFile + " not found in descriptor set");
      }
      return target;
    }
  }
}
