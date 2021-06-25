package simulation;

import data.LogEntry;
import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.util.TimerTask;
import simulation.crash.CrashReceiver;

import java.util.List;

public class Client extends CrashReceiver implements Raft.LogChangeListener {
    private Raft raft;

    public Client(NekoProcess process, String name, Raft raft) {
        super(process, name);

        this.raft = raft;

        raft.addLogChangeListener(this);
    }

    public Raft getRaft() {
        return raft;
    }

    public void setRaft(Raft raft) {
        this.raft = raft;
    }

    @Override
    public void run() {
            NekoSystem.instance().getTimer().schedule(new TimerTask() {
                @Override
                public void run() {
//                    Client.this.raft.doRequest(String.format());
                }
            }, 200);


    }

    @Override
    public boolean appliedValues(LogEntry data) {
            System.out.printf("p%s:  ### Aplicado v='%s' em cliente at %s \n",
                    process.getID(),
                    data.getData(),
                    process.clock());

        return true;
    }
}
