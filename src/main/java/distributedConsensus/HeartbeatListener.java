package distributedConsensus;

import org.apache.log4j.Logger;

public class HeartbeatListener extends Thread {
    private LeaderCandidate follower;

    private static final Logger LOGGER = Logger.getLogger(LeaderCandidate.class);

    public HeartbeatListener(LeaderCandidate follower){
        this.follower = follower;
    }

    public void run() {
        try {
            Thread.sleep(2000);
            System.out.println("LEADER FAILED :: " + java.time.LocalTime.now());
            follower.setElectedLeader(null);
            LOGGER.info("LeaderCandidate" +
                    " " + follower.getNodeId() + " identified leader FAILURE");

            follower.startNewRound();
        } catch (InterruptedException e) {

            System.out.println("GOT HB :: " + java.time.LocalTime.now());
            run();
        }
    }
}
