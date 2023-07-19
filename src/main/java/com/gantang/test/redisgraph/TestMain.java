package com.gantang.test.redisgraph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import com.redislabs.redisgraph.impl.api.RedisGraph;

import cn.hutool.core.util.IdUtil;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

public class TestMain {

    public static final String CURRENT_GRAPH_NAME_1 = "Graph1";
    public static final String CURRENT_GRAPH_NAME_2 = "Graph2";

    public static final String ORGANIZATION_ROOT_TYPE = "ORGANIZATION_ROOT";
    public static final String ORGANIZATION_TYPE = "ORGANIZATION";
    public static final String USER_TYPE = "USER";
    public static final String ORGANIZATION_ROOT_ID = "ORG-ROOT";
    public static final String P_ORG_MEMBER = "org_member";
    public static final String P_USER_MEMBER = "user_member";

    private String host = "localhost";
    private int port = 6379;
    private String password = "123456";

    private RedisGraph graph;
    private List<List<Node>> nodes = new ArrayList<>();

    private boolean graphTurn = true;
    private String graphName = null;

    public static void main(String[] args) {
        TestMain testMain = new TestMain();
        testMain.start();
    }

    public void start() {
        initPool();
        initData();

        Thread testThread = new Thread(() -> {
            while (true) {
                graphName = graphTurn ? CURRENT_GRAPH_NAME_1 : CURRENT_GRAPH_NAME_2;

                System.out.println("Create graph ...");
                long beginTime = System.currentTimeMillis();

                try {
                    try {
                        graph.deleteGraph(graphName);
                    } catch (Exception e) {
                    }

                    graphQuery("CREATE INDEX ON :" + ORGANIZATION_ROOT_TYPE + "(oid)");
                    graphQuery("CREATE INDEX ON :" + ORGANIZATION_TYPE + "(oid)");
                    graphQuery("CREATE INDEX ON :" + USER_TYPE + "(oid)");

                    // Create Root Organization
                    graphQuery("CREATE " + String.format("(:%s{oid:'%s',name:'%s'})", ORGANIZATION_ROOT_TYPE, ORGANIZATION_ROOT_ID, "ROOT"));

                    // Create node
                    final CountDownLatch nodeCountDownLatch = new CountDownLatch(nodes.size());
                    for (List<Node> subNodes : nodes) {
                        Thread createNodeThread = new Thread(() -> {
                            createNode(subNodes, nodeCountDownLatch);
                        });
                        createNodeThread.setDaemon(true);
                        createNodeThread.start();
                    }
                    nodeCountDownLatch.await();

                    // Create path
                    final CountDownLatch pathCountDownLatch = new CountDownLatch(nodes.size());
                    for (List<Node> subNodes : nodes) {
                        Thread createPathThread = new Thread(() -> {
                            createPath(subNodes, pathCountDownLatch);
                        });
                        createPathThread.setDaemon(true);
                        createPathThread.start();
                    }
                    pathCountDownLatch.await();

                    graphTurn = !graphTurn;
                    System.out.println("Successfully created graph（" + (System.currentTimeMillis() - beginTime) + "ms): " + graphName);
                } catch (Exception e) {
                    System.out.println("Failed to create graph（" + (System.currentTimeMillis() - beginTime) + "ms): " + e.getMessage());
                }

                try {
                    Thread.sleep(60 * 1000);
                } catch (Exception e) {
                }
            }
        });
        testThread.start();
    }

    private void initPool() {
        // Connection pool Configuration
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(50);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(30000);
        poolConfig.setMinEvictableIdleTimeMillis(60000);
        poolConfig.setNumTestsPerEvictionRun(-1);

        // Create RedisGraph Connection pool
        JedisPool jedisPool = new JedisPool(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT * 10, password);
        graph = new RedisGraph(jedisPool);
    }

