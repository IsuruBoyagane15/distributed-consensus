package ConsensusTest;

import distributedConsensus.ConsumerGenerator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.graalvm.polyglot.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class Tester {

    private final String kafkaServerAddress;
    private final Context jsContext;
    private final int maxProcessCount;
    private String immortalProcess;
    private KafkaConsumer<String, String> kafkaConsumer;
    private String jarLocation;
    private final String kafkaTopic;
    private boolean terminate;
    private HashMap<String, Process> activeProcesses;
    private boolean maxProcessCountReached;



    public Tester(String jarLocation, String kafkaServerAddress, String kafkaTopic, int maxProcessCount){
        this.jarLocation = jarLocation;
        this.kafkaTopic = kafkaTopic;
        this.kafkaServerAddress = kafkaServerAddress;
        this.kafkaConsumer = ConsumerGenerator.generateConsumer(kafkaServerAddress, kafkaTopic, "tester");
        this.jsContext = Context.create("js");
        this.immortalProcess = null;
        this.activeProcesses = new HashMap<String, Process>();
        this.terminate = false;
        this.maxProcessCountReached = false;
        this.maxProcessCount = maxProcessCount;
    }

    public void read(){
        final int[] roundNumber = {-1};
            Runnable consuming = () -> {

                try {
                    while (!terminate) {
                        ConsumerRecords<String, String> records = kafkaConsumer.poll(10);
                        for (ConsumerRecord<String, String> record : records) {
                            if (!record.value().startsWith("CHECK")){
                                String[] recordContent = record.value().split(",", 2);
                                int recordNumber = Integer.parseInt(recordContent[0]);
                                String recordMessage = recordContent[1];
                                if(!recordMessage.startsWith("ALIVE")){
                                    if (recordNumber > roundNumber[0]){
                                        roundNumber[0] = recordNumber;
                                        this.immortalProcess = jsContext.eval("js","result = {timeout : false}; var nodeRanks = [];" + recordMessage + "nodeRanks[0].client;").toString();
                                    }
                                    else{
                                        if (recordMessage.equals("result.timeout = true;")){
                                            this.immortalProcess = null;
                                        }
                                    }
                                }
                            }

                        }
                    }
                } catch(Exception exception) {
                    System.out.println(exception);
                }finally {
                    kafkaConsumer.close();
                }
            };
            new Thread(consuming).start();
    }

    public void startNewProcess(String jarLocation, String kafkaServerAddress, String kafkaTopic){
        if (this.activeProcesses.size() < maxProcessCount){
            String nodeId = UUID.randomUUID().toString();
            System.setProperty("id", nodeId);
            System.out.println("Id of the new process : " + nodeId);
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", "-Did=" + nodeId, jarLocation, nodeId, kafkaServerAddress, kafkaTopic);
            try {
                Process process = processBuilder.start();
                this.activeProcesses.put(nodeId,process);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Processes after adding new one : " + activeProcesses);
        }
        else{
            System.out.println("Maximum number of processes are running");
        }

    }

    public void killProcess(){
        Object[] nodeIds = activeProcesses.keySet().toArray();
        Object nodeId = nodeIds[new Random().nextInt(nodeIds.length)];
        System.out.println("Id of the process to be killed  : " + nodeId);

        if (nodeId.equals(this.immortalProcess)){
            System.out.println("Can't kill " + nodeId);
            killProcess();
        }
        else{
            Process processToBeKilled = activeProcesses.get(nodeId);
            processToBeKilled.destroy();
            activeProcesses.remove(nodeId);
            System.out.println("Processes after killing one : " + activeProcesses);
            if (this.activeProcesses.size() == 0){
                //FINISH TESTING
                this.terminate = true;
            }
        }
    }

    public static void main(String args[]){
        Tester tester = new Tester(args[0], args[1], args[2], Integer.parseInt(args[3]));
        tester.read();

        while(!tester.terminate){

            int randInt = (int)(1 + Math.random()*tester.maxProcessCount);

            if(randInt> tester.activeProcesses.size()){
                if(!tester.maxProcessCountReached){
                    tester.startNewProcess(tester.jarLocation, tester.kafkaServerAddress, tester.kafkaTopic);
                    if( tester.activeProcesses.size() == tester.maxProcessCount){
                        tester.maxProcessCountReached = true;
                    }
                }
            }
            else{
                tester.killProcess();
            }

            int randWait = (int)(1 + Math.random()*8)*1000;

            try {
                Thread.sleep(randWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }




    }
}