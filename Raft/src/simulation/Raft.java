package simulation;

import configurations.Parameters;
import data.LogEntry;
import data.Role;
import data.State;
import implementation.RaftContext;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.util.TimerTask;
import messages.AppendEntriesRequest;
import messages.AppendEntriesResponse;
import simulation.crash.CrashReceiver;
import simulation.parameters.Configuration;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Raft extends CrashReceiver {
    private RaftContext raftContext;

    // Utilizado para contar as respostas obtidas para cada entrada no log
    private Map<Integer,Integer> commitCounter;


    public Raft(NekoProcess process, String name) {
        super(process, name);

        this.raftContext = new RaftContext(process.getN());
        this.commitCounter = new HashMap<>();
    }

    private static final int APPEND_ENTRY_REQ = 1013;
    private static final int APPEND_ENTRY_RES = 1014;
    private static final int REQUEST_VOTE_REQ = 1015;
    private static final int REQUEST_VOTE_RES = 1016;

    static {
        MessageTypes.instance().register(APPEND_ENTRY_REQ,"Append_Req");
        MessageTypes.instance().register(APPEND_ENTRY_RES,"Append_Res");
        MessageTypes.instance().register(REQUEST_VOTE_RES,"Vote_Res");
        MessageTypes.instance().register(REQUEST_VOTE_REQ,"Vote_Req");
    }

    @Override
    public void deliver(NekoMessage m) {
        int messageType = m.getType();

        if (messageType == APPEND_ENTRY_REQ){ // Recebido por cada Follower
            AppendEntriesRequest request = (AppendEntriesRequest) m.getContent();

            boolean success = raftContext.doAppendEntries(request);

            int logSize = raftContext.getState().getLog().size(); // Indice do log

            if (raftContext.getState().getLastApplied() < request.getLeaderCommit() ){

                raftContext.getState().setLastApplied(request.getLeaderCommit());

                if (Parameters.DEBUG){
                    System.out.printf("p%s:Applied in followers at %f: %d\n",process.getID(),process.clock(),request.getLeaderCommit());
                }
            }
            AppendEntriesResponse response = new AppendEntriesResponse(
                    success,
                    raftContext.getState().getCurrentTerm(),
                    logSize
            );

            NekoMessage mr = new NekoMessage(new int[]{m.getSource()},RaftInitializer.PROTOCOL_APP,
                                            response,APPEND_ENTRY_RES);
            sender.send(mr);
        } else if (messageType == APPEND_ENTRY_RES){ // Resposta recebida pelo lider
            AppendEntriesResponse response = (AppendEntriesResponse) m.getContent();

            if (response.isSuccess()){
                int indexI = response.getMatchIndex();
                int votes = 0;

                if (raftContext.getState().getLastApplied() < indexI) {
                    if (commitCounter.containsKey(indexI)) {
                        votes = commitCounter.get(indexI);
                    }

                    // Adiciona voto
                    votes++;

                    if (votes >= ((process.getN() / 2) + 1)) {
                        if (Parameters.DEBUG)
                            System.out.println("Applied index:" + indexI);

                        raftContext.getState().setLastApplied(indexI);
                        commitCounter.remove(indexI);
                    } else {
                        commitCounter.put(indexI, votes);
                    }

                }
            } else { // Recebeu falso, indice não corresponde
                int src = m.getSource();

                List<Integer> matchIndex = raftContext.getMatchIndex();
                matchIndex.set(src,matchIndex.get(src) - 1);
            }


        }
    }

    @Override
    public void run() {
        // Inicialmente p0 será o líder
        if (process.getID() == 0){
            int heartbeatInterval = ThreadLocalRandom.current().nextInt(Parameters.minElectionTimeout, Parameters.maxElectionTimeout);

            State state = raftContext.getState();
            state.setRole(Role.LEADER);
            state.setCurrentTerm(1);

            while (process.clock() < simulation_time){
                serverAppendEntries();

                try {
                    sleep(heartbeatInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }

        }
    }

    public void serverAppendEntries(){

        double delay = Configuration.ts;

        for (int i=0; i < process.getN(); i++){
            if (i == process.getID())
                continue;

            AppendEntriesRequest appendEntry = new AppendEntriesRequest();

            List<LogEntry> entries = new ArrayList<>();
            int p_nextIndex = raftContext.getNextIndex().get(i);
            int server_index = raftContext.getState().getCommitIndex();

            appendEntry.setTerm(raftContext.getState().getLastApplied());
            appendEntry.setLeaderId(process.getID());


            appendEntry.setLeaderCommit(raftContext.getState().getLastApplied());

            if (server_index > 0){
                List<LogEntry> logEntries = raftContext.getState().getLog();
                if (server_index >= p_nextIndex){
                    entries.add(logEntries.get(p_nextIndex-1));
                }

                if (server_index < p_nextIndex){
                    LogEntry logEntry = logEntries.get(server_index-1);

                    appendEntry.setPrevLogIndex(server_index);
                    appendEntry.setPrevLogTerm(logEntry.getTerm());
                }
                raftContext.getNextIndex().set(i,p_nextIndex+1);
            }
            appendEntry.setEntries(entries);

            NekoMessage m = new NekoMessage(new int[]{i}, RaftInitializer.PROTOCOL_APP, appendEntry, APPEND_ENTRY_REQ);
            NekoSystem.instance().getTimer().schedule(new SenderTask(m),delay);

            delay += Configuration.ts;
        }

    }

    public synchronized void doRequest(Object object){
        LogEntry logEntry = new LogEntry(object,raftContext.getState().getCurrentTerm());

        State state = raftContext.getState();
        state.getLog().add(logEntry);
        state.setCommitIndex(state.getCommitIndex()+1);

        commitCounter.put(state.getCommitIndex(),1);


    }

}
