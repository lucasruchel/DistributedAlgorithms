package data;

public class LogEntry {
    private Object data;
    private long term;

    public LogEntry(Object data, long term) {
        this.data = data;
        this.term = term;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    @Override
    public String toString() {
        return "d{" +
                term + "->" +
                "data=" + data
                ;
    }
}
