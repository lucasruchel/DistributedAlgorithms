package simulation;

import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.SenderInterface;
import org.apache.java.util.Configurations;

public class RaftInitializer implements NekoProcessInitializer {
    public static final String PROTOCOL_NAME = "Raft";
    public static final String PROTOCOL_CLIENT = "Client";
    public static final String PROTOCOL_APP = "replication";
    public static final String PROTOCOL_APP_CLIENT = "client";

    @Override
    public void init(NekoProcess process, Configurations config) throws Exception {
        // Rede utilizada para comunicacao entre processos
        SenderInterface sender = process.getDefaultNetwork();

        Raft raftAlgorithm = new Raft(process,PROTOCOL_NAME);
        raftAlgorithm.setSender(sender);

        raftAlgorithm.setId(PROTOCOL_APP);
        raftAlgorithm.launch();

        Client client = new Client(process,PROTOCOL_CLIENT, raftAlgorithm);
        client.setRaft(raftAlgorithm);
        client.setId(PROTOCOL_APP_CLIENT);
        client.launch();

    }
}
