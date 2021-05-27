package simulation.parameters;

public interface Configuration {
    double ts = 0.1; //Time to send
    double tr = 0.1; //Time to receive
    double tt = 0.8; //Time to transmit a message
}
