package simulation;

import configurations.Parameters;
import data.LogEntry;
import data.Role;
import data.State;
import implementation.RaftContext;
import lse.neko.*;
import lse.neko.util.Timer;
import lse.neko.util.TimerTask;
import messages.AppendEntriesRequest;
import messages.AppendEntriesResponse;
import messages.RequestVote;
import messages.ResponseVote;
import simulation.crash.CrashReceiver;
import simulation.parameters.Configuration;

import java.util.*;
import java.util.concurrent.*;

public class Raft extends CrashReceiver {
    private RaftContext raftContext;

    // Utilizado para contar as respostas obtidas para cada entrada no log
    private Map<Integer,Integer> commitCounter;

    private long votesForTerm;


    private static final int APPEND_ENTRY_REQ = 1013;
    private static final int APPEND_ENTRY_RES = 1014;
    private static final int VOTE_REQ = 1015;
    private static final int VOTE_RES = 1016;

    private int leaderTimeout;
    private double lastSeen;
    private int votesFor[];

    private NekoMessageQueue messageQueue;
    private TimeoutTask timeoutTask;
    private Timer timer;

    private List<LogChangeListener> listeners;

    static {
        MessageTypes.instance().register(APPEND_ENTRY_REQ,"Append_Req");
        MessageTypes.instance().register(APPEND_ENTRY_RES,"Append_Res");
        MessageTypes.instance().register(VOTE_RES,"Vote_Res");
        MessageTypes.instance().register(VOTE_REQ,"Vote_Req");
    }

    public Raft(NekoProcess process, String name) {
        super(process, name);

        this.raftContext = new RaftContext(process.getN());
        this.commitCounter = new HashMap<>();

        messageQueue = new NekoMessageQueue();
        timer = NekoSystem.instance().getTimer();

        listeners = new ArrayList<>();
        votesFor = new int[process.getN()];
    }

    public void addLogChangeListener(LogChangeListener listener){
        listeners.add(listener);
    }

    public void removeLogChangeListener(LogChangeListener listener){
        listeners.remove(listener);
    }

    private void publishValues(LogEntry data){
        for (LogChangeListener l : listeners)
            l.appliedValues(data);
    }


