/*
 * Copyright (c) 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jponge.vertx.elasticsearch;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

class DocumentApiImpl implements DocumentApi {

  private static final DocumentApiOptions EMPTY_OPTIONS = new DocumentApiOptions();

  private final Vertx vertx;
  private final ElasticSearchClientOptions options;
  private final WebClient webClient;

  DocumentApiImpl(Vertx vertx, ElasticSearchClientOptions options) {
    this.vertx = vertx;
    this.options = options;
    webClient = WebClient.create(vertx, new WebClientOptions()
      .setKeepAlive(true)
      .setDefaultHost(options.getHostname())
      .setDefaultPort(options.getPort()));
  }

  @Override
  public DocumentApi addOrUpdate(String index, String type, String id, JsonObject document, Handler<AsyncResult<JsonObject>> handler) {
    return addOrUpdate(index, type, id, EMPTY_OPTIONS, document, handler);
  }

  @Override
  public DocumentApi addOrUpdate(String index, String type, String id, DocumentApiOptions options, JsonObject document, Handler<AsyncResult<JsonObject>> handler) {
    String url = index + "/" + type + "/" + id;
    HttpRequest<JsonObject> request = webClient
      .put(url)
      .as(BodyCodec.jsonObject());
    applyOptions(options, request);
    request.sendJsonObject(document, ar -> {
      if (ar.succeeded()) {
        handler.handle(succeededFuture(ar.result().body()));
      } else {
        handler.handle(failedFuture(ar.cause()));
      }
    });
    return this;
  }

  private void applyOptions(DocumentApiOptions options, HttpRequest<JsonObject> request) {
    options.getOptions().forEach((k, v) -> request.addQueryParam(k, v.toString()));
  }

  @Override
  public DocumentApi add(String index, String type, JsonObject document, Handler<AsyncResult<JsonObject>> handler) {
    String url = index + "/" + type;
    webClient
      .post(url)
      .as(BodyCodec.jsonObject())
      .sendJsonObject(document, ar -> {
        if (ar.succeeded()) {
          handler.handle(succeededFuture(ar.result().body()));
        } else {
          handler.handle(failedFuture(ar.cause()));
        }
      });
    return this;
  }

  @Override
  public DocumentApi exists(String index, String type, String id, Handler<AsyncResult<Boolean>> handler) {
    String url = index + "/" + type + "/" + id;
    webClient
      .head(url)
      .send(ar -> {
        if (ar.succeeded()) {
          handler.handle(succeededFuture(ar.result().statusCode() == 200));
        } else {
          handler.handle(failedFuture(ar.cause()));
        }
      });
    return this;
  }

  @Override
  public DocumentApi get(String index, String type, String id, Handler<AsyncResult<JsonObject>> handler) {
    return get(index, type, id, EMPTY_OPTIONS, handler);
  }

  @Override
  public DocumentApi get(String index, String type, String id, DocumentApiOptions options, Handler<AsyncResult<JsonObject>> handler) {
    String url = index + "/" + type + "/" + id;
    HttpRequest<JsonObject> request = webClient
      .get(url)
      .as(BodyCodec.jsonObject());
    applyOptions(options, request);
    request.send(ar -> {
      if (ar.succeeded()) {
        handler.handle(succeededFuture(ar.result().body()));
      } else {
        handler.handle(failedFuture(ar.cause()));
      }
    });
    return this;
  }
}
