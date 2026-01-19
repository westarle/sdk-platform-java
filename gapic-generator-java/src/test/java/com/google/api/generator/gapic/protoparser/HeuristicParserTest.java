package com.google.api.generator.gapic.protoparser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.api.generator.gapic.model.Method;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.model.Transport;
import com.google.testgapic.v1beta1.LockerProto;
import com.google.testgapic.v1beta1.ExplodedProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeuristicParserTest {
  private ServiceDescriptor lockerService;
  private FileDescriptor lockerFileDescriptor;
  private ServiceDescriptor explodedService;
  private FileDescriptor explodedFileDescriptor;

  @BeforeEach
  void setUp() {
    lockerFileDescriptor = LockerProto.getDescriptor();
    lockerService = lockerFileDescriptor.getServices().get(0);
    explodedFileDescriptor = ExplodedProto.getDescriptor();
    explodedService = explodedFileDescriptor.getServices().get(0);
  }

  @Test
  void heuristic_getFolder_standardPattern() {
    Map<String, Message> messageTypes = Parser.parseMessages(lockerFileDescriptor);
    Map<String, ResourceName> resourceNames = Parser.parseResourceNames(lockerFileDescriptor);
    Set<ResourceName> outputResourceNames = new HashSet<>();
    
    List<Method> methods = Parser.parseMethods(
        lockerService,
        "com.google.testgapic.v1beta1",
        "com.google.testgapic.v1beta1",
        messageTypes,
        resourceNames,
        Optional.empty(),
        Optional.empty(),
        outputResourceNames,
        Transport.GRPC);
        
    Method getFolder = methods.stream()
        .filter(m -> m.name().equals("GetFolder"))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("GetFolder not found"));
        
    assertEquals("name", getFolder.resourceNameField());
  }

  @Test
  void heuristic_getInstance_explodedPattern() {
    Map<String, Message> messageTypes = Parser.parseMessages(explodedFileDescriptor);
    Map<String, ResourceName> resourceNames = Parser.parseResourceNames(explodedFileDescriptor);
    Set<ResourceName> outputResourceNames = new HashSet<>();

    List<Method> methods = Parser.parseMethods(
        explodedService,
        "com.google.testgapic.v1beta1",
        "com.google.testgapic.v1beta1",
        messageTypes,
        resourceNames,
        Optional.empty(),
        Optional.empty(),
        outputResourceNames,
        Transport.GRPC);

    Method getInstance = methods.stream()
        .filter(m -> m.name().equals("GetInstance"))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("GetInstance not found"));

    // For exploded pattern projects/{project}/zones/{zone}/instances/{instance},
    // the heuristic should identify the leaf variable "instance".
    assertEquals("instance", getInstance.resourceNameField());
  }
}