    @Override
    public void deliver(NekoMessage m) {
        if (isCrashed())
            return;

        int messageType = m.getType();

        if (messageType == APPEND_ENTRY_REQ){ // Recebido por cada Follower
            AppendEntriesRequest request = (AppendEntriesRequest) m.getContent();

            boolean success = raftContext.doAppendEntries(request);

            int logSize = raftContext.getState().getLog().size(); // Indice do log

            // Cancela leader timeout somente se a requisição for lider e estiver válida
            if (success){
                timeoutTask.cancel();

                if (raftContext.getState().getLastApplied() < request.getLeaderCommit() ){
                    raftContext.getState().setLastApplied(request.getLeaderCommit());

                    LogEntry logValue = raftContext.getState().getLog().get((int) request.getLeaderCommit() - 1);
                    publishValues(logValue);

                    if (Parameters.DEBUG){
                        System.out.printf("p%s:Applied in followers at %f: %d\n",process.getID(),process.clock(),request.getLeaderCommit());
                    }
                    // inicia o timer novamente para aguardar o heartbeat do lider
                }
                timer.schedule(timeoutTask,leaderTimeout);
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
                int src = m.getSource();


                // Adicionar nextIndex ao receber resposta para indice
                if(raftContext.getNextIndex().get(src) <= indexI){
//                    Atualiza indice de registros a serem enviados ao processo
                    raftContext.getNextIndex().set(src,indexI+1);
                }


//                    Ao receber success entrada esta replicada
                raftContext.getMatchIndex().set(src,indexI);

                long lastApplied = raftContext.getState().getLastApplied();
                List<Integer> matchIndex = raftContext.getMatchIndex();

                if (lastApplied < indexI) {
                    // Conta processos que contém indices maiores que lastApplied
                    int votes = 1;
                    for (int i = 0; i < matchIndex.size(); i++){
                        if (matchIndex.get(i) > lastApplied){
                            votes++;
                        }
                    }
                    if (votes >= (process.getN()/2)+1){
                        if (Parameters.DEBUG)
                            System.out.printf("p%s: Applied index: %s at %s\n", process.getID(),
                                    indexI, process.clock());

                        raftContext.getState().setLastApplied(++lastApplied);
                        publishValues(raftContext.getState().getLog().get(indexI - 1));
                        commitCounter.remove(indexI);
                    }

//
//                    if (commitCounter.containsKey(indexI)) {
//                        votes = commitCounter.get(indexI);
//                    }
//
//                    // Adiciona voto
//                    votes++;

//                    if (votes >= ((process.getN() / 2) + 1)) {
//                        if (Parameters.DEBUG)
//                            System.out.printf("p%s: Applied index: %s at %s\n", process.getID(),
//                                    indexI, process.clock());
//
//                        raftContext.getState().setLastApplied(indexI);
//                        publishValues(raftContext.getState().getLog().get(indexI - 1));
//                        commitCounter.remove(indexI);
//                    } else {
//                        commitCounter.put(indexI, votes);
//                    }

                }
            } else { // Recebeu falso, indice não corresponde
                int src = m.getSource();

                List<Integer> nextIndex = raftContext.getNextIndex();
                nextIndex.set(src,nextIndex.get(src) - 1);
            }


        } else if (messageType == VOTE_REQ) {
            timeoutTask.cancel();

            if (Parameters.DEBUG)
                System.out.printf("p%s: Recebido VoteRequest de %s\n",process.getID(),m.getSource());

            RequestVote requestVote = (RequestVote) m.getContent();

            boolean voteResult = doVoting(requestVote);

            ResponseVote responseVote = new ResponseVote(
                    raftContext.getState().getCurrentTerm(),
                    voteResult
            );

            if (Parameters.DEBUG)
                System.out.printf("p%s: Resultado para o candidato %s: %s \n",
                        process.getID(),
                        m.getSource(),
                        voteResult);

            NekoMessage response = new NekoMessage(
                    new int[]{m.getSource()},
                    RaftInitializer.PROTOCOL_APP,
                    responseVote,
                    VOTE_RES
            );
            sender.send(response);


        } else if (m.getType() == VOTE_RES){
            ResponseVote response = (ResponseVote) m.getContent();

            if (raftContext.getState().getCurrentTerm() == response.getTerm() &&
            response.isGranted() && raftContext.getState().getRole() != Role.LEADER){
               votesForTerm++;


            if (votesForTerm >= (( process.getN()/2)+1)){
                timeoutTask.cancel();

                raftContext.getState().setRole(Role.LEADER);
                if (Parameters.DEBUG){
                    System.out.printf("p%s: Lider eleito!! \n",process.getID());
                }


                // inicia processo para heartbeat e appendentries
                timer.schedule(new ServerTask(),1);
            }
            }
        }
    }


    public boolean doVoting(RequestVote request){
        State state = raftContext.getState();

        if (state.getCurrentTerm() >= request.getTerm())
            return false;
        else{
            state.setCurrentTerm(request.getTerm());
            state.setVotedFor(request.getCandidateId());
        }

        if (state.getLog().size() > request.getLastLogIndex())
            return false;

        return true;
    }

    @Override
    public void run() {
        // Aqui deve iniciar o algoritmo
        // Inicialmente é definido o tempo que o algoritmo deve aguardar para obter resposta de algum líder
        // Cada processo inicia como FOLLOWER
        leaderTimeout = this.getRandomTimeout();
        lastSeen = 0;

        if (process.getID() == 1)
            leaderTimeout = 150;
        else if (process.getID() == 2)
            leaderTimeout = 231;
        else if (process.getID() == 0)
            leaderTimeout = 290;

        timeoutTask = new TimeoutTask();
        timer.schedule(timeoutTask,leaderTimeout);

    }

