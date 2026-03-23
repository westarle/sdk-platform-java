/*
 * Copyright 2026 Google LLC
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

package com.google.api.gax.tracing;

import com.google.api.core.BetaApi;
import com.google.api.core.InternalApi;
import java.util.List;

/** A composite {@link ApiTracer} that delegates to a list of {@link ApiTracer}s. */
@BetaApi
@InternalApi
public class CompositeTracer implements ApiTracer {
  private final List<ApiTracer> tracers;

  public CompositeTracer(List<ApiTracer> tracers) {
    this.tracers = tracers;
  }

  public List<ApiTracer> getTracers() {
    return tracers;
  }

  @Override
  public Scope inScope() {
    // Returning a scope that closes all sub-scopes
    final Scope[] scopes = new Scope[tracers.size()];
    for (int i = 0; i < tracers.size(); i++) {
      scopes[i] = tracers.get(i).inScope();
    }
    return () -> {
      for (int i = scopes.length - 1; i >= 0; i--) {
        Scope scope = scopes[i];
        if (scope != null) {
          scope.close();
        }
      }
    };
  }

  @Override
  public void operationSucceeded() {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).operationSucceeded();
    }
  }

  @Override
  public void operationCancelled() {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).operationCancelled();
    }
  }

  @Override
  public void operationFailed(Throwable error) {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).operationFailed(error);
    }
  }

  @Override
  public void connectionSelected(String id) {
    for (ApiTracer tracer : tracers) {
      tracer.connectionSelected(id);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void attemptStarted(int attemptNumber) {
    for (ApiTracer tracer : tracers) {
      tracer.attemptStarted(attemptNumber);
    }
  }

  @Override
  public void attemptStarted(Object request, int attemptNumber) {
    for (ApiTracer tracer : tracers) {
      tracer.attemptStarted(request, attemptNumber);
    }
  }

  @Override
  public void attemptSucceeded() {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).attemptSucceeded();
    }
  }

  @Override
  public void attemptCancelled() {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).attemptCancelled();
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void attemptFailed(Throwable error, org.threeten.bp.Duration delay) {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).attemptFailed(error, delay);
    }
  }

  @Override
  public void attemptFailedDuration(Throwable error, java.time.Duration delay) {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).attemptFailedDuration(error, delay);
    }
  }

  @Override
  public void attemptFailedRetriesExhausted(Throwable error) {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).attemptFailedRetriesExhausted(error);
    }
  }

  @Override
  public void attemptPermanentFailure(Throwable error) {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).attemptPermanentFailure(error);
    }
  }

  @Override
  public void lroStartFailed(Throwable error) {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).lroStartFailed(error);
    }
  }

  @Override
  public void lroStartSucceeded() {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).lroStartSucceeded();
    }
  }

  @Override
  public void responseReceived() {
    for (int i = tracers.size() - 1; i >= 0; i--) {
      tracers.get(i).responseReceived();
    }
  }

  @Override
  public void requestSent() {
    for (ApiTracer tracer : tracers) {
      tracer.requestSent();
    }
  }

  @Override
  public void batchRequestSent(long elementCount, long requestSize) {
    for (ApiTracer tracer : tracers) {
      tracer.batchRequestSent(elementCount, requestSize);
    }
  }
}
