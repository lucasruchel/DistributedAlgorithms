package data;

public class LogEntry {
    private Object data;
    private int term;

    public LogEntry(Object data, int term) {
        this.data = data;
        this.term = term;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
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
