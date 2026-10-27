package dfs.common;

public final class Protocol {
    //constants for all programs
    private Protocol() {
    }

    // how big each piece of a file is: 4 * 1024 * 1024 bytes = 4 MB
    public static final int CHUNK_SIZE = 4 * 1024 * 1024;

    // how many copies of each chunk are kept on diff nodes
    public static final int REPLICATION_FACTOR = 3;

    // each node sends a heartbeat every defined time
    public static final long HEARTBEAT_INTERVAL_MS = 2000;
    // how many heartbeats in a row can be missed a node is considered dead
    public static final int MISSED_HEARTBEATS_UNTIL_DEAD = 3;
    // silent for 2s x 3 = 6s, therefore dead
    public static final long DEAD_AFTER_MS = HEARTBEAT_INTERVAL_MS * MISSED_HEARTBEATS_UNTIL_DEAD;

    // how often the coordinator checks for chunks with too few copies
    public static final long REPAIR_SCAN_INTERVAL_MS = 3000;
    // if a repair hasn't reported back in this long, assume it has failed and try again
    public static final long REPAIR_TIMEOUT_MS = 60000;

    // the first word of every reply
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
}