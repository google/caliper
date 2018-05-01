package com.google.caliper.worker.handler;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.bridge.WorkerRequest;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;

/** Dispatches {@link WorkerRequest}s to the appropriate handler. */
public final class RequestDispatcher {

  private final ImmutableMap<Class<? extends WorkerRequest>, Provider<RequestHandler>>
      requestHandlers;

  @Inject
  RequestDispatcher(Map<Class<? extends WorkerRequest>, Provider<RequestHandler>> requestHandlers) {
    this.requestHandlers = ImmutableMap.copyOf(requestHandlers);
  }

  /** Dispatches the given request to the appropriate handler. */
  public void dispatch(WorkerRequest request) throws Exception {
    Class<? extends WorkerRequest> requestType = request.type();
    Provider<RequestHandler> handlerProvider =
        checkNotNull(
            requestHandlers.get(requestType),
            "No handler found for request of type %s",
            requestType.getName());

    handlerProvider.get().handleRequest(request);
  }
}
