#!/bin/bash

export JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64/
export CLASSPATH_ATOMIC=out/production/VCube/:neko.jar
export CLASSPATH_RAFT=out/production/Raft/:neko.jar

java_ops="-Xmx8g"
main="lse.neko.Main"
# config_atomic="simulation-vcube.config"
# config_raft="simulation-raft.config"

template_file="simulation.config.template"
simulation_file="simulation.config"

ATOMIC_CLASS="br.unioeste.ppgcomp.initializers.AtomicInitializer"
RAFT_CLASS="simulation.RaftInitializer"

# Informações de falha por parada em processos
fault_info="process.<id>.crash.start = <time>"

# Números aleatórios no shell: https://blog.eduonix.com/shell-scripting/generating-random-numbers-in-linux-shell-scripting/
SEED=1
export RANDOM=1

getRandom(){
    # $1=random e $2=número de processos
    echo $(($1 % $2))
}



randomInRange(){
    # $1=randomValue $2=Tempo de duração do brcast sem falhas  $3=tempo de inicio do broadcast
    echo $(($1 % $2 + $3))
}



# Execução sem falhas dos algoritmos implementados
# for np in 8 16 32; do
#     for msgs in 5 10 20 50 100; do
#         nm=$(($msgs*1000))
        
#         sed -e "s;<np>;$np;g" -e "s;<nmsgs>;$nm;g" -e "s;<algo>;atomic;g" -e "s;<initclass>;$ATOMIC_CLASS;g" $template_file > $simulation_file
#         java $java_ops -cp $CLASSPATH_ATOMIC $main $simulation_file
       
        
#         # sed -e "s;<np>;$np;g" -e "s;<nmsgs>;$nm;g" -e "s;<algo>;raft;g" -e "s;<initclass>;$RAFT_CLASS;g" $template_file > $simulation_file
#         # java $java_ops -cp $CLASSPATH_RAFT $main $simulation_file
      
#         # break
        
#     done

#     # break
# done

# Execução com falhas
for np in 8; do
     
    for execs in $(seq 1 100); do
        # Número de mensagens
        nm=1

        # Número de falhas
        f=3
        
        # Configuração geral do arquivo de simulação, sem a parte de falhas
        sed -e "s;<np>;$np;g" -e "s;<nmsgs>;$nm;g" -e "s;<algo>;atomic-exec-$execs-f$f;g" -e "s;<initclass>;$ATOMIC_CLASS;g" $template_file > $simulation_file
        
        randomP=''
        for i in $(seq 1 $f); do
            # Laço para garantir que dois processos identicos não sejam sorteados
            while : 
            do
                a=$RANDOM
                rp="$(getRandom $a $np) "
                
                # Verifica se o processo já foi sorteado
                # echo $rp 
                echo $randomP | grep -q $rp
                if [ $? -eq 1 ]; then
                    # Adiciona o número processo sorteado
                    randomP=$randomP"$rp "
                    break
                fi
            done
        done

        # Para cada processo sorteia um tempo para falhar
        for p in $randomP; do
            a=$RANDOM
            t=$(randomInRange $a 92 200)

            fault=$(echo $fault_info | sed -e "s;<id>;$p;g" -e "s;<time>;$t;g")
            sed -i -e "$ a $fault" $simulation_file
        done

        # execução do script 
        java $java_ops -cp $CLASSPATH_ATOMIC $main $simulation_file

        # Se houver falha do comando interrompe execução
        if [ ! $? -eq 0 ]; then
            break
        fi
    done
done




# java $java_ops -cp $CLASSPATH_ATOMIC $main $config_atomic
# java $java_ops -cp $CLASSPATH_RAFT $main $config_raft

