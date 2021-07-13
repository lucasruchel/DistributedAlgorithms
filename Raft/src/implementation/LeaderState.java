package implementation;

import configurations.Parameters;
import data.LogEntry;
import data.Role;
import data.State;
import messages.AppendEntriesRequest;

import java.util.ArrayList;
import java.util.List;

public class LeaderState {

    // indice do proximo registro de cada processo no cluster
    private List<Integer> nextIndex;

    // indice do ultimo registro conhecido de ter sido replicado em cada processo
    private List<Integer> matchIndex;

    // Variaveis armazenadas por cada processo
    private State state;

    /***
     *
     * @param raftState estado do processo criado
     * @param np número de processos ativos no sistema
     */
    public LeaderState(State raftState, int np){
        this.state = raftState;

//        Próximo índice do log a ser enviado aos processos partipantes
        this.nextIndex = new ArrayList<>(np);
//        Última entrada conhecida a ser replicada nos processos
        this.matchIndex = new ArrayList<>(np);

//        Como o líder está iniciado agora é necessario que as entradas sejam zeradas para o matchiIndex e o nextIndex consiste na última entrada conhecida no log do processo líder.
        for (int i = 0; i < np; i++){
            matchIndex.add((int) state.getCommitIndex());
            nextIndex.add((int) (state.getCommitIndex()+1));
        }
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
