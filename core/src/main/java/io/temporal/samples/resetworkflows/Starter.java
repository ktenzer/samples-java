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
import java.util.List;
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

    // query "new" customers for all "CustomerWorkflow" workflows with status "Running" (1)
    ListWorkflowExecutionsResponse listFailedWorkflows =
        getExecutionsResponse(
            "WorkflowType='ResetWorkflow' and ExecutionStatus="
                + WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_FAILED_VALUE);

    // sleep for 3 seconds before we shut down the worker
    sleep(5);

    List<WorkflowExecutionInfo> newExecutionInfo = listFailedWorkflows.getExecutionsList();
    batchResetWorkflows(newExecutionInfo);
    // for (WorkflowExecutionInfo wei : newExecutionInfo) {
    //  System.out.println("Resetting workflow: " + wei.getExecution().getWorkflowId());
    //  System.out.println(wei.getStatusValue());
    //  resetWorkflow(wei.getExecution().getWorkflowId());
    // }

    System.exit(0);
  }

  private static ListWorkflowExecutionsResponse getExecutionsResponse(String query) {
    ListWorkflowExecutionsRequest listWorkflowExecutionRequest =
        ListWorkflowExecutionsRequest.newBuilder()
            .setNamespace(client.getOptions().getNamespace())
            .setQuery(query)
            .build();
    ListWorkflowExecutionsResponse listWorkflowExecutionsResponse =
        service.blockingStub().listWorkflowExecutions(listWorkflowExecutionRequest);
    return listWorkflowExecutionsResponse;
  }

  //  private static void resetWorkflow(String workflowId) {
  //    long eventId = 3;
  //    WorkflowStub existingUntyped = client.newUntypedWorkflowStub(workflowId);
  //
  //    ResetWorkflowExecutionRequest resetWorkflowExecutionRequest =
  //        ResetWorkflowExecutionRequest.newBuilder()
  //            .setRequestId(UUID.randomUUID().toString())
  //            .setNamespace("default")
  //            .setWorkflowExecution(existingUntyped.getExecution())
  //            .setWorkflowTaskFinishEventId(eventId)
  //            .setReason("Doing a workflow reset...")
  //            .build();
  //
  //    try {
  //      ResetWorkflowExecutionResponse resetWorkflowExecutionResponse =
  //          service.blockingStub().resetWorkflowExecution(resetWorkflowExecutionRequest);
  //      System.out.println(
  //          "Workflow with ID "
  //              + workflowId
  //              + " has been reset. New RunId: "
  //              + resetWorkflowExecutionResponse.getRunId());
  //    } catch (Exception e) {
  //      System.err.println("Failed to reset workflow with ID " + workflowId);
  //    }
  //
  //    // Cleanup
  //    service.shutdown();
  //  }

  private static void batchResetWorkflows(List<WorkflowExecutionInfo> newExecutionInfo) {
    for (WorkflowExecutionInfo wei : newExecutionInfo) {
      String workflowId = wei.getExecution().getWorkflowId();
      long eventId = 3;
      WorkflowStub existingUntyped = client.newUntypedWorkflowStub(workflowId);

      ResetWorkflowExecutionRequest resetWorkflowExecutionRequest =
          ResetWorkflowExecutionRequest.newBuilder()
              .setNamespace("default")
              .setWorkflowExecution(existingUntyped.getExecution())
              .setWorkflowTaskFinishEventId(eventId)
              .setRequestId(java.util.UUID.randomUUID().toString())
              .setReason("Doing a batch reset...")
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

    // Cleanup
    service.shutdown();
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
}
