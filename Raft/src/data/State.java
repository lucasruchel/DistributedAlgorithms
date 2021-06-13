package data;

import java.util.ArrayList;
import java.util.List;

public class State {
    //Dito que persistente apos falhas
    //Termo atual utilizado pelo líder
    private long currentTerm;

    // Identificao do lider para o termo atual
    private long votedFor;

    // registro de entradas recebidas pelo líder
    private List<LogEntry> log;

    // ********* Dados em memoria em cada processo *******

    // Indice do ultimo registro conhecido a ser aplicado, ainda pendente
    private long commitIndex;

    // Indice do ultimo registro aplicado pelo algoritmo, entregue a aplicacao
    private long lastApplied;

    // Papel inicial de cada processo
    private Role role;



    public State(){
        this.role = Role.FOLLOWER;
        this.log = new ArrayList<>();
    }





    public long getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public long getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(long votedFor) {
        this.votedFor = votedFor;
    }

    public long getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(long commitIndex) {
        this.commitIndex = commitIndex;
    }

    public long getLastApplied() {
        return lastApplied;
    }

    public long getLastLogTerm(){
        int logSize = log.size();
        if (logSize > 0)
            return log.get(logSize - 1).getTerm();

        return 0;
    }

    public void setLastApplied(long lastApplied) {
        this.lastApplied = lastApplied;
    }

    public List<LogEntry> getLog() {
        return log;
    }

    public void setLog(List<LogEntry> log) {
        this.log = log;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
