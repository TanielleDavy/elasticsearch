/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.snapshotlifecycle.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.snapshotlifecycle.SnapshotLifecycleMetadata;
import org.elasticsearch.xpack.core.snapshotlifecycle.SnapshotLifecyclePolicyMetadata;
import org.elasticsearch.xpack.core.snapshotlifecycle.action.ExecuteSnapshotLifecycleAction;
import org.elasticsearch.xpack.core.snapshotlifecycle.history.SnapshotHistoryStore;
import org.elasticsearch.xpack.snapshotlifecycle.SnapshotLifecycleService;
import org.elasticsearch.xpack.snapshotlifecycle.SnapshotLifecycleTask;

import java.io.IOException;
import java.util.Optional;

public class TransportExecuteSnapshotLifecycleAction
    extends TransportMasterNodeAction<ExecuteSnapshotLifecycleAction.Request, ExecuteSnapshotLifecycleAction.Response> {

    private static final Logger logger = LogManager.getLogger(TransportExecuteSnapshotLifecycleAction.class);

    private final Client client;
    private final SnapshotHistoryStore historyStore;

    @Inject
    public TransportExecuteSnapshotLifecycleAction(TransportService transportService, ClusterService clusterService, ThreadPool threadPool,
                                                   ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                                   Client client, SnapshotHistoryStore historyStore) {
        super(ExecuteSnapshotLifecycleAction.NAME, transportService, clusterService, threadPool, actionFilters, indexNameExpressionResolver,
            ExecuteSnapshotLifecycleAction.Request::new);
        this.client = client;
        this.historyStore = historyStore;
    }
    @Override
    protected String executor() {
        return ThreadPool.Names.SNAPSHOT;
    }

    @Override
    protected ExecuteSnapshotLifecycleAction.Response read(StreamInput in) throws IOException {
        return new ExecuteSnapshotLifecycleAction.Response(in);
    }

    @Override
    protected void masterOperation(final Task task, final ExecuteSnapshotLifecycleAction.Request request,
                                   final ClusterState state,
                                   final ActionListener<ExecuteSnapshotLifecycleAction.Response> listener) {
        try {
            final String policyId = request.getLifecycleId();
            SnapshotLifecycleMetadata snapMeta = state.metaData().custom(SnapshotLifecycleMetadata.TYPE);
            if (snapMeta == null) {
                listener.onFailure(new IllegalArgumentException("no such snapshot lifecycle policy [" + policyId + "]"));
                return;
            }

            SnapshotLifecyclePolicyMetadata policyMetadata = snapMeta.getSnapshotConfigurations().get(policyId);
            if (policyMetadata == null) {
                listener.onFailure(new IllegalArgumentException("no such snapshot lifecycle policy [" + policyId + "]"));
                return;
            }

            final Optional<String> snapshotName = SnapshotLifecycleTask.maybeTakeSnapshot(SnapshotLifecycleService.getJobId(policyMetadata),
                client, clusterService, historyStore);
            if (snapshotName.isPresent()) {
                listener.onResponse(new ExecuteSnapshotLifecycleAction.Response(snapshotName.get()));
            } else {
                listener.onFailure(new ElasticsearchException("failed to execute snapshot lifecycle policy [" + policyId + "]"));
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    protected ClusterBlockException checkBlock(ExecuteSnapshotLifecycleAction.Request request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
