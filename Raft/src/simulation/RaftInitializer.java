package simulation;

import data.LogEntry;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.NekoSystem;
import lse.neko.SenderInterface;
import lse.neko.util.TimerTask;
import lse.neko.util.logging.NekoLogger;
import org.apache.java.util.Configurations;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class RaftInitializer implements NekoProcessInitializer {
    public static final String PROTOCOL_NAME = "Raft";
    public static final String PROTOCOL_CLIENT = "Client";
    public static final String PROTOCOL_APP = "replication";
    public static final String PROTOCOL_APP_CLIENT = "client";

    private int counter = 0;
    private int clients = 256;

    private Set<String> messages;

    @Override
    public void init(NekoProcess process, Configurations config) throws Exception {
        int executions = config.getInteger("messages.number",1);
        Logger logger = NekoLogger.getLogger("messages");

//       Registra mensagens enviadas aos processos, desta forma é possível
//       rastrear se a mensagem foi origem no processo atual
        messages = new HashSet<>();

        // Rede utilizada para comunicacao entre processos
        SenderInterface sender = process.getDefaultNetwork();

        Raft raftAlgorithm = new Raft(process,PROTOCOL_NAME);
        raftAlgorithm.setSender(sender);

        raftAlgorithm.setId(PROTOCOL_APP);
        raftAlgorithm.launch();

        int id = process.getID();

//        Agenda envio de mensagens, aguardando que líder seja eleito
//        if (id == 6)
            NekoSystem.instance().getTimer().schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
//                        logger.info("starting-experiment");
                            counter++;
                            for (int i=0; i < clients; i++){
                                String data = String.format("p%d:exec-%d-cli-%s",id,counter,i);
                                messages.add(data);
                                raftAlgorithm.doRequest(data);
                            }
                        }
                    }, 200
            );

//        Loop para envio de requisições
        raftAlgorithm.addLogChangeListener(new Raft.LogChangeListener() {
            int exec = 1;

            @Override
            public boolean appliedValues(LogEntry data) {
                logger.fine(String.format("p%s:entregue:%s",id, data));

                if (messages.contains(data.getData())){
                    messages.remove(data.getData());
                }

                raftAlgorithm.increaseSimulationTime(100);
//                Ainda existem mensagens para serem enviadas por cada processo
//                E as mensagens anteriores enviadas já foram entregues.
//                if (process.getID() == 6)
                if (counter < executions && messages.size() == 0){
                    for (int i=0; i < clients; i++){
                        counter++;
                        String raw = String.format("p%d:exec-%d-cli-%s",id,counter,i);
                        messages.add(raw);
                        raftAlgorithm.doRequest(raw);
                    }

                }

                return true;
            }
        });
    }
}
