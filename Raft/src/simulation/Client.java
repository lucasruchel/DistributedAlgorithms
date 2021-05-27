package simulation;

import lse.neko.NekoProcess;
import lse.neko.NekoSystem;
import lse.neko.util.TimerTask;
import simulation.crash.CrashReceiver;

public class Client extends CrashReceiver {
    private Raft raft;

    public Client(NekoProcess process, String name) {
        super(process, name);
    }

    public Raft getRaft() {
        return raft;
    }

    public void setRaft(Raft raft) {
        this.raft = raft;
    }

    @Override
    public void run() {
        if (process.getID() == 0){
            NekoSystem.instance().getTimer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Client.this.raft.doRequest("Olá Mundo");
                }
            }, 10);
        }
    }
}
