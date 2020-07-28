/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.releasenotes.gitlab.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Github Pull Request.
 *
 * @author Madhura Bhave
 */
public class PullRequest {

  private final Integer mergeRequestsCount;

  public PullRequest( Integer mergeRequestsCount) {
    this.mergeRequestsCount = mergeRequestsCount;
  }

  public Integer getMergeRequestsCount() {
    return this.mergeRequestsCount;
  }

}
