package data;

import java.util.ArrayList;
import java.util.List;

public class State {
    //Dito que persistente apos falhas
    //Termo atual utilizado pelo líder
    private int currentTerm;

    // Identificao do lider para o termo atual
    private int votedFor;

    // registro de entradas recebidas pelo líder
    private List<LogEntry> log;

    // ********* Dados em memoria em cada processo *******

    // Indice do ultimo registro conhecido a ser aplicado, ainda pendente
    private int commitIndex;

    // Indice do ultimo registro aplicado pelo algoritmo, entregue a aplicacao
    private int lastApplied;

    // Papel inicial de cada processo
    private Role role;


    public State(){
        this.role = Role.FOLLOWER;
        this.log = new ArrayList<>();
    }





    public int getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }

    public int getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(int votedFor) {
        this.votedFor = votedFor;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public int getLastApplied() {
        return lastApplied;
    }

    public void setLastApplied(int lastApplied) {
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
