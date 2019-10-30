package distributed_consensus_se;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class HeartbeatListener extends Thread {
    private LeaderCandidate follower;

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderCandidate.class);

    public HeartbeatListener(LeaderCandidate follower){
        this.follower = follower;
    }

    public void run() {
        try {
            Thread.sleep(3000);
            System.out.println("LEADER FAILED");
            LOGGER.info("Leader failed :: " + java.time.LocalTime.now());
            follower.startNewRound();
        } catch (InterruptedException e) {
            System.out.println("GOT HB :: " + java.time.LocalTime.now());
            run();
        }
    }

}