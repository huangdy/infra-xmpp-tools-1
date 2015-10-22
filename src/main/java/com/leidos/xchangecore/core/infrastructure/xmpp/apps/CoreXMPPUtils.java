package com.leidos.xchangecore.core.infrastructure.xmpp.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;

import com.leidos.xchangecore.core.infrastructure.xmpp.communications.CommandWithReply;
import com.leidos.xchangecore.core.infrastructure.xmpp.communications.CoreConnection;
import com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory;

public class CoreXMPPUtils {

    public class XMPPNode {
        public String name;

        public String type;
        public HashSet<XMPPNode> children;
        private int indents = 0;

        public XMPPNode() {
            this.children = new HashSet<XMPPNode>();
        }

        private void printRecursive(XMPPNode node, StringBuilder result) {
            final String newLine = System.getProperty("line.separator");

            result.append(this.indents++);
            result.append(" ");
            result.append(node.name);
            result.append(" : ");
            result.append(node.type);
            result.append(newLine);

            // leaf node
            if (node.children.size() == 0) {
                this.indents--;
                if (CoreXMPPUtils.this.interestManager != null) {
                    final ArrayList<String> items = CoreXMPPUtils.this.interestManager
                            .getAllNodeItems(CoreXMPPUtils.this.coreConnection.getPubSubSvc(),
                                    node.name);
                    if (items != null) {
                        for (final String item : items) {
                            result.append(item);
                            result.append(newLine);
                        }
                    }
                }
                return;
            }
            // collection node
            else {
                for (final XMPPNode childNode : node.children) {
                    this.printRecursive(childNode, result);
                }
                this.indents--;
            }
        }

        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder();

            final String newLine = System.getProperty("line.separator");

            result.append(this.getClass().getName() + " Object {");
            result.append(newLine);

            this.printRecursive(this, result);

            result.append("}");

