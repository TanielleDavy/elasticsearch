/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.action;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.support.master.AcknowledgedRequest;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.core.ml.dataframe.DataFrameAnalyticsConfig;
import org.elasticsearch.xpack.core.ml.job.messages.Messages;

import java.io.IOException;
import java.util.Objects;

public class PutDataFrameAnalyticsAction extends ActionType<PutDataFrameAnalyticsAction.Response> {

    public static final PutDataFrameAnalyticsAction INSTANCE = new PutDataFrameAnalyticsAction();
    public static final String NAME = "cluster:admin/xpack/ml/data_frame/analytics/put";

    private PutDataFrameAnalyticsAction() {
        super(NAME);
    }

    public static class Request extends AcknowledgedRequest<Request> implements ToXContentObject {

        public static Request parseRequest(String id, XContentParser parser) {
            DataFrameAnalyticsConfig.Builder config = DataFrameAnalyticsConfig.STRICT_PARSER.apply(parser, null);
            if (config.getId() == null) {
                config.setId(id);
            } else if (!Strings.isNullOrEmpty(id) && !id.equals(config.getId())) {
                // If we have both URI and body ID, they must be identical
                throw new IllegalArgumentException(Messages.getMessage(Messages.INCONSISTENT_ID, DataFrameAnalyticsConfig.ID,
                    config.getId(), id));
            }

            return new PutDataFrameAnalyticsAction.Request(config.build());
        }

        private DataFrameAnalyticsConfig config;

        public Request() {}

        public Request(StreamInput in) throws IOException {
            super(in);
            config = new DataFrameAnalyticsConfig(in);
        }

        public Request(DataFrameAnalyticsConfig config) {
            this.config = config;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            throw new UnsupportedOperationException("usage of Streamable is to be replaced by Writeable");
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            config.writeTo(out);
        }

        public DataFrameAnalyticsConfig getConfig() {
            return config;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            config.toXContent(builder, params);
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PutDataFrameAnalyticsAction.Request request = (PutDataFrameAnalyticsAction.Request) o;
            return Objects.equals(config, request.config);
        }

        @Override
        public int hashCode() {
            return Objects.hash(config);
        }
    }

    public static class Response extends ActionResponse implements ToXContentObject {

        private DataFrameAnalyticsConfig config;

        public Response(DataFrameAnalyticsConfig config) {
            this.config = config;
        }

        Response() {}

        Response(StreamInput in) throws IOException {
            super(in);
            config = new DataFrameAnalyticsConfig(in);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            throw new UnsupportedOperationException("usage of Streamable is to be replaced by Writeable");
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            config.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            config.toXContent(builder, params);
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Response response = (Response) o;
            return Objects.equals(config, response.config);
        }

        @Override
        public int hashCode() {
            return Objects.hash(config);
        }
    }

    public static class RequestBuilder extends MasterNodeOperationRequestBuilder<Request, Response, RequestBuilder> {

        protected RequestBuilder(ElasticsearchClient client, PutDataFrameAnalyticsAction action) {
            super(client, action, new Request());
        }
    }

}
