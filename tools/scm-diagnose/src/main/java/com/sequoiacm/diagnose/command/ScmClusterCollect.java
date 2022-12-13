package com.sequoiacm.diagnose.command;

import com.sequoiacm.diagnose.collect.ClusterCollector;
import com.sequoiacm.diagnose.collect.LocalClusterCollector;
import com.sequoiacm.diagnose.collect.RemoteClusterCollector;
import com.sequoiacm.diagnose.common.CollectResult;
import com.sequoiacm.diagnose.common.ScmNodeInfo;
import com.sequoiacm.diagnose.config.CollectConfig;
import com.sequoiacm.diagnose.execption.CollectException;
import com.sequoiacm.diagnose.ssh.Ssh;
import com.sequoiacm.diagnose.utils.HostAddressUtils;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommon;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Command
public class ScmClusterCollect extends SubCommand {

    private static final String NAME = "cluster-info";
    private static final String URL_PREFIX = "http://";
    private static final String SERVICE_CENTER = "/service-center";
    private static final String INTERNAL_VERSION_INSTANCES = "/internal/v1/instances";
    private static final String GATEWAY = "gateway";
    private static final String HOSTS = "hosts";
    private static final String CONF = "conf";
    private static final String OUTPUT_PATH = "output-path";
    private static final String SHORT_OUTPUT_PATH = "o";
    private static final String THREAD = "thread-size";
    private static final String NEED_ZIP = "need-zip";
    private static final String RESULT_DIR = "scm-collect-cluster";
    public static String currentCollectPath = null;
    private static RestTemplate restTemplate;
    private static Integer collectFailCount = -1;
    // key:hostName,value:scm nodes on the host,the name is hostName
    private Map<String, List<ScmNodeInfo>> hostsMap = new TreeMap<>();
    // key:serviceName,value:number of scm node named serviceName
    private Map<String, Integer> servicesMap = new TreeMap<>();
    // key:region/zone,value:scm nodes in the region/zone
    private Map<String, Set<ScmNodeInfo>> nodesMap = new HashMap<>();
    private List<Ssh> sshList = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(ScmClusterCollect.class);

    static {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        restTemplate = new RestTemplate(factory);
    }

    @Override
    public void run(String[] args)
            throws ParseException, IOException, ScmToolsException, InterruptedException {
        Options ops = addParam();
        CommandLine commandLine = new DefaultParser().parse(ops, args, false);
        if (commandLine.hasOption("help")) {
            printHelp();
            System.exit(0);
        }

        System.out.println("[INFO ] start analyze parameter");
        CollectConfig.setResultDir(RESULT_DIR);
        String gatewayAddress = commandLine.getOptionValue(GATEWAY);

        CollectConfig.init(commandLine, sshList);

        System.out.println("[INFO ] analyze parameter finished");
        logger.info("analyze parameter finished");

        BasicBSONList resp;
        String url = URL_PREFIX + gatewayAddress + SERVICE_CENTER + INTERNAL_VERSION_INSTANCES;
        try {
            resp = restTemplate.getForObject(url, BasicBSONList.class);
        }
        catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            throw e;
        }
        for (Object o : resp) {
            Map resultMap = (Map) o;
            ScmNodeInfo result = new ScmNodeInfo(resultMap);
            nodeCollect(result);
        }

