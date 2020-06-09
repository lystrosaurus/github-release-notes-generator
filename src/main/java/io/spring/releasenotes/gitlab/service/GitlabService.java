/*
 * Copyright 2018-2020 the original author or authors.
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

package io.spring.releasenotes.gitlab.service;

import io.spring.releasenotes.gitlab.payload.Issue;
import io.spring.releasenotes.gitlab.payload.Milestone;
import io.spring.releasenotes.properties.ApplicationProperties;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Central class for interacting with GitHub's REST API.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
@Component
public class GitlabService {

  private static final Pattern LINK_PATTERN = Pattern.compile("<(.+)>; rel=\"(.+)\"");

  private static final String MILESTONES_URI = "/projects/{repository}/milestones";

  private static final String ISSUES_URI =
      "/projects/{repository}/issues?milestone={milestone}&state=closed";

  private static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";

  private final RestTemplate restTemplate;

  private final String privateToken;

  public GitlabService(RestTemplateBuilder builder, ApplicationProperties properties) {
    String privateToken = properties.getGitlab().getPrivateToken();
    if (StringUtils.hasLength(privateToken)) {
      this.privateToken = privateToken;
    } else {
      this.privateToken = "";
    }
    builder = builder.rootUri(properties.getGitlab().getApiUrl());
    this.restTemplate = builder.build();
  }

  public int getMilestoneNumber(String milestoneTitle, String repository) {
    Assert.hasText(milestoneTitle, "MilestoneName must not be empty");
    List<Milestone> milestones = getAll(Milestone.class, MILESTONES_URI, repository);
    for (Milestone milestone : milestones) {
      if (milestoneTitle.equalsIgnoreCase(milestone.getTitle())) {
        return milestone.getNumber();
      }
    }
    throw new IllegalStateException(
        "Unable to find open milestone with title '" + milestoneTitle + "'");
  }

  public List<Issue> getIssuesForMilestone(int milestoneNumber, String repository) {
    return getAll(Issue.class, ISSUES_URI, repository, milestoneNumber);
  }

  public List<Issue> getIssuesForMilestone(String milestoneTitle, String repository) {
    return getAll(Issue.class, ISSUES_URI, repository, milestoneTitle);
  }

  private <T> List<T> getAll(Class<T> type, String url, Object... uriVariables) {
    List<T> all = new ArrayList<>();
    Page<T> page = getPage(type, url, uriVariables);
    while (page != null) {
      all.addAll(page.getContent());
      page = page.getNextPage();
    }
    return all;
  }

  private <T> Page<T> getPage(Class<T> type, String url, Object... uriVariables) {
    if (!StringUtils.hasText(url)) {
      return null;
    }
    HttpHeaders headers = new HttpHeaders();
    headers.add(PRIVATE_TOKEN, privateToken);

    HttpEntity<String> entity = new HttpEntity<>("", headers);

    ResponseEntity<T[]> response =
        this.restTemplate.exchange(url, HttpMethod.GET, entity, arrayType(type), uriVariables);
    return new Page<>(
        Arrays.asList(response.getBody()), () -> getPage(type, getNextUrl(response.getHeaders())));
  }

  @SuppressWarnings("unchecked")
  private <T> Class<T[]> arrayType(Class<T> elementType) {
    return (Class<T[]>) Array.newInstance(elementType, 0).getClass();
  }

  private String getNextUrl(HttpHeaders headers) {
    String links = headers.getFirst("Link");
    for (String link : StringUtils.commaDelimitedListToStringArray(links)) {
      Matcher matcher = LINK_PATTERN.matcher(link.trim());
      if (matcher.matches() && "next".equals(matcher.group(2))) {
        return matcher.group(1);
      }
    }
    return null;
  }
}
