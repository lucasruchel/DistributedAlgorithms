package configurations;

public interface Parameters {
//    Limite de tempo para resposta do líder, atingí-lo inicia uma nova eleição
//    É escolhido um tempo aleatório entre min e max timeout
   int minElectionTimeout = 150;
   int maxElectionTimeout = 300;

   boolean DEBUG = true;


}
