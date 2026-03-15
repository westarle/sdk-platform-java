/*
 * Copyright 2025 Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.api.gax.grpc;

import static com.google.api.gax.logging.LoggingUtils.executeWithTryCatch;
import static com.google.api.gax.logging.LoggingUtils.logRequest;
import static com.google.api.gax.logging.LoggingUtils.logResponse;
import static com.google.api.gax.logging.LoggingUtils.recordResponseHeaders;
import static com.google.api.gax.logging.LoggingUtils.recordResponsePayload;
import static com.google.api.gax.logging.LoggingUtils.recordServiceRpcAndRequestHeaders;

import com.google.api.core.InternalApi;
import com.google.api.gax.logging.LogData;
import com.google.api.gax.logging.LoggerProvider;
import com.google.api.gax.logging.LoggingUtils;
import com.google.api.gax.rpc.ErrorDetails;
import com.google.rpc.ErrorInfo;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.HashMap;
import java.util.Map;

@InternalApi
public class GrpcLoggingInterceptor implements ClientInterceptor {

  private static final LoggerProvider LOGGER_PROVIDER =
      LoggerProvider.forClazz(GrpcLoggingInterceptor.class);

  ClientCall.Listener<?> currentListener; // expose for test setup

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
        next.newCall(method, callOptions)) {
      LogData.Builder logDataBuilder = LogData.builder();

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        recordServiceRpcAndRequestHeaders(
            method.getServiceName(),
            method.getFullMethodName(),
            null, // endpoint is for http request only
            metadataHeadersToMap(headers),
            logDataBuilder,
            LOGGER_PROVIDER);
        SimpleForwardingClientCallListener<RespT> responseLoggingListener =
            new SimpleForwardingClientCallListener<RespT>(responseListener) {
              @Override
              public void onHeaders(Metadata headers) {
                recordResponseHeaders(
                    metadataHeadersToMap(headers), logDataBuilder, LOGGER_PROVIDER);
                super.onHeaders(headers);
              }

              @Override
              public void onMessage(RespT message) {
                recordResponsePayload(message, logDataBuilder, LOGGER_PROVIDER);
                super.onMessage(message);
              }

              @Override
              public void onClose(Status status, Metadata trailers) {
                executeWithTryCatch(
                    () -> {
                      if (!status.isOk()
                          && LoggingUtils.isLoggingEnabled()
                          && LOGGER_PROVIDER.getLogger().isInfoEnabled()) {
                        Map<String, Object> logContext = new HashMap<>();
                        logContext.put("rpc.system", "grpc");
                        logContext.put("rpc.service", method.getServiceName());
                        logContext.put("rpc.method", method.getFullMethodName());
                        logContext.put("rpc.response.status_code", status.getCode().value());
                        logContext.put("grpc.status", status.getCode().name());
                        if (status.getDescription() != null) {
                          logContext.put("status.message", status.getDescription());
                        }

                        if (trailers != null) {
                          byte[] bytes =
                              trailers.get(
                                  Metadata.Key.of(
                                      "grpc-status-details-bin", Metadata.BINARY_BYTE_MARSHALLER));
                          if (bytes != null) {
                            com.google.rpc.Status rpcStatus =
                                com.google.rpc.Status.parseFrom(bytes);
                            ErrorDetails errorDetails =
                                ErrorDetails.builder()
                                    .setRawErrorMessages(rpcStatus.getDetailsList())
                                    .build();
                            ErrorInfo errorInfo = errorDetails.getErrorInfo();

                            if (errorInfo != null) {
                              if (!errorInfo.getReason().isEmpty()) {
                                logContext.put("error.type", errorInfo.getReason());
                              }
                              if (!errorInfo.getDomain().isEmpty()) {
                                logContext.put("error.domain", errorInfo.getDomain());
                              }
                              for (Map.Entry<String, String> entry :
                                  errorInfo.getMetadataMap().entrySet()) {
                                logContext.put(
                                    "error.metadata." + entry.getKey(), entry.getValue());
                              }
                            }
                          }
                        }
                        LoggingUtils.logActionableError(
                            logContext, LOGGER_PROVIDER, "Operation failed with actionable error");
                      }
                    });

                logResponse(status.getCode().toString(), logDataBuilder, LOGGER_PROVIDER);
                super.onClose(status, trailers);
              }
            };
        currentListener = responseLoggingListener;
        super.start(responseLoggingListener, headers);
      }

      @Override
      public void sendMessage(ReqT message) {
        logRequest(message, logDataBuilder, LOGGER_PROVIDER);
        super.sendMessage(message);
      }
    };
  }

  // Helper methods for logging
  private static Map<String, String> metadataHeadersToMap(Metadata headers) {

    Map<String, String> headersMap = new HashMap<>();
    executeWithTryCatch(
        () -> {
          for (String key : headers.keys()) {
            // grpc header values can be either ASCII strings or binary
            // https://grpc.io/docs/guides/metadata/#overview
            // this condition identified binary headers and skip for logging
            if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
              continue;
            }
            Metadata.Key<String> metadataKey =
                Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
            String headerValue = headers.get(metadataKey);

            headersMap.put(key, headerValue);
          }
        });

    return headersMap;
  }
}