    private void initData() {
        String id = null;

        for (int l0 = 0; l0 < 20; l0++) {
            List<Node> subNodes = new ArrayList<>();

            id = IdUtil.fastSimpleUUID();
            Node orgNode0 = new Node(id, ORGANIZATION_TYPE, "ORG-" + id, ORGANIZATION_ROOT_ID, ORGANIZATION_ROOT_TYPE, P_ORG_MEMBER);
            subNodes.add(orgNode0);

            for (int ui = 0; ui < 20; ui++) {
                id = IdUtil.fastSimpleUUID();
                Node userNode = new Node(id, USER_TYPE, "USER-" + id, orgNode0.getId(), ORGANIZATION_TYPE, P_USER_MEMBER);
                subNodes.add(userNode);
            }

            for (int l1 = 0; l1 < 20; l1++) {
                id = IdUtil.fastSimpleUUID();
                Node orgNode1 = new Node(id, ORGANIZATION_TYPE, "ORG-" + id, orgNode0.getId(), ORGANIZATION_TYPE, P_ORG_MEMBER);
                subNodes.add(orgNode1);

                for (int ui = 0; ui < 20; ui++) {
                    id = IdUtil.fastSimpleUUID();
                    Node userNode = new Node(id, USER_TYPE, "USER-" + id, orgNode1.getId(), ORGANIZATION_TYPE, P_USER_MEMBER);
                    subNodes.add(userNode);
                }

                for (int l2 = 0; l2 < 20; l2++) {
                    id = IdUtil.fastSimpleUUID();
                    Node orgNode2 = new Node(id, ORGANIZATION_TYPE, "ORG-" + id, orgNode1.getId(), ORGANIZATION_TYPE, P_ORG_MEMBER);
                    subNodes.add(orgNode2);

                    for (int ui = 0; ui < 20; ui++) {
                        id = IdUtil.fastSimpleUUID();
                        Node userNode = new Node(id, USER_TYPE, "USER-" + id, orgNode2.getId(), ORGANIZATION_TYPE, P_USER_MEMBER);
                        subNodes.add(userNode);
                    }
                }
            }

            nodes.add(subNodes);
        }
    }

    private void createNode(List<Node> subNodes, CountDownLatch nodeCountDownLatch) {
        String cmd = "(:%s{oid:'%s',name:'%s'})";

        try {
            StringBuffer subCmd = new StringBuffer();
            int count = 0;
            for (Node subNode : subNodes) {
                if (subCmd.length() > 0) {
                    subCmd.append(",");
                }
                subCmd.append(String.format(cmd, subNode.getType(), subNode.getId(), subNode.getName()));
                count++;

                if (count == 5000) {
                    graphQuery("CREATE " + subCmd.toString());
                    count = 0;
                    subCmd.setLength(0);
                }
            }

            if (subCmd.length() > 0) {
                graphQuery("CREATE " + subCmd.toString());
            }
        } catch (Exception e) {
        } finally {
            nodeCountDownLatch.countDown();
        }
    }

    private void createPath(List<Node> subNodes, CountDownLatch nodeCountDownLatch) {
        String matchCmd = "MATCH (s:%s{oid:'%s'}) MATCH (t:%s{oid:'%s'}) ";
        String mergeCmd = "CREATE (s)-[:%s]->(t)";

        try {
            StringBuffer matchSubCmd = new StringBuffer();
            StringBuffer mergeSubCmd = new StringBuffer();
            for (Node subNode : subNodes) {
                matchSubCmd.append(String.format(matchCmd, subNode.getType(), subNode.getId(), subNode.getParenType(), subNode.getParenId()));
                mergeSubCmd.append(String.format(mergeCmd, subNode.getPathType()));

                graphQuery(matchSubCmd.toString() + mergeSubCmd.toString());
                matchSubCmd.setLength(0);
                mergeSubCmd.setLength(0);
            }
        } catch (Exception e) {
        } finally {
            nodeCountDownLatch.countDown();
        }
    }

    private void graphQuery(String cmd) {
        graph.query(graphName, cmd);
    }

}