        String dateTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        currentCollectPath = CollectConfig.getResultDir() + "_" + dateTime;
        File outputFile = new File(
                CollectConfig.getOutputPath() + File.separator + currentCollectPath);
        if (outputFile.isDirectory()) {
            FileUtils.deleteDirectory(outputFile);
        }
        FileUtils.forceMkdir(outputFile);
        String clusterInfoPath = outputFile.getAbsolutePath() + File.separator + "cluster_info.txt";
        List<CollectResult> collectResult = clusterCollect();
        printClusterInfo(resp.size(), clusterInfoPath);
        printResult(collectResult);
    }

    private void printResult(List<CollectResult> collectResult) {
        if (collectFailCount == 0) {
            System.out.println("[INFO ] scm cluster collect successfully："
                    + CollectConfig.getOutputPath() + File.separator + currentCollectPath);
        }
        else {
            for (CollectResult result : collectResult) {
                if (result.getCode() == 0) {
                    System.out.println("[INFO ] " + result.getMsg());
                }
                else {
                    System.err.println("[ERROR] " + result.getMsg());
                }
            }
            System.err.println("[ERROR] scm cluster collect failed: collect result in "
                    + CollectConfig.getOutputPath() + File.separator + currentCollectPath);
            System.err.println(
                    "[ERROR] Execution detail " + ScmHelper.getPwd() + "/log/scm-diagnose.log");
        }
    }

    private List<CollectResult> clusterCollect()
            throws InterruptedException, ScmToolsException, IOException {
        List<ClusterCollector> clusterCollector = chooseClusterHost();
        ExecutorService threadPool = null;
        List<Future<CollectResult>> futureResult = new ArrayList<>();
        List<CollectResult> collectResults = new ArrayList<>();
        logger.info("cluster collect start");
        collectFailCount = 0;
        try {
            threadPool = Executors.newFixedThreadPool(CollectConfig.getThreadSize());
            for (ClusterCollector collector : clusterCollector) {
                Future<CollectResult> future = threadPool.submit(collector);
                futureResult.add(future);
            }
            for (Future<CollectResult> future : futureResult) {
                CollectResult collectResult = future.get();
                if (collectResult.getCode() != 0) {
                    collectFailCount++;
                    Exception e = collectResult.getException();
                    logger.error("Execution failed,cause by:{}", e.getMessage(), e);
                }
                collectResults.add(collectResult);
            }
        }
        catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
        return collectResults;
    }

    private List<ClusterCollector> chooseClusterHost() throws IOException, ScmToolsException {
        List<ClusterCollector> clusterCollector = new ArrayList<>();
        for (String hostName : hostsMap.keySet()) {
            if (HostAddressUtils.getLocalHostAddress().equals(hostName)
                    || HostAddressUtils.getLocalHostName().equals(hostName)) {
                clusterCollector.add(new LocalClusterCollector(hostsMap.get(hostName)));
            }
            else {
                Ssh ssh = getSshByHost(hostName);
                if (ssh != null) {
                    RemoteClusterCollector remoteClusterCollector = new RemoteClusterCollector(
                            hostsMap.get(hostName));
                    remoteClusterCollector.setSsh(ssh);
                    clusterCollector.add(remoteClusterCollector);
                }
                else {
                    // no configure ssh
                    System.err.println("[WARN ] host " + hostName
                            + " not configure ssh,it don't collect host and node info");
                }
            }
        }
        return clusterCollector;
    }

    private Ssh getSshByHost(String host) throws UnknownHostException {
        for (Ssh ssh : sshList) {
            if (HostAddressUtils.getIpByHostName(host)
                    .equals(HostAddressUtils.getIpByHostName(ssh.getHost()))) {
                return ssh;
            }
        }
        return null;
    }

    private void printClusterInfo(int total, String outputPath) throws ScmToolsException {
        BufferedWriter bw = null;
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outputPath, true);
            bw = new BufferedWriter(fileWriter);
            String str = "****************************Overview****************************";
            printMsgToFile(str, bw);
            str = "\nhostsCount:" + hostsMap.size();
            printMsgToFile(str, bw);
            str = "\nserviceCount:" + servicesMap.size();
            printMsgToFile(str, bw);
            str = "\nnodeCount:" + total;
            printMsgToFile(str, bw);

            printClusterHostInfo(bw);
            printClusterServiceInfo(bw);
            printClusterNodeInfo(bw);
        }
        catch (Exception e) {
            throw new ScmToolsException("cluster info write to file failed ",
                    CollectException.PERMISSION_ERROR);
        }
        finally {
            ScmContentCommon.closeResource(bw);
            ScmContentCommon.closeResource(fileWriter);
        }
    }

    private void printClusterHostInfo(BufferedWriter bw) throws ScmToolsException {
        int count = 0;
        String str = "\n\n*****************************Hosts*****************************";
        printMsgToFile(str, bw);
        for (String host : hostsMap.keySet()) {
            str = "\n" + (++count) + "." + host + ":(nodeCount=" + hostsMap.get(host).size() + ")";
            printMsgToFile(str, bw);
        }
    }

    private void printClusterServiceInfo(BufferedWriter bw) throws ScmToolsException {
        int count = 0;
        String str = "\n\n****************************Services****************************";
        printMsgToFile(str, bw);
        for (String serviceName : servicesMap.keySet()) {
            str = "\n" + (++count) + "." + serviceName + ":(nodeCount="
                    + servicesMap.get(serviceName) + ")";
            printMsgToFile(str, bw);
        }
    }

    private void printClusterNodeInfo(BufferedWriter bw) throws ScmToolsException {
        int count = 0;
        String str = "\n\n****************************Nodes*****************************";
        printMsgToFile(str, bw);
        for (String regionAndZone : nodesMap.keySet()) {
            str = "\n" + regionAndZone;
            printMsgToFile(str, bw);
            Set<ScmNodeInfo> nodeSet = nodesMap.get(regionAndZone);
            for (ScmNodeInfo nodeInfo : nodeSet) {
                if (nodeInfo.isManualStopped()) {
                    str = "\n" + (++count) + "." + nodeInfo.getHostName() + ":" + nodeInfo.getPort()
                            + "(service=" + nodeInfo.getServiceName() + ",ip="
                            + nodeInfo.getIp_addr() + (nodeInfo.getNodeGroup() == null ? ""
                                    : ",nodeGroup=" + nodeInfo.getNodeGroup())
                            + ",is stopped)";
                }
                else {
                    str = "\n" + (++count) + "." + nodeInfo.getHostName() + ":" + nodeInfo.getPort()
                            + "(service=" + nodeInfo.getServiceName() + ",ip="
                            + nodeInfo.getIp_addr()
                            + (nodeInfo.getNodeGroup() == null ? ""
                                    : ",nodeGroup=" + nodeInfo.getNodeGroup())
                            + (nodeInfo.getPid() == -1 ? "" : ",pid=" + nodeInfo.getPid()) + ")";
                }
                printMsgToFile(str, bw);
            }
            printMsgToFile("\n", bw);
        }
    }

    private void printMsgToFile(String msg, BufferedWriter bw) throws ScmToolsException {
        try {
            bw.write(msg);
        }
        catch (IOException e) {
            throw new ScmToolsException("cluster info write to file failed ",
                    CollectException.PERMISSION_ERROR);
        }
    }

    private void nodeCollect(ScmNodeInfo nodeInfo) {
        String hostName = nodeInfo.getHostName();
        if (hostsMap.containsKey(hostName)) {
            List<ScmNodeInfo> nodeInfoList = hostsMap.get(hostName);
            nodeInfoList.add(nodeInfo);
        }
        else {
            List<ScmNodeInfo> nodeInfoList = new ArrayList<>();
            nodeInfoList.add(nodeInfo);
            hostsMap.put(hostName, nodeInfoList);
        }

        String serviceName = nodeInfo.getServiceName();
        if (servicesMap.containsKey(serviceName)) {
            servicesMap.put(serviceName, servicesMap.get(serviceName) + 1);
        }
        else {
            servicesMap.put(serviceName, 1);
        }

        String regionAndZone = nodeInfo.getRegion() + "/" + nodeInfo.getZone();
        if (nodesMap.containsKey(regionAndZone)) {
            Set<ScmNodeInfo> nodeSet = nodesMap.get(regionAndZone);
            nodeSet.add(nodeInfo);
        }
        else {
            Set<ScmNodeInfo> nodeSet = new TreeSet<>();
            nodeSet.add(nodeInfo);
            nodesMap.put(regionAndZone, nodeSet);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "execute scm cluster info collect.";
    }

    @Override
    protected Options addParam() throws ParseException {
        Options ops = new Options();
        ops.addOption(Option.builder("h").longOpt("help").hasArg(false).required(false).build());
        ops.addOption(Option.builder(null).longOpt(GATEWAY).desc("gateway url").optionalArg(true)
                .hasArg(true).required(true).build());
        ops.addOption(Option.builder(null).longOpt(HOSTS).desc("scm collect cluster machines")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(null).longOpt(CONF).desc("scm collect log conf path")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(Option.builder(SHORT_OUTPUT_PATH).longOpt(OUTPUT_PATH)
                .desc("scm cluster collect outputPath").optionalArg(true).hasArg(true)
                .required(false).build());
        ops.addOption(Option.builder(null).longOpt(THREAD).desc("scm cluster collect thread size")
                .optionalArg(true).hasArg(true).required(false).build());
        ops.addOption(
                Option.builder(null).longOpt(NEED_ZIP).desc("scm collect cluster files need zip")
                        .optionalArg(true).hasArg(true).required(false).build());
        return ops;
    }

    protected void printHelp() throws ParseException {
        Options ops = addParam();
        HelpFormatter help = new HelpFormatter();
        help.printHelp(getName() + " [options]", ops);
    }

}
