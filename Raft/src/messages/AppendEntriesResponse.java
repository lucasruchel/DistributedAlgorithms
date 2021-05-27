package messages;

public class AppendEntriesResponse {
    private boolean success;
    private int term;
    private int matchIndex;

    public AppendEntriesResponse(boolean success, int term, int matchIndex) {
        this.success = success;
        this.term = term;
        this.matchIndex = matchIndex;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public int getMatchIndex() {
        return matchIndex;
    }

    public void setMatchIndex(int matchIndex) {
        this.matchIndex = matchIndex;
    }

    @Override
    public String toString() {
        return "append_res{" +
                "success=" + success +
                ", t=" + term +
                ", index=" + matchIndex +
                '}';
    }
}
