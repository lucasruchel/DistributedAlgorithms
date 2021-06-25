package simulation;

import data.LogEntry;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.NekoSystem;
import lse.neko.SenderInterface;
import lse.neko.util.TimerTask;
import lse.neko.util.logging.NekoLogger;
import org.apache.java.util.Configurations;

import java.util.logging.Logger;

public class RaftInitializer implements NekoProcessInitializer {
    public static final String PROTOCOL_NAME = "Raft";
    public static final String PROTOCOL_CLIENT = "Client";
    public static final String PROTOCOL_APP = "replication";
    public static final String PROTOCOL_APP_CLIENT = "client";

    @Override
    public void init(NekoProcess process, Configurations config) throws Exception {
        int executions = config.getInteger("messages.number",1);
        Logger logger = NekoLogger.getLogger("messages");

        // Rede utilizada para comunicacao entre processos
        SenderInterface sender = process.getDefaultNetwork();

        Raft raftAlgorithm = new Raft(process,PROTOCOL_NAME);
        raftAlgorithm.setSender(sender);

        raftAlgorithm.setId(PROTOCOL_APP);
        raftAlgorithm.launch();

        int id = process.getID();

//        Agenda envio de mensagens, aguardando que líder seja eleito
        if (id == 0)
        NekoSystem.instance().getTimer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        logger.info("starting-experiment");
                        raftAlgorithm.doRequest(String.format("p%d:exec-%d",id,1));
                    }
                }, 200
        );

//        Loop para envio de requisições
        raftAlgorithm.addLogChangeListener(new Raft.LogChangeListener() {
            int exec = 1;

            @Override
            public boolean appliedValues(LogEntry data) {
                logger.fine(String.format("p%s:entregue:%s",id, data));

                if (exec < executions){
                    raftAlgorithm.increaseSimulationTime(200);
                    raftAlgorithm.doRequest(String.format("p%d:exec-%d",id,++exec));

                } else if (exec >= executions)
                    logger.info("Finished-experiment");

                return true;
            }
        });

//        Client client = new Client(process,PROTOCOL_CLIENT, raftAlgorithm);
//        client.setRaft(raftAlgorithm);
//        client.setId(PROTOCOL_APP_CLIENT);
//        client.launch();

    }
}
