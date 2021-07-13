package simulation;

import configurations.Parameters;
import data.LogEntry;
import data.Role;
import data.State;
import implementation.LeaderState;
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

    // Utilizado para contar as respostas obtidas para cada entrada no log
    private Map<Integer,Integer> commitCounter;

    private long votesForTerm;

    private static final int APPEND_ENTRY_REQ = 1013;
    private static final int APPEND_ENTRY_RES = 1014;
    private static final int VOTE_REQ = 1015;
    private static final int VOTE_RES = 1016;
    private static final int FWD = 1017;

    private static final double DEFAULT_INTERVAL = 50;
    private static final double DEFAULT_TIMEOUT = 200;

    private double leaderTimeout;
    private double lastSeen;
    private int votesFor[];

    private NekoMessageQueue messageQueue;
    private TimeoutTask timeoutTask;


    private List<LogChangeListener> listeners;

    private State raftState;
    private LeaderState leaderState;
    private Map<Integer,AppendTask> senderGroup;

//    Registra qual foi o último líder eleito
    private int lastLeader;

    private int np;

    static {
        MessageTypes.instance().register(APPEND_ENTRY_REQ,"Append_Req");
        MessageTypes.instance().register(APPEND_ENTRY_RES,"Append_Res");
        MessageTypes.instance().register(VOTE_RES,"Vote_Res");
        MessageTypes.instance().register(VOTE_REQ,"Vote_Req");
        MessageTypes.instance().register(FWD,"FWD");
    }

    public Raft(NekoProcess process, String name) {
        super(process, name);

        this.raftState = new State();
        this.commitCounter = new HashMap<>();

        this.np = process.getN();

        messageQueue = new NekoMessageQueue();


        listeners = new ArrayList<>();
        votesFor = new int[np];

        lastLeader = -1;

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

    private void doAppend(NekoMessage m){
        AppendEntriesRequest request = (AppendEntriesRequest) m.getContent();

        int src = m.getSource();

        boolean success = raftState.doAppendEntries(request);

        int logSize = raftState.getLog().size(); // Indice do log

        timeoutTask.cancel();

        // Cancela leader timeout somente se a requisição for lider e estiver válida
        if (success){

//            É seguro afirmar que a request é de um
//           processo líder válido, desta forma optei
//          por registrar o último leader eleito
            if (lastLeader != src){
                lastLeader = src;
                 if (Parameters.DEBUG)
                     logger.info(String.format("p%s: Identificado um novo líder p%s",me,src));
            }

            if (raftState.getLastApplied() < request.getLeaderCommit() ){
                int lastApplied = (int) raftState.getLastApplied();
                int leaderCommit = (int) request.getLeaderCommit();

                raftState.setLastApplied(leaderCommit);

                LogEntry logValue = raftState.getLog().get((leaderCommit - 1));


                List<LogEntry> logEntries = raftState.getLog().subList(lastApplied, leaderCommit);

                for (int i = 0; i < logEntries.size(); i++) {
                    publishValues(logEntries.get(i));
                    if (Parameters.DEBUG){
//                    System.out.printf("p%s:Applied in followers at %f: %d\n",process.getID(),process.clock(),request.getLeaderCommit());

                        logger.info(String.format("p%s: delivered in followers at %s", me, process.clock()) );
                    }
                }




            }
            // inicia o timer novamente para aguardar o heartbeat do lider
            timer.schedule(timeoutTask,leaderTimeout);
        }

        AppendEntriesResponse response = new AppendEntriesResponse(
                success,
                raftState.getCurrentTerm(),
                logSize
        );

        NekoMessage mr = new NekoMessage(new int[]{src},RaftInitializer.PROTOCOL_APP,
                response,APPEND_ENTRY_RES);
        send(mr);
    }

    private void appendResponse(NekoMessage m){
//      Não deve tomar nenhuma ação caso não for o atual líder
        if (raftState.getRole() != Role.LEADER )
            return;

        AppendEntriesResponse response = (AppendEntriesResponse) m.getContent();

        int src = m.getSource();

        if (response.isSuccess()){
            int indexI = response.getMatchIndex();

            // Adicionar nextIndex ao receber resposta para indice
            if(leaderState.getNextIndex().get(src) <= indexI){
//                    Atualiza indice de registros a serem enviados ao processo
                leaderState.getNextIndex().set(src,indexI+1);
            }
//          Ao receber success entrada esta replicada
            leaderState.getMatchIndex().set(src,indexI);

            long lastApplied = raftState.getLastApplied();
            List<Integer> matchIndex = leaderState.getMatchIndex();

            if (lastApplied < indexI) {
                // Conta processos que contém indices maiores que lastApplied
                int votes = 1;
                for (int i = 0; i < matchIndex.size(); i++){
                    if (matchIndex.get(i) > lastApplied){
                        votes++;
                    }
                }
                if (votes >= (process.getN()/2)+1){
                    if (Parameters.DEBUG){
//                        System.out.printf("p%s: Applied index: %s at %s\n", process.getID(),
//                                indexI, process.clock());
                        logger.info(String.format("p%s: delivered in master at %s", me, process.clock()) );
                    }

                    raftState.setLastApplied(++lastApplied);
                    publishValues(raftState.getLog().get(indexI - 1));
                    commitCounter.remove(indexI);
                }
            }
        } else { // Recebeu falso, indice não corresponde

            List<Integer> nextIndex = leaderState.getNextIndex();
            nextIndex.set(src,nextIndex.get(src) - 1);
        }

//       Caso o índice não corresponda inicia imediamente um novo envio,
//       sem que seja aguardado o tempo padrão de envio
        if (leaderState.getNextIndex().get(src) <= raftState.getLog().size()){
            AppendTask task = senderGroup.get(src);
            task.cancel();
            task.run();
        }
    }

    private void processVoteRequest(NekoMessage m){


        if (Parameters.DEBUG)
            System.out.printf("p%s: Recebido VoteRequest de %s\n",process.getID(),m.getSource());

        RequestVote requestVote = (RequestVote) m.getContent();

        boolean voteResult = doVoting(requestVote);

        if (voteResult){
//          Cancela tarefa e agenda uma nova para garantir que se o líder deixar de responder algum outro possa
            timeoutTask.cancel();
            timer.schedule(timeoutTask,leaderTimeout);

            if (raftState.getRole() != Role.FOLLOWER){
                leaderState = null;
                if (senderGroup != null){
                    senderGroup.forEach((i, t) -> t.cancel());
                    senderGroup.clear();
                }
                raftState.setRole(Role.FOLLOWER);
            }
        }

        ResponseVote responseVote = new ResponseVote(
                raftState.getCurrentTerm(),
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
        send(response);

    }

    private void voteResponse(NekoMessage m){
        ResponseVote response = (ResponseVote) m.getContent();

        if (raftState.getCurrentTerm() == response.getTerm() &&
                response.isGranted() && raftState.getRole() != Role.LEADER){
            votesForTerm++;


            if (votesForTerm >= (( process.getN()/2)+1)){
                timeoutTask.cancel();

                raftState.setRole(Role.LEADER);
                logger.fine("---> líder eleito!!");

//                inicia estados do leader e as variáveis de controle
                initLeader();
            }
        }
    }

    private void initLeader(){
//        Inicia o controle do leader sobre o estado da replicação no followers
        leaderState = new LeaderState(raftState,np);

//        Referência para os objetos que irão replicar as mensagens nos processoos
       senderGroup = new HashMap<>();

//        Necessário criar uma thread para cada processo, afinal na prática cada processo pode ter diferentes atrasos
        for (int i = 0; i < process.getN(); i++) {
            if (me != i){
                AppendTask task = new AppendTask(i);
                timer.schedule(task,DEFAULT_INTERVAL);
                senderGroup.put(i,task);
            }

        }

    }

    @Override
    protected synchronized void doDeliver(NekoMessage m) {
        if (isCrashed())
            return;

        int messageType = m.getType();

        switch (messageType){
            case APPEND_ENTRY_REQ:
                doAppend(m);
                break;
            case APPEND_ENTRY_RES:
                appendResponse(m);
                break;
            case VOTE_REQ:
                processVoteRequest(m);
                break;
            case VOTE_RES:
                voteResponse(m);
                break;
            case FWD:
                registerData(m);
        }
    }
    
    private void registerData(NekoMessage m){
        if (raftState.getRole() != Role.LEADER)
            return;

        Object data = m.getContent();

        LogEntry logEntry = new LogEntry(data, raftState.getCurrentTerm());
        raftState.getLog().add(logEntry);
    }

    private boolean doVoting(RequestVote request){
        State state = raftState;

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

        switch (process.getID()){
            case 0: {
                // Vamos fixar o p0 como líder, disparando o evento de timeout do líder antes de qualquer outro processo
                leaderTimeout = 100;
            }
            break;
            case 1: {
                // O timeout do líder para p1 será fixo, garantindo que seja o próximo líder
                leaderTimeout = DEFAULT_TIMEOUT;
            }
        }



        timeoutTask = new TimeoutTask();
        timer.schedule(timeoutTask,leaderTimeout);

    }

    private void appendEntries(int serverId){

        double delay = Configuration.ts;

            if (serverId == process.getID())
                return;

            // Requisicao a ser enviada
            AppendEntriesRequest appendEntry = new AppendEntriesRequest();

//            Entradas adionadas em cada requisição
            List<LogEntry> entries = new ArrayList<>();

//            Indice do proxima mensagem que processo deverá enviar
            int p_nextIndex = leaderState.getNextIndex().get(serverId);

//            Indice atual do log
            long server_index = raftState.getLog().size();

//          Indice que corresponde a última entrada conhecida de ser replicada
            int matchIndex = leaderState.getMatchIndex().get(serverId);

//            Termo utilizado pelo líder, atualmente eleito
            appendEntry.setTerm(raftState.getCurrentTerm());

//            Identificacao do lider
            appendEntry.setLeaderId(process.getID());

//          TODO verificar se nesse sentido esta correto
            appendEntry.setLeaderCommit(raftState.getLastApplied());

//            Se o registro do servidor ser maior do 0
            if (server_index > 0){
                List<LogEntry> logEntries = raftState.getLog();

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

            NekoMessage m = new NekoMessage(new int[]{serverId}, RaftInitializer.PROTOCOL_APP, appendEntry, APPEND_ENTRY_REQ);
            send(m);
        }



    public synchronized void doRequest(Object object){
        if (raftState.getRole() == Role.LEADER) {

            LogEntry logEntry = new LogEntry(object, raftState.getCurrentTerm());

            State state = raftState;
            state.getLog().add(logEntry);

        } else if ( raftState.getRole() == Role.FOLLOWER){
            /*
            Neste caso é necessário encaminhar a requisição ao líder atual
            lastLeader - possui o último leader conhecido por um processo que recebeu um append entry
            É importante considerar que pode haver um período em que não
            exista um líder eleito e que talvez a requisição seja descartada.
            Ou até mesmo que antigo leader não seja mais válido ou esteja com o papel de candidato.
             */

            NekoMessage m = new NekoMessage(new int[]{lastLeader},
                    RaftInitializer.PROTOCOL_APP,
                    object,
                    FWD);
            send(m);

            if (Parameters.DEBUG)
                logger.info(String.format("p%s: Encaminhando %s para o líder p%s",me,object,lastLeader));
        }
    }

    private int getRandomTimeout(){
        return ThreadLocalRandom.current().
                nextInt(
                        Parameters.minElectionTimeout,
                        Parameters.maxElectionTimeout
                );
    }

    private void sendRPCVoteRequest(){
        // Para tratar split votes indefinidos electionTime deve ser novamente randomizado
        leaderTimeout = getRandomTimeout();


        State state = raftState;

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

//        timeoutTask.start();

        for (int i = 0; i < process.getN(); i++){
            if (i == process.getID())
                continue;

            NekoMessage m = new NekoMessage(new int[]{i},
                    RaftInitializer.PROTOCOL_APP,
                    requestVote,
                    VOTE_REQ
            );
            send(m);
        }

    }

    class TimeoutTask extends TimerTask {

        TimeoutTask(){}

        @Override
        public void run() {
            if (process.clock() > simulation_time || isCrashed())
                return;
            logger.fine("-------->>   Timeout do líder !!!!!");

            if (Parameters.DEBUG) {
                System.out.printf("p%s: Iniciando eleição de líder at %s\n",process.getID(),process.clock());
            }
//            Inicia eleição para líder
            sendRPCVoteRequest();
        }
    }
/*
Necessário criar uma task para cada processo para envio de mensagens, importante para paralelizar a execução
 */
    class AppendTask extends TimerTask{

        private int serverId;

        public AppendTask(int serverId){
            this.serverId = serverId;
        }

        @Override
        public void run() {
            if (isCrashed())
                return;

            // Se o processo continua com papel de líder envia o appendEntries
            if (raftState.getRole() == Role.LEADER)
                appendEntries(serverId);
            else
                return;

            if (process.clock() < simulation_time){
               timer.schedule(this,DEFAULT_INTERVAL);
            }
        }
    }

    interface LogChangeListener{
        boolean appliedValues(LogEntry data);
    }

}
