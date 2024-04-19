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

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ResetWorkflowImpl implements ResetWorkflow {
  // private boolean exit;
  private final ResetActivities resetActivities =
      Workflow.newActivityStub(
          ResetActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(10))
              .setRetryOptions(
                  RetryOptions.newBuilder()
                      .setInitialInterval(Duration.ofSeconds(1))
                      .setDoNotRetry(IllegalArgumentException.class.getName())
                      .build())
              .build());
  // private final RetryOptions customerRetryOptions =
  //    RetryOptions.newBuilder().setMaximumAttempts(5).build();
  // private final Duration expiration = Duration.ofMinutes(1);

  @Override
  public void runActivities(String message) {

    try {
      resetActivities.activityOne();
      resetActivities.activityTwo();
      resetActivities.activityThree();
    } catch (ActivityFailure e) {
      System.out.println("Activity failed: " + e.getMessage());
      throw ApplicationFailure.newNonRetryableFailure(
          "simulated failure", "some error", "ActivityFailure");
    }
  }
}
