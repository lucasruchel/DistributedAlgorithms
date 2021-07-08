#!/bin/bash

atomic(){
    # Inicio do experimento em cada processo, considerar somente o primeiro
    START_STRING="starting-experiment"

    # Necessários para execucao do algoritmo
    ALG_MSGS="TREE \|ACK "

    # Ultima mensagem entregue
    END_STRING="Finished-experiment"


    COUNTER_ALG=$(grep -c "$ALG_MSGS" $1)

    COUNTER_TIME_START=$(grep -m 1 "$START_STRING" $1 | cut -f1 -d ' ')

    COUNTER_TIME_END=$(grep "$END_STRING" $1 | tail -n 1 | cut -f1 -d' ')

    echo "Mensagens: $COUNTER_ALG"
    echo "Tempo de inicio: $COUNTER_TIME_START"
    echo "Tempo de término: $COUNTER_TIME_END"
}

raft(){
    # Inicio do experimento em cada processo, considerar somente o primeiro
    START_STRING="starting-experiment"

    # Necessários para execucao do algoritmo
    ALG_MSGS="Append_Req \|Append_Res "

    # Ultima mensagem entregue
    END_STRING="Finished-experiment"

    # END_LINE=$(grep "$END_STRING" $1 | tail -n 1)

    nump=$2

    csplit -s $1 "/$START_STRING/" '{*}'

    rm xx00
    mv xx01 raft.log
    csplit -s raft.log "/$END_STRING/" '{*}'

    last=$(ls -tr xx* | tail -n 1)
    rm $last

    COUNTER=0
    for f in $(ls -tr xx*); do
        COUNTER_ALG=$(grep -c "$ALG_MSGS" $f)
        ((COUNTER=$COUNTER+$COUNTER_ALG))
    done

    
    COUNTER_TIME_START=$(head -n 1 xx00 | cut -f1 -d ' ')
    COUNTER_TIME_END=$(tail -n 1 $f | cut -f1 -d' ')

    rm raft.log xx*
    
    echo "Mensagens: $COUNTER"
    echo "Tempo de inicio: $COUNTER_TIME_START"
    echo "Tempo de término: $COUNTER_TIME_END"

}


for np in 8 16 32; do
    for msgs in 5 10 20 50 100; do
       
        echo "######################"
        echo "raft-$np-$(($msgs*1000)).log"
        echo "######################"

        raft raft-$np-$(($msgs*1000)).log $np

        echo ""

        # break
    done
    # break
done


   



