/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.resetworkflows;

import com.google.protobuf.ByteString;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import io.temporal.api.workflowservice.v1.ResetWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.ResetWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Starter {
  public static final String TASK_QUEUE = "resetTaskQueue";
  private static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
  private static final WorkflowClient client = WorkflowClient.newInstance(service);
  // private static final WorkerFactory factory = WorkerFactory.newInstance(client);

  public static void main(String[] args) {

    // start customer workflows and define custom search attributes for each
    startWorkflow();

    // small delay before we start querying executions
    try {
      Thread.sleep(2 * 1000);
    } catch (InterruptedException e) {
      throw new RuntimeException("Exception happened in thread sleep: ", e);
    }

    String fiveMinutesAgo = dateOffset(20);
    resetWorkflowExecutions(
        "WorkflowType='ResetWorkflow' and ExecutionStatus="
            + WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_FAILED_VALUE
            + " and CloseTime>"
            + "'"
            + fiveMinutesAgo
            + "'",
        null);

    // sleep for 3 seconds before we shut down the worker
    sleep(5);

    // Cleanup
    service.shutdown();
    System.exit(0);
  }

  private static void resetWorkflowExecutions(String query, ByteString token) {

    ListWorkflowExecutionsRequest request;

    if (token == null) {
      request =
          ListWorkflowExecutionsRequest.newBuilder()
              .setNamespace(client.getOptions().getNamespace())
              .setQuery(query)
              .build();
    } else {
      request =
          ListWorkflowExecutionsRequest.newBuilder()
              .setNamespace(client.getOptions().getNamespace())
              .setQuery(query)
              .setNextPageToken(token)
              .build();
    }

    ListWorkflowExecutionsResponse response =
        service.blockingStub().listWorkflowExecutions(request);

    for (WorkflowExecutionInfo workflowExecutionInfo : response.getExecutionsList()) {
      System.out.println(
          "Workflow ID: "
              + workflowExecutionInfo.getExecution().getWorkflowId()
              + " Run ID: "
              + workflowExecutionInfo.getExecution().getRunId()
              + " Status: "
              + workflowExecutionInfo.getStatus());
      if (workflowExecutionInfo.getParentExecution() != null) {
        System.out.println(
            "****** PARENT: "
                + workflowExecutionInfo.getExecution().getWorkflowId()
                + " - "
                + workflowExecutionInfo.getExecution().getRunId());
        resetWorkflow(workflowExecutionInfo.getExecution().getWorkflowId());
      }
    }

    if (response.getNextPageToken() != null && response.getNextPageToken().size() > 0) {
      resetWorkflowExecutions(query, response.getNextPageToken());
    }
  }

  private static void resetWorkflow(String workflowId) {
    long eventId = 3;
    WorkflowStub existingUntyped = client.newUntypedWorkflowStub(workflowId);

    ResetWorkflowExecutionRequest resetWorkflowExecutionRequest =
        ResetWorkflowExecutionRequest.newBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setNamespace("default")
            .setWorkflowExecution(existingUntyped.getExecution())
            .setWorkflowTaskFinishEventId(eventId)
            .setReason("Doing a workflow reset...")
            .build();

    try {
      ResetWorkflowExecutionResponse resetWorkflowExecutionResponse =
          service.blockingStub().resetWorkflowExecution(resetWorkflowExecutionRequest);
      System.out.println(
          "Workflow with ID "
              + workflowId
              + " has been reset. New RunId: "
              + resetWorkflowExecutionResponse.getRunId());
    } catch (Exception e) {
      System.err.println("Failed to reset workflow with ID " + workflowId);
    }
  }

  private static void startWorkflow() {
    // start a workflow for each customer that we need to add message to account

    for (int i = 1; i < 6; i++) {
      String message = "Running reset workflow: " + i;

      WorkflowOptions newResetWorkflowOptions =
          WorkflowOptions.newBuilder()
              .setWorkflowId("reset-workflow-" + i)
              .setTaskQueue(TASK_QUEUE)
              .build();
      ResetWorkflow newResetWorkflow =
          client.newWorkflowStub(ResetWorkflow.class, newResetWorkflowOptions);
      // start async
      WorkflowClient.start(newResetWorkflow::runActivities, message);
    }
  }

  private static void sleep(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException e) {
      System.out.println("Exception: " + e.getMessage());
      System.exit(0);
    }
  }

  private static String dateOffset(long seconds) {
    long currentTimeMillis = System.currentTimeMillis() - (seconds * 1000);
    OffsetDateTime offsetDateTime =
        OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(currentTimeMillis), ZoneOffset.UTC);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    String formattedDateTime = offsetDateTime.format(formatter);

    return formattedDateTime;
  }
}
