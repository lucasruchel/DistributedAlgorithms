package implementation;

import configurations.Parameters;
import data.LogEntry;
import data.Role;
import data.State;
import messages.AppendEntriesRequest;

import java.util.ArrayList;
import java.util.List;

public class RaftContext {



//     **********************
//      Somente para o lider
//     **********************
    // indice do proximo registro de cada processo no cluster
    private List<Integer> nextIndex;

    // indice do ultimo registro conhecido de ter sido replicado em cada processo
    private List<Integer> matchIndex;

    // Variaveis armazenadas por cada processo
    private State state;





    /***
     *
     * @param np - numero de processos
     */
    public RaftContext(int np){
        this.state = new State();


        this.nextIndex = new ArrayList<>(np);
        this.matchIndex = new ArrayList<>(np);

        state.setCurrentTerm(0);
        state.setLastApplied(0);
        state.setCommitIndex(0);

        for (int i = 0; i < np; i++){
           matchIndex.add((int) state.getCommitIndex());
           nextIndex.add((int) (state.getCommitIndex()+1));
        }
    }


    public boolean doAppendEntries(AppendEntriesRequest m){
        List<LogEntry> log = state.getLog();

        // Condicoes em que não ocorre com sucesso
        if (m.getTerm() < state.getCurrentTerm())
            return false;


        else if (m.getTerm() > state.getCurrentTerm()){
            state.setCurrentTerm(m.getTerm());
            state.setRole(Role.FOLLOWER);

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
        if (m.getLastLogIndex() > state.getCommitIndex())
            state.setCommitIndex(Math.min(m.getLastLogIndex(),m.getLeaderCommit()));

         return true;
    }

    public List<Integer> getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(List<Integer> nextIndex) {
        this.nextIndex = nextIndex;
    }

    public List<Integer> getMatchIndex() {
        return matchIndex;
    }

    public void setMatchIndex(List<Integer> matchIndex) {
        this.matchIndex = matchIndex;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
