/**
 * Copyright (c) 2015 Cloudant, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.sync.replication;

import com.cloudant.common.CouchUtils;
import com.cloudant.common.RequireRunningCouchDB;
import com.cloudant.mazha.Response;
import com.cloudant.sync.datastore.BasicDocumentRevision;
import com.cloudant.sync.datastore.DocumentBodyFactory;
import com.cloudant.sync.datastore.MutableDocumentRevision;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tomblench on 11/10/2014.
 */
@Category(RequireRunningCouchDB.class)
public class ResurrectedDocumentTest extends ReplicationTestBase {
    @Test
    public void resurrectedDocumentTest() throws Exception {
        for (int i=0; i<250; i++) {
            Map<String, Object> foo1 = new HashMap<String, Object>();
            foo1.put("_id", "doc-a-"+i);
            foo1.put("foo", "(from remoteDb)");
            Response response = remoteDb.create(foo1);
        }

        for (int i=0; i<10; i++) {
            Map<String, Object> foo1 = new HashMap<String, Object>();
            foo1.put("_id", "doc-b-"+i);
            foo1.put("foo", "(from remoteDb)");
            Response response = remoteDb.create(foo1);
        }

        for (int i=0; i<250; i++) {
            Map<String, String> foo1 = remoteDb.get(Map.class, "doc-a-"+i);
            remoteDb.delete(foo1.get("_id"), foo1.get("_rev"));
        }

        pull();
        push();

        for (int i=0; i<100; i++) {
            MutableDocumentRevision rev = new MutableDocumentRevision();
            rev.docId = "doc-a-"+i;
            rev.body = DocumentBodyFactory.create("{\"test\": \"content\"}".getBytes());
            BasicDocumentRevision newRev = datastore.createDocumentFromRevision(rev);
            Assert.assertEquals(3, CouchUtils.generationFromRevId(newRev.getRevision()));
        }

        push();
    }
    private void push() throws Exception {
        TestStrategyListener listener = new TestStrategyListener();
        BasicPushStrategy push = new BasicPushStrategy(this.createPushReplication());
        push.eventBus.register(listener);

        Thread t = new Thread(push);
        t.start();
        t.join();
        Assert.assertTrue(listener.finishCalled);
        Assert.assertFalse(listener.errorCalled);
    }

    private void pull() throws Exception {
        TestStrategyListener listener = new TestStrategyListener();
        BasicPullStrategy pull = new BasicPullStrategy(this.createPullReplication());
        pull.getEventBus().register(listener);

        Thread t = new Thread(pull);
        t.start();
        t.join();
        Assert.assertTrue(listener.finishCalled);
        Assert.assertFalse(listener.errorCalled);
    }
}