    private void serverAppendEntries(){

        double delay = Configuration.ts;

        for (int i=0; i < process.getN(); i++){
            // Nao executa enviando para ele mesmo
            if (i == process.getID())
                continue;

            // Requisicao a ser enviada
            AppendEntriesRequest appendEntry = new AppendEntriesRequest();

//            Entradas adionadas em cada requisição
            List<LogEntry> entries = new ArrayList<>();

//            Indice do proxima mensagem que processo deverá enviar
            int p_nextIndex = raftContext.getNextIndex().get(i);

//            Indice atual do log
            long server_index = raftContext.getState().getLog().size();

//          Indice que corresponde a última entrada conhecida de ser replicada
            int matchIndex = raftContext.getMatchIndex().get(i);

//            Termo utilizado pelo líder, atualmente eleito
            appendEntry.setTerm(raftContext.getState().getCurrentTerm());

//            Identificacao do lider
            appendEntry.setLeaderId(process.getID());

//          TODO verificar se nesse sentido esta correto
            appendEntry.setLeaderCommit(raftContext.getState().getLastApplied());

//            Se o registro do servidor ser maior do 0
            if (server_index > 0){
                List<LogEntry> logEntries = raftContext.getState().getLog();

                LogEntry prevTerm = null;
                LogEntry entry = null;



                 if ((p_nextIndex - 2 >= 0 ) && (p_nextIndex - 2 <= logEntries.size()) ){
                    prevTerm = logEntries.get(p_nextIndex - 2);

                }


//                else if (server_index < p_nextIndex){
//                    entry = logEntries.get((int) (server_index-1));
//                }


                if (prevTerm != null){
                    appendEntry.setPrevLogIndex(p_nextIndex - 1);
                    appendEntry.setPrevLogTerm(prevTerm.getTerm());

                }

                if (matchIndex == p_nextIndex - 1){
                    if (server_index >= p_nextIndex){
                        entry = logEntries.get(p_nextIndex-1);
                        entries.add(entry);
                    }
                }


            }
            appendEntry.setEntries(entries);

            NekoMessage m = new NekoMessage(new int[]{i}, RaftInitializer.PROTOCOL_APP, appendEntry, APPEND_ENTRY_REQ);
            NekoSystem.instance().getTimer().schedule(new SenderTask(m),delay);

            delay += Configuration.ts;
        }

    }

    public synchronized void doRequest(Object object){
        if (raftContext.getState().getRole() == Role.LEADER) {

            LogEntry logEntry = new LogEntry(object, raftContext.getState().getCurrentTerm());

            State state = raftContext.getState();
            state.getLog().add(logEntry);

        }
    }

    private int getRandomTimeout(){
        return ThreadLocalRandom.current().
                nextInt(
                        Parameters.minElectionTimeout,
                        Parameters.maxElectionTimeout
                );
    }

    public void sendRPCVoteRequest(){
        // Para tratar split votes indefinidos electionTime deve ser novamente randomizado
        leaderTimeout = getRandomTimeout();


        State state = raftContext.getState();

        // Define papel como candidato
        state.setRole(Role.CANDIDATE);

        // Incrementa o termo a atual
        long term = state.getCurrentTerm();
        term++;
        state.setCurrentTerm(term);

        RequestVote requestVote = new RequestVote(
                term,
                process.getID(),
                state.getLog().size(),
                state.getLastLogTerm()
        );

        state.setVotedFor(process.getID());
        votesForTerm = 1;

        timer.schedule(timeoutTask,leaderTimeout);

        for (int i = 0; i < process.getN(); i++){
            if (i == process.getID())
                continue;

            NekoMessage m = new NekoMessage(new int[]{i},
                    RaftInitializer.PROTOCOL_APP,
                    requestVote,
                    VOTE_REQ
            );
            sender.send(m);
        }

    }

    class TimeoutTask extends TimerTask {

        TimeoutTask(){}

        @Override
        public void run() {

            if (process.clock() > simulation_time || isCrashed())
                return;

            if (Parameters.DEBUG)
                System.out.printf("p%s: Iniciando eleição de líder at %s\n",process.getID(),process.clock());

                sendRPCVoteRequest();
        }
    }

    class ServerTask extends TimerTask{

        public ServerTask(){
            raftContext = Raft.this.raftContext;

            int logSize = raftContext.getState().getLog().size();

            List<Integer> nextIndex = raftContext.getNextIndex();
            List<Integer> matchIndex = raftContext.getMatchIndex();
            for (int i = 0; i < nextIndex.size(); i++){
                nextIndex.set(i,logSize+1);
                matchIndex.set(i,0);
            }


        }

        @Override
        public void run() {
            if (isCrashed())
                return;

            // Se o processo continua com papel de líder envia o appendEntries
            if (raftContext.getState().getRole() == Role.LEADER)
                serverAppendEntries();
            else
                return;

            if (process.clock() < simulation_time)
                timer.schedule(this,150); // Agenda a proxima execucao para o lider
        }
    }

    interface LogChangeListener{
        boolean appliedValues(LogEntry data);
    }

}
