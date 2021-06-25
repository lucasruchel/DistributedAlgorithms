package messages;

import configurations.Parameters;
import data.LogEntry;

import java.util.Arrays;
import java.util.List;

public class AppendEntriesRequest {

    /***
     * Utilizado tanto como keep-alive enviado pelo líder quando para novos valores
     */

    private long term;
    private long leaderId;
    private long prevLogIndex;
    private long prevLogTerm;
    private List<LogEntry> entries;
    private long leaderCommit;


    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public long getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(long leaderId) {
        this.leaderId = leaderId;
    }

    public long getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(long prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public long getPrevLogTerm() {
        return prevLogTerm;
    }

    public void setPrevLogTerm(long prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LogEntry> entries) {
        this.entries = entries;
    }

    public long getLeaderCommit() {
        return leaderCommit;
    }

    public void setLeaderCommit(long leaderCommit) {
        this.leaderCommit = leaderCommit;
    }

    public long getLastLogIndex(){
        return this.prevLogIndex + entries.size();
    }

    @Override
    public String toString() {
        if (Parameters.DEBUG)
            return "append_req{" +
                "term=" + term +
                ", leaderId=" + leaderId +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", entries=" + entries +
                ", leaderCommit=" + leaderCommit +
                '}';
        return "append_req";
    }
}
