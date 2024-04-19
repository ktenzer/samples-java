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

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResetActivitiesImpl implements ResetActivities {

  private static final Logger log = LoggerFactory.getLogger(ResetActivitiesImpl.class);

  @Override
  public void activityOne() {
    // simulate some actual work...
    sleepSeconds(1);
    thisMayOrMayNotThrowAnError("activityOne");
  }

  @Override
  public void activityTwo() {
    // simulate some actual work...
    sleepSeconds(1);
    thisMayOrMayNotThrowAnError("activityTwo");
  }

  @Override
  public void activityThree() {
    // simulate some actual work...
    sleepSeconds(1);
    thisMayOrMayNotThrowAnError("activityThree");
  }

  private void sleepSeconds(int seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException e) {
      // This is being swallowed on purpose
      Thread.currentThread().interrupt();
      log.error("Exception in thread sleep: ", e);
    }
  }

  private void thisMayOrMayNotThrowAnError(String activityName) {
    Random random = new Random();
    double randomValue = random.nextDouble();
    log.info("**** Random value: {}", randomValue);
    if (randomValue < 0.10) { // 10% chance of failure
      log.info("Activity {} failed...", activityName);
      throw new IllegalArgumentException("Activity has illegal argument...failing");
    }
  }
}
