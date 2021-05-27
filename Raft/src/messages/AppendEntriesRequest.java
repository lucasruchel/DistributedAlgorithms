package messages;

import data.LogEntry;

import java.util.Arrays;
import java.util.List;

public class AppendEntriesRequest {

    /***
     * Utilizado tanto como keep-alive enviado pelo líder quando para novos valores
     */

    private int term;
    private int leaderId;
    private int prevLogIndex;
    private int prevLogTerm;
    private List<LogEntry> entries;
    private int leaderCommit;

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(int prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public void setPrevLogTerm(int prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LogEntry> entries) {
        this.entries = entries;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }

    public void setLeaderCommit(int leaderCommit) {
        this.leaderCommit = leaderCommit;
    }

    public int getLastLogIndex(){
        return this.prevLogIndex + entries.size();
    }

    @Override
    public String toString() {
        return "append_req{" +
                "term=" + term +
                ", leaderId=" + leaderId +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", entries=" + entries +
                ", leaderCommit=" + leaderCommit +
                '}';
    }
}
