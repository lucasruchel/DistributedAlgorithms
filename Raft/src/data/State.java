package data;

import configurations.Parameters;
import messages.AppendEntriesRequest;

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

    public boolean doAppendEntries(AppendEntriesRequest m){
        List<LogEntry> log = getLog();

        // Condicoes em que não ocorre com sucesso
        if (m.getTerm() < getCurrentTerm())
            return false;


        else if (m.getTerm() > getCurrentTerm()){
            setCurrentTerm(m.getTerm());
            setRole(Role.FOLLOWER);

            if (Parameters.DEBUG)
                System.out.printf("Atualizando para Follower t=%s", m.getTerm());
        }

        long prevLog = m.getPrevLogIndex();
//        && log.get((int) prevLog).getTerm() == m.getPrevLogTerm()
        if (log.size() > prevLog || log.size() < prevLog)
            return false;



        // Condições de sucesso
        // Entrada sobrepoe log, ainda não está aplicada
        List<LogEntry> entries = m.getEntries();
        long nextIndex = m.getPrevLogIndex();

        for (LogEntry entry: entries){
            if (log.size() > nextIndex)     // Sobrepoe indices ja existentes, se houverem
                log.add((int) nextIndex, entry);
            else
                log.add(entry); // Adiciona novos valores ao log

            nextIndex++; // Avanca o log
        }

        // Menor indice entre o leaderCommitIndex e os valores enviados
        // Evitando que o leader envie o indice dele sem os valores correspondentes, marcando como pendente ainda
        if (m.getLastLogIndex() > getCommitIndex())
            setCommitIndex(Math.min(m.getLastLogIndex(),m.getLeaderCommit()));

        return true;
    }
}