            return result.toString();
        }
    }

    private CoreConnection coreConnection;
    private InterestManager interestManager;
    public static String LEAF_NODE = "leaf";

    // private String startingNode = "";

    public static String COLLECTION_NODE = "collection";

    public CoreXMPPUtils() {
    }

    public void deleteAllNodesRecursivly() {
        final List<String> nodes = this.getAllNodesRecursivly();
        for (final String node : nodes) {
            final IQ iq = PubSubIQFactory.deleteNode(this.coreConnection.getPubSubSvc(), node);
            CommandWithReply command;
            try {
                command = this.coreConnection.createCommandWithReply(iq);
                if (command.waitForSuccessOrFailure()) {
                    System.out.println("Deleting node " + node);
                } else {
                    if (command.getErrorCode() != 403) {
                        System.err.println("Interest group error deleting node " + node);
                        System.err.println("  message: " + command.getErrorMessage());
                        System.err.println("     code: " + command.getErrorCode());
                    } else {
                        System.err.println("No permissions to delete: " + node + " skipping it");
                        continue;
                    }
                }
            } catch (final XMPPException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    public List<String> getAllNodesRecursivly() {
        final ArrayList<String> nodeList = new ArrayList<String>();

        // Get the top level nodes
        final Set<String> topNodes = this.getChildrenNodes("");

        // Work down each top level node and add nodes to the list when they are leaf nodes
        for (final String topNode : topNodes) {
            final Set<String> nodeSet = this.getNodesRecusivly(topNode);
            nodeList.addAll(nodeSet);
        }

        return nodeList;
    }

    public Set<String> getChildrenNodes(String collectionNode) {
        final HashSet<String> nodeSet = new HashSet<String>();
        if (this.isCollection(collectionNode)) {
            final DiscoverItems discovers = this.interestManager.getFolderContents(
                    this.coreConnection.getPubSubSvc(), collectionNode);
            if (discovers != null) {
                final Iterator<DiscoverItems.Item> iterator = discovers.getItems();
                while (iterator.hasNext()) {
                    final DiscoverItems.Item item = iterator.next();
                    nodeSet.add(item.getNode());
                }
            }
        }
        return nodeSet;
    }

    public Map<String, Set<String>> getNodeMap() {
        final HashMap<String, Set<String>> nodeMap = new HashMap<String, Set<String>>();
        final Set<String> topNodes = this.getChildrenNodes("");
        for (final String node : topNodes) {
            System.out.println(node);
            nodeMap.put(node, this.getChildrenNodes(node));
        }
        return nodeMap;
    }

    public Set<XMPPNode> getNodeSet() {
        final HashSet<XMPPNode> xmppNodeSet = new HashSet<XMPPNode>();

        // Get the top level nodes
        final Set<String> topNodes = this.getChildrenNodes("");

        // Work down each top level node and add nodes to the list when they are leaf nodes
        for (final String topNode : topNodes) {
            final Set<XMPPNode> nodeSet = this.getXMPPNodesRecusivly(topNode);
            xmppNodeSet.addAll(nodeSet);
        }

        return xmppNodeSet;
    }

    public Set<String> getNodesRecusivly(String node) {
        final HashSet<String> nodes = new HashSet<String>();

        // If a collection node then recurse, else add node
        if (!this.isLeaf(node)) {
            Set<String> subNodes = this.getChildrenNodes(node);
            if (subNodes.size() > 0) {
                for (final String n : subNodes) {
                    subNodes = this.getNodesRecusivly(n);
                    nodes.addAll(subNodes);
                }
                nodes.add(node);
            } else {
                nodes.add(node);
            }
        } else {
            nodes.add(node);
        }

        return nodes;
    }

    public Set<XMPPNode> getXMPPNodesRecusivly(String node) {
        final HashSet<XMPPNode> nodes = new HashSet<XMPPNode>();

        // If a collection node then recurse, else add node
        if (!this.isLeaf(node)) {
            final XMPPNode xmppNode = new XMPPNode();
            xmppNode.name = node;
            xmppNode.type = COLLECTION_NODE;
            final Set<String> subNodes = this.getChildrenNodes(node);
            if (subNodes.size() > 0) {
                for (final String n : subNodes) {
                    final Set<XMPPNode> xmppNodes = this.getXMPPNodesRecusivly(n);
                    xmppNode.children.addAll(xmppNodes);
                }
                nodes.add(xmppNode);
            } else {
                nodes.add(xmppNode);
            }
        } else {
            final XMPPNode xmppNode = new XMPPNode();
            xmppNode.name = node;
            xmppNode.type = LEAF_NODE;
            nodes.add(xmppNode);
        }

        return nodes;
    }

    public boolean isCollection(String node) {
        boolean is = false;
        final DiscoverInfo info = this.coreConnection.discoverNodeInfo(node);
        // getDiscoManager().discoverInfo(
        // coreConnection.getPubSubSvc(), node);
        if (info != null) {
            final Iterator<org.jivesoftware.smackx.packet.DiscoverInfo.Identity> ids = info
                    .getIdentities();
            while (ids.hasNext()) {
                final String type = ids.next().getType();
                is = type.equalsIgnoreCase(COLLECTION_NODE);
            }
        }
        return is;
    }

    public boolean isLeaf(String node) {
        boolean is = false;
        final DiscoverInfo info = this.coreConnection.discoverNodeInfo(node);
        // .getDiscoManager().discoverInfo(
        // coreConnection.getPubSubSvc(), node);
        if (info != null) {
            final Iterator<org.jivesoftware.smackx.packet.DiscoverInfo.Identity> ids = info
                    .getIdentities();
            while (ids.hasNext()) {
                final String type = ids.next().getType();
                is = type.equalsIgnoreCase(LEAF_NODE);
            }
        }
        return is;
    }

    public void setCoreConnection(CoreConnection c) {
        this.coreConnection = c;
    }

    public void setInterestManager(InterestManager n) {
        this.interestManager = n;
    }
}
