package com.leidos.xchangecore.core.infrastructure.xmpp.apps;

import java.io.FileNotFoundException;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.leidos.xchangecore.core.infrastructure.xmpp.communications.CoreConnection;

public class XmppNodesUtil {

    private static final Object DisplayNodesCommand = "-d";

    private static final Object CleanupNodesCommand = "-c";

    static Logger logger = Logger.getLogger(XmppNodesUtil.class);
    static String ContextFile = "XmppNodesUtilContext.xml";

    synchronized private static ApplicationContext initContext(String contextFile) {

        final String localContextFile = "./" + contextFile;
        ApplicationContext context = null;
        try {
            context = new FileSystemXmlApplicationContext(localContextFile);
            logger.debug("initContext: Using local context file: " + localContextFile);
        } catch (final BeanDefinitionStoreException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                logger.debug("initContext: Local Context File: " + localContextFile +
                             " not found, will load the default contexts path");
                try {
                    context = new ClassPathXmlApplicationContext(new String[] {
                        "contexts/" + contextFile
                    });
                } catch (final Exception ee) {
                    logger.error("initContext: Cannot load from classpath: " + ee.getMessage());
                }
            } else
                logger.error("initContext: Loading Context File: " + e.getCause().getMessage());
        }

        return context;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        final ApplicationContext context = initContext(ContextFile);

        final XmppNodesUtil util = (XmppNodesUtil) context.getBean("util");
        util.getCoreConnection().initialize();
        if (util.getCoreConnection().isConnected() == false) {
            System.err.println("Cannot connect to XMPP Server ...");
            System.exit(-1);
        }

        if (args[0].equals(CleanupNodesCommand)) {
            System.out.println("Clean up all nodes ...");
            util.getCoreXMPPUtils().deleteAllNodesRecursivly();
        } else if (args[0].equals(DisplayNodesCommand)) {
            System.out.println("Display all nodes ...");
            final List<String> nodeList = util.getCoreXMPPUtils().getAllNodesRecursivly();
            System.out.println("+++++ Display Nodes +++++");
            for (final String node : nodeList)
                System.out.println("\tNode: " + node);
            System.out.println("+++++++++++++++++++++++++");
        }
        // util.getCoreConnection().disconnect();
    }
    private CoreConnection coreConnection;

    private CoreXMPPUtils coreXMPPUtils;

    public CoreConnection getCoreConnection() {

        return this.coreConnection;
    }

    public CoreXMPPUtils getCoreXMPPUtils() {

        return this.coreXMPPUtils;
    }

    public void setCoreConnection(CoreConnection c) {

        this.coreConnection = c;
    }

    public void setCoreXMPPUtils(CoreXMPPUtils c) {

        this.coreXMPPUtils = c;
    }
}
