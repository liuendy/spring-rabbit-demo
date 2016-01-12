#Para rodar a Demo 

## Precisa criar um o docker container do Rabbit (precisa fazer uma unica vez)"
###    sudo docker run -d --hostname localhost --name rabbit -p 15672:15672 -p 5672:5672  rabbitmq:3-management "
## Rabbit Stop"
###    sudo docker stop rabbit"
## Rabbit Start"
###    sudo docker start rabbit"
## Entrar no Container Rabbit"
###    docker exec -ti rabbit bash"
## Resetar as filas de dentro do container"
###    rabbitmqctl stop_app && rabbitmqctl reset && rabbitmqctl start_app"
