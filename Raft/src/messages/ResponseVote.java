package messages;

public class ResponseVote {
    private long term;
    private boolean granted;

    public ResponseVote(long term, boolean granted) {
        this.term = term;
        this.granted = granted;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    @Override
    public String toString() {
        return "VoteResponse{" +
                "term=" + term +
                ", granted=" + granted +
                '}';
    }
}
